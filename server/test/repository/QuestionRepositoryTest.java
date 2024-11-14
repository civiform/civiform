package repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.ebean.DataIntegrityException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import models.QuestionModel;
import models.QuestionTag;
import org.junit.Before;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.PrimaryApplicantInfoTag;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.TextQuestionDefinition;

public class QuestionRepositoryTest extends ResetPostgres {

  private QuestionRepository repo;
  private VersionRepository versionRepo;

  @Before
  public void setupQuestionRepository() {
    repo = instanceOf(QuestionRepository.class);
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
    QuestionDefinition questionDefinition =
        new TextQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("question")
                .setDescription("applicant's name")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is your name?"))
                .setQuestionHelpText(LocalizedStrings.empty())
                .build());
    QuestionModel question = new QuestionModel(questionDefinition);

    repo.insertQuestion(question).toCompletableFuture().join();

    long id = question.id;
    QuestionModel q = repo.lookupQuestion(id).toCompletableFuture().join().get();
    assertThat(q.id).isEqualTo(id);
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

    assertThat(repo.lookupQuestion(question.id).toCompletableFuture().join()).hasValue(question);
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
    QuestionModel question = resourceCreator.insertQuestion();
    QuestionDefinition questionDefinition = question.getQuestionDefinition();
    questionDefinition =
        new QuestionDefinitionBuilder(questionDefinition).setDescription("new description").build();

    repo.updateQuestion(new QuestionModel(questionDefinition)).toCompletableFuture().join();

    QuestionModel q = repo.lookupQuestion(question.id).toCompletableFuture().join().get();
    assertThat(q.getQuestionDefinition()).isEqualTo(questionDefinition);
  }

  @Test
  public void updateQuestionSync() throws UnsupportedQuestionTypeException {
    QuestionModel question = resourceCreator.insertQuestion();
    QuestionDefinition questionDefinition = question.getQuestionDefinition();
    questionDefinition =
        new QuestionDefinitionBuilder(questionDefinition).setDescription("new description").build();

    repo.updateQuestionSync(new QuestionModel(questionDefinition));

    QuestionModel q = repo.lookupQuestion(question.id).toCompletableFuture().join().get();
    assertThat(q.getQuestionDefinition()).isEqualTo(questionDefinition);
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
  public void getQuestionsWithSimilarAdminNames_returnsSimilarAdminNames() {
    resourceCreator.insertQuestion("name-question");
    resourceCreator.insertQuestion("name-question-1");
    resourceCreator.insertQuestion("name-question-2");
    ImmutableList<String> adminNames = repo.getSimilarAdminNames("name-question");
    assertThat(adminNames.size()).isEqualTo(3);
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
