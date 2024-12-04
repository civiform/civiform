package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.ebean.DB;
import io.ebean.Database;
import models.ApplicationStatusesModel;
import models.ProgramModel;
import models.QuestionModel;
import org.junit.Before;
import org.junit.Test;
import play.data.FormFactory;
import play.mvc.Result;
import repository.ApplicationStatusesRepository;
import repository.ProgramRepository;
import repository.QuestionRepository;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.migration.ProgramMigrationService;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramService;
import services.question.types.QuestionDefinition;
import services.settings.SettingsManifest;
import services.statuses.StatusDefinitions;
import support.ProgramBuilder;
import views.admin.migration.AdminImportView;
import views.admin.migration.AdminImportViewPartial;

public class AdminImportControllerTest extends ResetPostgres {
  private AdminImportController controller;
  private final SettingsManifest mockSettingsManifest = mock(SettingsManifest.class);
  private VersionRepository versionRepository;
  private Database database;

  @Before
  public void setUp() {
    controller =
        new AdminImportController(
            instanceOf(AdminImportView.class),
            instanceOf(AdminImportViewPartial.class),
            instanceOf(FormFactory.class),
            instanceOf(ProfileUtils.class),
            instanceOf(ProgramMigrationService.class),
            mockSettingsManifest,
            instanceOf(VersionRepository.class),
            instanceOf(ProgramRepository.class),
            instanceOf(QuestionRepository.class),
            instanceOf(ApplicationStatusesRepository.class),
            instanceOf(ProgramService.class));
    database = DB.getDefault();
    versionRepository = instanceOf(VersionRepository.class);
  }

  @Test
  public void index_migrationNotEnabled_notFound() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(false);

    Result result = controller.index(fakeRequest());

    assertThat(result.status()).isEqualTo(NOT_FOUND);
    assertThat(contentAsString(result)).contains("import is not enabled");
  }

  @Test
  public void index_migrationEnabled_ok() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);
    ProgramBuilder.newActiveProgram("active-program-1").build();
    ProgramBuilder.newActiveProgram("active-program-2").build();
    ProgramBuilder.newDraftProgram("draft-program").build();

    Result result = controller.index(fakeRequest());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Import a program");
  }

  @Test
  public void hxImportProgram_migrationNotEnabled_notFound() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(false);

    Result result = controller.hxImportProgram(fakeRequest());

    assertThat(result.status()).isEqualTo(NOT_FOUND);
    assertThat(contentAsString(result)).contains("import is not enabled");
  }

  @Test
  public void hxImportProgram_noRequestBody_redirectsToIndex() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    Result result = controller.hxImportProgram(fakeRequest());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue(routes.AdminImportController.index().url());
  }

  @Test
  public void hxImportProgram_malformattedJson_error() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", "{\"adminName : \"admin-name\"}"))
                .build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Error processing JSON");
    assertThat(contentAsString(result)).contains("JSON is incorrectly formatted");
  }

  @Test
  public void hxImportProgram_noDuplicatesEnabled_malformattedJson_error() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);
    when(mockSettingsManifest.getNoDuplicateQuestionsForMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", "{\"adminName : \"admin-name\"}"))
                .build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Error processing JSON");
    assertThat(contentAsString(result)).contains("JSON is incorrectly formatted");
  }

  @Test
  public void hxImportProgram_noTopLevelProgramFieldInJson_error() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(
                    ImmutableMap.of(
                        "programJson",
                        "{ \"id\" : 32, \"adminName\" : \"admin-name\","
                            + " \"adminDescription\" : \"description\"}"))
                .build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Error processing JSON");
    assertThat(contentAsString(result))
        .containsPattern("JSON did not have a top-level .*program.* field");
  }

  @Test
  public void hxImportProgram_noDuplicatesEnabled_noTopLevelProgramFieldInJson_error() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);
    when(mockSettingsManifest.getNoDuplicateQuestionsForMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(
                    ImmutableMap.of(
                        "programJson",
                        "{ \"id\" : 32, \"adminName\" : \"admin-name\","
                            + " \"adminDescription\" : \"description\"}"))
                .build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Error processing JSON");
    assertThat(contentAsString(result))
        .containsPattern("JSON did not have a top-level .*program.* field");
  }

  @Test
  public void hxImportProgram_notEnoughInfoToCreateProgramDef_error() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(
                    ImmutableMap.of(
                        "programJson",
                        "{ \"program\": { \"adminName\" : \"admin-name\","
                            + " \"adminDescription\" : \"description\"}}"))
                .build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Error processing JSON");
    assertThat(contentAsString(result)).contains("JSON is incorrectly formatted");
  }

  @Test
  public void hxImportProgram_noDuplicatesEnabled_notEnoughInfoToCreateProgramDef_error() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);
    when(mockSettingsManifest.getNoDuplicateQuestionsForMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(
                    ImmutableMap.of(
                        "programJson",
                        "{ \"program\": { \"adminName\" : \"admin-name\","
                            + " \"adminDescription\" : \"description\"}}"))
                .build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Error processing JSON");
    assertThat(contentAsString(result)).contains("JSON is incorrectly formatted");
  }

  @Test
  public void hxImportProgram_programAlreadyExists_error() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    // save a program
    controller.hxSaveProgram(
        fakeRequestBuilder()
            .method("POST")
            .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_ONE_QUESTION))
            .build());

    // attempt to import the program again
    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_ONE_QUESTION))
                .build());

    // see the error
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("This program already exists in our system.");
    assertThat(contentAsString(result)).contains("Please check your file and and try again.");
  }

  @Test
  public void hxImportProgram_noDuplicatesEnabled_programAlreadyExists_error() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);
    when(mockSettingsManifest.getNoDuplicateQuestionsForMigrationEnabled(any())).thenReturn(true);

    // save a program
    controller.hxSaveProgram(
        fakeRequestBuilder()
            .method("POST")
            .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_ONE_QUESTION))
            .build());

    // attempt to import the program again
    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_ONE_QUESTION))
                .build());

    // see the error
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("This program already exists in our system.");
    assertThat(contentAsString(result)).contains("Please check your file and and try again.");
  }

  @Test
  public void hxImportProgram_negativeBlockId_error() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    // attempt to import a program with a negative block id
    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_NEGATIVE_BLOCK_ID))
                .build());

    // see the error
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Block definition ids must be greater than 0.");
    assertThat(contentAsString(result))
        .contains("Please check your block definition ids and try again.");
  }

  @Test
  public void hxImportProgram_handlesServerError() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    // attempt to import program with bad json - question id in block definition does not match
    // question id on question
    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_MISMATCHED_QUESTION_ID))
                .build());

    // see the error
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("There was an error rendering your program.");
    assertThat(contentAsString(result)).contains("Please check your data and try again.");
  }

  @Test
  public void hxImportProgram_noDuplicatesNotEnabled_draftsExist_noError() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    // Create a draft program, so that there are unpublished programs
    ProgramBuilder.newDraftProgram("draft-program").build();

    // save a program
    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_ONE_QUESTION))
                .build());

    // no error because duplicate questions for migration is not enabled
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Minimal Sample Program");
    assertThat(contentAsString(result)).contains("minimal-sample-program");
    assertThat(contentAsString(result)).contains("Screen 1");
    assertThat(contentAsString(result)).contains("Please enter your first and last name");
  }

  @Test
  public void hxImportProgram_noDuplicatesEnabled_draftProgramExists_error() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);
    when(mockSettingsManifest.getNoDuplicateQuestionsForMigrationEnabled(any())).thenReturn(true);

    // Create a draft program, so that there are unpublished programs
    ProgramBuilder.newDraftProgram("draft-program").build();

    // save a program
    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_ONE_QUESTION))
                .build());

    // see the error
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains("There are draft programs and questions in our system.");
    assertThat(contentAsString(result)).contains("Please publish all drafts and try again.");
  }

  @Test
  public void hxImportProgram_noDuplicatesEnabled_draftQuestionExists_error() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);
    when(mockSettingsManifest.getNoDuplicateQuestionsForMigrationEnabled(any())).thenReturn(true);

    // Create a draft question, so there is an unpublished question
    versionRepository
        .getDraftVersionOrCreate()
        .addQuestion(resourceCreator.insertQuestion("draft-question"))
        .save();

    // save a program
    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_ONE_QUESTION))
                .build());

    // see the error
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains("There are draft programs and questions in our system.");
    assertThat(contentAsString(result)).contains("Please publish all drafts and try again.");
  }

  @Test
  public void hxImportProgram_jsonHasAllProgramInfo_resultHasProgramAndQuestionInfo() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_ONE_QUESTION))
                .build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Minimal Sample Program");
    assertThat(contentAsString(result)).contains("minimal-sample-program");
    assertThat(contentAsString(result)).contains("Screen 1");
    assertThat(contentAsString(result)).contains("Please enter your first and last name");
  }

  @Test
  public void
      hxImportProgram_noDuplicatesEnabled_jsonHasAllProgramInfo_resultHasProgramAndQuestionInfo() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);
    when(mockSettingsManifest.getNoDuplicateQuestionsForMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_ONE_QUESTION))
                .build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Minimal Sample Program");
    assertThat(contentAsString(result)).contains("minimal-sample-program");
    assertThat(contentAsString(result)).contains("Screen 1");
    assertThat(contentAsString(result)).contains("Please enter your first and last name");
  }

  @Test
  public void hxImportProgram_showsWarningAndOverwritesQuestionAdminNamesIfTheyAlreadyExist() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    // save the program
    controller.hxSaveProgram(
        fakeRequestBuilder()
            .method("POST")
            .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_ONE_QUESTION))
            .build());

    // update the program admin name so we don't receive an error
    String UPDATED_PROGRAM_JSON_WITH_ONE_QUESTION =
        PROGRAM_JSON_WITH_ONE_QUESTION.replace(
            "minimal-sample-program", "minimal-sample-program-new");

    // parse the program for import
    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", UPDATED_PROGRAM_JSON_WITH_ONE_QUESTION))
                .build());

    assertThat(result.status()).isEqualTo(OK);

    // warning is shown
    assertThat(contentAsString(result))
        .contains("Importing this program will add 1 duplicate question to the question bank.");
    // question has the new admin name
    assertThat(contentAsString(result)).contains("Name -_- a");
    // other information in the question is unchanged
    assertThat(contentAsString(result)).contains("Please enter your first and last name");
  }

  @Test
  public void
      hxImportProgram_noDuplicatesEnabled_showsWarningAndDoesNotOverwriteQuestionAdminNamesIfTheyAlreadyExist() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);
    when(mockSettingsManifest.getNoDuplicateQuestionsForMigrationEnabled(any())).thenReturn(true);

    // save the program
    controller.hxSaveProgram(
        fakeRequestBuilder()
            .method("POST")
            .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_ONE_QUESTION))
            .build());

    // publish the drafts
    versionRepository.publishNewSynchronizedVersion();

    // update the program admin name so we don't receive an error
    String UPDATED_PROGRAM_JSON_WITH_ONE_QUESTION =
        PROGRAM_JSON_WITH_ONE_QUESTION.replace(
            "minimal-sample-program", "minimal-sample-program-new");

    // parse the program for import
    Result result =
        controller.hxImportProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", UPDATED_PROGRAM_JSON_WITH_ONE_QUESTION))
                .build());

    assertThat(result.status()).isEqualTo(OK);

    // warning is shown
    assertThat(contentAsString(result))
        .contains("There is 1 existing question that will appear as draft in the question bank.");
    // question has the same admin name
    assertThat(contentAsString(result)).contains("Name");
    // other information in the question is unchanged
    assertThat(contentAsString(result)).contains("Please enter your first and last name");
  }

  @Test
  public void hxSaveProgram_savesTheProgramWithoutQuestions() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.hxSaveProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITHOUT_QUESTIONS))
                .build());

    assertThat(result.status()).isEqualTo(OK);

    ProgramDefinition programDefinition =
        database
            .find(ProgramModel.class)
            .where()
            .eq("name", "no-questions")
            .findOne()
            .getProgramDefinition();

    assertThat(programDefinition.externalLink()).isEqualTo("https://www.example.com");
  }

  @Test
  public void hxSaveProgram_noDuplicatesEnabled_savesTheProgramWithoutQuestions() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);
    when(mockSettingsManifest.getNoDuplicateQuestionsForMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.hxSaveProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITHOUT_QUESTIONS))
                .build());

    assertThat(result.status()).isEqualTo(OK);

    ProgramDefinition programDefinition =
        database
            .find(ProgramModel.class)
            .where()
            .eq("name", "no-questions")
            .findOne()
            .getProgramDefinition();

    assertThat(programDefinition.externalLink()).isEqualTo("https://www.example.com");
  }

  @Test
  public void hxSaveProgram_savesTheProgramWithQuestions() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.hxSaveProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_ONE_QUESTION))
                .build());

    assertThat(result.status()).isEqualTo(OK);

    ProgramDefinition programDefinition =
        database
            .find(ProgramModel.class)
            .where()
            .eq("name", "minimal-sample-program")
            .findOne()
            .getProgramDefinition();
    QuestionDefinition questionDefinition =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "Name")
            .findOne()
            .getQuestionDefinition();

    assertThat(programDefinition.externalLink()).isEqualTo("https://github.com/civiform/civiform");
    assertThat(questionDefinition.getQuestionText().getDefault())
        .isEqualTo("Please enter your first and last name");
    assertThat(programDefinition.getQuestionIdsInProgram()).contains(questionDefinition.getId());
  }

  @Test
  public void hxSaveProgram_noDuplicatesEnabled_savesTheProgramWithQuestions() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);
    when(mockSettingsManifest.getNoDuplicateQuestionsForMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.hxSaveProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_ONE_QUESTION))
                .build());

    assertThat(result.status()).isEqualTo(OK);

    ProgramDefinition programDefinition =
        database
            .find(ProgramModel.class)
            .where()
            .eq("name", "minimal-sample-program")
            .findOne()
            .getProgramDefinition();
    QuestionDefinition questionDefinition =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "Name")
            .findOne()
            .getQuestionDefinition();

    assertThat(programDefinition.externalLink()).isEqualTo("https://github.com/civiform/civiform");
    assertThat(questionDefinition.getQuestionText().getDefault())
        .isEqualTo("Please enter your first and last name");
    assertThat(programDefinition.getQuestionIdsInProgram()).contains(questionDefinition.getId());
  }

  @Test
  public void hxSaveProgram_handlesNestEnumeratorQuestions() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.hxSaveProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_ENUMERATORS))
                .build());

    assertThat(result.status()).isEqualTo(OK);

    QuestionDefinition enumeratorQuestionDefinition =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "Sample Enumerator Question")
            .findOne()
            .getQuestionDefinition();

    QuestionDefinition nestedEnumeratorQuestionDefinition =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "cats")
            .findOne()
            .getQuestionDefinition();

    QuestionDefinition childQuestionDefinition =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "cat-color")
            .findOne()
            .getQuestionDefinition();

    assertThat(enumeratorQuestionDefinition.getId())
        .isEqualTo(nestedEnumeratorQuestionDefinition.getEnumeratorId().get());
    assertThat(nestedEnumeratorQuestionDefinition.getId())
        .isEqualTo(childQuestionDefinition.getEnumeratorId().get());
  }

  @Test
  public void hxSaveProgram_noDuplicatesEnabled_handlesNestEnumeratorQuestions() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);
    when(mockSettingsManifest.getNoDuplicateQuestionsForMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.hxSaveProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_ENUMERATORS))
                .build());

    assertThat(result.status()).isEqualTo(OK);

    QuestionDefinition enumeratorQuestionDefinition =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "Sample Enumerator Question")
            .findOne()
            .getQuestionDefinition();

    QuestionDefinition nestedEnumeratorQuestionDefinition =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "cats")
            .findOne()
            .getQuestionDefinition();

    QuestionDefinition childQuestionDefinition =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "cat-color")
            .findOne()
            .getQuestionDefinition();

    assertThat(enumeratorQuestionDefinition.getId())
        .isEqualTo(nestedEnumeratorQuestionDefinition.getEnumeratorId().get());
    assertThat(nestedEnumeratorQuestionDefinition.getId())
        .isEqualTo(childQuestionDefinition.getEnumeratorId().get());
  }

  @Test
  public void hxSaveProgram_savesUpdatedQuestionIdsOnPredicates()
      throws ProgramBlockDefinitionNotFoundException {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.hxSaveProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_PREDICATES))
                .build());

    assertThat(result.status()).isEqualTo(OK);

    ProgramDefinition programDefinition =
        database
            .find(ProgramModel.class)
            .where()
            .eq("name", "visibility-eligibility")
            .findOne()
            .getProgramDefinition();
    Long eligibilityQuestionId =
        programDefinition
            .getBlockDefinition(1)
            .eligibilityDefinition()
            .get()
            .predicate()
            .rootNode()
            .getOrNode()
            .children()
            .get(0)
            .getAndNode()
            .children()
            .get(0)
            .getLeafOperationNode()
            .questionId();
    Long visibilityQuestionId =
        programDefinition
            .getBlockDefinition(2)
            .visibilityPredicate()
            .get()
            .rootNode()
            .getLeafOperationNode()
            .questionId();
    Long savedQuestionId =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "id-test")
            .findOne()
            .getQuestionDefinition()
            .getId();

    assertThat(eligibilityQuestionId).isEqualTo(savedQuestionId);
    assertThat(visibilityQuestionId).isEqualTo(savedQuestionId);

    Long eligibilityInServiceAreaAddressQuestionId =
        programDefinition
            .getBlockDefinition(3)
            .eligibilityDefinition()
            .get()
            .predicate()
            .rootNode()
            .getLeafAddressNode()
            .questionId();
    Long savedInServiceAreaAddressQuestionId =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "Address")
            .findOne()
            .getQuestionDefinition()
            .getId();
    assertThat(eligibilityInServiceAreaAddressQuestionId)
        .isEqualTo(savedInServiceAreaAddressQuestionId);

    Long eligibilityNotInServiceAreaAddressQuestionId =
        programDefinition
            .getBlockDefinition(4)
            .eligibilityDefinition()
            .get()
            .predicate()
            .rootNode()
            .getLeafAddressNode()
            .questionId();
    Long savedNotInServiceAreaAddressQuestionId =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "second-address")
            .findOne()
            .getQuestionDefinition()
            .getId();
    assertThat(eligibilityNotInServiceAreaAddressQuestionId)
        .isEqualTo(savedNotInServiceAreaAddressQuestionId);
  }

  @Test
  public void hxSaveProgram_noDuplicatesEnabled_savesUpdatedQuestionIdsOnPredicates()
      throws ProgramBlockDefinitionNotFoundException {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);
    when(mockSettingsManifest.getNoDuplicateQuestionsForMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.hxSaveProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_PREDICATES))
                .build());

    assertThat(result.status()).isEqualTo(OK);

    ProgramDefinition programDefinition =
        database
            .find(ProgramModel.class)
            .where()
            .eq("name", "visibility-eligibility")
            .findOne()
            .getProgramDefinition();
    Long eligibilityQuestionId =
        programDefinition
            .getBlockDefinition(1)
            .eligibilityDefinition()
            .get()
            .predicate()
            .rootNode()
            .getOrNode()
            .children()
            .get(0)
            .getAndNode()
            .children()
            .get(0)
            .getLeafOperationNode()
            .questionId();
    Long visibilityQuestionId =
        programDefinition
            .getBlockDefinition(2)
            .visibilityPredicate()
            .get()
            .rootNode()
            .getLeafOperationNode()
            .questionId();
    Long savedQuestionId =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "id-test")
            .findOne()
            .getQuestionDefinition()
            .getId();

    assertThat(eligibilityQuestionId).isEqualTo(savedQuestionId);
    assertThat(visibilityQuestionId).isEqualTo(savedQuestionId);

    Long eligibilityInServiceAreaAddressQuestionId =
        programDefinition
            .getBlockDefinition(3)
            .eligibilityDefinition()
            .get()
            .predicate()
            .rootNode()
            .getLeafAddressNode()
            .questionId();
    Long savedInServiceAreaAddressQuestionId =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "Address")
            .findOne()
            .getQuestionDefinition()
            .getId();
    assertThat(eligibilityInServiceAreaAddressQuestionId)
        .isEqualTo(savedInServiceAreaAddressQuestionId);

    Long eligibilityNotInServiceAreaAddressQuestionId =
        programDefinition
            .getBlockDefinition(4)
            .eligibilityDefinition()
            .get()
            .predicate()
            .rootNode()
            .getLeafAddressNode()
            .questionId();
    Long savedNotInServiceAreaAddressQuestionId =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "second-address")
            .findOne()
            .getQuestionDefinition()
            .getId();
    assertThat(eligibilityNotInServiceAreaAddressQuestionId)
        .isEqualTo(savedNotInServiceAreaAddressQuestionId);
  }

  @Test
  public void hxSaveProgram_discardsPaiTagsOnImportedQuestions() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.hxSaveProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_PAI_TAGS))
                .build());

    assertThat(result.status()).isEqualTo(OK);

    QuestionDefinition questionDefinition =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "dob")
            .findOne()
            .getQuestionDefinition();

    assertThat(questionDefinition.getPrimaryApplicantInfoTags()).isEqualTo(ImmutableSet.of());
  }

  @Test
  public void hxSaveProgram_noDuplicatesEnabled_discardsPaiTagsOnImportedQuestions() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);
    when(mockSettingsManifest.getNoDuplicateQuestionsForMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.hxSaveProgram(
            fakeRequestBuilder()
                .method("POST")
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_PAI_TAGS))
                .build());

    assertThat(result.status()).isEqualTo(OK);

    QuestionDefinition questionDefinition =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "dob")
            .findOne()
            .getQuestionDefinition();

    assertThat(questionDefinition.getPrimaryApplicantInfoTags()).isEqualTo(ImmutableSet.of());
  }

  @Test
  public void hxSaveProgram_preservesUniversalSettingOnImportedQuestions() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.hxSaveProgram(
            fakeRequestBuilder()
                .method("POST")
                // Questions must be marked as "universal" before being tagged with a PAI
                // tag, so we can reuse the PROGRAM_JSON_WITH_PAI_TAGS json
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_PAI_TAGS))
                .build());

    assertThat(result.status()).isEqualTo(OK);

    QuestionDefinition questionDefinition =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "dob")
            .findOne()
            .getQuestionDefinition();

    assertThat(questionDefinition.isUniversal()).isTrue();
  }

  @Test
  public void hxSaveProgram_noDuplicatesEnabled_preservesUniversalSettingOnImportedQuestions() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);
    when(mockSettingsManifest.getNoDuplicateQuestionsForMigrationEnabled(any())).thenReturn(true);

    Result result =
        controller.hxSaveProgram(
            fakeRequestBuilder()
                .method("POST")
                // Questions must be marked as "universal" before being tagged with a PAI
                // tag, so we can reuse the PROGRAM_JSON_WITH_PAI_TAGS json
                .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_PAI_TAGS))
                .build());

    assertThat(result.status()).isEqualTo(OK);

    QuestionDefinition questionDefinition =
        database
            .find(QuestionModel.class)
            .where()
            .eq("name", "dob")
            .findOne()
            .getQuestionDefinition();

    assertThat(questionDefinition.isUniversal()).isTrue();
  }

  @Test
  public void hxSaveProgram_addsAnEmptyStatus() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);

    controller.hxSaveProgram(
        fakeRequestBuilder()
            .method("POST")
            .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_ONE_QUESTION))
            .build());

    StatusDefinitions statusDefinitions =
        database
            .find(ApplicationStatusesModel.class)
            .where()
            .eq("program_name", "minimal-sample-program")
            .findOne()
            .getStatusDefinitions();

    assertThat(statusDefinitions.getStatuses()).isEmpty();
  }

  @Test
  public void hxSaveProgram_noDuplicatesEnabled_addsAnEmptyStatus() {
    when(mockSettingsManifest.getProgramMigrationEnabled(any())).thenReturn(true);
    when(mockSettingsManifest.getNoDuplicateQuestionsForMigrationEnabled(any())).thenReturn(true);

    controller.hxSaveProgram(
        fakeRequestBuilder()
            .method("POST")
            .bodyForm(ImmutableMap.of("programJson", PROGRAM_JSON_WITH_ONE_QUESTION))
            .build());

    StatusDefinitions statusDefinitions =
        database
            .find(ApplicationStatusesModel.class)
            .where()
            .eq("program_name", "minimal-sample-program")
            .findOne()
            .getStatusDefinitions();

    assertThat(statusDefinitions.getStatuses()).isEmpty();
  }

  public static final String PROGRAM_JSON_WITHOUT_QUESTIONS =
      """
      {
            "program" : {
              "id" : 9,
              "adminName" : "no-questions",
              "adminDescription" : "",
              "externalLink" : "https://www.example.com",
              "displayMode" : "PUBLIC",
              "notificationPreferences" : [ ],
              "localizedName" : {
                "translations" : {
                  "en_US" : "Program With No Questions"
                },
                "isRequired" : true
              },
              "localizedDescription" : {
                "translations" : {
                  "en_US" : "No questions"
                },
                "isRequired" : true
              },
              "localizedConfirmationMessage" : {
                "translations" : {
                  "en_US" : ""
                },
                "isRequired" : true
              },
              "blockDefinitions" : [ {
                "id" : 1,
                "name" : "Screen 1",
                "description" : "Screen 1 description",
                "localizedName" : {
                  "translations" : {
                    "en_US" : "Screen 1"
                  },
                  "isRequired" : true
                },
                "localizedDescription" : {
                  "translations" : {
                    "en_US" : "Screen 1 description"
                  },
                  "isRequired" : true
                },
                "repeaterId" : null,
                "hidePredicate" : null,
                "optionalPredicate" : null,
                "questionDefinitions" : [ ]
              } ],
              "statusDefinitions" : {
                "statuses" : [ ]
              },
              "programType" : "DEFAULT",
              "eligibilityIsGating" : true,
              "acls" : {
                "tiProgramViewAcls" : [ ]
              },
              "categories" : [ ],
              "localizedSummaryImageDescription" : null,
              "applicationSteps" : [ {
                 "title" : {
                    "translations" : {
                      "en_US" : "step one"
                    },
                    "isRequired" : true
                },
                "description" : {
                   "translations" : {
                   "en_US" : "step one"
                },
                  "isRequired" : true
                }
             }]
            }
          }
      """;

  public static final String PROGRAM_JSON_WITH_NEGATIVE_BLOCK_ID =
      """
      {
            "program" : {
              "id" : 9,
              "adminName" : "no-questions",
              "adminDescription" : "",
              "externalLink" : "https://www.example.com",
              "displayMode" : "PUBLIC",
              "notificationPreferences" : [ ],
              "localizedName" : {
                "translations" : {
                  "en_US" : "Program With No Questions"
                },
                "isRequired" : true
              },
              "localizedDescription" : {
                "translations" : {
                  "en_US" : "No questions"
                },
                "isRequired" : true
              },
              "localizedConfirmationMessage" : {
                "translations" : {
                  "en_US" : ""
                },
                "isRequired" : true
              },
              "blockDefinitions" : [ {
                "id" : -1,
                "name" : "Screen 1",
                "description" : "Screen 1 description",
                "localizedName" : {
                  "translations" : {
                    "en_US" : "Screen 1"
                  },
                  "isRequired" : true
                },
                "localizedDescription" : {
                  "translations" : {
                    "en_US" : "Screen 1 description"
                  },
                  "isRequired" : true
                },
                "repeaterId" : null,
                "hidePredicate" : null,
                "optionalPredicate" : null,
                "questionDefinitions" : [ ]
              } ],
              "statusDefinitions" : {
                "statuses" : [ ]
              },
              "programType" : "DEFAULT",
              "eligibilityIsGating" : true,
              "acls" : {
                "tiProgramViewAcls" : [ ]
              },
              "categories" : [ ],
              "localizedSummaryImageDescription" : null,
              "applicationSteps" : [ {
                 "title" : {
                    "translations" : {
                      "en_US" : "step one"
                    },
                    "isRequired" : true
                },
                "description" : {
                   "translations" : {
                   "en_US" : "step one"
                },
                  "isRequired" : true
                }
             }]
            }
          }
      """;

  public static final String PROGRAM_JSON_WITH_ONE_QUESTION =
      """
          {
          "program" : {
              "id" : 7,
              "adminName" : "minimal-sample-program",
              "adminDescription" : "desc",
              "externalLink" : "https://github.com/civiform/civiform",
              "displayMode" : "PUBLIC",
              "notificationPreferences" : [ ],
              "localizedName" : {
              "translations" : {
                  "en_US" : "Minimal Sample Program"
              },
              "isRequired" : true
              },
              "localizedDescription" : {
              "translations" : {
                  "en_US" : "display description"
              },
              "isRequired" : true
              },
              "localizedShortDescription" : {
              "translations" : {
                  "en_US" : "short display description"
              },
              "isRequired" : true
              },
              "localizedConfirmationMessage" : {
              "translations" : {
                  "en_US" : ""
              },
              "isRequired" : true
              },
              "blockDefinitions" : [ {
              "id" : 1,
              "name" : "Screen 1",
              "description" : "Screen 1",
              "localizedName" : {
                  "translations" : {
                  "en_US" : "Screen 1"
                  },
                  "isRequired" : true
              },
              "localizedDescription" : {
                  "translations" : {
                  "en_US" : "Screen 1"
                  },
                  "isRequired" : true
              },
              "repeaterId" : null,
              "hidePredicate" : null,
              "optionalPredicate" : null,
              "questionDefinitions" : [ {
                  "id" : 1,
                  "optional" : true,
                  "addressCorrectionEnabled" : false
              } ]
              } ],
              "statusDefinitions" : {
              "statuses" : [ ]
              },
              "programType" : "DEFAULT",
              "eligibilityIsGating" : true,
              "acls" : {
              "tiProgramViewAcls" : [ ]
              },
              "categories" : [ ],
              "localizedSummaryImageDescription" : null,
              "applicationSteps" : [ {
                 "title" : {
                    "translations" : {
                      "en_US" : "step one"
                    },
                    "isRequired" : true
                },
                "description" : {
                   "translations" : {
                   "en_US" : "step one"
                },
                  "isRequired" : true
                }
             }]
          },
          "questions" : [ {
              "type" : "name",
              "config" : {
              "name" : "Name",
              "description" : "The applicant's name",
              "questionText" : {
                  "translations" : {
                  "am" : "ስም (የመጀመሪያ ስም እና የመጨረሻ ስም አህጽሮት ይሆናል)",
                  "ko" : "성함 (이름 및 성의 경우 이니셜도 괜찮음)",
                  "lo" : "ຊື່ (ນາມສະກຸນ ແລະ ຕົວອັກສອນທຳອິດຂອງນາມສະກຸນແມ່ນຖືກຕ້ອງ)",
                  "so" : "Magaca (magaca koowaad iyo kan dambe okay)",
                  "tl" : "Pangalan (unang pangalan at ang unang titik ng apilyedo ay okay)",
                  "vi" : "Tên (tên và họ viết tắt đều được)",
                  "en_US" : "Please enter your first and last name",
                  "es_US" : "Nombre (nombre y la inicial del apellido está bien)",
                  "zh_TW" : "姓名（名字和姓氏第一個字母便可）"
                  },
                  "isRequired" : true
              },
              "questionHelpText" : {
                  "translations" : { },
                  "isRequired" : false
              },
              "validationPredicates" : {
                  "type" : "name"
              },
              "id" : 1,
              "universal" : false,
              "primaryApplicantInfoTags" : [ ]
              }
          } ]
          }
      """;

  public static final String PROGRAM_JSON_WITH_ENUMERATORS =
      """
      {
            "program" : {
              "id" : 18,
              "adminName" : "nested-enumerator",
              "adminDescription" : "",
              "externalLink" : "",
              "displayMode" : "PUBLIC",
              "notificationPreferences" : [ ],
              "localizedName" : {
                "translations" : {
                  "en_US" : "nest enumerator program"
                },
                "isRequired" : true
              },
              "localizedDescription" : {
                "translations" : {
                  "en_US" : "nested enumerator program"
                },
                "isRequired" : true
              },
              "localizedConfirmationMessage" : {
                "translations" : {
                  "en_US" : ""
                },
                "isRequired" : true
              },
              "blockDefinitions" : [ {
                "id" : 5,
                "name" : "Screen 5",
                "description" : "Screen 5 description",
                "localizedName" : {
                  "translations" : {
                    "en_US" : "Screen 5"
                  },
                  "isRequired" : true
                },
                "localizedDescription" : {
                  "translations" : {
                    "en_US" : "Screen 5 description"
                  },
                  "isRequired" : true
                },
                "repeaterId" : null,
                "hidePredicate" : null,
                "optionalPredicate" : null,
                "questionDefinitions" : [ {
                  "id" : 13,
                  "optional" : false,
                  "addressCorrectionEnabled" : false
                } ]
              }, {
                "id" : 1,
                "name" : "Screen 1",
                "description" : "Screen 1 description",
                "localizedName" : {
                  "translations" : {
                    "en_US" : "Screen 1"
                  },
                  "isRequired" : true
                },
                "localizedDescription" : {
                  "translations" : {
                    "en_US" : "Screen 1 description"
                  },
                  "isRequired" : true
                },
                "repeaterId" : null,
                "hidePredicate" : null,
                "optionalPredicate" : null,
                "questionDefinitions" : [ {
                  "id" : 10,
                  "optional" : false,
                  "addressCorrectionEnabled" : false
                } ]
              }, {
                "id" : 3,
                "name" : "Screen 3 (repeated from 1)",
                "description" : "Screen 3 description",
                "localizedName" : {
                  "translations" : {
                    "en_US" : "Screen 3 (repeated from 1)"
                  },
                  "isRequired" : true
                },
                "localizedDescription" : {
                  "translations" : {
                    "en_US" : "Screen 3 description"
                  },
                  "isRequired" : true
                },
                "repeaterId" : 1,
                "hidePredicate" : null,
                "optionalPredicate" : null,
                "questionDefinitions" : [ {
                  "id" : 94,
                  "optional" : false,
                  "addressCorrectionEnabled" : false
                } ]
              }, {
                "id" : 4,
                "name" : "Screen 4 (repeated from 3)",
                "description" : "Screen 4 description",
                "localizedName" : {
                  "translations" : {
                    "en_US" : "Screen 4 (repeated from 3)"
                  },
                  "isRequired" : true
                },
                "localizedDescription" : {
                  "translations" : {
                    "en_US" : "Screen 4 description"
                  },
                  "isRequired" : true
                },
                "repeaterId" : 3,
                "hidePredicate" : null,
                "optionalPredicate" : null,
                "questionDefinitions" : [ {
                  "id" : 95,
                  "optional" : false,
                  "addressCorrectionEnabled" : false
                } ]
              } ],
              "programType" : "DEFAULT",
              "eligibilityIsGating" : true,
              "acls" : {
                "tiProgramViewAcls" : [ ]
              },
              "localizedSummaryImageDescription" : null,
              "categories" : [ ],
              "applicationSteps" : [ {
                 "title" : {
                    "translations" : {
                      "en_US" : "step one"
                    },
                    "isRequired" : true
                },
                "description" : {
                   "translations" : {
                   "en_US" : "step one"
                },
                  "isRequired" : true
                }
             }]
            },
            "questions" : [ {
              "type" : "name",
              "config" : {
                "name" : "Sample Name Question",
                "description" : "description",
                "questionText" : {
                  "translations" : {
                    "en_US" : "What is your name?"
                  },
                  "isRequired" : true
                },
                "questionHelpText" : {
                  "translations" : {
                    "en_US" : "help text"
                  },
                  "isRequired" : true
                },
                "validationPredicates" : {
                  "type" : "name"
                },
                "id" : 13,
                "universal" : true,
                "primaryApplicantInfoTags" : [ "APPLICANT_NAME" ]
              }
            }, {
              "type" : "enumerator",
              "config" : {
                "name" : "Sample Enumerator Question",
                "description" : "description",
                "questionText" : {
                  "translations" : {
                    "en_US" : "List all members of your household."
                  },
                  "isRequired" : true
                },
                "questionHelpText" : {
                  "translations" : {
                    "en_US" : "help text"
                  },
                  "isRequired" : true
                },
                "validationPredicates" : {
                  "type" : "enumerator",
                  "minEntities" : null,
                  "maxEntities" : null
                },
                "id" : 10,
                "universal" : false,
                "primaryApplicantInfoTags" : [ ]
              },
              "entityType" : {
                "translations" : {
                  "en_US" : "household member"
                },
                "isRequired" : true
              }
            }, {
              "type" : "enumerator",
              "config" : {
                "name" : "cats",
                "description" : "",
                "questionText" : {
                  "translations" : {
                    "en_US" : "Please list each cat owned by $this"
                  },
                  "isRequired" : true
                },
                "questionHelpText" : {
                  "translations" : { },
                  "isRequired" : false
                },
                "validationPredicates" : {
                  "type" : "enumerator",
                  "minEntities" : null,
                  "maxEntities" : null
                },
                "id" : 94,
                "enumeratorId" : 10,
                "universal" : false,
                "primaryApplicantInfoTags" : [ ]
              },
              "entityType" : {
                "translations" : {
                  "en_US" : "cat"
                },
                "isRequired" : true
              }
            }, {
              "type" : "text",
              "config" : {
                "name" : "cat-color",
                "description" : "",
                "questionText" : {
                  "translations" : {
                    "en_US" : "What color is $this?"
                  },
                  "isRequired" : true
                },
                "questionHelpText" : {
                  "translations" : { },
                  "isRequired" : false
                },
                "validationPredicates" : {
                  "type" : "text",
                  "minLength" : null,
                  "maxLength" : null
                },
                "id" : 95,
                "enumeratorId" : 94,
                "universal" : false,
                "primaryApplicantInfoTags" : [ ]
              }
            } ]
          }
      """;

  // Can't use multistring here because of escaped characters in predicates
  public static final String PROGRAM_JSON_WITH_PREDICATES =
      "{\n"
          + "  \"program\" : {\n"
          + "    \"id\" : 1,\n"
          + "    \"adminName\" : \"visibility-eligibility\",\n"
          + "    \"adminDescription\" : \"\",\n"
          + "    \"externalLink\" : \"\",\n"
          + "    \"displayMode\" : \"PUBLIC\",\n"
          + "    \"notificationPreferences\" : [ ],\n"
          + "    \"localizedName\" : {\n"
          + "      \"translations\" : {\n"
          + "        \"en_US\" : \"visibility and eligibility test\"\n"
          + "      },\n"
          + "      \"isRequired\" : true\n"
          + "    },\n"
          + "    \"localizedDescription\" : {\n"
          + "      \"translations\" : {\n"
          + "        \"en_US\" : \"visibility and eligibility test\"\n"
          + "      },\n"
          + "      \"isRequired\" : true\n"
          + "    },\n"
          + "    \"localizedConfirmationMessage\" : {\n"
          + "      \"translations\" : {\n"
          + "        \"en_US\" : \"\"\n"
          + "      },\n"
          + "      \"isRequired\" : true\n"
          + "    },\n"
          + "    \"blockDefinitions\" : [ {\n"
          + "      \"id\" : 1,\n"
          + "      \"name\" : \"Screen 1\",\n"
          + "      \"description\" : \"Screen 1 description\",\n"
          + "      \"localizedName\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"Screen 1\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"localizedDescription\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"Screen 1 description\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"repeaterId\" : null,\n"
          + "      \"hidePredicate\" : null,\n"
          + "      \"eligibilityDefinition\" : {\n"
          + "        \"predicate\" : {\n"
          + "          \"rootNode\" : {\n"
          + "            \"node\" : {\n"
          + "              \"type\" : \"or\",\n"
          + "              \"children\" : [ {\n"
          + "                \"node\" : {\n"
          + "                  \"type\" : \"and\",\n"
          + "                  \"children\" : [ {\n"
          + "                    \"node\" : {\n"
          + "                      \"type\" : \"leaf\",\n"
          + "                      \"questionId\" : 3,\n"
          + "                      \"scalar\" : \"SELECTIONS\",\n"
          + "                      \"operator\" : \"ANY_OF\",\n"
          + "                      \"value\" : {\n"
          + "                        \"value\" : \"[\\\"0\\\", \\\"9\\\", \\\"11\\\"]\",\n"
          + "                        \"type\" : \"LIST_OF_STRINGS\"\n"
          + "                      }\n"
          + "                    }\n"
          + "                  }, {\n"
          + "                    \"node\" : {\n"
          + "                      \"type\" : \"leaf\",\n"
          + "                      \"questionId\" : 3,\n"
          + "                      \"scalar\" : \"SELECTION\",\n"
          + "                      \"operator\" : \"IN\",\n"
          + "                      \"value\" : {\n"
          + "                        \"value\" : \"[\\\"1\\\"]\",\n"
          + "                        \"type\" : \"LIST_OF_STRINGS\"\n"
          + "                      }\n"
          + "                    }\n"
          + "                  } ]\n"
          + "                }\n"
          + "              } ]\n"
          + "            }\n"
          + "          },\n"
          + "          \"action\" : \"ELIGIBLE_BLOCK\"\n"
          + "        }\n"
          + "      },\n"
          + "      \"optionalPredicate\" : null,\n"
          + "      \"questionDefinitions\" : [ {\n"
          + "        \"id\" : 3,\n"
          + "        \"optional\" : false,\n"
          + "        \"addressCorrectionEnabled\" : false\n"
          + "      } ]\n"
          + "    }, {\n"
          + "      \"id\" : 2,\n"
          + "      \"name\" : \"Screen 2\",\n"
          + "      \"description\" : \"Screen 2 description\",\n"
          + "      \"localizedName\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"Screen 2\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"localizedDescription\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"Screen 2 description\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"repeaterId\" : null,\n"
          + "      \"hidePredicate\" : {\n"
          + "        \"rootNode\" : {\n"
          + "          \"node\" : {\n"
          + "            \"type\" : \"leaf\",\n"
          + "            \"questionId\" : 3,\n"
          + "            \"scalar\" : \"ID\",\n"
          + "            \"operator\" : \"EQUAL_TO\",\n"
          + "            \"value\" : {\n"
          + "              \"value\" : \"\\\"1\\\"\",\n"
          + "              \"type\" : \"STRING\"\n"
          + "            }\n"
          + "          }\n"
          + "        },\n"
          + "        \"action\" : \"HIDE_BLOCK\"\n"
          + "      },\n"
          + "      \"optionalPredicate\" : null,\n"
          + "      \"questionDefinitions\" : [ {\n"
          + "        \"id\" : 4,\n"
          + "        \"optional\" : false,\n"
          + "        \"addressCorrectionEnabled\" : false\n"
          + "      } ]\n"
          + "    }, {\n"
          + "      \"id\" : 3,\n"
          + "      \"name\" : \"Screen 3\",\n"
          + "      \"description\" : \"Screen 3 description\",\n"
          + "      \"localizedName\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"Screen 3\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"localizedDescription\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"Screen 3 description\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"repeaterId\" : null,\n"
          + "      \"hidePredicate\" : null,\n"
          + "      \"eligibilityDefinition\" : {\n"
          + "        \"predicate\" : {\n"
          + "          \"rootNode\" : {\n"
          + "            \"node\" : {\n"
          + "              \"type\" : \"leafAddressServiceArea\",\n"
          + "              \"questionId\" : 5,\n"
          + "              \"serviceAreaId\" : \"Seattle\",\n"
          + "              \"operator\" : \"IN_SERVICE_AREA\"\n"
          + "            }\n"
          + "          },\n"
          + "          \"action\" : \"ELIGIBLE_BLOCK\"\n"
          + "        }\n"
          + "      },\n"
          + "      \"optionalPredicate\" : null,\n"
          + "      \"questionDefinitions\" : [ {\n"
          + "        \"id\" : 5,\n"
          + "        \"optional\" : false,\n"
          + "        \"addressCorrectionEnabled\" : true\n"
          + "      } ]\n"
          + "    }, {\n"
          + "      \"id\" : 4,\n"
          + "      \"name\" : \"Screen 4\",\n"
          + "      \"description\" : \"Screen 4 description\",\n"
          + "      \"localizedName\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"Screen 4\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"localizedDescription\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"Screen 4 description\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"repeaterId\" : null,\n"
          + "      \"hidePredicate\" : null,\n"
          + "      \"eligibilityDefinition\" : {\n"
          + "        \"predicate\" : {\n"
          + "          \"rootNode\" : {\n"
          + "            \"node\" : {\n"
          + "              \"type\" : \"leafAddressServiceArea\",\n"
          + "              \"questionId\" : 6,\n"
          + "              \"serviceAreaId\" : \"Seattle\",\n"
          + "              \"operator\" : \"NOT_IN_SERVICE_AREA\"\n"
          + "            }\n"
          + "          },\n"
          + "          \"action\" : \"ELIGIBLE_BLOCK\"\n"
          + "        }\n"
          + "      },\n"
          + "      \"optionalPredicate\" : null,\n"
          + "      \"questionDefinitions\" : [ {\n"
          + "        \"id\" : 6,\n"
          + "        \"optional\" : false,\n"
          + "        \"addressCorrectionEnabled\" : true\n"
          + "      } ]\n"
          + "    } ],\n"
          + "    \"programType\" : \"DEFAULT\",\n"
          + "    \"eligibilityIsGating\" : true,\n"
          + "    \"acls\" : {\n"
          + "      \"tiProgramViewAcls\" : [ ]\n"
          + "    },\n"
          + "    \"localizedSummaryImageDescription\" : null,\n"
          + "    \"categories\" : [ ],\n"
          + "    \"applicationSteps\" : [ {\n"
          + "      \"title\" : {\n"
          + "         \"translations\" : {\n"
          + "           \"en_US\" : \"step one\"\n"
          + "         },\n"
          + "         \"isRequired\" : true\n"
          + "     },\n"
          + "     \"description\" : {\n"
          + "        \"translations\" : {\n"
          + "        \"en_US\" : \"step one\"\n"
          + "     },\n"
          + "       \"isRequired\" : true\n"
          + "     }\n"
          + "   }]\n"
          + "  },\n"
          + "  \"questions\" : [ {\n"
          + "    \"type\" : \"id\",\n"
          + "    \"config\" : {\n"
          + "      \"name\" : \"id-test\",\n"
          + "      \"description\" : \"\",\n"
          + "      \"questionText\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"Enter a number\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"questionHelpText\" : {\n"
          + "        \"translations\" : { },\n"
          + "        \"isRequired\" : false\n"
          + "      },\n"
          + "      \"validationPredicates\" : {\n"
          + "        \"type\" : \"id\",\n"
          + "        \"minLength\" : null,\n"
          + "        \"maxLength\" : null\n"
          + "      },\n"
          + "      \"id\" : 3,\n"
          + "      \"universal\" : false,\n"
          + "      \"primaryApplicantInfoTags\" : [ ]\n"
          + "    }\n"
          + "  }, {\n"
          + "    \"type\" : \"text\",\n"
          + "    \"config\" : {\n"
          + "      \"name\" : \"text test\",\n"
          + "      \"description\" : \"\",\n"
          + "      \"questionText\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"text test\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"questionHelpText\" : {\n"
          + "        \"translations\" : { },\n"
          + "        \"isRequired\" : false\n"
          + "      },\n"
          + "      \"validationPredicates\" : {\n"
          + "        \"type\" : \"text\",\n"
          + "        \"minLength\" : null,\n"
          + "        \"maxLength\" : null\n"
          + "      },\n"
          + "      \"id\" : 4,\n"
          + "      \"universal\" : false,\n"
          + "      \"primaryApplicantInfoTags\" : [ ]\n"
          + "    }\n"
          + "  }, {\n"
          + "    \"type\" : \"address\",\n"
          + "    \"config\" : {\n"
          + "      \"name\" : \"Address\",\n"
          + "      \"description\" : \"\",\n"
          + "      \"questionText\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"What is your address?\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"questionHelpText\" : {\n"
          + "        \"translations\" : { },\n"
          + "        \"isRequired\" : false\n"
          + "      },\n"
          + "      \"validationPredicates\" : {\n"
          + "        \"type\" : \"address\",\n"
          + "        \"disallowPoBox\" : false\n"
          + "      },\n"
          + "      \"id\" : 5,\n"
          + "      \"universal\" : false,\n"
          + "      \"primaryApplicantInfoTags\" : [ ]\n"
          + "    }\n"
          + "  }, {\n"
          + "    \"type\" : \"address\",\n"
          + "    \"config\" : {\n"
          + "      \"name\" : \"second-address\",\n"
          + "      \"description\" : \"\",\n"
          + "      \"questionText\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"Second address question\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"questionHelpText\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"Second address question\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"validationPredicates\" : {\n"
          + "        \"type\" : \"address\",\n"
          + "        \"disallowPoBox\" : false\n"
          + "      },\n"
          + "      \"id\" : 6,\n"
          + "      \"universal\" : false,\n"
          + "      \"primaryApplicantInfoTags\" : [ ]\n"
          + "    }\n"
          + "  } ]\n"
          + "}";

  public static final String PROGRAM_JSON_WITH_PAI_TAGS =
      """
          {
          "program" : {
              "id" : 2,
              "adminName" : "pai-program",
              "adminDescription" : "admin description",
              "externalLink" : "https:usa.gov",
              "displayMode" : "PUBLIC",
              "notificationPreferences" : [],
              "localizedName" : {
              "translations" : {
                  "en_US" : "PAI Program"
              },
              "isRequired" : true
              },
              "localizedDescription" : {
              "translations" : {
                  "en_US" : "program description"
              },
              "isRequired" : true
              },
              "blockDefinitions" : [{
              "id" : 1,
              "name" : "Screen 1",
              "description" : "dummy description",
              "localizedName" : {
                  "translations" : {
                  "en_US" : "Screen 1"
                  },
                  "isRequired" : true
              },
              "localizedDescription" : {
                  "translations" : {
                  "en_US" : "dummy description"
                  },
                  "isRequired" : true
              },
              "repeaterId" : null,
              "hidePredicate" : null,
              "optionalPredicate" : null,
              "questionDefinitions" : [{
                  "id" : 3,
                  "optional" : false,
                  "addressCorrectionEnabled" : false
              }, {
                  "id" : 4,
                  "optional" : false,
                  "addressCorrectionEnabled" : false
              }, {
                  "id" : 5,
                  "optional" : false,
                  "addressCorrectionEnabled" : false
              }, {
                  "id" : 6,
                  "optional" : false,
                  "addressCorrectionEnabled" : false
              }]
              }],
              "statusDefinitions" : {
              "statuses" : []
              },
              "programType" : "DEFAULT",
              "eligibilityIsGating" : true,
              "acls" : {
              "tiProgramViewAcls" : []
              },
              "categories" : [],
              "localizedSummaryImageDescription" : null,
              "applicationSteps" : [ {
                 "title" : {
                    "translations" : {
                      "en_US" : "step one"
                    },
                    "isRequired" : true
                },
                "description" : {
                   "translations" : {
                   "en_US" : "step one"
                },
                  "isRequired" : true
                }
             }]
          },
          "questions" : [{
              "type" : "date",
              "config" : {
              "name" : "dob",
              "description" : "date description",
              "questionText" : {
                  "translations" : {
                  "en_US" : "Date of birth"
                  },
                  "isRequired" : true
              },
              "questionHelpText" : {
                  "translations" : {
                  "en_US" : "date question help text"
                  },
                  "isRequired" : true
              },
              "validationPredicates" : {
                  "type" : "date"
              },
              "id" : 3,
              "universal" : true,
              "primaryApplicantInfoTags" : []
              }
          }, {
              "type" : "name",
              "config" : {
              "name" : "name",
              "description" : "name description",
              "questionText" : {
                  "translations" : {
                  "en_US" : "Name"
                  },
                  "isRequired" : true
              },
              "questionHelpText" : {
                  "translations" : {
                  "en_US" : "name question help text"
                  },
                  "isRequired" : true
              },
              "validationPredicates" : {
                  "type" : "name"
              },
              "id" : 4,
              "universal" : true,
              "primaryApplicantInfoTags" : []
              }
          }, {
              "type" : "phone",
              "config" : {
              "name" : "phone",
              "description" : "Phone description",
              "questionText" : {
                  "translations" : {
                  "en_US" : "Phone"
                  },
                  "isRequired" : true
              },
              "questionHelpText" : {
                  "translations" : {
                  "en_US" : "Phone question help text"
                  },
                  "isRequired" : true
              },
              "validationPredicates" : {
                  "type" : "phone"
              },
              "id" : 5,
              "universal" : true,
              "primaryApplicantInfoTags" : []
              }
          }, {
              "type" : "email",
              "config" : {
              "name" : "email",
              "description" : "email description",
              "questionText" : {
                  "translations" : {
                  "en_US" : "Email"
                  },
                  "isRequired" : true
              },
              "questionHelpText" : {
                  "translations" : {
                  "en_US" : "email question help text"
                  },
                  "isRequired" : true
              },
              "validationPredicates" : {
                  "type" : "email"
              },
              "id" : 6,
              "universal" : true,
              "primaryApplicantInfoTags" : []
              }
          }]
          }
      """;

  public static final String PROGRAM_JSON_WITH_MISMATCHED_QUESTION_ID =
      """
      {
        "program" : {
          "id" : 7,
          "adminName" : "minimal-sample-program",
          "adminDescription" : "desc",
          "externalLink" : "https://github.com/civiform/civiform",
          "displayMode" : "PUBLIC",
          "notificationPreferences" : [ ],
          "localizedName" : {
            "translations" : {
              "en_US" : "Minimal Sample Program"
            },
            "isRequired" : true
          },
          "localizedDescription" : {
            "translations" : {
              "en_US" : "display description"
            },
            "isRequired" : true
          },
          "localizedShortDescription" : {
            "translations" : {
              "en_US" : "short display description"
            },
            "isRequired" : true
          },
          "localizedConfirmationMessage" : {
            "translations" : {
              "en_US" : ""
            },
            "isRequired" : true
          },
          "blockDefinitions" : [ {
            "id" : 1,
            "name" : "Screen 1",
            "description" : "Screen 1",
            "localizedName" : {
              "translations" : {
                "en_US" : "Screen 1"
              },
              "isRequired" : true
            },
            "localizedDescription" : {
              "translations" : {
                "en_US" : "Screen 1"
              },
              "isRequired" : true
            },
            "repeaterId" : null,
            "hidePredicate" : null,
            "optionalPredicate" : null,
            "questionDefinitions" : [ {
              "id" : 2,
              "optional" : true,
              "addressCorrectionEnabled" : false
            } ]
          } ],
          "statusDefinitions" : {
            "statuses" : [ ]
          },
          "programType" : "DEFAULT",
          "eligibilityIsGating" : true,
          "acls" : {
            "tiProgramViewAcls" : [ ]
          },
          "categories" : [ ],
          "localizedSummaryImageDescription" : null,
          "applicationSteps" : [ {
                 "title" : {
                    "translations" : {
                      "en_US" : "step one"
                    },
                    "isRequired" : true
                },
                "description" : {
                   "translations" : {
                   "en_US" : "step one"
                },
                  "isRequired" : true
                }
             }]
        },
        "questions" : [ {
          "type" : "name",
          "config" : {
            "name" : "Name",
            "description" : "The applicants name",
            "questionText" : {
              "translations" : {
                "am" : "ስም (የመጀመሪያ ስም እና የመጨረሻ ስም አህጽሮት ይሆናል)",
                "ko" : "성함 (이름 및 성의 경우 이니셜도 괜찮음)",
                "lo" : "ຊື່ (ນາມສະກຸນ ແລະ ຕົວອັກສອນທຳອິດຂອງນາມສະກຸນແມ່ນຖືກຕ້ອງ)",
                "so" : "Magaca (magaca koowaad iyo kan dambe okay)",
                "tl" : "Pangalan (unang pangalan at ang unang titik ng apilyedo ay okay)",
                "vi" : "Tên (tên và họ viết tắt đều được)",
                "en_US" : "Please enter your first and last name",
                "es_US" : "Nombre (nombre y la inicial del apellido está bien)",
                "zh_TW" : "姓名（名字和姓氏第一個字母便可）"
              },
              "isRequired" : true
            },
            "questionHelpText" : {
              "translations" : { },
              "isRequired" : false
            },
            "validationPredicates" : {
              "type" : "name"
            },
            "id" : 1,
            "universal" : false,
            "primaryApplicantInfoTags" : [ ]
          }
        } ]
      }
      """;
}
