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

// TODO(#4912): Add more tests for VersionModel.java.
public class VersionModelTest extends ResetPostgres {

  private VersionRepository versionRepository;

  @Before
  public void setupProgramRepository() {
    versionRepository = instanceOf(VersionRepository.class);
  }

  @Test
  public void addAndRemoveProgram() {
    VersionModel version = versionRepository.getDraftVersionOrCreate();
    ProgramModel program =
        new ProgramModel(
            "adminName",
            "adminDescription",
            "displayName",
            "displayDescription",
            "confirmationMessage",
            "https://usa.gov",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            /* blockDefinitions= */ ImmutableList.of(),
            version,
            ProgramType.DEFAULT,
            /* eligibilityIsGating= */ true,
            new ProgramAcls(),
            /* categories= */ ImmutableList.of());
    ;

    version.addProgram(program);
    assertThat(version.getPrograms()).hasSize(1);
    assertThat(version.getPrograms().get(0)).isEqualTo(program);

    version.removeProgram(program);
    assertThat(version.getPrograms()).isEmpty();
  }

  @Test
  public void addAndRemoveQuestion() {
    VersionModel version = versionRepository.getDraftVersionOrCreate();
    QuestionModel question = resourceCreator.insertQuestion("name");

    version.addQuestion(question);
    assertThat(version.getQuestions()).hasSize(1);
    assertThat(version.getQuestions().get(0)).isEqualTo(question);

    version.removeQuestion(question);
    assertThat(version.getQuestions()).isEmpty();
  }

  @Test
  public void getProgramByName_found() {
    VersionModel version = versionRepository.getDraftVersionOrCreate();
    String programName = "program";
    resourceCreator.insertDraftProgram(programName);
    version.refresh();

    Optional<ProgramModel> result =
        versionRepository.getProgramByNameForVersion(programName, version);
    assertThat(result.isPresent()).isTrue();
    assertThat(result.get().getProgramDefinition().adminName()).isEqualTo(programName);
  }

  @Test
  public void getProgramByName_notFound() {
    VersionModel version = versionRepository.getDraftVersionOrCreate();
    String programName = "program";
    resourceCreator.insertDraftProgram(programName);
    version.refresh();

    Optional<ProgramModel> result =
        versionRepository.getProgramByNameForVersion(programName + "other", version);
    assertThat(result.isPresent()).isFalse();
  }

  @Test
  public void getProgramNames() {
    VersionModel version = versionRepository.getDraftVersionOrCreate();
    String programNameOne = "programone";
    ProgramModel programOne = resourceCreator.insertDraftProgram(programNameOne);
    String programNameTwo = "programtwo";
    ProgramModel programTwo = resourceCreator.insertDraftProgram(programNameTwo);
    String programNameThree = "programthree";
    ProgramModel programThree = resourceCreator.insertDraftProgram(programNameThree);

    version.addProgram(programOne);
    version.addProgram(programTwo);
    version.addProgram(programThree);

    assertThat(versionRepository.getProgramNamesForVersion(version)).hasSize(3);
    assertThat(versionRepository.getProgramNamesForVersion(version))
        .containsExactlyInAnyOrder(programNameOne, programNameTwo, programNameThree);
  }

  @Test
  public void getQuestionNames() throws Exception {
    VersionModel version = versionRepository.getDraftVersionOrCreate();
    String questionOneName = "one";
    QuestionModel questionOne = resourceCreator.insertQuestion(questionOneName);
    String questionTwoName = "two";
    QuestionModel questionTwo = resourceCreator.insertQuestion(questionTwoName);
    String tombstonedQuestionOneName = "tombstoneOne";
    QuestionModel tombstonedQuestionOne = resourceCreator.insertQuestion(tombstonedQuestionOneName);

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
    VersionModel version = versionRepository.getDraftVersionOrCreate();
    String programNameOne = "programone";
    ProgramModel programOne = resourceCreator.insertDraftProgram(programNameOne);
    String tombstonedProgramNameOne = "tombstoneOne";
    ProgramModel tombstonedProgramOne =
        resourceCreator.insertDraftProgram(tombstonedProgramNameOne);
    String tombstonedProgramNameTwo = "tombstoneTwo";
    ProgramModel tombstonedProgramTwo =
        resourceCreator.insertDraftProgram(tombstonedProgramNameTwo);

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
    VersionModel version = versionRepository.getDraftVersionOrCreate();
    String questionOneName = "one";
    QuestionModel questionOne = resourceCreator.insertQuestion(questionOneName);
    String tombstonedQuestionOneName = "tombstoneOne";
    QuestionModel tombstonedQuestionOne = resourceCreator.insertQuestion(tombstonedQuestionOneName);
    String tombstonedQuestionTwoName = "tombstoneTwo";
    QuestionModel tombstonedQuestionTwo = resourceCreator.insertQuestion(tombstonedQuestionTwoName);

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
    VersionModel version = versionRepository.getDraftVersionOrCreate();
    String questionOneName = "one";
    QuestionModel questionOne = resourceCreator.insertQuestion(questionOneName);
    String tombstonedQuestionOneName = "tombstoneOne";
    QuestionModel tombstonedQuestionOne = resourceCreator.insertQuestion(tombstonedQuestionOneName);

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
    VersionModel version = versionRepository.getDraftVersionOrCreate();
    String programNameOne = "programone";
    ProgramModel programOne = resourceCreator.insertDraftProgram(programNameOne);
    String tombstonedProgramNameOne = "tombstoneOne";
    ProgramModel tombstonedProgramOne =
        resourceCreator.insertDraftProgram(tombstonedProgramNameOne);

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
    VersionModel version = versionRepository.getDraftVersionOrCreate();
    String questionOneName = "one";
    QuestionModel questionOne = resourceCreator.insertQuestion(questionOneName);
    version.addQuestion(questionOne);

    boolean succeeded = versionRepository.addTombstoneForQuestionInVersion(questionOne, version);

    assertThat(succeeded).isTrue();
    assertThat(version.questionIsTombstoned(questionOneName)).isTrue();
    assertThat(version.getTombstonedQuestionNames()).hasSize(1);
    assertThat(version.getTombstonedQuestionNames().get(0)).isEqualTo(questionOneName);
  }

  @Test
  public void addTombstoneForQuestion_alreadyTombstoned() throws Exception {
    VersionModel version = versionRepository.getDraftVersionOrCreate();
    String questionOneName = "one";
    QuestionModel questionOne = resourceCreator.insertQuestion(questionOneName);
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
    VersionModel activeVersion = versionRepository.getActiveVersion();
    VersionModel draftVersion = versionRepository.getDraftVersionOrCreate();

    QuestionModel question = resourceCreator.insertQuestion("question");
    question.addVersion(activeVersion).save();
    activeVersion.refresh();

    assertThatThrownBy(
            () -> versionRepository.addTombstoneForQuestionInVersion(question, draftVersion))
        .isInstanceOf(QuestionNotFoundException.class);
  }

  @Test
  public void removeTombstoneForQuestion() throws Exception {
    VersionModel version = versionRepository.getDraftVersionOrCreate();
    String questionOneName = "one";
    QuestionModel questionOne = resourceCreator.insertQuestion(questionOneName);
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
    VersionModel version = versionRepository.getDraftVersionOrCreate();
    String questionOneName = "one";
    QuestionModel questionOne = resourceCreator.insertQuestion(questionOneName);
    version.addQuestion(questionOne);

    boolean succeeded = version.removeTombstoneForQuestion(questionOne);

    assertThat(succeeded).isFalse();
  }

  @Test
  public void removeTombstoneForProgram() {
    VersionModel version = versionRepository.getDraftVersionOrCreate();
    String programNameOne = "programone";
    ProgramModel programOne = resourceCreator.insertDraftProgram(programNameOne);

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
    VersionModel version = versionRepository.getDraftVersionOrCreate();
    String programNameOne = "programone";
    ProgramModel programOne = resourceCreator.insertDraftProgram(programNameOne);
    version.addProgram(programOne);

    boolean suceeded = version.removeTombstoneForProgram(programOne);

    assertThat(suceeded).isFalse();
    assertThat(version.getTombstonedProgramNames()).isEmpty();
  }

  @Test
  public void hasAnyChanges_hasTombstonedQuestions() throws Exception {
    VersionModel version = versionRepository.getDraftVersionOrCreate();
    String questionOneName = "one";
    QuestionModel questionOne = resourceCreator.insertQuestion(questionOneName);
    version.addQuestion(questionOne);
    versionRepository.addTombstoneForQuestionInVersion(questionOne, version);

    assertThat(version.hasAnyChanges()).isTrue();
  }

  @Test
  public void hasAnyChanges_hasTombstonedPrograms() {
    VersionModel version = versionRepository.getDraftVersionOrCreate();
    String programNameOne = "programone";
    ProgramModel programOne = resourceCreator.insertDraftProgram(programNameOne);
    version.addTombstoneForProgramForTest(programOne);

    assertThat(version.hasAnyChanges()).isTrue();
  }

  @Test
  public void hasAnyChanges_hasQuestions() {
    VersionModel version = versionRepository.getDraftVersionOrCreate();
    String questionOneName = "one";
    QuestionModel questionOne = resourceCreator.insertQuestion(questionOneName);
    version.addQuestion(questionOne);

    assertThat(version.hasAnyChanges()).isTrue();
  }

  @Test
  public void hasAnyChanges_hasPrograms() {
    VersionModel version = versionRepository.getDraftVersionOrCreate();
    String programNameOne = "programone";
    ProgramModel programOne = resourceCreator.insertDraftProgram(programNameOne);
    version.addProgram(programOne);

    assertThat(version.hasAnyChanges()).isTrue();
  }

  @Test
  public void hasAnyChanges_noChanges() {
    VersionModel version = versionRepository.getDraftVersionOrCreate();
    String questionOneName = "one";
    QuestionModel questionOne = resourceCreator.insertQuestion(questionOneName);
    version.addQuestion(questionOne);

    // Revert changes.
    version.removeQuestion(questionOne);

    assertThat(version.hasAnyChanges()).isFalse();
  }
}
