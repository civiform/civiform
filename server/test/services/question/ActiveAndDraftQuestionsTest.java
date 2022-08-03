package services.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import models.Program;
import models.Question;
import models.Version;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.DeletionStatus;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;
import support.ProgramBuilder;

public class ActiveAndDraftQuestionsTest extends ResetPostgres {

  private static final String TEST_QUESTION_NAME = "test-question";

  private VersionRepository versionRepository;

  @Before
  public void setUp() {
    versionRepository = instanceOf(VersionRepository.class);
  }

  @Test
  public void getQuestionNames() {
    Question tombstonedQuestionFromActiveVersion =
        resourceCreator.insertQuestion("tombstoned-question");

    versionRepository
        .getActiveVersion()
        .addQuestion(tombstonedQuestionFromActiveVersion)
        .addQuestion(resourceCreator.insertQuestion("active-and-draft-question"))
        .addQuestion(resourceCreator.insertQuestion("active-only-question"))
        .save();
    addTombstoneToVersion(
        versionRepository.getActiveVersion(), tombstonedQuestionFromActiveVersion);

    versionRepository
        .getDraftVersion()
        .addQuestion(resourceCreator.insertQuestion("active-and-draft-question"))
        .addQuestion(resourceCreator.insertQuestion("draft-only-question"))
        .save();

    assertThat(newActiveAndDraftQuestions().getQuestionNames())
        .containsExactly(
            "tombstoned-question",
            "active-and-draft-question",
            "active-only-question",
            "draft-only-question");
  }

  @Test
  public void getActiveOrDraftQuestionDefinition() {
    Question tombstonedQuestionFromActiveVersion =
        resourceCreator.insertQuestion("tombstoned-question");
    Question activeAndDraftQuestion = resourceCreator.insertQuestion("active-and-draft-question");
    Question activeOnlyQuestion = resourceCreator.insertQuestion("active-only-question");
    Question activeAndDraftQuestionUpdated =
        resourceCreator.insertQuestion("active-and-draft-question");
    Question draftOnlyQuestion = resourceCreator.insertQuestion("draft-only-question");

    versionRepository
        .getActiveVersion()
        .addQuestion(tombstonedQuestionFromActiveVersion)
        .addQuestion(activeAndDraftQuestion)
        .addQuestion(activeOnlyQuestion)
        .save();
    addTombstoneToVersion(
        versionRepository.getActiveVersion(), tombstonedQuestionFromActiveVersion);

    versionRepository
        .getDraftVersion()
        .addQuestion(activeAndDraftQuestionUpdated)
        .addQuestion(draftOnlyQuestion)
        .save();

    ActiveAndDraftQuestions questions = newActiveAndDraftQuestions();
    assertThat(
            questions
                .getActiveQuestionDefinition("tombstoned-question")
                .map(QuestionDefinition::getId))
        .isEqualTo(Optional.of(tombstonedQuestionFromActiveVersion.id));
    assertThat(
            questions
                .getDraftQuestionDefinition("tombstoned-question")
                .map(QuestionDefinition::getId))
        .isEqualTo(Optional.empty());
    assertThat(
            questions
                .getActiveQuestionDefinition("active-and-draft-question")
                .map(QuestionDefinition::getId))
        .isEqualTo(Optional.of(activeAndDraftQuestion.id));
    assertThat(
            questions
                .getDraftQuestionDefinition("active-and-draft-question")
                .map(QuestionDefinition::getId))
        .isEqualTo(Optional.of(activeAndDraftQuestionUpdated.id));
    assertThat(
            questions
                .getActiveQuestionDefinition("active-only-question")
                .map(QuestionDefinition::getId))
        .isEqualTo(Optional.of(activeOnlyQuestion.id));
    assertThat(
            questions
                .getDraftQuestionDefinition("active-only-question")
                .map(QuestionDefinition::getId))
        .isEqualTo(Optional.empty());
    assertThat(
            questions
                .getActiveQuestionDefinition("draft-only-question")
                .map(QuestionDefinition::getId))
        .isEqualTo(Optional.empty());
    assertThat(
            questions
                .getDraftQuestionDefinition("draft-only-question")
                .map(QuestionDefinition::getId))
        .isEqualTo(Optional.of(draftOnlyQuestion.id));
    assertThat(
            questions
                .getActiveQuestionDefinition("non-existent-question")
                .map(QuestionDefinition::getId))
        .isEqualTo(Optional.empty());
    assertThat(
            questions
                .getDraftQuestionDefinition("non-existent-question")
                .map(QuestionDefinition::getId))
        .isEqualTo(Optional.empty());
  }

  @Test
  public void getDeletionStatus_notPartOfEitherVersion() {
    resourceCreator.insertQuestion(TEST_QUESTION_NAME);

    assertThat(newActiveAndDraftQuestions().getDeletionStatus(TEST_QUESTION_NAME))
        .isEqualTo(DeletionStatus.NOT_ACTIVE);
  }

  @Test
  public void getDeletionStatus_notReferencedByProgramButStillReferencedByVersion() {
    Question activeVersionQuestion = resourceCreator.insertQuestion(TEST_QUESTION_NAME);
    versionRepository.getActiveVersion().addQuestion(activeVersionQuestion).save();

    Question draftVersionQuestion = resourceCreator.insertQuestion("draft-version-question");
    versionRepository.getDraftVersion().addQuestion(draftVersionQuestion).save();

    assertThat(newActiveAndDraftQuestions().getDeletionStatus(TEST_QUESTION_NAME))
        .isEqualTo(DeletionStatus.DELETABLE);
    // TODO(#2788): Allow archiving newly created questions in the draft version
    // that are not referenced by any programs.
    assertThat(newActiveAndDraftQuestions().getDeletionStatus("draft-version-question"))
        .isEqualTo(DeletionStatus.NOT_ACTIVE);
  }

  @Test
  public void getDeletionStatus_tombstoned() {
    Question question = resourceCreator.insertQuestion(TEST_QUESTION_NAME);
    versionRepository.getActiveVersion().addQuestion(question).save();
    addTombstoneToVersion(versionRepository.getDraftVersion(), question);

    assertThat(newActiveAndDraftQuestions().getDeletionStatus(TEST_QUESTION_NAME))
        .isEqualTo(DeletionStatus.PENDING_DELETION);

    // Create an edited version of the question in the draft version
    // and ensure that it's still considered as pending deletion.
    versionRepository
        .getDraftVersion()
        .addQuestion(resourceCreator.insertQuestion(TEST_QUESTION_NAME))
        .save();
    assertThat(newActiveAndDraftQuestions().getDeletionStatus(TEST_QUESTION_NAME))
        .isEqualTo(DeletionStatus.PENDING_DELETION);
  }

  @Test
  public void getDeletionStatus_stillReferencedInActiveVersion() {
    Question questionActive = resourceCreator.insertQuestion(TEST_QUESTION_NAME);
    versionRepository.getActiveVersion().addQuestion(questionActive).save();
    // newActiveProgram automatically adds the program to the active version.
    ProgramBuilder.newActiveProgram("foo")
        .withBlock("Screen 1")
        .withRequiredQuestion(questionActive)
        .build();

    assertThat(newActiveAndDraftQuestions().getDeletionStatus(TEST_QUESTION_NAME))
        .isEqualTo(DeletionStatus.NOT_DELETABLE);

    // An invalid state where the question has been tombstoned even though it's still referenced.
    // TODO(#2788): Prevent allowing this state to occur and adjust the expectation accordingly.
    addTombstoneToVersion(versionRepository.getDraftVersion(), questionActive);

    assertThat(newActiveAndDraftQuestions().getDeletionStatus(TEST_QUESTION_NAME))
        .isEqualTo(DeletionStatus.PENDING_DELETION);
  }

  @Test
  public void getDeletionStatus_newReferenceInDraftVersion() {
    // Simulates the state where the question was created in the active version
    // and wasn't referenced. Then it was referenced by a program in the draft
    // version. In this case, the draft won't yet contain a reference to the question.
    Question questionActive = resourceCreator.insertQuestion(TEST_QUESTION_NAME);
    versionRepository.getActiveVersion().addQuestion(questionActive).save();
    // newDraftProgram automatically adds the program to the draft version.
    ProgramBuilder.newDraftProgram("foo")
        .withBlock("Screen 1")
        .withRequiredQuestion(questionActive)
        .build();

    assertThat(newActiveAndDraftQuestions().getDeletionStatus(TEST_QUESTION_NAME))
        .isEqualTo(DeletionStatus.NOT_DELETABLE);

    // An invalid state where the question has been tombstoned even though it's still referenced.
    // TODO(#2788): Prevent allowing this state to occur and adjust the expectation accordingly.
    addTombstoneToVersion(versionRepository.getDraftVersion(), questionActive);

    assertThat(newActiveAndDraftQuestions().getDeletionStatus(TEST_QUESTION_NAME))
        .isEqualTo(DeletionStatus.PENDING_DELETION);
  }

  @Test
  public void getDeletionStatus_noLongerReferencedInDraftVersion() {
    // Simulate the only reference to the question having been removed in an edit of the program.
    Question questionActive = resourceCreator.insertQuestion(TEST_QUESTION_NAME);
    versionRepository.getActiveVersion().addQuestion(questionActive).save();
    // newActiveProgram automatically adds the program to the active version.
    ProgramBuilder.newActiveProgram("foo")
        .withBlock("Screen 1")
        .withRequiredQuestion(questionActive)
        .build();
    // Create a draft version of the program that no longer references the question.
    // newDraftProgram automatically adds the program to the draft version.
    ProgramBuilder.newDraftProgram("foo").withBlock("Screen 1").build();

    // TODO(#2788): Allow archiving questions that aren't referenced in the draft
    // version of the program.
    assertThat(newActiveAndDraftQuestions().getDeletionStatus(TEST_QUESTION_NAME))
        .isEqualTo(DeletionStatus.NOT_DELETABLE);

    // Adding a draft edit of the question continues to be considered deletable.
    Question questionDraft = resourceCreator.insertQuestion(TEST_QUESTION_NAME);
    versionRepository.getDraftVersion().addQuestion(questionDraft).save();

    // TODO(#2788): Allow archiving questions that aren't referenced in the draft
    // version of the program.
    assertThat(newActiveAndDraftQuestions().getDeletionStatus(TEST_QUESTION_NAME))
        .isEqualTo(DeletionStatus.NOT_DELETABLE);
  }

  @Test
  public void getDeletionStatus_notReferencedByProgramInEitherVersion() {
    // Create an active and draft version of a question that isn't referenced
    // by any programs.
    versionRepository
        .getActiveVersion()
        .addQuestion(resourceCreator.insertQuestion(TEST_QUESTION_NAME))
        .save();
    versionRepository
        .getDraftVersion()
        .addQuestion(resourceCreator.insertQuestion(TEST_QUESTION_NAME))
        .save();

    assertThat(newActiveAndDraftQuestions().getDeletionStatus(TEST_QUESTION_NAME))
        .isEqualTo(DeletionStatus.DELETABLE);
  }

  @Test
  public void getReferencingPrograms_unreferencedQuestion() {
    versionRepository
        .getActiveVersion()
        .addQuestion(resourceCreator.insertQuestion(TEST_QUESTION_NAME))
        .save();

    ActiveAndDraftQuestions.ReferencingPrograms result =
        newActiveAndDraftQuestions().getReferencingPrograms(TEST_QUESTION_NAME);
    assertThat(result)
        .isEqualTo(
            ActiveAndDraftQuestions.ReferencingPrograms.builder()
                .setActiveReferences(ImmutableSet.of())
                .build());

    // Make an edit of the question in the draft version and leave it unreferenced.
    versionRepository
        .getDraftVersion()
        .addQuestion(resourceCreator.insertQuestion(TEST_QUESTION_NAME))
        .save();
    result = newActiveAndDraftQuestions().getReferencingPrograms(TEST_QUESTION_NAME);
    assertThat(result)
        .isEqualTo(
            ActiveAndDraftQuestions.ReferencingPrograms.builder()
                .setActiveReferences(ImmutableSet.of())
                // Not Optional.empty because the draft version actually has edits applied.
                .setDraftReferences(Optional.of(ImmutableSet.of()))
                .build());
  }

  @Test
  public void getReferencingPrograms_multipleProgramReferencesForSameQuestionVersion() {
    Question question = resourceCreator.insertQuestion(TEST_QUESTION_NAME);

    // Set up state where the question is referenced from:
    // ACTIVE version - first-program and second-program
    // DRAFT version - second-program and third-program

    // newActiveProgram / newDraftProgram automatically adds the program to the specified version.
    Program firstProgramActive =
        ProgramBuilder.newActiveProgram("first-program")
            .withBlock("Screen 1")
            .withRequiredQuestion(question)
            .build();
    Program secondProgramActive =
        ProgramBuilder.newActiveProgram("second-program")
            .withBlock("Screen 1")
            .withBlock("Screen 2")
            .withRequiredQuestion(question)
            .build();
    versionRepository.getActiveVersion().addQuestion(question).save();

    // No longer reference the question from the first program.
    ProgramBuilder.newDraftProgram("first-program").withBlock("Screen 1").build();
    Program secondProgramDraft =
        ProgramBuilder.newDraftProgram("second-program")
            .withBlock("Screen 1")
            .withBlock("Screen 2")
            .withBlock("Screen 3")
            .withRequiredQuestion(question)
            .build();
    Program thirdProgramDraft =
        ProgramBuilder.newDraftProgram("third-program")
            .withBlock("Screen 1")
            .withRequiredQuestion(question)
            .build();
    versionRepository.getDraftVersion().addQuestion(question).save();

    ActiveAndDraftQuestions.ReferencingPrograms result =
        newActiveAndDraftQuestions().getReferencingPrograms(TEST_QUESTION_NAME);
    assertThat(
            result.activeReferences().stream()
                .map(ProgramDefinition::id)
                .collect(ImmutableSet.toImmutableSet()))
        .isEqualTo(ImmutableSet.of(firstProgramActive.id, secondProgramActive.id));
    assertThat(result.draftReferences()).isPresent();
    assertThat(
            result.draftReferences().get().stream()
                .map(ProgramDefinition::id)
                .collect(ImmutableSet.toImmutableSet()))
        .isEqualTo(ImmutableSet.of(secondProgramDraft.id, thirdProgramDraft.id));
  }

  @Test
  public void getReferencingPrograms_multipleProgramReferencesForDifferentQuestionVersions() {
    // Set up state where the question is referenced from:
    // ACTIVE version - first-program and second-program
    // DRAFT version - second-program and third-program
    // In addition, the DRAFT version references are to an edited question.

    Question activeQuestion = resourceCreator.insertQuestion(TEST_QUESTION_NAME);
    versionRepository.getActiveVersion().addQuestion(activeQuestion).save();
    // newActiveProgram / newDraftProgram automatically adds the program to the specified version.
    Program firstProgramActive =
        ProgramBuilder.newActiveProgram("first-program")
            .withBlock("Screen 1")
            .withRequiredQuestion(activeQuestion)
            .build();
    Program secondProgramActive =
        ProgramBuilder.newActiveProgram("second-program")
            .withBlock("Screen 1")
            .withBlock("Screen 2")
            .withRequiredQuestion(activeQuestion)
            .build();

    // No longer reference the question from the first program.
    Question draftQuestion = resourceCreator.insertQuestion(TEST_QUESTION_NAME);
    versionRepository.getDraftVersion().addQuestion(draftQuestion).save();
    ProgramBuilder.newDraftProgram("first-program").withBlock("Screen 1").build();
    Program secondProgramDraft =
        ProgramBuilder.newDraftProgram("second-program")
            .withBlock("Screen 1")
            .withBlock("Screen 2")
            .withBlock("Screen 3")
            .withRequiredQuestion(draftQuestion)
            .build();
    Program thirdProgramDraft =
        ProgramBuilder.newDraftProgram("third-program")
            .withBlock("Screen 1")
            .withRequiredQuestion(draftQuestion)
            .build();

    ActiveAndDraftQuestions.ReferencingPrograms result =
        newActiveAndDraftQuestions().getReferencingPrograms(TEST_QUESTION_NAME);
    assertThat(
            result.activeReferences().stream()
                .map(ProgramDefinition::id)
                .collect(ImmutableSet.toImmutableSet()))
        .isEqualTo(ImmutableSet.of(firstProgramActive.id, secondProgramActive.id));
    assertThat(result.draftReferences()).isPresent();
    assertThat(
            result.draftReferences().get().stream()
                .map(ProgramDefinition::id)
                .collect(ImmutableSet.toImmutableSet()))
        .isEqualTo(ImmutableSet.of(secondProgramDraft.id, thirdProgramDraft.id));
  }

  @Test
  public void getReferencingPrograms_unrecognizedQuestion() {
    ActiveAndDraftQuestions.ReferencingPrograms result =
        newActiveAndDraftQuestions().getReferencingPrograms("random-question-name");
    assertThat(result)
        .isEqualTo(
            ActiveAndDraftQuestions.ReferencingPrograms.builder()
                .setActiveReferences(ImmutableSet.of())
                .build());
  }

  private ActiveAndDraftQuestions newActiveAndDraftQuestions() {
    return new ActiveAndDraftQuestions(versionRepository);
  }

  private void addTombstoneToVersion(Version version, Question question) {
    assertThat(version.addTombstoneForQuestion(question)).isTrue();
    version.save();
  }
}
