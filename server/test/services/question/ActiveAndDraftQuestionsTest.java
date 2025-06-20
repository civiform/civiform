package services.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableSet;
import io.ebean.DB;
import io.ebean.Transaction;
import io.ebean.TxScope;
import io.ebean.annotation.TxIsolation;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.ProgramModel;
import models.QuestionModel;
import models.VersionModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.DeletionStatus;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;
import support.ProgramBuilder;

@RunWith(JUnitParamsRunner.class)
public class ActiveAndDraftQuestionsTest extends ResetPostgres {

  private static final String TEST_QUESTION_NAME = "test-question";

  private VersionRepository versionRepository;

  @Before
  public void setUp() {
    versionRepository = instanceOf(VersionRepository.class);
  }

  @Test
  @Parameters({"false", "true"})
  public void getQuestionNames(Boolean useTransaction) throws Exception {
    QuestionModel tombstonedQuestionFromActiveVersion =
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
        .getDraftVersionOrCreate()
        .addQuestion(resourceCreator.insertQuestion("active-and-draft-question"))
        .addQuestion(resourceCreator.insertQuestion("draft-only-question"))
        .save();

    Optional<Transaction> maybeTransaction = maybeMakeTransaction(useTransaction);

    assertThat(newActiveAndDraftQuestions().getQuestionNames())
        .containsExactlyInAnyOrder(
            "active-and-draft-question",
            "active-only-question",
            "draft-only-question",
            "tombstoned-question");

    maybeTransaction.ifPresent(Transaction::end);
  }

  @Test
  @Parameters({"false", "true"})
  public void getActiveOrDraftQuestionDefinition(Boolean useTransaction) throws Exception {
    QuestionModel tombstonedQuestionFromActiveVersion =
        resourceCreator.insertQuestion("tombstoned-question");
    QuestionModel activeAndDraftQuestion =
        resourceCreator.insertQuestion("active-and-draft-question");
    QuestionModel activeOnlyQuestion = resourceCreator.insertQuestion("active-only-question");
    QuestionModel activeAndDraftQuestionUpdated =
        resourceCreator.insertQuestion("active-and-draft-question");
    QuestionModel draftOnlyQuestion = resourceCreator.insertQuestion("draft-only-question");

    versionRepository
        .getActiveVersion()
        .addQuestion(tombstonedQuestionFromActiveVersion)
        .addQuestion(activeAndDraftQuestion)
        .addQuestion(activeOnlyQuestion)
        .save();
    addTombstoneToVersion(
        versionRepository.getActiveVersion(), tombstonedQuestionFromActiveVersion);

    versionRepository
        .getDraftVersionOrCreate()
        .addQuestion(activeAndDraftQuestionUpdated)
        .addQuestion(draftOnlyQuestion)
        .save();

    Optional<Transaction> maybeTransaction = maybeMakeTransaction(useTransaction);

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

    maybeTransaction.ifPresent(Transaction::end);
  }

  @Test
  @Parameters({"false", "true"})
  public void getDeletionStatus_notPartOfEitherVersion(Boolean useTransaction) {
    Optional<Transaction> maybeTransaction = maybeMakeTransaction(useTransaction);

    resourceCreator.insertQuestion(TEST_QUESTION_NAME);

    assertThat(newActiveAndDraftQuestions().getDeletionStatus(TEST_QUESTION_NAME))
        .isEqualTo(DeletionStatus.NOT_ACTIVE);

    maybeTransaction.ifPresent(Transaction::end);
  }

  @Test
  @Parameters({"false", "true"})
  public void getDeletionStatus_notReferencedByProgramButStillReferencedByVersion(
      Boolean useTransaction) {
    QuestionModel activeVersionQuestion = resourceCreator.insertQuestion(TEST_QUESTION_NAME);
    versionRepository.getActiveVersion().addQuestion(activeVersionQuestion).save();

    QuestionModel draftVersionQuestion = resourceCreator.insertQuestion("draft-version-question");
    versionRepository.getDraftVersionOrCreate().addQuestion(draftVersionQuestion).save();

    Optional<Transaction> maybeTransaction = maybeMakeTransaction(useTransaction);

    assertThat(newActiveAndDraftQuestions().getDeletionStatus(TEST_QUESTION_NAME))
        .isEqualTo(DeletionStatus.DELETABLE);
    assertThat(newActiveAndDraftQuestions().getDeletionStatus("draft-version-question"))
        .isEqualTo(DeletionStatus.DELETABLE);

    maybeTransaction.ifPresent(Transaction::end);
  }

  @Test
  @Parameters({"false", "true"})
  public void getDeletionStatus_tombstoned(Boolean useTransaction) throws Exception {
    Optional<Transaction> maybeTransaction = maybeMakeTransaction(useTransaction);

    QuestionModel question = resourceCreator.insertQuestion(TEST_QUESTION_NAME);
    versionRepository.getActiveVersion().addQuestion(question).save();
    versionRepository
        .getDraftVersionOrCreate()
        .addQuestion(resourceCreator.insertQuestion(TEST_QUESTION_NAME))
        .save();
    addTombstoneToVersion(versionRepository.getDraftVersionOrCreate(), question);

    assertThat(newActiveAndDraftQuestions().getDeletionStatus(TEST_QUESTION_NAME))
        .isEqualTo(DeletionStatus.PENDING_DELETION);

    maybeTransaction.ifPresent(Transaction::end);
  }

  @Test
  @Parameters({"false", "true"})
  public void getDeletionStatus_createdAndTombstonedInDraftVersion(Boolean useTransaction)
      throws Exception {
    QuestionModel question = resourceCreator.insertQuestion(TEST_QUESTION_NAME);
    versionRepository.getDraftVersionOrCreate().addQuestion(question).save();
    addTombstoneToVersion(versionRepository.getDraftVersionOrCreate(), question);

    Optional<Transaction> maybeTransaction = maybeMakeTransaction(useTransaction);

    assertThat(newActiveAndDraftQuestions().getDeletionStatus(TEST_QUESTION_NAME))
        .isEqualTo(DeletionStatus.PENDING_DELETION);

    maybeTransaction.ifPresent(Transaction::end);
  }

  @Test
  @Parameters({"false", "true"})
  public void getDeletionStatus_stillReferencedInActiveVersion(Boolean useTransaction) {
    QuestionModel questionActive = resourceCreator.insertQuestion(TEST_QUESTION_NAME);
    versionRepository.getActiveVersion().addQuestion(questionActive).save();
    // newActiveProgram automatically adds the program to the active version.
    ProgramBuilder.newActiveProgram("foo")
        .withBlock("Screen 1")
        .withRequiredQuestion(questionActive)
        .build();

    Optional<Transaction> maybeTransaction = maybeMakeTransaction(useTransaction);

    assertThat(newActiveAndDraftQuestions().getDeletionStatus(TEST_QUESTION_NAME))
        .isEqualTo(DeletionStatus.NOT_DELETABLE);

    maybeTransaction.ifPresent(Transaction::end);
  }

  @Test
  @Parameters({"false", "true"})
  public void getDeletionStatus_newReferenceInDraftVersion(Boolean useTransaction) {
    // Simulates the state where the question was created in the active version
    // and wasn't referenced. Then it was referenced by a program in the draft
    // version. In this case, the draft won't yet contain a reference to the question.
    QuestionModel questionActive = resourceCreator.insertQuestion(TEST_QUESTION_NAME);
    versionRepository.getActiveVersion().addQuestion(questionActive).save();
    // newDraftProgram automatically adds the program to the draft version.
    ProgramBuilder.newDraftProgram("foo")
        .withBlock("Screen 1")
        .withRequiredQuestion(questionActive)
        .build();

    Optional<Transaction> maybeTransaction = maybeMakeTransaction(useTransaction);

    assertThat(newActiveAndDraftQuestions().getDeletionStatus(TEST_QUESTION_NAME))
        .isEqualTo(DeletionStatus.NOT_DELETABLE);

    maybeTransaction.ifPresent(Transaction::end);
  }

  @Test
  @Parameters({"false", "true"})
  public void getDeletionStatus_noLongerReferencedInDraftVersion(Boolean useTransaction) {
    // Simulate the only reference to the question having been removed in an edit of the program.
    QuestionModel questionActive = resourceCreator.insertQuestion(TEST_QUESTION_NAME);
    versionRepository.getActiveVersion().addQuestion(questionActive).save();
    // newActiveProgram automatically adds the program to the active version.
    ProgramBuilder.newActiveProgram("foo")
        .withBlock("Screen 1")
        .withRequiredQuestion(questionActive)
        .build();
    // Create a draft version of the program that no longer references the question.
    // newDraftProgram automatically adds the program to the draft version.
    ProgramBuilder.newDraftProgram("foo").withBlock("Screen 1").build();

    Optional<Transaction> maybeTransaction = maybeMakeTransaction(useTransaction);

    assertThat(newActiveAndDraftQuestions().getDeletionStatus(TEST_QUESTION_NAME))
        .isEqualTo(DeletionStatus.DELETABLE);

    // Adding a draft edit of the question continues to be considered deletable.
    QuestionModel questionDraft = resourceCreator.insertQuestion(TEST_QUESTION_NAME);
    versionRepository.getDraftVersionOrCreate().addQuestion(questionDraft).save();

    assertThat(newActiveAndDraftQuestions().getDeletionStatus(TEST_QUESTION_NAME))
        .isEqualTo(DeletionStatus.DELETABLE);

    maybeTransaction.ifPresent(Transaction::end);
  }

  @Test
  @Parameters({"false", "true"})
  public void getDeletionStatus_notReferencedByProgramInEitherVersion(Boolean useTransaction) {
    // Create an active and draft version of a question that isn't referenced
    // by any programs.
    versionRepository
        .getActiveVersion()
        .addQuestion(resourceCreator.insertQuestion(TEST_QUESTION_NAME))
        .save();
    versionRepository
        .getDraftVersionOrCreate()
        .addQuestion(resourceCreator.insertQuestion(TEST_QUESTION_NAME))
        .save();

    Optional<Transaction> maybeTransaction = maybeMakeTransaction(useTransaction);

    assertThat(newActiveAndDraftQuestions().getDeletionStatus(TEST_QUESTION_NAME))
        .isEqualTo(DeletionStatus.DELETABLE);

    maybeTransaction.ifPresent(Transaction::end);
  }

  @Test
  @Parameters({"false", "true"})
  public void getReferencingPrograms_unreferencedQuestion(Boolean useTransaction) {
    versionRepository
        .getActiveVersion()
        .addQuestion(resourceCreator.insertQuestion(TEST_QUESTION_NAME))
        .save();

    Optional<Transaction> maybeTransaction = maybeMakeTransaction(useTransaction);

    ActiveAndDraftQuestions.ReferencingPrograms result =
        newActiveAndDraftQuestions().getReferencingPrograms(TEST_QUESTION_NAME);
    assertThat(result)
        .isEqualTo(
            ActiveAndDraftQuestions.ReferencingPrograms.builder()
                .setActiveReferences(ImmutableSet.of())
                .setDraftReferences(ImmutableSet.of())
                .build());

    // Make an edit of the question in the draft version and leave it unreferenced.
    versionRepository
        .getDraftVersionOrCreate()
        .addQuestion(resourceCreator.insertQuestion(TEST_QUESTION_NAME))
        .save();
    result = newActiveAndDraftQuestions().getReferencingPrograms(TEST_QUESTION_NAME);
    assertThat(result)
        .isEqualTo(
            ActiveAndDraftQuestions.ReferencingPrograms.builder()
                .setActiveReferences(ImmutableSet.of())
                .setDraftReferences(ImmutableSet.of())
                .build());

    maybeTransaction.ifPresent(Transaction::end);
  }

  @Test
  @Parameters({"false", "true"})
  public void getReferencingPrograms_multipleProgramReferencesForSameQuestionVersion(
      Boolean useTransaction) {
    QuestionModel question = resourceCreator.insertQuestion(TEST_QUESTION_NAME);

    // Set up state where the question is referenced from:
    // ACTIVE version - first-program and second-program
    // DRAFT version - second-program and third-program

    // newActiveProgram / newDraftProgram automatically adds the program to the specified version.
    ProgramModel firstProgramActive =
        ProgramBuilder.newActiveProgram("first-program")
            .withBlock("Screen 1")
            .withRequiredQuestion(question)
            .build();
    ProgramModel secondProgramActive =
        ProgramBuilder.newActiveProgram("second-program")
            .withBlock("Screen 1")
            .withBlock("Screen 2")
            .withRequiredQuestion(question)
            .build();
    versionRepository.getActiveVersion().addQuestion(question).save();

    // No longer reference the question from the first program.
    ProgramBuilder.newDraftProgram("first-program").withBlock("Screen 1").build();
    ProgramModel secondProgramDraft =
        ProgramBuilder.newDraftProgram("second-program")
            .withBlock("Screen 1")
            .withBlock("Screen 2")
            .withBlock("Screen 3")
            .withRequiredQuestion(question)
            .build();
    ProgramModel thirdProgramDraft =
        ProgramBuilder.newDraftProgram("third-program")
            .withBlock("Screen 1")
            .withRequiredQuestion(question)
            .build();
    versionRepository.getDraftVersionOrCreate().addQuestion(question).save();

    Optional<Transaction> maybeTransaction = maybeMakeTransaction(useTransaction);

    ActiveAndDraftQuestions.ReferencingPrograms result =
        newActiveAndDraftQuestions().getReferencingPrograms(TEST_QUESTION_NAME);
    assertThat(result.activeReferences().stream().map(ProgramDefinition::id))
        .containsExactlyInAnyOrder(firstProgramActive.id, secondProgramActive.id);
    assertThat(result.draftReferences().stream().map(ProgramDefinition::id))
        .containsExactlyInAnyOrder(secondProgramDraft.id, thirdProgramDraft.id);

    maybeTransaction.ifPresent(Transaction::end);
  }

  @Test
  @Parameters({"false", "true"})
  public void getReferencingPrograms_multipleProgramReferencesForDifferentQuestionVersions(
      Boolean useTransaction) {
    // Set up state where the question is referenced from:
    // ACTIVE version - first-program and second-program
    // DRAFT version - second-program and third-program
    // In addition, the DRAFT version references are to an edited question.

    QuestionModel activeQuestion = resourceCreator.insertQuestion(TEST_QUESTION_NAME);
    versionRepository.getActiveVersion().addQuestion(activeQuestion).save();
    // newActiveProgram / newDraftProgram automatically adds the program to the specified version.
    ProgramModel firstProgramActive =
        ProgramBuilder.newActiveProgram("first-program")
            .withBlock("Screen 1")
            .withRequiredQuestion(activeQuestion)
            .build();
    ProgramModel secondProgramActive =
        ProgramBuilder.newActiveProgram("second-program")
            .withBlock("Screen 1")
            .withBlock("Screen 2")
            .withRequiredQuestion(activeQuestion)
            .build();

    // No longer reference the question from the first program.
    QuestionModel draftQuestion = resourceCreator.insertQuestion(TEST_QUESTION_NAME);
    versionRepository.getDraftVersionOrCreate().addQuestion(draftQuestion).save();
    ProgramBuilder.newDraftProgram("first-program").withBlock("Screen 1").build();
    ProgramModel secondProgramDraft =
        ProgramBuilder.newDraftProgram("second-program")
            .withBlock("Screen 1")
            .withBlock("Screen 2")
            .withBlock("Screen 3")
            .withRequiredQuestion(draftQuestion)
            .build();
    ProgramModel thirdProgramDraft =
        ProgramBuilder.newDraftProgram("third-program")
            .withBlock("Screen 1")
            .withRequiredQuestion(draftQuestion)
            .build();

    Optional<Transaction> maybeTransaction = maybeMakeTransaction(useTransaction);

    ActiveAndDraftQuestions.ReferencingPrograms result =
        newActiveAndDraftQuestions().getReferencingPrograms(TEST_QUESTION_NAME);
    assertThat(result.activeReferences().stream().map(ProgramDefinition::id))
        .containsExactlyInAnyOrder(firstProgramActive.id, secondProgramActive.id);
    assertThat(result.draftReferences().stream().map(ProgramDefinition::id))
        .containsExactlyInAnyOrder(secondProgramDraft.id, thirdProgramDraft.id);

    maybeTransaction.ifPresent(Transaction::end);
  }

  @Test
  @Parameters({"false", "true"})
  public void getReferencingPrograms_unrecognizedQuestion(Boolean useTransaction) {
    Optional<Transaction> maybeTransaction = maybeMakeTransaction(useTransaction);

    ActiveAndDraftQuestions.ReferencingPrograms result =
        newActiveAndDraftQuestions().getReferencingPrograms("random-question-name");
    assertThat(result)
        .isEqualTo(
            ActiveAndDraftQuestions.ReferencingPrograms.builder()
                .setActiveReferences(ImmutableSet.of())
                .setDraftReferences(ImmutableSet.of())
                .build());
    maybeTransaction.ifPresent(Transaction::end);
  }

  private ActiveAndDraftQuestions newActiveAndDraftQuestions() {
    return ActiveAndDraftQuestions.buildFromCurrentVersions(versionRepository);
  }

  private void addTombstoneToVersion(VersionModel version, QuestionModel question)
      throws Exception {
    assertThat(versionRepository.addTombstoneForQuestionInVersion(question, version)).isTrue();
    version.save();
  }

  private static Optional<Transaction> maybeMakeTransaction(Boolean useTransaction) {
    if (useTransaction) {
      return Optional.of(
          DB.beginTransaction(TxScope.required().setIsolation(TxIsolation.SERIALIZABLE)));
    }
    return Optional.empty();
  }
}
