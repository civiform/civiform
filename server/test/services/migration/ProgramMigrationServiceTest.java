package services.migration;

import static controllers.admin.AdminImportControllerTest.PROGRAM_JSON_WITH_ONE_QUESTION;
import static controllers.admin.AdminImportControllerTest.PROGRAM_JSON_WITH_PREDICATES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import auth.ProgramAcls;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import repository.VersionRepository;
import services.ErrorAnd;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramType;
import services.question.exceptions.UnsupportedQuestionTypeException;
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
  private static final String PROGRAM_NAME_1 = "Program 1";
  private static final Long PROGRAM_ID_1 = 1000L;
  private final ProgramMigrationService service =
      new ProgramMigrationService(
          instanceOf(ApplicationStatusesRepository.class),
          instanceOf(ObjectMapper.class),
          instanceOf(ProgramRepository.class),
          instanceOf(QuestionRepository.class),
          instanceOf(VersionRepository.class));
  ApplicationStatusesRepository applicationStatusesRepository;
  private QuestionRepository questionRepository;
  private VersionRepository versionRepository;

  @Before
  public void setup() {
    applicationStatusesRepository = instanceOf(ApplicationStatusesRepository.class);
    questionRepository = instanceOf(QuestionRepository.class);
    versionRepository = instanceOf(VersionRepository.class);
  }

  @Test
  public void serialize_mapperThrowsException_returnsError() throws JsonProcessingException {
    ObjectMapper badObjectMapper = spy(new ObjectMapper());
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
            instanceOf(VersionRepository.class));

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
    questionRepository.bulkCreateQuestions(questionsOne);

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
  public void findUniqueAdminName_generatesCorrectAdminNames() {
    resourceCreator.insertQuestion("name-question");
    resourceCreator.insertQuestion("name-question -_- a");
    resourceCreator.insertQuestion("name-question -_- b");

    String newAdminName = service.findUniqueAdminName("name-question", new ArrayList<>());
    assertThat(newAdminName).isEqualTo("name-question -_- c");
    String unmatchedAdminName =
        service.findUniqueAdminName("admin-name-unmatched", new ArrayList<>());
    assertThat(unmatchedAdminName).isEqualTo("admin-name-unmatched");
  }

  @Test
  public void findUniqueAdminName_generatesCorrectAdminNamesForAdminNamesWithSuffixes() {
    resourceCreator.insertQuestion("name-question");
    resourceCreator.insertQuestion("name-question -_- a");
    resourceCreator.insertQuestion("name-question -_- b");

    String newAdminName = service.findUniqueAdminName("name-question -_- a", new ArrayList<>());
    assertThat(newAdminName).isEqualTo("name-question -_- c");
  }

  @Test
  public void
      findUniqueAdminName_generatesCorrectAdminNamesWhenAlreadyGeneratedNameMightConflict() {
    List<String> namesSoFar = List.of("name-question -_- a", "name-question -_- b");

    String newAdminName = service.findUniqueAdminName("name-question -_- a", namesSoFar);
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
            .withProgramType(ProgramType.COMMON_INTAKE_FORM)
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

    // Method under test
    ProgramModel savedProgram = service.saveImportedProgram(programDefinition, null, false, false);

    assertThat(savedProgram).isNotNull();
    assertThat(savedProgram.getProgramDefinition().adminName()).isEqualTo(PROGRAM_NAME_1);
    assertThat(savedProgram.getProgramDefinition().blockDefinitions()).hasSize(1);
    assertThat(savedProgram.getProgramDefinition().blockDefinitions().get(0).name())
        .isEqualTo("Block A");
  }

  @Test
  public void saveImportedProgram_withDuplicateHandlingEnabled_renamesQuestions() throws Exception {
    QuestionDefinition question1 = createTextQuestion(QUESTION_1_NAME, 1L);
    QuestionDefinition question2 = createTextQuestion(QUESTION_2_NAME, 2L);
    ImmutableList<QuestionDefinition> questionDefinitions = ImmutableList.of(question1, question2);
    ProgramDefinition programDefinition =
        ProgramBuilder.newProgram("program1", PROGRAM_ID_1)
            .withBlock("Block A")
            .withRequiredQuestionDefinition(question1)
            .withRequiredQuestionDefinition(question2)
            .buildDefinition();
    // Insert a new text question with the same name to create a conflict
    resourceCreator.insertQuestion(QUESTION_1_NAME);
    assertThat(questionRepository.listQuestions().toCompletableFuture().join()).hasSize(1);

    // Method under test
    ProgramModel savedProgram =
        service.saveImportedProgram(
            programDefinition,
            questionDefinitions,
            true, // withDuplicates
            true // duplicateHandlingEnabled
            );

    // Verify results
    assertThat(savedProgram).isNotNull();
    assertThat(savedProgram.getProgramDefinition().adminName())
        .isEqualTo(programDefinition.adminName());
    ProgramDefinition savedProgramDefinition = savedProgram.getProgramDefinition();
    assertThat(savedProgramDefinition.getBlockDefinitionByIndex(0).get().getQuestionCount())
        .isEqualTo(2);
    ImmutableList<ProgramQuestionDefinition> savedQs =
        savedProgramDefinition.getBlockDefinitionByIndex(0).get().programQuestionDefinitions();
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
  public void saveImportedProgram_withoutDuplicates_putsAllProgramsInDraft() throws Exception {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram(PROGRAM_NAME_1)
            .withProgramType(ProgramType.DEFAULT)
            .withBlock("Block A")
            .buildDefinition();
    QuestionDefinition question1 = createTextQuestion(QUESTION_1_NAME, 1L);
    QuestionDefinition question2 = createTextQuestion(QUESTION_2_NAME, 2L);
    ImmutableList<QuestionDefinition> questionDefinitions = ImmutableList.of(question1, question2);
    // Create an active program that should also be moved to draft
    ProgramModel activeProgram = ProgramBuilder.newActiveProgram("Active Program").build();
    ImmutableList<Long> activeProgramIds =
        versionRepository.getActiveVersion().getPrograms().stream()
            .map(p -> p.id)
            .collect(ImmutableList.toImmutableList());
    assertThat(activeProgramIds)
        .containsExactlyInAnyOrder(activeProgram.id, programDefinition.id());

    // Method under test
    ProgramModel savedProgram =
        service.saveImportedProgram(
            programDefinition,
            questionDefinitions,
            /* withDuplicates= */ false,
            /* duplicateHandlingEnabled= */ false);

    // Verify results
    assertThat(savedProgram).isNotNull();
    assertThat(versionRepository.getDraftVersion()).isPresent();
    ImmutableList<ProgramModel> draftPrograms =
        versionRepository.getDraftVersion().get().getPrograms();
    for (ProgramModel program : draftPrograms) {
      program.loadProgramDefinition();
    }
    assertThat(
            draftPrograms.stream()
                .map(p -> p.getProgramDefinition().adminName())
                .collect(ImmutableList.toImmutableList()))
        .containsExactlyInAnyOrder(PROGRAM_NAME_1, "Active Program");
  }

  @Test
  public void saveImportedProgram_createsStatusDefinitions() throws Exception {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram(PROGRAM_NAME_1)
            .withProgramType(ProgramType.DEFAULT)
            .buildDefinition();
    QuestionDefinition question = createTextQuestion(QUESTION_1_NAME, 1L);
    ImmutableList<QuestionDefinition> questionDefinitions = ImmutableList.of(question);

    // Method under test
    service.saveImportedProgram(programDefinition, questionDefinitions, false, false);

    assertThat(applicationStatusesRepository.lookupActiveStatusDefinitions(PROGRAM_NAME_1))
        .isNotNull();
  }

  @Test
  public void updateEnumeratorIdsAndSaveAllQuestions_noEnumeratorIds_savesProgramAndQuestions()
      throws Exception {
    // Create question definitions without enumerator IDs
    QuestionDefinition question1 = createTextQuestion(QUESTION_1_NAME, 1L);
    QuestionDefinition question2 = createTextQuestion(QUESTION_2_NAME, 2L);
    ImmutableList<QuestionDefinition> questionDefinitions = ImmutableList.of(question1, question2);

    // Method under test
    ImmutableMap<String, QuestionDefinition> result =
        service.updateEnumeratorIdsAndSaveAllQuestions(
            questionDefinitions, ImmutableMap.of(1L, question1, 2L, question2));

    // Verify results
    assertThat(result).hasSize(2);
    assertThat(result).containsKeys(QUESTION_1_NAME, QUESTION_2_NAME);
    assertThat(result.get(QUESTION_1_NAME).getName()).isEqualTo(question1.getName());
    assertThat(result.get(QUESTION_2_NAME).getName()).isEqualTo(question2.getName());
    assertThat(result.get(QUESTION_1_NAME).getEnumeratorId()).isEmpty();
    assertThat(result.get(QUESTION_2_NAME).getEnumeratorId()).isEmpty();
  }

  @Test
  public void
      updateEnumeratorIdsAndSaveAllQuestions_withEnumeratorIds_updatesProgramQuestionsBeforeSaving()
          throws Exception {
    QuestionDefinition parentQuestion = createTextQuestion(QUESTION_1_NAME, 1L);
    // Create child questions with enumerator IDs
    QuestionDefinition childQuestion1 =
        createTextQuestionWithEnumerator(QUESTION_2_NAME, 2L, Optional.of(1L));
    QuestionDefinition childQuestion2 =
        createTextQuestionWithEnumerator(QUESTION_3_NAME, 3L, Optional.of(1L));
    ImmutableList<QuestionDefinition> questionDefinitions =
        ImmutableList.of(parentQuestion, childQuestion1, childQuestion2);

    // Method under test
    ImmutableMap<String, QuestionDefinition> result =
        service.updateEnumeratorIdsAndSaveAllQuestions(
            questionDefinitions,
            ImmutableMap.of(1L, parentQuestion, 2L, childQuestion1, 3L, childQuestion2));

    // Verify results
    assertThat(result).hasSize(3);
    assertThat(result).containsKeys(QUESTION_1_NAME, QUESTION_2_NAME, QUESTION_3_NAME);
    assertThat(result.get(QUESTION_1_NAME).getName()).isEqualTo(parentQuestion.getName());
    assertThat(result.get(QUESTION_2_NAME).getName()).isEqualTo(childQuestion1.getName());
    assertThat(result.get(QUESTION_3_NAME).getName()).isEqualTo(childQuestion2.getName());
    assertThat(result.get(QUESTION_1_NAME).getEnumeratorId()).isEmpty();
    assertThat(result.get(QUESTION_2_NAME).getEnumeratorId())
        .hasValue(result.get(QUESTION_1_NAME).getId());
    assertThat(result.get(QUESTION_3_NAME).getEnumeratorId())
        .hasValue(result.get(QUESTION_1_NAME).getId());
  }

  // Helper methods to create test questions
  private QuestionDefinition createTextQuestion(String name, Long id)
      throws UnsupportedQuestionTypeException {
    return createTextQuestionWithEnumerator(name, id, Optional.empty());
  }

  private QuestionDefinition createTextQuestionWithEnumerator(
      String name, Long id, Optional<Long> enumeratorId) throws UnsupportedQuestionTypeException {
    return new QuestionDefinitionBuilder()
        .setName(name)
        .setId(id)
        .setQuestionType(QuestionType.TEXT)
        .setQuestionText(LocalizedStrings.withDefaultValue(name))
        .setDescription(name)
        .setEnumeratorId(enumeratorId)
        .build();
  }
}
