package support;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.dev.seeding.CategoryTranslationFileParser;
import io.ebean.DB;
import io.ebean.Database;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import models.AccountModel;
import models.ApiKeyModel;
import models.ApplicantModel;
import models.ApplicationModel;
import models.CategoryModel;
import models.LifecycleStage;
import models.Models;
import models.ProgramModel;
import models.QuestionModel;
import models.TrustedIntermediaryGroupModel;
import play.Environment;
import play.Mode;
import play.inject.Injector;
import services.LocalizedStrings;
import services.apikey.ApiKeyService;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.TextQuestionDefinition;

public class ResourceCreator {

  private final Database database;
  private final Injector injector;
  private static final int SECONDS_PER_YEAR = 31536000;

  public ResourceCreator(Injector injector) {
    this.database = DB.getDefault();
    this.injector = injector;
    ProgramBuilder.setInjector(injector);
  }

  /**
   * Create an API key with subnet of "8.8.8.8/32,1.1.1.1/32" and an expiration date one year in the
   * future.
   */
  public ApiKeyModel createActiveApiKey(String name, String keyId, String keySecret) {
    ApiKeyModel apiKey =
        new ApiKeyModel()
            .setName(name)
            .setKeyId(keyId)
            .setExpiration(Instant.now().plusSeconds(SECONDS_PER_YEAR))
            .setSubnet("8.8.8.8/32,1.1.1.1/32")
            .setSaltedKeySecret(injector.instanceOf(ApiKeyService.class).salt(keySecret))
            .setCreatedBy("test");

    apiKey.save();

    return apiKey;
  }

  public void truncateTables() {
    Models.truncate(database);
  }

  public void publishNewSynchronizedVersion() {
    injector.instanceOf(repository.VersionRepository.class).publishNewSynchronizedVersion();
  }

  public QuestionModel insertQuestion(String name) {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName(name)
                .setDescription("")
                .setQuestionText(LocalizedStrings.of())
                .setQuestionHelpText(LocalizedStrings.empty())
                .build());
    QuestionModel question = new QuestionModel(definition);
    question.save();
    return question;
  }

  public QuestionModel insertEnum(String name) {
    QuestionDefinition enumDefinition =
        new services.question.types.EnumeratorQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName(name)
                .setDescription("The applicant's household members")
                .setQuestionText(LocalizedStrings.of(Locale.US, "Who are your household members?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "This is sample help text."))
                .build(),
            LocalizedStrings.empty());
    QuestionModel enumQuestion = new QuestionModel(enumDefinition);
    enumQuestion.save();
    return enumQuestion;
  }

  public QuestionModel insertEnumQuestion(String enumName, QuestionModel question) {
    QuestionDefinition enumDefinition =
        new services.question.types.EnumeratorQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName(enumName)
                .setDescription("The applicant's household member's jobs")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What are the $this's jobs?"))
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "Where does $this work?"))
                .setEnumeratorId(Optional.of(question.id))
                .build(),
            LocalizedStrings.empty());
    QuestionModel enumQuestion = new QuestionModel(enumDefinition);
    enumQuestion.save();
    return enumQuestion;
  }

  public QuestionModel insertQuestion() {
    String name = UUID.randomUUID().toString();
    QuestionDefinition definition =
        new TextQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName(name)
                .setDescription("")
                .setQuestionText(LocalizedStrings.of())
                .setQuestionHelpText(LocalizedStrings.empty())
                .build());
    QuestionModel question = new QuestionModel(definition);
    question.save();
    return question;
  }

  public ProgramModel insertActiveProgram(String name) {
    return ProgramBuilder.newActiveProgram(name, "description").build();
  }

  public ProgramModel insertActiveDisabledProgram(String name) {
    return ProgramBuilder.newDisabledActiveProgram(name).build();
  }

  public ProgramModel insertActiveTiOnlyProgram(String name) {
    return ProgramBuilder.newActiveTiOnlyProgram(name).build();
  }

  public ProgramModel insertActiveHiddenInIndexProgram(String name) {
    return ProgramBuilder.newActiveHiddenInIndexProgram(name).build();
  }

  public ProgramModel insertActiveProgram(Locale locale, String name) {
    return ProgramBuilder.newActiveProgram().withLocalizedName(locale, name).build();
  }

  public ProgramModel insertActiveCommonIntakeForm(String name) {
    return ProgramBuilder.newActiveCommonIntakeForm(name).build();
  }

  public ProgramModel insertDraftProgram(String name) {
    return ProgramBuilder.newDraftProgram(name, "description").build();
  }

  public ApplicationModel insertActiveApplication(ApplicantModel applicant, ProgramModel program) {
    return ApplicationModel.create(applicant, program, LifecycleStage.ACTIVE);
  }

  public ApplicationModel insertDraftApplication(ApplicantModel applicant, ProgramModel program) {
    return ApplicationModel.create(applicant, program, LifecycleStage.DRAFT);
  }

  public ApplicationModel insertApplication(
      ApplicantModel applicant, ProgramModel program, LifecycleStage lifecycleStage) {
    return ApplicationModel.create(applicant, program, lifecycleStage);
  }

  public ApplicantModel insertApplicant() {
    ApplicantModel applicant = new ApplicantModel();
    applicant.save();
    return applicant;
  }

  public AccountModel insertAccount() {
    AccountModel account = new AccountModel();
    account.save();
    return account;
  }

  public TrustedIntermediaryGroupModel insertTiGroup(String groupName) {
    TrustedIntermediaryGroupModel tiGroup =
        new TrustedIntermediaryGroupModel(groupName, "A TI group for all your TI needs!");
    tiGroup.save();
    return tiGroup;
  }

  public CategoryModel insertCategory(ImmutableMap<Locale, String> translations) {
    CategoryModel category = new CategoryModel(translations);
    category.save();
    return category;
  }

  public ImmutableList<CategoryModel> insertCategoriesFromParser() {
    CategoryTranslationFileParser parser =
        new CategoryTranslationFileParser(new Environment(Mode.PROD));
    List<CategoryModel> parsedCategories = parser.createCategoryModelList();
    ImmutableList.Builder<CategoryModel> savedCategoriesBuilder =
        ImmutableList.<CategoryModel>builder();

    parsedCategories.forEach(
        parsedCategory -> {
          parsedCategory.id = null;
          database.save(parsedCategory);
          parsedCategory.refresh();
          savedCategoriesBuilder.add(parsedCategory);
        });

    return savedCategoriesBuilder.build();
  }

  public ApplicantModel insertApplicantWithAccount() {
    return insertApplicantWithAccount(/* accountEmail= */ Optional.empty());
  }

  /**
   * Inserts and Applicant and accompanying Account into the database.
   *
   * @param accountEmail an Optional representing the email address of the account. If empty, we
   *     also don't populate the authority ID, which makes this test user a guest.
   * @return the applicant
   */
  public ApplicantModel insertApplicantWithAccount(Optional<String> accountEmail) {
    ApplicantModel applicant = insertApplicant();
    AccountModel account = insertAccount();

    accountEmail.ifPresent(account::setEmailAddress);
    // If the account has an email, it is an authorized user and should have an
    // authority ID.
    accountEmail.ifPresent(unused -> account.setAuthorityId(UUID.randomUUID().toString()));
    account.save();
    applicant.setAccount(account);
    applicant.save();
    account.setApplicants(ImmutableList.of(applicant));
    account.save();

    return applicant;
  }

  /**
   * Inserts an Account with the given email address into the database. Sets an authority ID such
   * that the user will be "logged in".
   *
   * @param email the email address to use for the account
   * @return the account
   */
  public AccountModel insertAccountWithEmail(String email) {
    AccountModel account = new AccountModel();
    account.setEmailAddress(email);
    // User is not a guest, so they should have an authority ID.
    account.setAuthorityId(UUID.randomUUID().toString());
    account.save();
    return account;
  }

  public TrustedIntermediaryGroupModel insertTrustedIntermediaryGroup() {
    return insertTrustedIntermediaryGroup("");
  }

  public TrustedIntermediaryGroupModel insertTrustedIntermediaryGroup(String name) {
    TrustedIntermediaryGroupModel group = new TrustedIntermediaryGroupModel(name, "description");
    group.save();
    return group;
  }
}
