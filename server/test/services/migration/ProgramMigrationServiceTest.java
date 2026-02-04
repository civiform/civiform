package services.migration;

import static controllers.admin.AdminImportControllerTest.PROGRAM_JSON_WITH_ONE_QUESTION;
import static controllers.admin.AdminImportControllerTest.PROGRAM_JSON_WITH_PREDICATES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static support.TestQuestionBank.createDropdownQuestionDefinition;
import static support.TestQuestionBank.createQuestionDefinition;
import static support.TestQuestionBank.createYesNoQuestionDefinition;

import auth.ProgramAcls;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import controllers.admin.ProgramMigrationWrapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import models.CategoryModel;
import models.DisplayMode;
import models.ProgramModel;
import models.ProgramNotificationPreference;
import models.QuestionModel;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Lang;
import repository.ApplicationStatusesRepository;
import repository.ProgramRepository;
import repository.QuestionRepository;
import repository.ResetPostgres;
import repository.TransactionManager;
import repository.VersionRepository;
import services.CiviFormError;
import services.ErrorAnd;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramType;
import services.question.QuestionService;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;
import support.ProgramBuilder;

/**
 * Test class for ProgramMigrationService, covering various scenarios of program and question
 * migration, serialization, deserialization, and handling of duplicate questions.
 *
 * <p>This test suite verifies the behavior of program migration operations including: - Serializing
 * and deserializing program definitions - Handling duplicate question names - Preparing programs
 * for export and import - Saving imported programs with different configuration options
 */
public final class ProgramMigrationServiceTest extends ResetPostgres {
  private static final String CREATE_DUPLICATE = "CREATE_DUPLICATE";
  private static final String OVERWRITE_EXISTING = "OVERWRITE_EXISTING";
  private static final String QUESTION_1_NAME = "questionOne";
  private static final String QUESTION_2_NAME = "questionTwo";
  private static final String QUESTION_3_NAME = "questionThree";
  private static final String QUESTION_4_NAME = "questionFour";
  private static final String VALID_YES_NO_NAME = "validYesNoQuestion";
  private static final String INVALID_YES_NO_NAME = "invalidYesNoQuestion";
  private static final String DROPDOWN_QUESTION_NAME = "dropdownQuestion";

  private static final QuestionDefinition QUESTION_1 =
      createQuestionDefinition(QUESTION_1_NAME, 1L, QuestionType.TEXT, Optional.empty());
  private static final QuestionDefinition QUESTION_2 =
      createQuestionDefinition(QUESTION_2_NAME, 2L, QuestionType.TEXT, Optional.empty());
  private static final QuestionDefinition QUESTION_3 =
      createQuestionDefinition(QUESTION_3_NAME, 3L, QuestionType.ADDRESS, Optional.empty());
  private static final QuestionDefinition ENUMERATOR =
      createQuestionDefinition("enumerator", 4L, QuestionType.ENUMERATOR, Optional.empty());
  private static final QuestionDefinition REPEATED =
      createQuestionDefinition("repeated", 5L, QuestionType.TEXT, Optional.of(4L));
  private static final QuestionDefinition VALID_YES_NO_QUESTION =
      createYesNoQuestionDefinition(
          VALID_YES_NO_NAME, 6L, ImmutableList.of("yes", "no", "maybe", "not-sure"));
  private static final QuestionDefinition INVALID_YES_NO_QUESTION =
      createYesNoQuestionDefinition(
          INVALID_YES_NO_NAME, 7L, ImmutableList.of("yes", "no", "absolutely"));
  private static final QuestionDefinition MINIMAL_VALID_YES_NO_QUESTION =
      createYesNoQuestionDefinition("minimalValidYesNo", 8L, ImmutableList.of("yes", "no"));
  private static final QuestionDefinition DROPDOWN_QUESTION =
      createDropdownQuestionDefinition(
          DROPDOWN_QUESTION_NAME, 9L, ImmutableList.of("option1", "option2"));

  private static final ImmutableList<QuestionDefinition> QUESTIONS_1_2 =
      ImmutableList.of(QUESTION_1, QUESTION_2);
  private static final String PROGRAM_NAME_1 = "Program 1";
  private static final String PROGRAM_NAME_2 = "Program 2";
  private static final Long PROGRAM_ID_1 = 1000L;

  private final ProgramMigrationService service =
      new ProgramMigrationService(
          instanceOf(ApplicationStatusesRepository.class),
          instanceOf(ObjectMapper.class),
          instanceOf(ProgramRepository.class),
          instanceOf(QuestionRepository.class),
          instanceOf(QuestionService.class),
          instanceOf(VersionRepository.class),
          instanceOf(TransactionManager.class));
  ApplicationStatusesRepository applicationStatusesRepository;
  private QuestionRepository questionRepository;
  private TransactionManager transactionManager;
  private VersionRepository versionRepository;

  @Before
  public void setup() {
    applicationStatusesRepository = instanceOf(ApplicationStatusesRepository.class);
    questionRepository = instanceOf(QuestionRepository.class);
    transactionManager = instanceOf(TransactionManager.class);
    versionRepository = instanceOf(VersionRepository.class);
  }

  @Test
  public void serialize_mapperThrowsException_returnsError() throws JsonProcessingException {
    ObjectMapper badObjectMapper = spy(instanceOf(ObjectMapper.class));
    ObjectWriter badObjectWriter = spy(badObjectMapper.writerWithDefaultPrettyPrinter());
    when(badObjectMapper.writerWithDefaultPrettyPrinter()).thenReturn(badObjectWriter);
    when(badObjectWriter.writeValueAsString(any()))
        .thenThrow(new JsonProcessingException("Test exception!") {});

    ProgramMigrationService badMapperService =
        new ProgramMigrationService(
            instanceOf(ApplicationStatusesRepository.class),
            badObjectMapper,
            instanceOf(ProgramRepository.class),
            instanceOf(QuestionRepository.class),
            instanceOf(QuestionService.class),
            instanceOf(VersionRepository.class),
            instanceOf(TransactionManager.class));

    ErrorAnd<String, String> result =
        badMapperService.serialize(
            ProgramBuilder.newActiveProgram().build().getProgramDefinition(), ImmutableList.of());

    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors()).hasSize(1);
    String error = result.getErrors().stream().findFirst().get();
    assertThat(error).contains("Program could not be serialized");
    assertThat(error).contains("Test exception!");
  }

  @Test
  public void serialize_normalMapper_returnsStringWithProgramDefAndQuestionDefs() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("Active Program")
            .withProgramType(ProgramType.DEFAULT)
            .withBlock("Block A")
            .withBlock("Block B")
            .buildDefinition();

    QuestionDefinition addressQuestionDefinition =
        testQuestionBank.addressApplicantAddress().getQuestionDefinition();
    QuestionDefinition nameQuestionDefinition =
        testQuestionBank.nameApplicantName().getQuestionDefinition();
    QuestionDefinition emailQuestionDefinition =
        testQuestionBank.emailApplicantEmail().getQuestionDefinition();

    ImmutableList<QuestionDefinition> questions =
        ImmutableList.of(
            addressQuestionDefinition, nameQuestionDefinition, emailQuestionDefinition);

    ErrorAnd<String, String> result = service.serialize(programDefinition, questions);

    assertThat(result.isError()).isFalse();
    String resultString = result.getResult();
    assertThat(resultString).contains("\"adminName\" : \"Active Program\"");
    assertThat(resultString).contains("\"programType\" : \"DEFAULT\"");
    assertThat(resultString).contains("\"localizedName\"");
    assertThat(resultString).contains("\"en_US\" : \"Active Program\"");
    assertThat(resultString).contains("\"blockDefinitions\" : [");
    assertThat(resultString).contains("\"Block A\"");
    assertThat(resultString).contains("\"Block B\"");
    assertThat(resultString).contains("What is your address?");
    assertThat(resultString).contains("what is your name?");
    assertThat(resultString).contains("What is your Email?");
    // the enumeratorId field should only show up if there is an enumerator question in the programs
    assertFalse(resultString.contains("enumeratorId"));
  }

  @Test
  public void deserialize_malformattedJson_returnsError() {
    ErrorAnd<ProgramMigrationWrapper, String> result =
        service.deserialize("{\"adminName\" : \"admin-name\"");

    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors()).hasSize(1);
    String error = result.getErrors().stream().findFirst().get();
    assertThat(error).contains("JSON is incorrectly formatted");
  }

  @Test
  public void deserialize_notEnoughInfoToCreateProgramDef_returnsError() {
    ErrorAnd<ProgramMigrationWrapper, String> result =
        service.deserialize(
            "{ \"program\" : {\"id\" : 32, \"adminName\" : \"admin-name\","
                + " \"adminDescription\" : \"description\"}}");

    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors()).hasSize(1);
    String error = result.getErrors().stream().findFirst().get();
    assertThat(error).contains("JSON is incorrectly formatted");
  }

  @Test
  public void deserialize_jsonHasAllInfo_returnsResult() {
    ErrorAnd<ProgramMigrationWrapper, String> result =
        service.deserialize(PROGRAM_JSON_WITH_ONE_QUESTION);

    assertThat(result.isError()).isFalse();
    ProgramMigrationWrapper wrapperResult = result.getResult();
    assertThat(wrapperResult.getProgram()).isNotNull();
    assertThat(wrapperResult.getDuplicateQuestionHandlingOptions()).isNotNull();
    assertThat(wrapperResult.getDuplicateQuestionHandlingOptions()).isEmpty();

    ProgramDefinition program = wrapperResult.getProgram();
    QuestionDefinition question = wrapperResult.getQuestions().get(0);

    assertThat(program.adminName()).isEqualTo("minimal-sample-program");
    assertThat(program.adminDescription()).isEqualTo("desc");
    assertThat(program.externalLink()).isEqualTo("https://github.com/civiform/civiform");
    assertThat(program.displayMode()).isEqualTo(DisplayMode.PUBLIC);
    assertThat(program.programType()).isEqualTo(ProgramType.DEFAULT);
    assertThat(question.getName()).isEqualTo("Name");
    assertThat(question.getDescription()).isEqualTo("The applicant's name");
    assertThat(question.getQuestionText().getDefault())
        .isEqualTo("Please enter your first and last name");
  }

  @Test
  public void deserialize_badDuplicateHandlingOptionIncluded_returnsError() {
    ErrorAnd<ProgramMigrationWrapper, String> result =
        service.deserialize(PROGRAM_JSON_WITH_ONE_QUESTION, ImmutableMap.of("NAME", "FOO"));

    assertThat(result.isError()).isTrue();
    assertThat(result.getErrors()).hasSize(1);
    assertThat(result.getErrors())
        .contains(
            "JSON is incorrectly formatted: No enum constant"
                + " controllers.admin.ProgramMigrationWrapper.DuplicateQuestionHandlingOption.FOO");
  }

  @Test
  public void deserialize_duplicateHandlingOptionsIncluded_parsesEverything() {
    ErrorAnd<ProgramMigrationWrapper, String> result =
        service.deserialize(
            PROGRAM_JSON_WITH_ONE_QUESTION,
            ImmutableMap.of("NAME", OVERWRITE_EXISTING, "Text", CREATE_DUPLICATE));

    assertThat(result.isError()).isFalse();
    ProgramMigrationWrapper wrapperResult = result.getResult();
    assertThat(wrapperResult.getProgram()).isNotNull();
    assertThat(wrapperResult.getDuplicateQuestionHandlingOptions()).isNotNull();

    ProgramDefinition program = wrapperResult.getProgram();
    QuestionDefinition question = wrapperResult.getQuestions().get(0);
    ImmutableMap<String, ProgramMigrationWrapper.DuplicateQuestionHandlingOption>
        duplicateQuestionHandlingOptions = wrapperResult.getDuplicateQuestionHandlingOptions();

    assertThat(program.adminName()).isEqualTo("minimal-sample-program");
    assertThat(program.adminDescription()).isEqualTo("desc");
    assertThat(program.externalLink()).isEqualTo("https://github.com/civiform/civiform");
    assertThat(program.displayMode()).isEqualTo(DisplayMode.PUBLIC);
    assertThat(program.programType()).isEqualTo(ProgramType.DEFAULT);
    assertThat(question.getName()).isEqualTo("Name");
    assertThat(question.getDescription()).isEqualTo("The applicant's name");
    assertThat(question.getQuestionText().getDefault())
        .isEqualTo("Please enter your first and last name");
    assertThat(duplicateQuestionHandlingOptions).hasSize(2);
    assertThat(duplicateQuestionHandlingOptions.get("NAME"))
        .isEqualTo(ProgramMigrationWrapper.DuplicateQuestionHandlingOption.OVERWRITE_EXISTING);
    assertThat(duplicateQuestionHandlingOptions.get("Text"))
        .isEqualTo(ProgramMigrationWrapper.DuplicateQuestionHandlingOption.CREATE_DUPLICATE);
  }

  @Test
  public void maybeOverwriteQuestionName_onlyOverwritesQuestionNamesIfAMatchIsFound() {
    ImmutableList<QuestionDefinition> questionsOne =
        service.deserialize(PROGRAM_JSON_WITH_PREDICATES).getResult().getQuestions();
    // Execute in a transaction so bulkCreateQuestions doesn't throw
    transactionManager.execute(
        () -> {
          questionRepository.bulkCreateQuestions(questionsOne);
        });

    // There are two questions in PROGRAM_JSON_WITH_PREDICATES: "id-test" and "text test"
    // We want to update the admin name of one of them so we can test that it is not changed by the
    // method
    String UPDATED_JSON = PROGRAM_JSON_WITH_PREDICATES.replace("text test", "new text test");

    ImmutableList<QuestionDefinition> questionsTwo =
        service.deserialize(UPDATED_JSON).getResult().getQuestions();
    ImmutableMap<String, QuestionDefinition> updatedQuestions =
        service.maybeOverwriteQuestionName(questionsTwo);

    // "id-test" should have been updated by the method
    assertThat(updatedQuestions.get("id-test").getName()).isEqualTo("id-test -_- a");
    // "new text test" should have not have been changed
    assertThat(updatedQuestions.get("new text test").getName()).isEqualTo("new text test");
  }

  @Test
  public void generateUniqueAdminName_generatesCorrectAdminNames() {
    resourceCreator.insertQuestion("name-question");
    resourceCreator.insertQuestion("name-question -_- a");
    resourceCreator.insertQuestion("name-question -_- b");

    String newAdminName = service.generateUniqueAdminName("name-question", new ArrayList<>());
    assertThat(newAdminName).isEqualTo("name-question -_- c");
    // Even though there is no existing match, this method should still return a unique name, since
    // it assumed that the caller has checked for an existing match before calling.
    String unmatchedAdminName =
        service.generateUniqueAdminName("admin-name-unmatched", new ArrayList<>());
    assertThat(unmatchedAdminName).isEqualTo("admin-name-unmatched -_- a");
  }

  @Test
  public void generateUniqueAdminName_generatesCorrectAdminNamesForAdminNamesWithSuffixes() {
    resourceCreator.insertQuestion("name-question");
    resourceCreator.insertQuestion("name-question -_- a");
    resourceCreator.insertQuestion("name-question -_- b");

    String newAdminName = service.generateUniqueAdminName("name-question -_- a", new ArrayList<>());
    assertThat(newAdminName).isEqualTo("name-question -_- c");
  }

  @Test
  public void
      generateUniqueAdminName_generatesCorrectAdminNamesWhenAlreadyGeneratedNameMightConflict() {
    List<String> namesSoFar = List.of("name-question -_- a", "name-question -_- b");

    String newAdminName = service.generateUniqueAdminName("name-question -_- a", namesSoFar);
    assertThat(newAdminName).isEqualTo("name-question -_- c");
  }

  @Test
  public void prepForExport_clearsNotificationPreferences() {
    ProgramDefinition program =
        ProgramBuilder.newActiveProgram()
            .setNotificationPreferences(
                ImmutableList.of(ProgramNotificationPreference.EMAIL_PROGRAM_ADMIN_ALL_SUBMISSIONS))
            .build()
            .getProgramDefinition();

    ProgramDefinition output = service.prepForExport(program);

    assertThat(output.notificationPreferences()).isEmpty();
  }

  @Test
  public void prepForExport_clearsTiGroupAcls() {
    ImmutableList<Long> tiGroups = ImmutableList.of(1L, 2L, 3L);
    ProgramAcls programAcls = new ProgramAcls(new HashSet<>(tiGroups));
    ProgramDefinition program =
        ProgramBuilder.newActiveProgram().withAcls(programAcls).build().getProgramDefinition();

    ProgramDefinition output = service.prepForExport(program);

    assertThat(output.acls().getTiProgramViewAcls()).isEmpty();
  }

  @Test
  public void prepForExport_clearsCategories() {
    ImmutableMap<Locale, String> translations =
        ImmutableMap.of(
            Lang.forCode("en-US").toLocale(), "Health", Lang.forCode("es-US").toLocale(), "Salud");
    CategoryModel category = new CategoryModel(translations);
    category.save();

    ProgramDefinition program =
        ProgramBuilder.newActiveProgram()
            .withCategories(ImmutableList.of(category))
            .build()
            .getProgramDefinition();

    ProgramDefinition output = service.prepForExport(program);

    assertThat(output.categories()).isEmpty();
  }

  @Test
  public void prepForExport_clearsPreScreenerSetting() {
    ProgramDefinition program =
        ProgramBuilder.newActiveProgram()
            .withProgramType(ProgramType.PRE_SCREENER_FORM)
            .build()
            .getProgramDefinition();

    ProgramDefinition output = service.prepForExport(program);

    assertThat(output.programType()).isEqualTo(ProgramType.DEFAULT);
  }

  @Test
  public void prepForImport_setNotificationPreferencesToDefaults() {
    ProgramDefinition program = ProgramBuilder.newActiveProgram().build().getProgramDefinition();

    ProgramDefinition output = service.prepForImport(program);

    assertThat(output.notificationPreferences())
        .containsExactlyInAnyOrderElementsOf(ProgramNotificationPreference.getDefaults());
  }

  @Test
  public void saveImportedProgram_withNullQuestionDefinitions_savesProgram() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram(PROGRAM_NAME_1)
            .withProgramType(ProgramType.DEFAULT)
            .withBlock("Block A")
            .buildDefinition();

    ErrorAnd<ProgramModel, String> savedProgram =
        service.saveImportedProgram(programDefinition, null, ImmutableMap.of());

    assertThat(savedProgram.hasResult()).isTrue();
    assertThat(savedProgram.getResult().getProgramDefinition().adminName())
        .isEqualTo(PROGRAM_NAME_1);
    assertThat(savedProgram.getResult().getProgramDefinition().blockDefinitions()).hasSize(1);
    assertThat(savedProgram.getResult().getProgramDefinition().blockDefinitions().get(0).name())
        .isEqualTo("Block A");
  }

  @Test
  public void saveImportedProgram_duplicateHandlingEnabled_renamesSpecifiedQuestions()
      throws Exception {
    ProgramDefinition programDefinition =
        ProgramBuilder.newProgram("program1", PROGRAM_ID_1)
            .withBlock("Block A")
            .withRequiredQuestionDefinition(QUESTION_1)
            .withRequiredQuestionDefinition(QUESTION_2)
            .buildDefinition();

    ErrorAnd<ProgramModel, String> savedProgram =
        service.saveImportedProgram(
            programDefinition,
            QUESTIONS_1_2,
            ImmutableMap.of(
                QUESTION_1_NAME,
                ProgramMigrationWrapper.DuplicateQuestionHandlingOption.CREATE_DUPLICATE));

    ImmutableList<ProgramQuestionDefinition> savedQs =
        savedProgram
            .getResult()
            .getProgramDefinition()
            .getBlockDefinitionByIndex(0)
            .get()
            .programQuestionDefinitions();
    QuestionModel savedQuestion1 =
        questionRepository.lookupQuestion(savedQs.get(0).id()).toCompletableFuture().join().get();
    savedQuestion1.loadQuestionDefinition();
    assertThat(savedQuestion1.getQuestionDefinition().getName())
        .isEqualTo(QUESTION_1_NAME + " -_- a");
    QuestionModel savedQuestion2 =
        questionRepository.lookupQuestion(savedQs.get(1).id()).toCompletableFuture().join().get();
    savedQuestion2.loadQuestionDefinition();
    assertThat(savedQuestion2.getQuestionDefinition().getName()).isEqualTo(QUESTION_2_NAME);
  }

  @Test
  public void saveImportedProgram_duplicateHandlingEnabled_newAndDuplicateNamesCollide()
      throws Exception {
    // Q3 has the same name as Q1 would have after adding a deduping suffix
    QuestionDefinition question3 =
        createQuestionDefinition(
            QUESTION_1_NAME + " -_- a", 3L, QuestionType.ADDRESS, Optional.empty());
    ProgramDefinition programDefinition =
        ProgramBuilder.newProgram("program1", PROGRAM_ID_1)
            .withBlock("Block A")
            .withRequiredQuestionDefinition(QUESTION_1)
            .withRequiredQuestionDefinition(question3)
            .buildDefinition();

    ErrorAnd<ProgramModel, String> savedProgram =
        service.saveImportedProgram(
            programDefinition,
            ImmutableList.of(QUESTION_1, question3),
            ImmutableMap.of(
                QUESTION_1_NAME,
                ProgramMigrationWrapper.DuplicateQuestionHandlingOption.CREATE_DUPLICATE));

    ImmutableList<ProgramQuestionDefinition> savedQs =
        savedProgram
            .getResult()
            .getProgramDefinition()
            .getBlockDefinitionByIndex(0)
            .get()
            .programQuestionDefinitions();
    QuestionModel savedQuestion1 =
        questionRepository.lookupQuestion(savedQs.get(0).id()).toCompletableFuture().join().get();
    savedQuestion1.loadQuestionDefinition();
    // Q1 was renamed with `-_- b` since Q3 had already "reserved" `-_- a`
    assertThat(savedQuestion1.getQuestionDefinition().getName())
        .isEqualTo(QUESTION_1_NAME + " -_- b");
    assertThat(savedQuestion1.getQuestionDefinition().getQuestionType())
        .isEqualTo(QuestionType.TEXT);
    QuestionModel savedQuestion2 =
        questionRepository.lookupQuestion(savedQs.get(1).id()).toCompletableFuture().join().get();
    savedQuestion2.loadQuestionDefinition();
    assertThat(savedQuestion2.getQuestionDefinition().getName())
        .isEqualTo(QUESTION_1_NAME + " -_- a");
    assertThat(savedQuestion2.getQuestionDefinition().getQuestionType())
        .isEqualTo(QuestionType.ADDRESS);
  }

  @Test
  public void
      saveImportedProgram_duplicateHandlingEnabled_overwritesWithExistingDrafts_returnsError()
          throws Exception {
    ProgramDefinition programDefinition =
        ProgramBuilder.newProgram("program1", PROGRAM_ID_1)
            .withBlock("Block A")
            .withRequiredQuestionDefinition(QUESTION_1)
            .withRequiredQuestionDefinition(QUESTION_2)
            .buildDefinition();
    // Create an existing draft program
    ProgramBuilder.newDraftProgram(PROGRAM_NAME_2).withProgramType(ProgramType.DEFAULT).build();

    ErrorAnd<ProgramModel, String> savedProgram =
        service.saveImportedProgram(
            programDefinition,
            QUESTIONS_1_2,
            ImmutableMap.of(
                QUESTION_1_NAME,
                ProgramMigrationWrapper.DuplicateQuestionHandlingOption.OVERWRITE_EXISTING));

    // Verify error
    assertThat(savedProgram.hasResult()).isFalse();
    assertThat(savedProgram.getErrors())
        .containsExactly(
            "Overwriting question definitions is only supported when there are no existing drafts."
                + " Please publish all drafts and try again.");
  }

  @Test
  public void saveImportedProgram_duplicateHandlingEnabled_overwritesAndReusesSpecifiedQuestions()
      throws Exception {
    QuestionModel question1 = new QuestionModel(QUESTION_1);
    question1.addVersion(versionRepository.getActiveVersion()).save();
    QuestionModel question2 = new QuestionModel(QUESTION_2);
    question2.addVersion(versionRepository.getActiveVersion()).save();
    ProgramDefinition programDefinition =
        ProgramBuilder.newProgram("program1", PROGRAM_ID_1)
            .withBlock("Block A")
            .withRequiredQuestionDefinition(QUESTION_1)
            .withRequiredQuestionDefinition(QUESTION_2)
            .withRequiredQuestionDefinition(QUESTION_3)
            .buildDefinition();

    // Verify the "current" definitions of Questions 1 & 2
    ImmutableMap<String, QuestionDefinition> currentQuestions =
        questionRepository.getExistingQuestions(
            ImmutableSet.of(QUESTION_1_NAME, QUESTION_2_NAME, QUESTION_3_NAME));
    assertThat(currentQuestions).hasSize(2);
    assertThat(currentQuestions.get(QUESTION_1_NAME).getQuestionText())
        .isEqualTo(QUESTION_1.getQuestionText());
    assertThat(currentQuestions.get(QUESTION_2_NAME).getQuestionText())
        .isEqualTo(QUESTION_2.getQuestionText());
    assertThat(versionRepository.getDraftVersion()).isEmpty();

    service.saveImportedProgram(
        programDefinition,
        ImmutableList.of(
            new QuestionDefinitionBuilder()
                .setName(QUESTION_1_NAME)
                .setId(QUESTION_1.getId())
                .setQuestionType(QuestionType.TEXT)
                .setQuestionText(LocalizedStrings.withDefaultValue("Question one new text"))
                .setDescription(QUESTION_1_NAME)
                .build(),
            new QuestionDefinitionBuilder()
                .setName(QUESTION_2_NAME)
                .setId(QUESTION_2.getId())
                .setQuestionType(QuestionType.TEXT)
                .setQuestionText(LocalizedStrings.withDefaultValue("Question two new text"))
                .setDescription(QUESTION_2_NAME)
                .build(),
            QUESTION_3),
        ImmutableMap.of(
            QUESTION_1_NAME,
            ProgramMigrationWrapper.DuplicateQuestionHandlingOption.OVERWRITE_EXISTING,
            QUESTION_2_NAME,
            ProgramMigrationWrapper.DuplicateQuestionHandlingOption.USE_EXISTING));

    currentQuestions =
        questionRepository.getExistingQuestions(
            ImmutableSet.of(QUESTION_1_NAME, QUESTION_2_NAME, QUESTION_3_NAME));
    assertThat(currentQuestions).hasSize(3);
    assertThat(currentQuestions.get(QUESTION_1_NAME).getQuestionText().getDefault())
        .isEqualTo("Question one new text");
    assertThat(currentQuestions.get(QUESTION_2_NAME).getQuestionText())
        .isEqualTo(QUESTION_2.getQuestionText());
    assertThat(currentQuestions.get(QUESTION_3_NAME).getQuestionText())
        .isEqualTo(QUESTION_3.getQuestionText());
  }

  @Test
  public void saveImportedProgram_duplicateHandlingEnabled_executesWithinTransaction()
      throws Exception {
    ProgramDefinition programDefinition =
        ProgramBuilder.newProgram("program1", PROGRAM_ID_1)
            .withBlock("Block A")
            .withRequiredQuestionDefinition(QUESTION_1)
            .buildDefinition();
    ProgramDefinition conflictingProgramDefinition =
        ProgramBuilder.newProgram("program1", PROGRAM_ID_1)
            .withBlock("Block A")
            .withRequiredQuestionDefinition(QUESTION_2)
            .buildDefinition();

    service.saveImportedProgram(programDefinition, ImmutableList.of(QUESTION_1), ImmutableMap.of());

    ImmutableMap<String, QuestionDefinition> currentQuestions =
        questionRepository.getExistingQuestions(ImmutableSet.of(QUESTION_1_NAME, QUESTION_2_NAME));
    assertThat(currentQuestions).hasSize(1);

    Exception e =
        assertThrows(
            RuntimeException.class,
            () ->
                service.saveImportedProgram(
                    conflictingProgramDefinition, ImmutableList.of(QUESTION_2), ImmutableMap.of()));

    assertThat(e).hasMessageContaining("Program program1 already has a draft!");
    // Confirm that the questions (which are written before attempting to save the program) were
    // rolled back since the transaction failed.
    currentQuestions =
        questionRepository.getExistingQuestions(ImmutableSet.of(QUESTION_1_NAME, QUESTION_2_NAME));
    assertThat(currentQuestions).hasSize(1);
  }

  @Test
  public void saveImportedProgram_createsStatusDefinitions() throws Exception {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram(PROGRAM_NAME_1)
            .withProgramType(ProgramType.DEFAULT)
            .buildDefinition();
    QuestionDefinition question =
        createQuestionDefinition(QUESTION_1_NAME, 1L, QuestionType.TEXT, Optional.empty());
    ImmutableList<QuestionDefinition> questionDefinitions = ImmutableList.of(question);

    service.saveImportedProgram(programDefinition, questionDefinitions, ImmutableMap.of());

    assertThat(applicationStatusesRepository.lookupActiveStatusDefinitions(PROGRAM_NAME_1))
        .isNotNull();
  }

  @Test
  public void updateEnumeratorIdsAndSaveQuestions_noEnumeratorIds_savesProgramAndQuestions()
      throws Exception {
    // Create question definitions without enumerator IDs
    ImmutableList<QuestionDefinition> questionsToWrite = ImmutableList.of(QUESTION_1, QUESTION_2);

    // Execute in a transaction so bulkCreateQuestions doesn't throw
    ImmutableMap<String, QuestionDefinition> result =
        transactionManager.execute(
            () -> {
              return service.updateEnumeratorIdsAndSaveQuestions(
                  questionsToWrite,
                  ImmutableList.of(),
                  ImmutableMap.of(1L, QUESTION_1, 2L, QUESTION_2));
            });

    assertThat(result).hasSize(2);
    assertThat(result).containsKeys(QUESTION_1_NAME, QUESTION_2_NAME);
    assertThat(result.get(QUESTION_1_NAME).getName()).isEqualTo(QUESTION_1.getName());
    assertThat(result.get(QUESTION_2_NAME).getName()).isEqualTo(QUESTION_2.getName());
    assertThat(result.get(QUESTION_1_NAME).getEnumeratorId()).isEmpty();
    assertThat(result.get(QUESTION_2_NAME).getEnumeratorId()).isEmpty();
  }

  @Test
  public void
      updateEnumeratorIdsAndSaveQuestions_withEnumeratorIds_updatesProgramQuestionsBeforeSaving()
          throws Exception {
    // Create child questions with enumerator IDs
    QuestionDefinition childQuestion1 =
        createQuestionDefinition(
            QUESTION_2_NAME, 2L, QuestionType.TEXT, Optional.of(QUESTION_1.getId()));
    QuestionDefinition childQuestion2 =
        createQuestionDefinition(
            QUESTION_3_NAME, 3L, QuestionType.TEXT, Optional.of(QUESTION_1.getId()));
    QuestionDefinition childQuestion3 =
        createQuestionDefinition(
            QUESTION_4_NAME, 4L, QuestionType.TEXT, Optional.of(QUESTION_1.getId()));
    QuestionModel childQuestion3model = new QuestionModel(childQuestion3);
    childQuestion3model.addVersion(versionRepository.getActiveVersion()).save();

    ImmutableList<QuestionDefinition> questionsToWrite =
        ImmutableList.of(QUESTION_1, childQuestion1, childQuestion2);

    // Execute in a transaction so bulkCreateQuestions doesn't throw
    ImmutableMap<String, QuestionDefinition> result =
        transactionManager.execute(
            () -> {
              return service.updateEnumeratorIdsAndSaveQuestions(
                  questionsToWrite,
                  /* questionsToReuseFromBank= */ ImmutableList.of(childQuestion3),
                  ImmutableMap.of(
                      1L, QUESTION_1, 2L, childQuestion1, 3L, childQuestion2, 4L, childQuestion3));
            });

    assertThat(result).hasSize(4);
    assertThat(result)
        .containsKeys(QUESTION_1_NAME, QUESTION_2_NAME, QUESTION_3_NAME, QUESTION_4_NAME);
    assertThat(result.get(QUESTION_1_NAME).getName()).isEqualTo(QUESTION_1.getName());
    assertThat(result.get(QUESTION_2_NAME).getName()).isEqualTo(childQuestion1.getName());
    assertThat(result.get(QUESTION_3_NAME).getName()).isEqualTo(childQuestion2.getName());
    assertThat(result.get(QUESTION_1_NAME).getEnumeratorId()).isEmpty();
    assertThat(result.get(QUESTION_2_NAME).getEnumeratorId())
        .hasValue(result.get(QUESTION_1_NAME).getId());
    assertThat(result.get(QUESTION_3_NAME).getEnumeratorId())
        .hasValue(result.get(QUESTION_1_NAME).getId());
    assertThat(result.get(QUESTION_4_NAME).getEnumeratorId())
        .hasValueSatisfying(
            id -> {
              // Since we are reusing the child question, the enumerator ID should not be the newly
              // saved parent question's ID.
              // Note: this is disallowed further upstream, in the
              // validateEnumeratorAndRepeatedQuestions method
              assertThat(id).isNotEqualTo(result.get(QUESTION_1_NAME).getId());
            });
  }

  @Test
  public void addProgramsToDraft_withDuplicateHandling_addsOnlyRelevantProgramsToDraft() {
    // Create question & program definitions, and automatically save them to the DB
    QuestionModel question1 = resourceCreator.insertQuestion(QUESTION_1_NAME);
    question1.addVersion(versionRepository.getActiveVersion()).save();
    ProgramBuilder.newActiveProgram(PROGRAM_NAME_1)
        .withProgramType(ProgramType.DEFAULT)
        .withBlock("Block 1")
        .withRequiredQuestion(question1)
        .buildDefinition();
    ProgramBuilder.newDraftProgram(PROGRAM_NAME_2)
        .withProgramType(ProgramType.DEFAULT)
        .withBlock("Block 1")
        .withRequiredQuestion(question1)
        .buildDefinition();
    ProgramBuilder.newActiveProgram("Program 3")
        .withProgramType(ProgramType.DEFAULT)
        .buildDefinition();

    // Verify that there is only one draft program, and no draft questions
    assertThat(versionRepository.getDraftVersion()).isNotEmpty();
    assertThat(versionRepository.isDraft(question1)).isFalse();
    assertThat(
            versionRepository.getProgramNamesForVersion(versionRepository.getDraftVersion().get()))
        .containsExactly(PROGRAM_NAME_2);

    service.addProgramsToDraft(/* overwrittenAdminNames= */ ImmutableList.of(QUESTION_1_NAME));

    // Verify that the draft version contains both programs that reference the question
    assertThat(versionRepository.getDraftVersion()).isPresent();
    assertThat(
            versionRepository.getProgramNamesForVersion(versionRepository.getDraftVersion().get()))
        .containsExactlyInAnyOrder(PROGRAM_NAME_1, PROGRAM_NAME_2);
  }

  @Test
  public void validateQuestionKeyUniqueness_noConflicts_doesNotThrow() {
    service.validateQuestionKeyUniqueness(QUESTIONS_1_2);
  }

  @Test
  public void validateQuestionKeyUniqueness_importTwoConflictingKeys_throws() {
    QuestionDefinition conflictingQuestion =
        createQuestionDefinition(
            QUESTION_1_NAME + "01_023", 2L, QuestionType.TEXT, Optional.empty());
    ImmutableList<QuestionDefinition> questions = ImmutableList.of(QUESTION_1, conflictingQuestion);

    Exception e =
        assertThrows(
            RuntimeException.class, () -> service.validateQuestionKeyUniqueness(questions));

    assertThat(e)
        .hasMessageContaining(
            "Question keys (Admin IDs with non-letter characters removed and spaces transformed to"
                + " underscores) must be unique. Duplicate question keys found:");
  }

  @Test
  public void validateQuestionKeyUniqueness_existingKeyConflictHasDifferentName_throws() {
    resourceCreator.insertQuestion(QUESTION_1_NAME);
    QuestionDefinition conflictingQuestion =
        createQuestionDefinition(
            QUESTION_1_NAME + "01_023", 2L, QuestionType.TEXT, Optional.empty());
    ImmutableList<QuestionDefinition> questions = ImmutableList.of(conflictingQuestion);

    Exception e =
        assertThrows(
            RuntimeException.class, () -> service.validateQuestionKeyUniqueness(questions));

    assertThat(e)
        .hasMessageContaining(
            "Please change the Admin ID so it either matches the existing one, or compiles to a"
                + " different question key.");
  }

  @Test
  public void validateEnumeratorAndRepeatedQuestions_noConflicts_doesNotThrow() {
    service.validateEnumeratorAndRepeatedQuestions(
        ImmutableList.of(ENUMERATOR, REPEATED),
        /* overwrittenQuestions= */ ImmutableList.of(),
        /* duplicatedQuestions= */ ImmutableList.of(ENUMERATOR.getName(), REPEATED.getName()),
        /* reusedQuestions= */ ImmutableList.of());
  }

  @Test
  public void validateEnumeratorAndRepeatedQuestions_reuseRepeatedWithDuplicateEnumerator_throws() {
    Exception e =
        assertThrows(
            RuntimeException.class,
            () ->
                service.validateEnumeratorAndRepeatedQuestions(
                    ImmutableList.of(ENUMERATOR, REPEATED),
                    /* overwrittenQuestions= */ ImmutableList.of(),
                    /* duplicatedQuestions= */ ImmutableList.of(ENUMERATOR.getName()),
                    /* reusedQuestions= */ ImmutableList.of(REPEATED.getName())));
    assertThat(e)
        .hasMessageContaining(
            String.format(
                "Cannot overwrite/reuse repeated question %s because enumerator %s is duplicated",
                REPEATED.getName(), ENUMERATOR.getName()));
  }

  @Test
  public void validateQuestions_validYesNoQuestion_noErrors() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test-program")
            .withBlock("Test Block")
            .withRequiredQuestionDefinition(VALID_YES_NO_QUESTION)
            .buildDefinition();

    ImmutableSet<CiviFormError> errors =
        service.validateQuestions(
            programDefinition, ImmutableList.of(VALID_YES_NO_QUESTION), ImmutableList.of());

    assertThat(errors).isEmpty();
  }

  @Test
  public void validateQuestions_invalidYesNoQuestion_returnsError() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test-program")
            .withBlock("Test Block")
            .withRequiredQuestionDefinition(INVALID_YES_NO_QUESTION)
            .buildDefinition();

    ImmutableSet<CiviFormError> errors =
        service.validateQuestions(
            programDefinition, ImmutableList.of(INVALID_YES_NO_QUESTION), ImmutableList.of());

    assertThat(errors).hasSize(1);
    assertThat(errors.iterator().next().message())
        .contains(
            "YES_NO question '" + INVALID_YES_NO_NAME + "' contains invalid option 'absolutely'");
    assertThat(errors.iterator().next().message())
        .contains("Only 'yes', 'no', 'maybe', and 'not-sure' options are allowed.");
  }

  @Test
  public void validateQuestions_mixedQuestionsWithInvalidYesNo_returnsYesNoError() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test-program")
            .withBlock("Test Block")
            .withRequiredQuestionDefinition(QUESTION_1)
            .withRequiredQuestionDefinition(INVALID_YES_NO_QUESTION)
            .buildDefinition();

    ImmutableSet<CiviFormError> errors =
        service.validateQuestions(
            programDefinition,
            ImmutableList.of(QUESTION_1, INVALID_YES_NO_QUESTION),
            ImmutableList.of());

    assertThat(errors).hasSize(1);
    assertThat(errors.iterator().next().message())
        .contains(
            "YES_NO question '" + INVALID_YES_NO_NAME + "' contains invalid option 'absolutely'");
  }

  @Test
  public void validateQuestions_withoutYesNoQuestions_noErrors() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test-program")
            .withBlock("Test Block")
            .withRequiredQuestionDefinition(QUESTION_1)
            .withRequiredQuestionDefinition(DROPDOWN_QUESTION)
            .buildDefinition();

    ImmutableSet<CiviFormError> errors =
        service.validateQuestions(
            programDefinition, ImmutableList.of(QUESTION_1, DROPDOWN_QUESTION), ImmutableList.of());

    assertThat(errors).isEmpty();
  }

  @Test
  public void saveImportedProgram_validYesNoQuestion_succeeds() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test-program")
            .withBlock("Test Block")
            .withRequiredQuestionDefinition(MINIMAL_VALID_YES_NO_QUESTION)
            .buildDefinition();

    ErrorAnd<ProgramModel, String> result =
        service.saveImportedProgram(
            programDefinition, ImmutableList.of(MINIMAL_VALID_YES_NO_QUESTION), ImmutableMap.of());

    assertThat(result.isError()).isFalse();
    assertThat(result.hasResult()).isTrue();
  }

  @Test
  public void saveImportedProgram_nonYesNoQuestionWithCustomOptions_succeeds() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test-program")
            .withBlock("Test Block")
            .withRequiredQuestionDefinition(DROPDOWN_QUESTION)
            .buildDefinition();

    ErrorAnd<ProgramModel, String> result =
        service.saveImportedProgram(
            programDefinition, ImmutableList.of(DROPDOWN_QUESTION), ImmutableMap.of());

    assertThat(result.isError()).isFalse();
    assertThat(result.hasResult()).isTrue();
  }
}
