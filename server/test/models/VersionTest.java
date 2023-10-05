package models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.program.ProgramType;
import services.question.exceptions.QuestionNotFoundException;

// TODO(#4912): Add more tests for Version.java.
public class VersionTest extends ResetPostgres {

  private VersionRepository versionRepository;

  @Before
  public void setupProgramRepository() {
    versionRepository = instanceOf(VersionRepository.class);
  }

  @Test
  public void addAndRemoveProgram() {
    Version version = versionRepository.getDraftVersionOrCreate();
    Program program =
        new Program(
            "adminName",
            "adminDescription",
            "displayName",
            "displayDescription",
            "confirmationMessage",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            /* blockDefinitions= */ ImmutableList.of(),
            version,
            ProgramType.DEFAULT,
            new ProgramAcls());

    version.addProgram(program);
    assertThat(version.getPrograms()).hasSize(1);
    assertThat(version.getPrograms().get(0)).isEqualTo(program);

    version.removeProgram(program);
    assertThat(version.getPrograms()).isEmpty();
  }

  @Test
  public void addAndRemoveQuestion() {
    Version version = versionRepository.getDraftVersionOrCreate();
    Question question = resourceCreator.insertQuestion("name");

    version.addQuestion(question);
    assertThat(version.getQuestions()).hasSize(1);
    assertThat(version.getQuestions().get(0)).isEqualTo(question);

    version.removeQuestion(question);
    assertThat(version.getQuestions()).isEmpty();
  }

  @Test
  public void getProgramByName_found() {
    Version version = versionRepository.getDraftVersionOrCreate();
    String programName = "program";
    resourceCreator.insertDraftProgram(programName);
    version.refresh();

    Optional<Program> result = versionRepository.getProgramByNameForVersion(programName, version);
    assertThat(result.isPresent()).isTrue();
    assertThat(result.get().getProgramDefinition().adminName()).isEqualTo(programName);
  }

  @Test
  public void getProgramByName_notFound() {
    Version version = versionRepository.getDraftVersionOrCreate();
    String programName = "program";
    resourceCreator.insertDraftProgram(programName);
    version.refresh();

    Optional<Program> result =
        versionRepository.getProgramByNameForVersion(programName + "other", version);
    assertThat(result.isPresent()).isFalse();
  }

  @Test
  public void getProgramNames() {
    Version version = versionRepository.getDraftVersionOrCreate();
    String programNameOne = "programone";
    Program programOne = resourceCreator.insertDraftProgram(programNameOne);
    String programNameTwo = "programtwo";
    Program programTwo = resourceCreator.insertDraftProgram(programNameTwo);
    String programNameThree = "programthree";
    Program programThree = resourceCreator.insertDraftProgram(programNameThree);

    version.addProgram(programOne);
    version.addProgram(programTwo);
    version.addProgram(programThree);

    assertThat(versionRepository.getProgramNamesForVersion(version)).hasSize(3);
    assertThat(versionRepository.getProgramNamesForVersion(version))
        .containsExactlyInAnyOrder(programNameOne, programNameTwo, programNameThree);
  }

  @Test
  public void getQuestionNames() throws Exception {
    Version version = versionRepository.getDraftVersionOrCreate();
    String questionOneName = "one";
    Question questionOne = resourceCreator.insertQuestion(questionOneName);
    String questionTwoName = "two";
    Question questionTwo = resourceCreator.insertQuestion(questionTwoName);
    String tombstonedQuestionOneName = "tombstoneOne";
    Question tombstonedQuestionOne = resourceCreator.insertQuestion(tombstonedQuestionOneName);

    version.addQuestion(questionOne);
    version.addQuestion(questionTwo);
    version.addQuestion(tombstonedQuestionOne);
    versionRepository.addTombstoneForQuestionInVersion(tombstonedQuestionOne, version);

    assertThat(versionRepository.getQuestionNamesForVersion(version)).hasSize(3);
    assertThat(versionRepository.getQuestionNamesForVersion(version))
        .containsExactlyInAnyOrder(questionOneName, questionTwoName, tombstonedQuestionOneName);
  }

  @Test
  public void getTombstonedProgramNames() {
    Version version = versionRepository.getDraftVersionOrCreate();
    String programNameOne = "programone";
    Program programOne = resourceCreator.insertDraftProgram(programNameOne);
    String tombstonedProgramNameOne = "tombstoneOne";
    Program tombstonedProgramOne = resourceCreator.insertDraftProgram(tombstonedProgramNameOne);
    String tombstonedProgramNameTwo = "tombstoneTwo";
    Program tombstonedProgramTwo = resourceCreator.insertDraftProgram(tombstonedProgramNameTwo);

    version.addProgram(programOne);
    version.addProgram(tombstonedProgramOne);
    version.addProgram(tombstonedProgramTwo);

    version.addTombstoneForProgramForTest(tombstonedProgramOne);
    version.addTombstoneForProgramForTest(tombstonedProgramTwo);

    assertThat(version.getTombstonedProgramNames()).hasSize(2);
    assertThat(version.getTombstonedProgramNames())
        .containsExactlyInAnyOrder(tombstonedProgramNameOne, tombstonedProgramNameTwo);
  }

  @Test
  public void getTombstonedQuestionNames() throws Exception {
    Version version = versionRepository.getDraftVersionOrCreate();
    String questionOneName = "one";
    Question questionOne = resourceCreator.insertQuestion(questionOneName);
    String tombstonedQuestionOneName = "tombstoneOne";
    Question tombstonedQuestionOne = resourceCreator.insertQuestion(tombstonedQuestionOneName);
    String tombstonedQuestionTwoName = "tombstoneTwo";
    Question tombstonedQuestionTwo = resourceCreator.insertQuestion(tombstonedQuestionTwoName);

    version.addQuestion(questionOne);
    version.addQuestion(tombstonedQuestionOne);
    version.addQuestion(tombstonedQuestionTwo);

    versionRepository.addTombstoneForQuestionInVersion(tombstonedQuestionOne, version);
    versionRepository.addTombstoneForQuestionInVersion(tombstonedQuestionTwo, version);

    assertThat(version.getTombstonedQuestionNames()).hasSize(2);
    assertThat(version.getTombstonedQuestionNames())
        .containsExactlyInAnyOrder(tombstonedQuestionOneName, tombstonedQuestionTwoName);
  }

  @Test
  public void questionIsTombstoned() throws Exception {
    Version version = versionRepository.getDraftVersionOrCreate();
    String questionOneName = "one";
    Question questionOne = resourceCreator.insertQuestion(questionOneName);
    String tombstonedQuestionOneName = "tombstoneOne";
    Question tombstonedQuestionOne = resourceCreator.insertQuestion(tombstonedQuestionOneName);

    version.addQuestion(questionOne);
    version.addQuestion(tombstonedQuestionOne);

    versionRepository.addTombstoneForQuestionInVersion(tombstonedQuestionOne, version);

    assertThat(version.questionIsTombstoned(tombstonedQuestionOneName)).isTrue();
    assertThat(version.questionIsTombstoned(questionOneName)).isFalse();

    version.removeTombstoneForQuestion(tombstonedQuestionOne);
    assertThat(version.questionIsTombstoned(tombstonedQuestionOneName)).isFalse();
  }

  @Test
  public void programIsTombstoned() {
    Version version = versionRepository.getDraftVersionOrCreate();
    String programNameOne = "programone";
    Program programOne = resourceCreator.insertDraftProgram(programNameOne);
    String tombstonedProgramNameOne = "tombstoneOne";
    Program tombstonedProgramOne = resourceCreator.insertDraftProgram(tombstonedProgramNameOne);

    version.addProgram(programOne);
    version.addProgram(tombstonedProgramOne);

    version.addTombstoneForProgramForTest(tombstonedProgramOne);

    assertThat(version.programIsTombstoned(tombstonedProgramNameOne)).isTrue();
    assertThat(version.programIsTombstoned(programNameOne)).isFalse();

    version.removeTombstoneForProgram(tombstonedProgramOne);
    assertThat(version.programIsTombstoned(tombstonedProgramNameOne)).isFalse();
  }

  @Test
  public void addTombstoneForQuestion() throws Exception {
    Version version = versionRepository.getDraftVersionOrCreate();
    String questionOneName = "one";
    Question questionOne = resourceCreator.insertQuestion(questionOneName);
    version.addQuestion(questionOne);

    boolean succeeded = versionRepository.addTombstoneForQuestionInVersion(questionOne, version);

    assertThat(succeeded).isTrue();
    assertThat(version.questionIsTombstoned(questionOneName)).isTrue();
    assertThat(version.getTombstonedQuestionNames()).hasSize(1);
    assertThat(version.getTombstonedQuestionNames().get(0)).isEqualTo(questionOneName);
  }

  @Test
  public void addTombstoneForQuestion_alreadyTombstoned() throws Exception {
    Version version = versionRepository.getDraftVersionOrCreate();
    String questionOneName = "one";
    Question questionOne = resourceCreator.insertQuestion(questionOneName);
    version.addQuestion(questionOne);

    boolean succeeded = versionRepository.addTombstoneForQuestionInVersion(questionOne, version);

    assertThat(succeeded).isTrue();
    assertThat(version.questionIsTombstoned(questionOneName)).isTrue();

    succeeded = versionRepository.addTombstoneForQuestionInVersion(questionOne, version);
    assertThat(succeeded).isFalse();
    assertThat(version.questionIsTombstoned(questionOneName)).isTrue();
  }

  @Test
  public void addTombstoneForQuestion_questionNotInVersion_throwsException() throws Exception {
    Version activeVersion = versionRepository.getActiveVersion();
    Version draftVersion = versionRepository.getDraftVersionOrCreate();

    Question question = resourceCreator.insertQuestion("question");
    question.addVersion(activeVersion).save();
    activeVersion.refresh();

    assertThatThrownBy(
            () -> versionRepository.addTombstoneForQuestionInVersion(question, draftVersion))
        .isInstanceOf(QuestionNotFoundException.class);
  }

  @Test
  public void removeTombstoneForQuestion() throws Exception {
    Version version = versionRepository.getDraftVersionOrCreate();
    String questionOneName = "one";
    Question questionOne = resourceCreator.insertQuestion(questionOneName);
    version.addQuestion(questionOne);

    versionRepository.addTombstoneForQuestionInVersion(questionOne, version);
    assertThat(version.questionIsTombstoned(questionOneName)).isTrue();

    boolean succeeded = version.removeTombstoneForQuestion(questionOne);

    assertThat(succeeded).isTrue();
    assertThat(version.questionIsTombstoned(questionOneName)).isFalse();
    assertThat(version.getTombstonedQuestionNames()).isEmpty();
  }

  @Test
  public void removeTombstoneForQuestion_forNonTombstonedQuestion() throws Exception {
    Version version = versionRepository.getDraftVersionOrCreate();
    String questionOneName = "one";
    Question questionOne = resourceCreator.insertQuestion(questionOneName);
    version.addQuestion(questionOne);

    boolean succeeded = version.removeTombstoneForQuestion(questionOne);

    assertThat(succeeded).isFalse();
  }

  @Test
  public void removeTombstoneForProgram() {
    Version version = versionRepository.getDraftVersionOrCreate();
    String programNameOne = "programone";
    Program programOne = resourceCreator.insertDraftProgram(programNameOne);

    version.addProgram(programOne);
    version.addTombstoneForProgramForTest(programOne);
    assertThat(version.programIsTombstoned(programNameOne)).isTrue();

    boolean suceeded = version.removeTombstoneForProgram(programOne);

    assertThat(suceeded).isTrue();
    assertThat(version.programIsTombstoned(programNameOne)).isFalse();
    assertThat(version.getTombstonedProgramNames()).isEmpty();
  }

  @Test
  public void removeTombstoneForProgram_forNonTombstonedProgram() {
    Version version = versionRepository.getDraftVersionOrCreate();
    String programNameOne = "programone";
    Program programOne = resourceCreator.insertDraftProgram(programNameOne);
    version.addProgram(programOne);

    boolean suceeded = version.removeTombstoneForProgram(programOne);

    assertThat(suceeded).isFalse();
    assertThat(version.getTombstonedProgramNames()).isEmpty();
  }

  @Test
  public void hasAnyChanges_hasTombstonedQuestions() throws Exception {
    Version version = versionRepository.getDraftVersionOrCreate();
    String questionOneName = "one";
    Question questionOne = resourceCreator.insertQuestion(questionOneName);
    version.addQuestion(questionOne);
    versionRepository.addTombstoneForQuestionInVersion(questionOne, version);

    assertThat(version.hasAnyChanges()).isTrue();
  }

  @Test
  public void hasAnyChanges_hasTombstonedPrograms() {
    Version version = versionRepository.getDraftVersionOrCreate();
    String programNameOne = "programone";
    Program programOne = resourceCreator.insertDraftProgram(programNameOne);
    version.addTombstoneForProgramForTest(programOne);

    assertThat(version.hasAnyChanges()).isTrue();
  }

  @Test
  public void hasAnyChanges_hasQuestions() {
    Version version = versionRepository.getDraftVersionOrCreate();
    String questionOneName = "one";
    Question questionOne = resourceCreator.insertQuestion(questionOneName);
    version.addQuestion(questionOne);

    assertThat(version.hasAnyChanges()).isTrue();
  }

  @Test
  public void hasAnyChanges_hasPrograms() {
    Version version = versionRepository.getDraftVersionOrCreate();
    String programNameOne = "programone";
    Program programOne = resourceCreator.insertDraftProgram(programNameOne);
    version.addProgram(programOne);

    assertThat(version.hasAnyChanges()).isTrue();
  }

  @Test
  public void hasAnyChanges_noChanges() {
    Version version = versionRepository.getDraftVersionOrCreate();
    String questionOneName = "one";
    Question questionOne = resourceCreator.insertQuestion(questionOneName);
    version.addQuestion(questionOne);

    // Revert changes.
    version.removeQuestion(questionOne);

    assertThat(version.hasAnyChanges()).isFalse();
  }
}
