package services.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import models.LifecycleStage;
import models.Question;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.CiviFormError;
import services.ErrorAnd;
import services.LocalizedStrings;
import services.question.exceptions.InvalidUpdateException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;
import services.question.types.TextQuestionDefinition;
import support.ProgramBuilder;

public class QuestionServiceTest extends ResetPostgres {
  private static final QuestionDefinition questionDefinition =
      new TextQuestionDefinition(
          "my name",
          Optional.empty(),
          "description",
          LocalizedStrings.of(Locale.US, "question?"),
          LocalizedStrings.of(Locale.US, "help text"));

  QuestionService questionService;
  VersionRepository versionRepository;

  @Before
  public void setUp() {
    questionService = instanceOf(QuestionService.class);
    versionRepository = instanceOf(VersionRepository.class);
  }

  @Test
  public void create_failsWhenQuestionsConflict() throws Exception {
    QuestionDefinition householdMemberName =
        testQuestionBank.applicantHouseholdMemberName().getQuestionDefinition();
    QuestionDefinition questionDefinition =
        new QuestionDefinitionBuilder(householdMemberName)
            .clearId()
            .setName(householdMemberName.getName() + "_")
            .build();

    ErrorAnd<QuestionDefinition, CiviFormError> errorAndResult =
        questionService.create(questionDefinition);

    assertThat(errorAndResult.hasResult()).isFalse();
    assertThat(errorAndResult.isError()).isTrue();
    assertThat(errorAndResult.getErrors())
        .containsOnly(
            CiviFormError.of(
                String.format(
                    "Question '%s' with Enumerator ID %d conflicts with question id: %d",
                    questionDefinition.getQuestionPathSegment(),
                    householdMemberName.getEnumeratorId().get(),
                    householdMemberName.getId())));
  }

  @Test
  public void create_returnsQuestionDefinitionWhenSucceeds() {
    ErrorAnd<QuestionDefinition, CiviFormError> errorAndResult =
        questionService.create(questionDefinition);

    assertThat(errorAndResult.isError()).isFalse();
    assertThat(errorAndResult.hasResult()).isTrue();
    assertThat(errorAndResult.getResult().getQuestionPathSegment())
        .isEqualTo(questionDefinition.getQuestionPathSegment());
  }

  @Test
  public void getReadOnlyQuestionService() {
    questionService.create(questionDefinition);

    CompletionStage<ReadOnlyQuestionService> completionStage =
        questionService.getReadOnlyQuestionService();

    ReadOnlyQuestionService roService = completionStage.toCompletableFuture().join();

    assertThat(roService.getAllQuestions().size()).isEqualTo(1);
  }

  @Test
  public void getReadOnlyQuestionService_empty() {
    CompletionStage<ReadOnlyQuestionService> completionStage =
        questionService.getReadOnlyQuestionService();

    ReadOnlyQuestionService emptyService = completionStage.toCompletableFuture().join();

    assertThat(emptyService.getAllQuestions()).isEmpty();
  }

  @Test
  public void update_returnsQuestionDefinitionWhenSucceeds() throws Exception {
    QuestionDefinition nameQuestion = testQuestionBank.applicantName().getQuestionDefinition();
    QuestionDefinition toUpdate =
        new QuestionDefinitionBuilder(nameQuestion).setDescription("updated description").build();

    ErrorAnd<QuestionDefinition, CiviFormError> errorAndResult = questionService.update(toUpdate);

    assertThat(errorAndResult.isError()).isFalse();
    assertThat(errorAndResult.hasResult()).isTrue();
    assertThat(errorAndResult.getResult().getDescription()).isEqualTo("updated description");
  }

  @Test
  public void update_failsWhenQuestionNotPersisted() {
    assertThatThrownBy(() -> questionService.update(questionDefinition))
        .isInstanceOf(InvalidUpdateException.class)
        .hasMessageContaining("question definition is not persisted");
  }

  @Test
  public void update_failsWhenQuestionNotExistent() throws UnsupportedQuestionTypeException {
    QuestionDefinition question =
        new QuestionDefinitionBuilder(questionDefinition).setId(9999L).build();
    assertThatThrownBy(() -> questionService.update(question))
        .isInstanceOf(InvalidUpdateException.class)
        .hasMessageContaining("question with id 9999 does not exist");
  }

  @Test
  public void update_failsWhenQuestionImmutableMembersChange() throws Exception {
    QuestionDefinition nameQuestion = testQuestionBank.applicantName().getQuestionDefinition();

    QuestionDefinition toUpdate =
        new QuestionDefinitionBuilder(nameQuestion)
            .setName("this is a new name")
            .setEnumeratorId(Optional.of(100L))
            .setQuestionType(QuestionType.ADDRESS)
            .setQuestionText(LocalizedStrings.withDefaultValue("$this new name"))
            .setQuestionHelpText(LocalizedStrings.withDefaultValue("$this again"))
            .build();

    ErrorAnd<QuestionDefinition, CiviFormError> errorAndResult = questionService.update(toUpdate);

    assertThat(errorAndResult.hasResult()).isFalse();
    assertThat(errorAndResult.isError()).isTrue();
    assertThat(errorAndResult.getErrors())
        .containsOnly(
            CiviFormError.of(
                String.format(
                    "question names mismatch: %s does not match %s",
                    nameQuestion.getName(), toUpdate.getName())),
            CiviFormError.of(
                String.format(
                    "question path segment mismatch: %s does not match %s",
                    nameQuestion.getQuestionPathSegment(), toUpdate.getQuestionPathSegment())),
            CiviFormError.of(
                String.format(
                    "question enumerator ids mismatch: [no enumerator] does not match %s",
                    toUpdate.getEnumeratorId().get())),
            CiviFormError.of(
                String.format(
                    "question types mismatch: %s does not match %s",
                    nameQuestion.getQuestionType(), toUpdate.getQuestionType())));
  }

  @Test
  public void discardDraft() throws Exception {
    QuestionDefinition nameQuestion = testQuestionBank.applicantName().getQuestionDefinition();
    long draftId = nameQuestion.getId() + 100000;
    QuestionDefinition toUpdate =
        new QuestionDefinitionBuilder(nameQuestion)
            .setId(draftId)
            .setQuestionText(LocalizedStrings.withDefaultValue("a different string"))
            .build();
    testQuestionBank.maybeSave(toUpdate, LifecycleStage.DRAFT);

    // Verify the draft is there.
    Optional<Question> draftQuestion =
        versionRepository.getDraftVersion().getQuestionByName(nameQuestion.getName());
    assertThat(draftQuestion).isPresent();
    assertThat(draftQuestion.get().getQuestionDefinition().getQuestionText())
        .isEqualTo(toUpdate.getQuestionText());

    // Execute.
    questionService.discardDraft(draftId);

    // Verify.
    assertThat(versionRepository.getDraftVersion().getQuestionByName(nameQuestion.getName()))
        .isNotPresent();
  }

  @Test
  public void discardDraft_updatesQuestionReferences() throws Exception {
    // Make a draft of an enumerator, and set a question to reference that draft.
    QuestionDefinition enumeratorQuestion =
        testQuestionBank
            .getSampleQuestionsForAllTypes()
            .get(QuestionType.ENUMERATOR)
            .getQuestionDefinition();
    long enumeratorActiveId = enumeratorQuestion.getId();
    long enumeratorDraftId = enumeratorActiveId + 100000;

    testQuestionBank.maybeSave(
        new QuestionDefinitionBuilder(enumeratorQuestion).setId(enumeratorDraftId).build(),
        LifecycleStage.DRAFT);

    QuestionDefinition dependentQuestion =
        new QuestionDefinitionBuilder(questionDefinition)
            .setEnumeratorId(Optional.of(enumeratorDraftId))
            .build();
    testQuestionBank.maybeSave(dependentQuestion, LifecycleStage.DRAFT);

    // Execute.
    questionService.discardDraft(enumeratorDraftId);

    // Verify.
    Optional<Question> dependentDraft =
        versionRepository.getDraftVersion().getQuestionByName(dependentQuestion.getName());
    assertThat(dependentDraft).isPresent();
    assertThat(dependentDraft.get().getQuestionDefinition().getEnumeratorId().get())
        .isEqualTo(enumeratorActiveId);
  }

  @Test
  public void archiveQuestion_notReferencedSucceeds() throws Exception {
    Question addressQuestion = testQuestionBank.applicantAddress();

    assertThat(versionRepository.getDraftVersion().getTombstonedQuestionNames())
        .doesNotContain(addressQuestion.getQuestionDefinition().getName());
    questionService.archiveQuestion(addressQuestion.id);
    assertThat(versionRepository.getDraftVersion().getTombstonedQuestionNames())
        .contains(addressQuestion.getQuestionDefinition().getName());
  }

  @Test
  public void archiveQuestion_referencedFails() {
    Question addressQuestion = testQuestionBank.applicantAddress();
    // Create a program that references the question.
    ProgramBuilder.newDraftProgram()
        .withBlock()
        .withRequiredQuestion(addressQuestion)
        .withBlock()
        .build();

    assertThat(versionRepository.getDraftVersion().getTombstonedQuestionNames())
        .doesNotContain(addressQuestion.getQuestionDefinition().getName());
    assertThatThrownBy(() -> questionService.archiveQuestion(addressQuestion.id))
        .isInstanceOf(InvalidUpdateException.class);
    assertThat(versionRepository.getDraftVersion().getTombstonedQuestionNames())
        .doesNotContain(addressQuestion.getQuestionDefinition().getName());
  }

  @Test
  public void archiveQuestion_alreadyArchivedFails() throws Exception {
    Question addressQuestion = testQuestionBank.applicantAddress();
    questionService.archiveQuestion(addressQuestion.id);

    assertThat(versionRepository.getDraftVersion().getTombstonedQuestionNames())
        .contains(addressQuestion.getQuestionDefinition().getName());
    assertThatThrownBy(() -> questionService.archiveQuestion(addressQuestion.id))
        .isInstanceOf(InvalidUpdateException.class);
    assertThat(versionRepository.getDraftVersion().getTombstonedQuestionNames())
        .contains(addressQuestion.getQuestionDefinition().getName());
  }

  @Test
  public void archiveQuestion_invalidIdFails() {
    assertThat(versionRepository.getDraftVersion().getTombstonedQuestionNames()).isEmpty();
    assertThatThrownBy(() -> questionService.archiveQuestion(Long.MAX_VALUE))
        .isInstanceOf(InvalidUpdateException.class);
    assertThat(versionRepository.getDraftVersion().getTombstonedQuestionNames()).isEmpty();
  }

  @Test
  public void restoreQuestion_pendingDeletionSucceeds() throws Exception {
    Question addressQuestion = testQuestionBank.applicantAddress();
    questionService.archiveQuestion(addressQuestion.id);

    assertThat(versionRepository.getDraftVersion().getTombstonedQuestionNames())
        .contains(addressQuestion.getQuestionDefinition().getName());
    questionService.restoreQuestion(addressQuestion.id);
    assertThat(versionRepository.getDraftVersion().getTombstonedQuestionNames())
        .doesNotContain(addressQuestion.getQuestionDefinition().getName());
  }

  @Test
  public void restoreQuestion_notArchivedFails() {
    Question addressQuestion = testQuestionBank.applicantAddress();

    assertThat(versionRepository.getDraftVersion().getTombstonedQuestionNames()).isEmpty();
    assertThatThrownBy(() -> questionService.restoreQuestion(addressQuestion.id))
        .isInstanceOf(InvalidUpdateException.class);
    assertThat(versionRepository.getDraftVersion().getTombstonedQuestionNames()).isEmpty();
  }

  @Test
  public void restoreQuestion_invalidIdFails() {
    assertThat(versionRepository.getDraftVersion().getTombstonedQuestionNames()).isEmpty();
    assertThatThrownBy(() -> questionService.restoreQuestion(Long.MAX_VALUE))
        .isInstanceOf(InvalidUpdateException.class);
    assertThat(versionRepository.getDraftVersion().getTombstonedQuestionNames()).isEmpty();
  }
}
