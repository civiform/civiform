package repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.ebean.DataIntegrityException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import models.LifecycleStage;
import models.ProgramModel;
import models.QuestionModel;
import models.QuestionTag;
import org.junit.Before;
import org.junit.Test;
import services.LocalizedStrings;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramQuestionDefinition;
import services.question.PrimaryApplicantInfoTag;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.TextQuestionDefinition;
import support.ProgramBuilder;
import support.TestQuestionBank;

public class QuestionRepositoryTest extends ResetPostgres {
  private static final TestQuestionBank disconnectedQuestionBank =
      new TestQuestionBank(/* canSave= */ false);
  private QuestionRepository repo;
  private TransactionManager transactionManager;
  private VersionRepository versionRepo;

  @Before
  public void setupQuestionRepository() {
    repo = instanceOf(QuestionRepository.class);
    transactionManager = instanceOf(TransactionManager.class);
    versionRepo = instanceOf(VersionRepository.class);
  }

  @Test
  public void listQuestions_empty() {
    assertThat(repo.listQuestions().toCompletableFuture().join()).isEmpty();
  }

  @Test
  public void listQuestions() {
    QuestionModel one = resourceCreator.insertQuestion();
    QuestionModel two = resourceCreator.insertQuestion();

    Set<QuestionModel> list = repo.listQuestions().toCompletableFuture().join();

    assertThat(list).containsExactly(one, two);
  }

  @Test
  public void lookupQuestion_returnsEmptyOptionalWhenQuestionNotFound() {
    Optional<QuestionModel> found = repo.lookupQuestion(1L).toCompletableFuture().join();

    assertThat(found).isEmpty();
  }

  @Test
  public void lookupQuestion_findsCorrectQuestion() {
    resourceCreator.insertQuestion();
    QuestionModel existing = resourceCreator.insertQuestion();

    Optional<QuestionModel> found = repo.lookupQuestion(existing.id).toCompletableFuture().join();

    assertThat(found).hasValue(existing);
  }

  @Test
  public void findConflictingQuestion_noConflicts_ok() throws Exception {
    QuestionDefinition applicantAddress =
        testQuestionBank.addressApplicantAddress().getQuestionDefinition();
    QuestionDefinition newQuestionDefinition =
        new QuestionDefinitionBuilder(applicantAddress)
            .clearId()
            .setName("a brand new question")
            .build();

    Optional<QuestionModel> maybeConflict = repo.findConflictingQuestion(newQuestionDefinition);

    assertThat(maybeConflict).isEmpty();
  }

  @Test
  public void findConflictingQuestion_sameName_hasConflict() throws Exception {
    QuestionModel applicantAddress = testQuestionBank.addressApplicantAddress();
    QuestionDefinition newQuestionDefinition =
        new QuestionDefinitionBuilder(applicantAddress.getQuestionDefinition())
            .clearId()
            .setEnumeratorId(Optional.of(1L))
            .build();

    Optional<QuestionModel> maybeConflict = repo.findConflictingQuestion(newQuestionDefinition);

    assertThat(maybeConflict).contains(applicantAddress);
  }

  @Test
  public void findConflictingQuestion_sameQuestionNameKey_viaNumbers_hasConflict()
      throws Exception {
    // Name is `applicant address`, the generated key is `applicant_address`
    QuestionModel applicantAddress = testQuestionBank.addressApplicantAddress();
    QuestionDefinition newQuestionDefinition =
        new QuestionDefinitionBuilder(applicantAddress.getQuestionDefinition())
            .clearId()
            .setName("applicant address1") // key form is `applicant_address`
            .build();

    Optional<QuestionModel> maybeConflict = repo.findConflictingQuestion(newQuestionDefinition);

    assertThat(maybeConflict).contains(applicantAddress);
  }

  @Test
  public void findConflictingQuestion_sameQuestionNameKey_viaCapitalization_hasConflict()
      throws Exception {
    // Name is `applicant address`, the generated key is `applicant_address`
    QuestionModel applicantAddress = testQuestionBank.addressApplicantAddress();
    QuestionDefinition newQuestionDefinition =
        new QuestionDefinitionBuilder(applicantAddress.getQuestionDefinition())
            .clearId()
            .setName("applicant Address") // key form is `applicant_address`
            .build();

    Optional<QuestionModel> maybeConflict = repo.findConflictingQuestion(newQuestionDefinition);

    assertThat(maybeConflict).contains(applicantAddress);
  }

  @Test
  public void findConflictingQuestion_sameQuestionNameKey_viaPunctuation_hasConflict()
      throws Exception {
    // Name is `applicant address`, the generated key is `applicant_address`
    QuestionModel applicantAddress = testQuestionBank.addressApplicantAddress();
    QuestionDefinition newQuestionDefinition =
        new QuestionDefinitionBuilder(applicantAddress.getQuestionDefinition())
            .clearId()
            .setName("applicant address!") // key form is `applicant_address`
            .build();

    Optional<QuestionModel> maybeConflict = repo.findConflictingQuestion(newQuestionDefinition);

    assertThat(maybeConflict).contains(applicantAddress);
  }

  @Test
  public void findConflictingQuestion_sameQuestion_hasConflict() {
    QuestionModel applicantAddress = testQuestionBank.addressApplicantAddress();
    Optional<QuestionModel> maybeConflict =
        repo.findConflictingQuestion(applicantAddress.getQuestionDefinition());

    assertThat(maybeConflict).contains(applicantAddress);
  }

  @Test
  public void findConflictingQuestion_differentVersion_hasConflict() throws Exception {
    QuestionModel applicantName = testQuestionBank.nameApplicantName();
    QuestionDefinition questionDefinition =
        new QuestionDefinitionBuilder(applicantName.getQuestionDefinition()).setId(123123L).build();

    Optional<QuestionModel> maybeConflict = repo.findConflictingQuestion(questionDefinition);

    assertThat(maybeConflict).contains(applicantName);
  }

  /* This test is meant to exercise the database trigger defined in server/conf/evolutions/default/54.sql */
  @Test
  public void insertingDuplicateDraftQuestions_raisesDatabaseException() throws Exception {
    var versionRepo = instanceOf(VersionRepository.class);
    var draftVersion = versionRepo.getDraftVersionOrCreate();
    QuestionModel activeQuestion = testQuestionBank.nameApplicantName();
    assertThat(activeQuestion.id).isNotNull();

    var draftOne =
        new QuestionModel(
            new QuestionDefinitionBuilder(activeQuestion.getQuestionDefinition())
                .setId(null)
                .build());
    draftOne.addVersion(draftVersion);
    draftOne.save();

    var draftTwo =
        new QuestionModel(
            new QuestionDefinitionBuilder(activeQuestion.getQuestionDefinition())
                .setId(null)
                .build());
    draftTwo.addVersion(draftVersion);

    var throwableAssert = assertThatThrownBy(() -> draftTwo.save());
    throwableAssert.hasMessageContaining("Question applicant name already has a draft!");
    throwableAssert.isExactlyInstanceOf(DataIntegrityException.class);
  }

  @Test
  public void insertQuestion() {
    repo.insertQuestion(disconnectedQuestionBank.nameApplicantName()).toCompletableFuture().join();
    long id = testQuestionBank.nameApplicantName().id;
    QuestionModel q = repo.lookupQuestion(id).toCompletableFuture().join().get();

    assertThat(q.id).isEqualTo(id);
    assertThat(q.getConcurrencyToken()).isNotNull();
  }

  @Test
  public void insertQuestionSync() {
    QuestionDefinition questionDefinition =
        new TextQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("question")
                .setDescription("applicant's name")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is your name?"))
                .setQuestionHelpText(LocalizedStrings.empty())
                .build());
    QuestionModel question = new QuestionModel(questionDefinition);

    repo.insertQuestionSync(question);
    QuestionModel q = repo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(q).isEqualTo(question);
  }

  @Test
  public void getExistingQuestions() {
    resourceCreator.insertQuestion("name-question");
    resourceCreator.insertQuestion("date-question");
    QuestionModel dateQuestionV2 = resourceCreator.insertQuestion("date-question");
    QuestionModel nameQuestionV2 = resourceCreator.insertQuestion("name-question");
    Map<String, QuestionDefinition> result =
        repo.getExistingQuestions(
            ImmutableSet.of("name-question", "date-question", "other-question"));
    assertThat(result).containsOnlyKeys("name-question", "date-question");
    assertThat(result.get("name-question").getId()).isEqualTo(nameQuestionV2.id);
    assertThat(result.get("date-question").getId()).isEqualTo(dateQuestionV2.id);
  }

  @Test
  public void updateQuestion() throws UnsupportedQuestionTypeException {
    QuestionModel initialQuestion = resourceCreator.insertQuestion();
    QuestionDefinition initialQuestionDefinition = initialQuestion.getQuestionDefinition();
    initialQuestionDefinition =
        new QuestionDefinitionBuilder(initialQuestionDefinition)
            .setDescription("new description")
            .build();

    repo.updateQuestion(new QuestionModel(initialQuestionDefinition)).toCompletableFuture().join();
    QuestionModel retrievedQuestion =
        repo.lookupQuestion(initialQuestion.id).toCompletableFuture().join().get();
    QuestionDefinition retrievedQuestionDefinition = retrievedQuestion.getQuestionDefinition();

    // assert concurrency token has changed
    assertThat(retrievedQuestionDefinition.getConcurrencyToken().get())
        .isNotEqualTo(initialQuestionDefinition.getConcurrencyToken().get());

    // assert other fields are equal via a QuestionDefinition with a matching token
    QuestionDefinition initialQuestionDefinitionWithMatchingToken =
        new QuestionDefinitionBuilder(initialQuestionDefinition)
            .setConcurrencyToken(retrievedQuestion.getConcurrencyToken())
            .build();
    assertThat(retrievedQuestionDefinition).isEqualTo(initialQuestionDefinitionWithMatchingToken);
  }

  @Test
  public void updateQuestionSync() throws UnsupportedQuestionTypeException {
    QuestionModel initialQuestion = resourceCreator.insertQuestion();
    QuestionDefinition initialQuestionDefinition = initialQuestion.getQuestionDefinition();
    initialQuestionDefinition =
        new QuestionDefinitionBuilder(initialQuestionDefinition)
            .setDescription("new description")
            .build();

    repo.updateQuestionSync(new QuestionModel(initialQuestionDefinition));
    QuestionModel retrievedQuestion =
        repo.lookupQuestion(initialQuestion.id).toCompletableFuture().join().get();
    QuestionDefinition retrievedQuestionDefinition = retrievedQuestion.getQuestionDefinition();

    // assert concurrency token has changed
    assertThat(retrievedQuestionDefinition.getConcurrencyToken().get())
        .isNotEqualTo(initialQuestionDefinition.getConcurrencyToken().get());

    // assert other fields are equal via a QuestionDefinition with a matching token
    QuestionDefinition initialQuestionDefinitionWithMatchingToken =
        new QuestionDefinitionBuilder(initialQuestionDefinition)
            .setConcurrencyToken(retrievedQuestion.getConcurrencyToken())
            .build();
    assertThat(retrievedQuestionDefinition).isEqualTo(initialQuestionDefinitionWithMatchingToken);
  }

  @Test
  public void bulkCreateQuestions_withoutTransaction_throws() {
    Exception e =
        assertThrows(
            IllegalStateException.class, () -> repo.bulkCreateQuestions(ImmutableList.of()));

    assertThat(e)
        .hasMessageContaining("bulkCreateQuestions must be called from within a transaction");
  }

  @Test
  public void bulkCreateQuestions_createsAllQuestions() {
    ImmutableList<QuestionDefinition> questionsToSave =
        ImmutableList.of(
            disconnectedQuestionBank.nameApplicantName().getQuestionDefinition(),
            disconnectedQuestionBank.addressApplicantAddress().getQuestionDefinition());

    ImmutableMap<String, QuestionDefinition> savedQuestions =
        transactionManager.execute(
            () -> {
              return repo.bulkCreateQuestions(questionsToSave);
            });

    assertThat(savedQuestions).hasSize(2);
    assertThat(repo.listQuestions().toCompletableFuture().join()).hasSize(2);
  }

  @Test
  public void createOrUpdateDraft_creatingDraftEnumeratorPreservesDraftRepeatedQuestions()
      throws UnsupportedQuestionTypeException {
    QuestionModel enumeratorQuestion = testQuestionBank.enumeratorApplicantHouseholdMembers();
    QuestionModel repeatedQuestion = testQuestionBank.idRepeatedHouseholdMemberId();

    // Create new draft of repeated question and publish it
    QuestionDefinition firstRepeatedQuestionUpdate =
        new QuestionDefinitionBuilder(repeatedQuestion.getQuestionDefinition())
            .setDescription("update 1")
            .build();
    repo.createOrUpdateDraft(firstRepeatedQuestionUpdate);
    versionRepo.publishNewSynchronizedVersion();

    // Create new draft of repeated question
    QuestionDefinition secondRepeatedQuestionUpdate =
        new QuestionDefinitionBuilder(repeatedQuestion.getQuestionDefinition())
            .setDescription("update 2")
            .build();
    QuestionModel secondRepeatedQuestion = repo.createOrUpdateDraft(secondRepeatedQuestionUpdate);

    // Create draft of enumerator question
    QuestionDefinition enumeratorQuestionUpdate =
        new QuestionDefinitionBuilder(enumeratorQuestion.getQuestionDefinition())
            .setDescription("updated")
            .build();
    repo.createOrUpdateDraft(enumeratorQuestionUpdate);

    secondRepeatedQuestion.refresh();

    assertThat(secondRepeatedQuestion.getQuestionDefinition().getDescription())
        .isEqualTo(secondRepeatedQuestionUpdate.getDescription());
  }

  @Test
  public void createOrUpdateDraft_managesUniversalTagCorrectly()
      throws UnsupportedQuestionTypeException {
    // Question will be published in an ACTIVE version
    QuestionModel question = testQuestionBank.nameApplicantName();
    QuestionDefinition nextQuestionDefinition;

    // Create new draft, ensure tags are correct
    nextQuestionDefinition =
        new QuestionDefinitionBuilder(question.getQuestionDefinition()).setUniversal(true).build();
    question = repo.createOrUpdateDraft(nextQuestionDefinition);
    assertThat(question.getQuestionTags().contains(QuestionTag.UNIVERSAL)).isTrue();

    versionRepo.publishNewSynchronizedVersion();
    nextQuestionDefinition =
        new QuestionDefinitionBuilder(question.getQuestionDefinition()).setUniversal(false).build();
    question = repo.createOrUpdateDraft(nextQuestionDefinition);
    assertThat(question.getQuestionTags().contains(QuestionTag.UNIVERSAL)).isFalse();

    // Update existing draft, ensure tags are correct
    nextQuestionDefinition =
        new QuestionDefinitionBuilder(question.getQuestionDefinition()).setUniversal(true).build();
    question = repo.createOrUpdateDraft(nextQuestionDefinition);
    assertThat(question.getQuestionTags().contains(QuestionTag.UNIVERSAL)).isTrue();

    nextQuestionDefinition =
        new QuestionDefinitionBuilder(question.getQuestionDefinition()).setUniversal(false).build();
    question = repo.createOrUpdateDraft(nextQuestionDefinition);
    assertThat(question.getQuestionTags().contains(QuestionTag.UNIVERSAL)).isFalse();
  }

  @Test
  public void createOrUpdateDraft_managesPrimaryApplicantInfoTagsCorrectl()
      throws UnsupportedQuestionTypeException {
    QuestionModel nameQuestion = testQuestionBank.nameApplicantName();
    QuestionModel dateQuestion = testQuestionBank.dateApplicantBirthdate();
    QuestionModel emailQuestion = testQuestionBank.emailApplicantEmail();
    QuestionModel phoneQuestion = testQuestionBank.phoneApplicantPhone();

    // Create new draft, ensure tags are correct
    QuestionDefinition nameQuestionDefinition = addTagToDefinition(nameQuestion);
    QuestionDefinition dateQuestionDefinition = addTagToDefinition(dateQuestion);
    QuestionDefinition emailQuestionDefinition = addTagToDefinition(emailQuestion);
    QuestionDefinition phoneQuestionDefinition = addTagToDefinition(phoneQuestion);

    nameQuestion = repo.createOrUpdateDraft(nameQuestionDefinition);
    dateQuestion = repo.createOrUpdateDraft(dateQuestionDefinition);
    emailQuestion = repo.createOrUpdateDraft(emailQuestionDefinition);
    phoneQuestion = repo.createOrUpdateDraft(phoneQuestionDefinition);

    assertThat(
            nameQuestion
                .getQuestionTags()
                .contains(PrimaryApplicantInfoTag.APPLICANT_NAME.getQuestionTag()))
        .isTrue();
    assertThat(
            dateQuestion
                .getQuestionTags()
                .contains(PrimaryApplicantInfoTag.APPLICANT_DOB.getQuestionTag()))
        .isTrue();
    assertThat(
            emailQuestion
                .getQuestionTags()
                .contains(PrimaryApplicantInfoTag.APPLICANT_EMAIL.getQuestionTag()))
        .isTrue();
    assertThat(
            phoneQuestion
                .getQuestionTags()
                .contains(PrimaryApplicantInfoTag.APPLICANT_PHONE.getQuestionTag()))
        .isTrue();

    versionRepo.publishNewSynchronizedVersion();

    // Remove tags on a new draft and ensure they are removed
    nameQuestionDefinition = removeTagsFromDefinition(nameQuestion);
    dateQuestionDefinition = removeTagsFromDefinition(dateQuestion);
    emailQuestionDefinition = removeTagsFromDefinition(emailQuestion);
    phoneQuestionDefinition = removeTagsFromDefinition(phoneQuestion);

    nameQuestion = repo.createOrUpdateDraft(nameQuestionDefinition);
    dateQuestion = repo.createOrUpdateDraft(dateQuestionDefinition);
    emailQuestion = repo.createOrUpdateDraft(emailQuestionDefinition);
    phoneQuestion = repo.createOrUpdateDraft(phoneQuestionDefinition);

    assertThat(
            nameQuestion
                .getQuestionTags()
                .contains(PrimaryApplicantInfoTag.APPLICANT_NAME.getQuestionTag()))
        .isFalse();
    assertThat(
            dateQuestion
                .getQuestionTags()
                .contains(PrimaryApplicantInfoTag.APPLICANT_DOB.getQuestionTag()))
        .isFalse();
    assertThat(
            emailQuestion
                .getQuestionTags()
                .contains(PrimaryApplicantInfoTag.APPLICANT_EMAIL.getQuestionTag()))
        .isFalse();
    assertThat(
            phoneQuestion
                .getQuestionTags()
                .contains(PrimaryApplicantInfoTag.APPLICANT_PHONE.getQuestionTag()))
        .isFalse();

    // Update existing draft, ensure tags are correct
    nameQuestionDefinition = addTagToDefinition(nameQuestion);
    dateQuestionDefinition = addTagToDefinition(dateQuestion);
    emailQuestionDefinition = addTagToDefinition(emailQuestion);
    phoneQuestionDefinition = addTagToDefinition(phoneQuestion);

    nameQuestion = repo.createOrUpdateDraft(nameQuestionDefinition);
    dateQuestion = repo.createOrUpdateDraft(dateQuestionDefinition);
    emailQuestion = repo.createOrUpdateDraft(emailQuestionDefinition);
    phoneQuestion = repo.createOrUpdateDraft(phoneQuestionDefinition);

    assertThat(
            nameQuestion
                .getQuestionTags()
                .contains(PrimaryApplicantInfoTag.APPLICANT_NAME.getQuestionTag()))
        .isTrue();
    assertThat(
            dateQuestion
                .getQuestionTags()
                .contains(PrimaryApplicantInfoTag.APPLICANT_DOB.getQuestionTag()))
        .isTrue();
    assertThat(
            emailQuestion
                .getQuestionTags()
                .contains(PrimaryApplicantInfoTag.APPLICANT_EMAIL.getQuestionTag()))
        .isTrue();
    assertThat(
            phoneQuestion
                .getQuestionTags()
                .contains(PrimaryApplicantInfoTag.APPLICANT_PHONE.getQuestionTag()))
        .isTrue();

    // Ensure we can remove tags on an existing draft question
    nameQuestionDefinition = removeTagsFromDefinition(nameQuestion);
    dateQuestionDefinition = removeTagsFromDefinition(dateQuestion);
    emailQuestionDefinition = removeTagsFromDefinition(emailQuestion);
    phoneQuestionDefinition = removeTagsFromDefinition(phoneQuestion);

    nameQuestion = repo.createOrUpdateDraft(nameQuestionDefinition);
    dateQuestion = repo.createOrUpdateDraft(dateQuestionDefinition);
    emailQuestion = repo.createOrUpdateDraft(emailQuestionDefinition);
    phoneQuestion = repo.createOrUpdateDraft(phoneQuestionDefinition);

    assertThat(
            nameQuestion
                .getQuestionTags()
                .contains(PrimaryApplicantInfoTag.APPLICANT_NAME.getQuestionTag()))
        .isFalse();
    assertThat(
            dateQuestion
                .getQuestionTags()
                .contains(PrimaryApplicantInfoTag.APPLICANT_DOB.getQuestionTag()))
        .isFalse();
    assertThat(
            emailQuestion
                .getQuestionTags()
                .contains(PrimaryApplicantInfoTag.APPLICANT_EMAIL.getQuestionTag()))
        .isFalse();
    assertThat(
            phoneQuestion
                .getQuestionTags()
                .contains(PrimaryApplicantInfoTag.APPLICANT_PHONE.getQuestionTag()))
        .isFalse();
  }

  @Test
  public void createOrUpdateDraft_draftingInitialQuestion_repointsEnumeratorAtNewDraft()
      throws ProgramBlockDefinitionNotFoundException, UnsupportedQuestionTypeException {
    // Creating a draft of an initial question creates an updated draft of its
    // enumerator.
    EnumeratorFixture fixture = newEnumeratorFixture();

    draftQuestion(fixture.newFlowSet1InitialQuestionId());

    QuestionDefinition initialQuestionAfter =
        latestDefinition(fixture.newFlowSet1InitialQuestionId());
    // There's a new initial question, and it kept the edit that created it.
    assertThat(initialQuestionAfter.getId()).isNotEqualTo(fixture.newFlowSet1InitialQuestionId());
    assertThat(initialQuestionAfter.getDescription()).isEqualTo("updated");
    // There's a new enumerator which now points at the new initial question.
    QuestionDefinition enumeratorAfter = latestDefinition(fixture.newFlowSet1EnumeratorId());
    assertThat(enumeratorAfter.getId()).isNotEqualTo(fixture.newFlowSet1EnumeratorId());
    assertThat(enumeratorAfter.getEnumeratorInitialQuestionId())
        .hasValue(initialQuestionAfter.getId());
    // The new initial question points at the new enumerator.
    assertThat(initialQuestionAfter.getEnumeratorId()).hasValue(enumeratorAfter.getId());
    // The previous active enumerator has not changed.
    assertThat(lookupDefinition(fixture.newFlowSet1EnumeratorId()).getEnumeratorInitialQuestionId())
        .hasValue(fixture.newFlowSet1InitialQuestionId());
    // Block 1 contains both of the new questions.
    assertThat(blockQuestionIds(fixture.program(), 1L))
        .containsExactly(enumeratorAfter.getId(), initialQuestionAfter.getId());
  }

  @Test
  public void createOrUpdateDraft_draftingInitialQuestion_repointsSiblingsAtNewDraftEnum()
      throws ProgramBlockDefinitionNotFoundException, UnsupportedQuestionTypeException {
    // Creating a draft of an initial question creates an updated draft of the other questions
    // under its enumerator, including a nested enumerator and its own initial question.
    EnumeratorFixture fixture = newEnumeratorFixture();

    draftQuestion(fixture.newFlowSet1InitialQuestionId());

    QuestionDefinition enumeratorAfter = latestDefinition(fixture.newFlowSet1EnumeratorId());
    QuestionDefinition repeatedQuestionAfter =
        latestDefinition(fixture.newFlowSet1RepeatedQuestionId());
    // There's a new repeated question draft.
    assertThat(repeatedQuestionAfter.getId()).isNotEqualTo(fixture.newFlowSet1RepeatedQuestionId());
    // The new repeated question draft points at the new enumerator.
    assertThat(repeatedQuestionAfter.getEnumeratorId()).hasValue(enumeratorAfter.getId());
    // Block 2 contains the new repeated question.
    assertThat(blockQuestionIds(fixture.program(), 2L))
        .containsExactly(repeatedQuestionAfter.getId());

    QuestionDefinition nestedEnumeratorAfter =
        latestDefinition(fixture.newFlowSet1NestedEnumeratorId());
    QuestionDefinition nestedInitialQuestionAfter =
        latestDefinition(fixture.newFlowSet1NestedInitialQuestionId());
    // There's a new nested enumerator draft, and it points at the new enumerator.
    assertThat(nestedEnumeratorAfter.getId()).isNotEqualTo(fixture.newFlowSet1NestedEnumeratorId());
    assertThat(nestedEnumeratorAfter.getEnumeratorId()).hasValue(enumeratorAfter.getId());
    // There's a new nested initial question draft, and it points at the new nested enumerator.
    assertThat(nestedInitialQuestionAfter.getId())
        .isNotEqualTo(fixture.newFlowSet1NestedInitialQuestionId());
    assertThat(nestedInitialQuestionAfter.getEnumeratorId())
        .hasValue(nestedEnumeratorAfter.getId());
    // The new nested enumerator points at the new nested initial question.
    assertThat(nestedEnumeratorAfter.getEnumeratorInitialQuestionId())
        .hasValue(nestedInitialQuestionAfter.getId());
    // Block 3 contains both of the new nested questions.
    assertThat(blockQuestionIds(fixture.program(), 3L))
        .containsExactly(nestedEnumeratorAfter.getId(), nestedInitialQuestionAfter.getId());
  }

  @Test
  public void createOrUpdateDraft_draftingInitialQuestion_leavesOtherNewFlowEnumeratorAlone()
      throws ProgramBlockDefinitionNotFoundException, UnsupportedQuestionTypeException {
    // Creating a draft of an initial question leaves an unrelated enumerator and its own initial
    // question alone.
    EnumeratorFixture fixture = newEnumeratorFixture();

    draftQuestion(fixture.newFlowSet1InitialQuestionId());

    QuestionDefinition otherNewFlowEnumerator = latestDefinition(fixture.newFlowSet2EnumeratorId());
    QuestionDefinition otherNewFlowRepeatedQuestion =
        latestDefinition(fixture.newFlowSet2InitialQuestionId());
    // There's no new enumerator, and it still points at the same initial question.
    assertThat(otherNewFlowEnumerator.getId()).isEqualTo(fixture.newFlowSet2EnumeratorId());
    assertThat(otherNewFlowEnumerator.getEnumeratorInitialQuestionId())
        .hasValue(fixture.newFlowSet2InitialQuestionId());
    // There's no new initial question, and it still points at the same enumerator.
    assertThat(otherNewFlowRepeatedQuestion.getId())
        .isEqualTo(fixture.newFlowSet2InitialQuestionId());
    assertThat(otherNewFlowRepeatedQuestion.getEnumeratorId())
        .hasValue(fixture.newFlowSet2EnumeratorId());
    // Block 4 still contains the original questions.
    assertThat(blockQuestionIds(fixture.program(), 4L))
        .containsExactly(fixture.newFlowSet2EnumeratorId(), fixture.newFlowSet2InitialQuestionId());
  }

  @Test
  public void createOrUpdateDraft_draftingInitialQuestion_leavesOldFlowEnumeratorAlone()
      throws ProgramBlockDefinitionNotFoundException, UnsupportedQuestionTypeException {
    // Creating a draft of an initial question leaves an old flow enumerator, which has no initial
    // question of its own, and its repeated question alone.
    EnumeratorFixture fixture = newEnumeratorFixture();

    draftQuestion(fixture.newFlowSet1InitialQuestionId());

    QuestionDefinition oldEnumerator = latestDefinition(fixture.oldFlowEnumeratorId());
    QuestionDefinition oldRepeatedQuestion = latestDefinition(fixture.oldFlowRepeatedQuestionId());
    // There's no new enumerator, and it still has no initial question.
    assertThat(oldEnumerator.getId()).isEqualTo(fixture.oldFlowEnumeratorId());
    assertThat(oldEnumerator.getEnumeratorInitialQuestionId()).isEmpty();
    // There's no new repeated question, and it still points at the same enumerator.
    assertThat(oldRepeatedQuestion.getId()).isEqualTo(fixture.oldFlowRepeatedQuestionId());
    assertThat(oldRepeatedQuestion.getEnumeratorId()).hasValue(fixture.oldFlowEnumeratorId());
    // Blocks 5 and 6 still contain the original questions.
    assertThat(blockQuestionIds(fixture.program(), 5L))
        .containsExactly(fixture.oldFlowEnumeratorId());
    assertThat(blockQuestionIds(fixture.program(), 6L))
        .containsExactly(fixture.oldFlowRepeatedQuestionId());
  }

  @Test
  public void createOrUpdateDraft_draftingEnumerator_repointsInitialAtDraft()
      throws ProgramBlockDefinitionNotFoundException, UnsupportedQuestionTypeException {
    // Creating a draft of an enumerator creates an updated draft of its initial question, and the
    // enumerator draft points at that new initial question rather than the published one.
    EnumeratorFixture fixture = newEnumeratorFixture();

    draftQuestion(fixture.newFlowSet1EnumeratorId());

    QuestionDefinition enumeratorAfter = latestDefinition(fixture.newFlowSet1EnumeratorId());
    QuestionDefinition initialQuestionAfter =
        latestDefinition(fixture.newFlowSet1InitialQuestionId());
    // There's a new enumerator, and it kept the edit that created it.
    assertThat(enumeratorAfter.getId()).isNotEqualTo(fixture.newFlowSet1EnumeratorId());
    assertThat(enumeratorAfter.getDescription()).isEqualTo("updated");
    // There's a new initial question.
    assertThat(initialQuestionAfter.getId()).isNotEqualTo(fixture.newFlowSet1InitialQuestionId());
    // The new enumerator points at the new initial question.
    assertThat(enumeratorAfter.getEnumeratorInitialQuestionId())
        .hasValue(initialQuestionAfter.getId());
    // The new initial question points at the new enumerator.
    assertThat(initialQuestionAfter.getEnumeratorId()).hasValue(enumeratorAfter.getId());
    // The previous active enumerator has not changed.
    assertThat(lookupDefinition(fixture.newFlowSet1EnumeratorId()).getEnumeratorInitialQuestionId())
        .hasValue(fixture.newFlowSet1InitialQuestionId());
    // Block 1 contains both of the new questions.
    assertThat(blockQuestionIds(fixture.program(), 1L))
        .containsExactly(enumeratorAfter.getId(), initialQuestionAfter.getId());
  }

  @Test
  public void createOrUpdateDraft_draftingEnumerator_carriesSiblingRepeatedQuestionForward()
      throws ProgramBlockDefinitionNotFoundException, UnsupportedQuestionTypeException {
    // Creating a draft of an enumerator creates an updated draft of the other questions
    // under it, and those do not take over its initial question reference.
    EnumeratorFixture fixture = newEnumeratorFixture();

    draftQuestion(fixture.newFlowSet1EnumeratorId());

    QuestionDefinition enumeratorAfter = latestDefinition(fixture.newFlowSet1EnumeratorId());
    QuestionDefinition repeatedAQuestionAfter =
        latestDefinition(fixture.newFlowSet1RepeatedQuestionId());
    // There's a new repeated question draft.
    assertThat(repeatedAQuestionAfter.getId())
        .isNotEqualTo(fixture.newFlowSet1RepeatedQuestionId());
    // The new repeated question points at the new enumerator.
    assertThat(repeatedAQuestionAfter.getEnumeratorId()).hasValue(enumeratorAfter.getId());
    // The new enumerator does not point at it, because it is not the initial question.
    assertThat(enumeratorAfter.getEnumeratorInitialQuestionId().orElseThrow())
        .isNotEqualTo(repeatedAQuestionAfter.getId());
    // Block 2 contains the new repeated question.
    assertThat(blockQuestionIds(fixture.program(), 2L))
        .containsExactly(repeatedAQuestionAfter.getId());
  }

  @Test
  public void createOrUpdateDraft_draftingEnumerator_leavesOtherNewFlowEnumeratorAlone()
      throws ProgramBlockDefinitionNotFoundException, UnsupportedQuestionTypeException {
    // Creating a draft of an enumerator leaves an unrelated enumerator and its own initial
    // question alone.
    EnumeratorFixture fixture = newEnumeratorFixture();

    draftQuestion(fixture.newFlowSet1EnumeratorId());

    QuestionDefinition otherNewFlowEnumerator = latestDefinition(fixture.newFlowSet2EnumeratorId());
    QuestionDefinition otherNewFlowRepatedQuestion =
        latestDefinition(fixture.newFlowSet2InitialQuestionId());
    // There's no new enumerator, and it still points at the same initial question.
    assertThat(otherNewFlowEnumerator.getId()).isEqualTo(fixture.newFlowSet2EnumeratorId());
    assertThat(otherNewFlowEnumerator.getEnumeratorInitialQuestionId())
        .hasValue(fixture.newFlowSet2InitialQuestionId());
    // There's no new initial question, and it still points at the same enumerator.
    assertThat(otherNewFlowRepatedQuestion.getId())
        .isEqualTo(fixture.newFlowSet2InitialQuestionId());
    assertThat(otherNewFlowRepatedQuestion.getEnumeratorId())
        .hasValue(fixture.newFlowSet2EnumeratorId());
    // Block 4 still contains the original questions.
    assertThat(blockQuestionIds(fixture.program(), 4L))
        .containsExactly(fixture.newFlowSet2EnumeratorId(), fixture.newFlowSet2InitialQuestionId());
  }

  @Test
  public void createOrUpdateDraft_draftingEnumerator_leavesOldFlowEnumeratorWithoutBackRef()
      throws ProgramBlockDefinitionNotFoundException, UnsupportedQuestionTypeException {
    // Creating a draft of an enumerator leaves an old flow enumerator, which has no initial
    // question of its own, and its repeated question alone.
    EnumeratorFixture fixture = newEnumeratorFixture();

    draftQuestion(fixture.newFlowSet1EnumeratorId());

    QuestionDefinition oldFlowEnumerator = latestDefinition(fixture.oldFlowEnumeratorId());
    QuestionDefinition oldFlowRepeatedQuestion =
        latestDefinition(fixture.oldFlowRepeatedQuestionId());
    // There's no new enumerator, and it still has no initial question.
    assertThat(oldFlowEnumerator.getId()).isEqualTo(fixture.oldFlowEnumeratorId());
    assertThat(oldFlowEnumerator.getEnumeratorInitialQuestionId()).isEmpty();
    // There's no new repeated question, and it still points at the same enumerator.
    assertThat(oldFlowRepeatedQuestion.getId()).isEqualTo(fixture.oldFlowRepeatedQuestionId());
    assertThat(oldFlowRepeatedQuestion.getEnumeratorId()).hasValue(fixture.oldFlowEnumeratorId());
    // Blocks 5 and 6 still contain the original questions.
    assertThat(blockQuestionIds(fixture.program(), 5L))
        .containsExactly(fixture.oldFlowEnumeratorId());
    assertThat(blockQuestionIds(fixture.program(), 6L))
        .containsExactly(fixture.oldFlowRepeatedQuestionId());
  }

  @Test
  public void createOrUpdateDraft_reeditingInitialQuestionDraft_keepsEnumeratorBackReference()
      throws UnsupportedQuestionTypeException {
    // The second edit reuses the existing draft row rather than minting a new id, so the
    // enumerator's back reference is already correct and must not churn.
    EnumeratorFixture fixture = newEnumeratorFixture();
    QuestionModel firstDraft =
        repo.createOrUpdateDraft(
            new QuestionDefinitionBuilder(lookupDefinition(fixture.newFlowSet1InitialQuestionId()))
                .setDescription("first edit")
                .build());
    long enumeratorDraftId = latestDefinition(fixture.newFlowSet1EnumeratorId()).getId();

    QuestionModel secondDraft =
        repo.createOrUpdateDraft(
            new QuestionDefinitionBuilder(lookupDefinition(firstDraft.id))
                .setDescription("second edit")
                .build());

    assertThat(secondDraft.id).isEqualTo(firstDraft.id);
    QuestionDefinition enumeratorAfter = latestDefinition(fixture.newFlowSet1EnumeratorId());
    assertThat(enumeratorAfter.getId()).isEqualTo(enumeratorDraftId);
    assertThat(enumeratorAfter.getEnumeratorInitialQuestionId()).hasValue(firstDraft.id);
  }

  /**
   * Container for the entities made in {@code newEnumeratorFixture}.
   *
   * <p>The default context is that items are for the new enumerator flow, and ones in the old flow
   * are indicated with 'old'.
   */
  private record EnumeratorFixture(
      long newFlowSet1EnumeratorId,
      long newFlowSet1InitialQuestionId,
      long newFlowSet1RepeatedQuestionId,
      long newFlowSet1NestedEnumeratorId,
      long newFlowSet1NestedInitialQuestionId,
      long newFlowSet2EnumeratorId,
      long newFlowSet2InitialQuestionId,
      long oldFlowEnumeratorId,
      long oldFlowRepeatedQuestionId,
      ProgramModel program) {}

  /**
   * Builds a draft program with six blocks and nine ACTIVE questions.
   *
   * <p>Block 1 is a new flow enumerator and holds the enumerator and its initial question, which
   * point at each other. Block 2 repeats on block 1 and holds a third question whose enumerator id
   * is the block 1 enumerator.
   *
   * <p>Block 3 also repeats on block 1 and holds a nested new-flow enumerator, whose enumerator id
   * is the block 1 enumerator, and the nested enumerator's own initial question. The nested
   * enumerator and its initial question point at each other.
   *
   * <p>Blocks 4, 5 and 6 are controls to allow for ensuring that when Blocks 1, 2 & 3 are changed
   * by code under test, that 4, 5 & 6 are not.
   *
   * <p>Block 4 holds a new-flow enumerator and its initial question, which point at each other.
   *
   * <p>Block 5 holds an old-flow enumerator. Block 6 repeats on block 5 and holds a repeated
   * question, where only the repeated question points at the enumerator.
   */
  private EnumeratorFixture newEnumeratorFixture() {
    QuestionModel newFlowSet1Enumerator =
        saveActiveEnumerator("newFlowSet1Enumerator", "Who is in your household?");
    QuestionModel newFlowSet1InitialQuestion =
        saveActiveRepeatedQuestion("household member name", newFlowSet1Enumerator);
    pointAtInitialQuestion(newFlowSet1Enumerator, newFlowSet1InitialQuestion);
    QuestionModel newFlowSet1RepeatedQuestion =
        saveActiveRepeatedQuestion("household member nickname", newFlowSet1Enumerator);

    QuestionModel newFlowSet1NestedEnumerator =
        saveActiveEnumerator(
            "newFlowSet1NestedEnumerator",
            "What jobs does $this have?",
            Optional.of(newFlowSet1Enumerator.id));
    QuestionModel newFlowSet1NestedInitialQuestion =
        saveActiveRepeatedQuestion("newFlowSet1NestedInitialQuestion", newFlowSet1NestedEnumerator);
    pointAtInitialQuestion(newFlowSet1NestedEnumerator, newFlowSet1NestedInitialQuestion);

    QuestionModel newFlowSet2Enumerator =
        saveActiveEnumerator("newFlowSet2Enumerator", "Where have you worked?");
    QuestionModel newFlowSet2InitialQuestion =
        saveActiveRepeatedQuestion("newFlowSet2InitialQuestion", newFlowSet2Enumerator);
    pointAtInitialQuestion(newFlowSet2Enumerator, newFlowSet2InitialQuestion);

    QuestionModel oldFlowEnumerator =
        saveActiveEnumerator("oldFlowEnumerator", "Where have you lived?");
    QuestionModel oldFlowRepeatedQuestion =
        saveActiveRepeatedQuestion("oldFlowRepeatedQuestion", oldFlowEnumerator);

    ProgramModel program =
        ProgramBuilder.newDraftProgram("enumerator program")
            .withBlock("block 1")
            .withRequiredQuestion(newFlowSet1Enumerator)
            .withRequiredQuestion(newFlowSet1InitialQuestion)
            .withRepeatedBlock("block 2")
            .withRequiredQuestion(newFlowSet1RepeatedQuestion)
            .withAnotherRepeatedBlock("block 3")
            .withRequiredQuestion(newFlowSet1NestedEnumerator)
            .withRequiredQuestion(newFlowSet1NestedInitialQuestion)
            .withBlock("block 4")
            .withRequiredQuestion(newFlowSet2Enumerator)
            .withRequiredQuestion(newFlowSet2InitialQuestion)
            .withBlock("block 5")
            .withRequiredQuestion(oldFlowEnumerator)
            .withRepeatedBlock("block 6")
            .withRequiredQuestion(oldFlowRepeatedQuestion)
            .build();
    return new EnumeratorFixture(
        newFlowSet1Enumerator.id,
        newFlowSet1InitialQuestion.id,
        newFlowSet1RepeatedQuestion.id,
        newFlowSet1NestedEnumerator.id,
        newFlowSet1NestedInitialQuestion.id,
        newFlowSet2Enumerator.id,
        newFlowSet2InitialQuestion.id,
        oldFlowEnumerator.id,
        oldFlowRepeatedQuestion.id,
        program);
  }

  /** Creates a new draft revision of the question with {@code questionId}. */
  private void draftQuestion(long questionId) throws UnsupportedQuestionTypeException {
    repo.createOrUpdateDraft(
        new QuestionDefinitionBuilder(lookupDefinition(questionId))
            .setDescription("updated")
            .build());
  }

  private QuestionModel saveActiveEnumerator(String name, String questionText) {
    return saveActiveEnumerator(name, questionText, /* enumeratorId= */ Optional.empty());
  }

  /** Saves an enumerator, nested under the enumerator with {@code enumeratorId} when given. */
  private QuestionModel saveActiveEnumerator(
      String name, String questionText, Optional<Long> enumeratorId) {
    return testQuestionBank.maybeSave(
        new EnumeratorQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName(name)
                .setDescription(name)
                .setQuestionText(LocalizedStrings.of(Locale.US, questionText))
                .setEnumeratorId(enumeratorId)
                .build(),
            LocalizedStrings.empty()),
        LifecycleStage.ACTIVE);
  }

  /** Completes the mutual reference in place, so no draft is created. */
  private void pointAtInitialQuestion(QuestionModel enumerator, QuestionModel initialQuestion) {
    new QuestionModel(
            repo.updateEnumeratorInitialQuestionId(
                enumerator.getQuestionDefinition(), initialQuestion.id))
        .update();
    enumerator.refresh();
  }

  private QuestionModel saveActiveRepeatedQuestion(String name, QuestionModel enumerator) {
    return testQuestionBank.maybeSave(
        new TextQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName(name)
                .setDescription(name)
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is $this's " + name + "?"))
                .setEnumeratorId(Optional.of(enumerator.id))
                .build()),
        LifecycleStage.ACTIVE);
  }

  private ImmutableList<Long> blockQuestionIds(ProgramModel program, long blockId)
      throws ProgramBlockDefinitionNotFoundException {
    program.refresh();
    return program
        .getProgramDefinition()
        .getBlockDefinition(blockId)
        .programQuestionDefinitions()
        .stream()
        .map(ProgramQuestionDefinition::id)
        .collect(ImmutableList.toImmutableList());
  }

  /** The definition stored under {@code id} itself, ignoring any newer revision. */
  private QuestionDefinition lookupDefinition(long id) {
    return repo.lookupQuestion(id)
        .toCompletableFuture()
        .join()
        .orElseThrow()
        .getQuestionDefinition();
  }

  /** The draft revision of the question named by {@code id}, or the active one if there is none. */
  private QuestionDefinition latestDefinition(long id) {
    return versionRepo.getLatestVersionOfQuestion(id).orElseThrow().getQuestionDefinition();
  }

  private QuestionDefinition addTagToDefinition(QuestionModel question)
      throws UnsupportedQuestionTypeException {
    QuestionDefinition definition = question.getQuestionDefinition();
    return new QuestionDefinitionBuilder(definition)
        .setPrimaryApplicantInfoTags(
            PrimaryApplicantInfoTag.getAllPaiTagsForQuestionType(definition.getQuestionType()))
        .build();
  }

  private QuestionDefinition removeTagsFromDefinition(QuestionModel question)
      throws UnsupportedQuestionTypeException {
    return new QuestionDefinitionBuilder(question.getQuestionDefinition())
        .setPrimaryApplicantInfoTags(ImmutableSet.of())
        .build();
  }
}
