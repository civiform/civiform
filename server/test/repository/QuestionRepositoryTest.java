package repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableSet;
import io.ebean.DB;
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
        testQuestionBank.applicantAddress().getQuestionDefinition();
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
    QuestionModel applicantAddress = testQuestionBank.applicantAddress();
    QuestionDefinition newQuestionDefinition =
        new QuestionDefinitionBuilder(applicantAddress.getQuestionDefinition())
            .clearId()
            .setEnumeratorId(Optional.of(1L))
            .build();

    Optional<QuestionModel> maybeConflict = repo.findConflictingQuestion(newQuestionDefinition);

    assertThat(maybeConflict).contains(applicantAddress);
  }

  @Test
  public void findConflictingQuestion_sameQuestionPathSegment_hasConflict() throws Exception {
    QuestionModel applicantAddress = testQuestionBank.applicantAddress();
    QuestionDefinition newQuestionDefinition =
        new QuestionDefinitionBuilder(applicantAddress.getQuestionDefinition())
            .clearId()
            .setName("applicant address!")
            .build();

    Optional<QuestionModel> maybeConflict = repo.findConflictingQuestion(newQuestionDefinition);

    assertThat(maybeConflict).contains(applicantAddress);
  }

  @Test
  public void findConflictingQuestion_sameQuestionPathSegmentButDifferentEnumeratorId_ok()
      throws Exception {
    QuestionModel applicantAddress = testQuestionBank.applicantAddress();
    QuestionDefinition newQuestionDefinition =
        new QuestionDefinitionBuilder(applicantAddress.getQuestionDefinition())
            .clearId()
            .setName("applicant_address")
            .setEnumeratorId(Optional.of(1L))
            .build();

    Optional<QuestionModel> maybeConflict = repo.findConflictingQuestion(newQuestionDefinition);

    assertThat(maybeConflict).isEmpty();
  }

  @Test
  public void findConflictingQuestion_sameQuestion_hasConflict() {
    QuestionModel applicantAddress = testQuestionBank.applicantAddress();
    Optional<QuestionModel> maybeConflict =
        repo.findConflictingQuestion(applicantAddress.getQuestionDefinition());

    assertThat(maybeConflict).contains(applicantAddress);
  }

  @Test
  public void findConflictingQuestion_differentVersion_hasConflict() throws Exception {
    QuestionModel applicantName = testQuestionBank.applicantName();
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
    QuestionModel activeQuestion = testQuestionBank.applicantName();
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
  public void loadLegacy() {
    DB.sqlUpdate(
            "insert into questions (name, description, legacy_question_text,"
                + " legacy_question_help_text, question_type) values ('old schema"
                + " entry', 'description', '{\"en_us\": \"text\"}', '{\"en_us\": \"help\"}',"
                + " 'REPEATER');")
        .execute();

    QuestionModel found =
        repo.listQuestions().toCompletableFuture().join().stream()
            .filter(
                question -> question.getQuestionDefinition().getName().equals("old schema entry"))
            .findFirst()
            .get();

    assertThat(found.getQuestionDefinition().getQuestionText())
        .isEqualTo(LocalizedStrings.of(Locale.US, "text"));
    assertThat(found.getQuestionDefinition().getQuestionHelpText())
        .isEqualTo(LocalizedStrings.of(Locale.US, "help"));
  }

  @Test
  public void createOrUpdateDraft_managesUniversalTagCorrectly()
      throws UnsupportedQuestionTypeException {
    // Question will be published in an ACTIVE version
    QuestionModel question = testQuestionBank.applicantName();
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
    QuestionModel nameQuestion = testQuestionBank.applicantName();
    QuestionModel dateQuestion = testQuestionBank.applicantDate();
    QuestionModel emailQuestion = testQuestionBank.applicantEmail();
    QuestionModel phoneQuestion = testQuestionBank.applicantPhone();
    ImmutableSet<PrimaryApplicantInfoTag> nameTag =
        ImmutableSet.of(PrimaryApplicantInfoTag.APPLICANT_NAME);
    ImmutableSet<PrimaryApplicantInfoTag> dateTag =
        ImmutableSet.of(PrimaryApplicantInfoTag.APPLICANT_DOB);
    ImmutableSet<PrimaryApplicantInfoTag> emailTag =
        ImmutableSet.of(PrimaryApplicantInfoTag.APPLICANT_EMAIL);
    ImmutableSet<PrimaryApplicantInfoTag> phoneTag =
        ImmutableSet.of(PrimaryApplicantInfoTag.APPLICANT_PHONE);

    // Create new draft, ensure tags are correct
    QuestionDefinition nameQuestionDefinition =
        new QuestionDefinitionBuilder(nameQuestion.getQuestionDefinition())
            .setPrimaryApplicantInfoTags(nameTag)
            .build();
    QuestionDefinition dateQuestionDefinition =
        new QuestionDefinitionBuilder(dateQuestion.getQuestionDefinition())
            .setPrimaryApplicantInfoTags(dateTag)
            .build();
    QuestionDefinition emailQuestionDefinition =
        new QuestionDefinitionBuilder(emailQuestion.getQuestionDefinition())
            .setPrimaryApplicantInfoTags(emailTag)
            .build();
    QuestionDefinition phoneQuestionDefinition =
        new QuestionDefinitionBuilder(phoneQuestion.getQuestionDefinition())
            .setPrimaryApplicantInfoTags(phoneTag)
            .build();

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
    nameQuestionDefinition =
        new QuestionDefinitionBuilder(nameQuestion.getQuestionDefinition())
            .setPrimaryApplicantInfoTags(ImmutableSet.of())
            .build();
    dateQuestionDefinition =
        new QuestionDefinitionBuilder(dateQuestion.getQuestionDefinition())
            .setPrimaryApplicantInfoTags(ImmutableSet.of())
            .build();
    emailQuestionDefinition =
        new QuestionDefinitionBuilder(emailQuestion.getQuestionDefinition())
            .setPrimaryApplicantInfoTags(ImmutableSet.of())
            .build();
    phoneQuestionDefinition =
        new QuestionDefinitionBuilder(phoneQuestion.getQuestionDefinition())
            .setPrimaryApplicantInfoTags(ImmutableSet.of())
            .build();

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
    nameQuestionDefinition =
        new QuestionDefinitionBuilder(nameQuestion.getQuestionDefinition())
            .setPrimaryApplicantInfoTags(nameTag)
            .build();
    dateQuestionDefinition =
        new QuestionDefinitionBuilder(dateQuestion.getQuestionDefinition())
            .setPrimaryApplicantInfoTags(dateTag)
            .build();
    emailQuestionDefinition =
        new QuestionDefinitionBuilder(emailQuestion.getQuestionDefinition())
            .setPrimaryApplicantInfoTags(emailTag)
            .build();
    phoneQuestionDefinition =
        new QuestionDefinitionBuilder(phoneQuestion.getQuestionDefinition())
            .setPrimaryApplicantInfoTags(phoneTag)
            .build();

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
    nameQuestionDefinition =
        new QuestionDefinitionBuilder(nameQuestion.getQuestionDefinition())
            .setPrimaryApplicantInfoTags(ImmutableSet.of())
            .build();
    dateQuestionDefinition =
        new QuestionDefinitionBuilder(dateQuestion.getQuestionDefinition())
            .setPrimaryApplicantInfoTags(ImmutableSet.of())
            .build();
    emailQuestionDefinition =
        new QuestionDefinitionBuilder(emailQuestion.getQuestionDefinition())
            .setPrimaryApplicantInfoTags(ImmutableSet.of())
            .build();
    phoneQuestionDefinition =
        new QuestionDefinitionBuilder(phoneQuestion.getQuestionDefinition())
            .setPrimaryApplicantInfoTags(ImmutableSet.of())
            .build();

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
}
