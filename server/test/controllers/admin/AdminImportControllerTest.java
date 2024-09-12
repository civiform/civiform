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
  public void hxImportProgram_noDuplicatesEnabled_draftsExist_error() {
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
    assertThat(contentAsString(result)).contains("Name-1");
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
      "{\n"
          + "  \"program\" : {\n"
          + "    \"id\" : 9,\n"
          + "    \"adminName\" : \"no-questions\",\n"
          + "    \"adminDescription\" : \"\",\n"
          + "    \"externalLink\" : \"https://www.example.com\",\n"
          + "    \"displayMode\" : \"PUBLIC\",\n"
          + "    \"notificationPreferences\" : [ ],\n"
          + "    \"localizedName\" : {\n"
          + "      \"translations\" : {\n"
          + "        \"en_US\" : \"Program With No Questions\"\n"
          + "      },\n"
          + "      \"isRequired\" : true\n"
          + "    },\n"
          + "    \"localizedDescription\" : {\n"
          + "      \"translations\" : {\n"
          + "        \"en_US\" : \"No questions\"\n"
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
          + "      \"optionalPredicate\" : null,\n"
          + "      \"questionDefinitions\" : [ ]\n"
          + "    } ],\n"
          + "    \"statusDefinitions\" : {\n"
          + "      \"statuses\" : [ ]\n"
          + "    },\n"
          + "    \"programType\" : \"DEFAULT\",\n"
          + "    \"eligibilityIsGating\" : true,\n"
          + "    \"acls\" : {\n"
          + "      \"tiProgramViewAcls\" : [ ]\n"
          + "    },\n"
          + "    \"categories\" : [ ], \n"
          + "    \"localizedSummaryImageDescription\" : null\n"
          + "  }\n"
          + "}";

  public static final String PROGRAM_JSON_WITH_ONE_QUESTION =
      "{\n"
          + "  \"program\" : {\n"
          + "    \"id\" : 7,\n"
          + "    \"adminName\" : \"minimal-sample-program\",\n"
          + "    \"adminDescription\" : \"desc\",\n"
          + "    \"externalLink\" : \"https://github.com/civiform/civiform\",\n"
          + "    \"displayMode\" : \"PUBLIC\",\n"
          + "    \"notificationPreferences\" : [ ],\n"
          + "    \"localizedName\" : {\n"
          + "      \"translations\" : {\n"
          + "        \"en_US\" : \"Minimal Sample Program\"\n"
          + "      },\n"
          + "      \"isRequired\" : true\n"
          + "    },\n"
          + "    \"localizedDescription\" : {\n"
          + "      \"translations\" : {\n"
          + "        \"en_US\" : \"display description\"\n"
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
          + "      \"description\" : \"Screen 1\",\n"
          + "      \"localizedName\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"Screen 1\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"localizedDescription\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"Screen 1\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"repeaterId\" : null,\n"
          + "      \"hidePredicate\" : null,\n"
          + "      \"optionalPredicate\" : null,\n"
          + "      \"questionDefinitions\" : [ {\n"
          + "        \"id\" : 1,\n"
          + "        \"optional\" : true,\n"
          + "        \"addressCorrectionEnabled\" : false\n"
          + "      } ]\n"
          + "    } ],\n"
          + "    \"statusDefinitions\" : {\n"
          + "      \"statuses\" : [ ]\n"
          + "    },\n"
          + "    \"programType\" : \"DEFAULT\",\n"
          + "    \"eligibilityIsGating\" : true,\n"
          + "    \"acls\" : {\n"
          + "      \"tiProgramViewAcls\" : [ ]\n"
          + "    },\n"
          + "    \"categories\" : [ ], \n"
          + "    \"localizedSummaryImageDescription\" : null\n"
          + "  },\n"
          + "  \"questions\" : [ {\n"
          + "    \"type\" : \"name\",\n"
          + "    \"config\" : {\n"
          + "      \"name\" : \"Name\",\n"
          + "      \"description\" : \"The applicant's name\",\n"
          + "      \"questionText\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"am\" : \"ስም (የመጀመሪያ ስም እና የመጨረሻ ስም አህጽሮት ይሆናል)\",\n"
          + "          \"ko\" : \"성함 (이름 및 성의 경우 이니셜도 괜찮음)\",\n"
          + "          \"lo\" : \"ຊື່ (ນາມສະກຸນ ແລະ ຕົວອັກສອນທຳອິດຂອງນາມສະກຸນແມ່ນຖືກຕ້ອງ)\",\n"
          + "          \"so\" : \"Magaca (magaca koowaad iyo kan dambe okay)\",\n"
          + "          \"tl\" : \"Pangalan (unang pangalan at ang unang titik ng apilyedo ay"
          + " okay)\",\n"
          + "          \"vi\" : \"Tên (tên và họ viết tắt đều được)\",\n"
          + "          \"en_US\" : \"Please enter your first and last name\",\n"
          + "          \"es_US\" : \"Nombre (nombre y la inicial del apellido está bien)\",\n"
          + "          \"zh_TW\" : \"姓名（名字和姓氏第一個字母便可）\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"questionHelpText\" : {\n"
          + "        \"translations\" : { },\n"
          + "        \"isRequired\" : false\n"
          + "      },\n"
          + "      \"validationPredicates\" : {\n"
          + "        \"type\" : \"name\"\n"
          + "      },\n"
          + "      \"id\" : 1,\n"
          + "      \"universal\" : false,\n"
          + "      \"primaryApplicantInfoTags\" : [ ]\n"
          + "    }\n"
          + "  } ]\n"
          + "}";
  public static final String PROGRAM_JSON_WITH_ENUMERATORS =
      "{\n"
          + "  \"program\" : {\n"
          + "    \"id\" : 18,\n"
          + "    \"adminName\" : \"nested-enumerator\",\n"
          + "    \"adminDescription\" : \"\",\n"
          + "    \"externalLink\" : \"\",\n"
          + "    \"displayMode\" : \"PUBLIC\",\n"
          + "    \"notificationPreferences\" : [ ],\n"
          + "    \"localizedName\" : {\n"
          + "      \"translations\" : {\n"
          + "        \"en_US\" : \"nest enumerator program\"\n"
          + "      },\n"
          + "      \"isRequired\" : true\n"
          + "    },\n"
          + "    \"localizedDescription\" : {\n"
          + "      \"translations\" : {\n"
          + "        \"en_US\" : \"nested enumerator program\"\n"
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
          + "      \"id\" : 5,\n"
          + "      \"name\" : \"Screen 5\",\n"
          + "      \"description\" : \"Screen 5 description\",\n"
          + "      \"localizedName\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"Screen 5\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"localizedDescription\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"Screen 5 description\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"repeaterId\" : null,\n"
          + "      \"hidePredicate\" : null,\n"
          + "      \"optionalPredicate\" : null,\n"
          + "      \"questionDefinitions\" : [ {\n"
          + "        \"id\" : 13,\n"
          + "        \"optional\" : false,\n"
          + "        \"addressCorrectionEnabled\" : false\n"
          + "      } ]\n"
          + "    }, {\n"
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
          + "      \"optionalPredicate\" : null,\n"
          + "      \"questionDefinitions\" : [ {\n"
          + "        \"id\" : 10,\n"
          + "        \"optional\" : false,\n"
          + "        \"addressCorrectionEnabled\" : false\n"
          + "      } ]\n"
          + "    }, {\n"
          + "      \"id\" : 3,\n"
          + "      \"name\" : \"Screen 3 (repeated from 1)\",\n"
          + "      \"description\" : \"Screen 3 description\",\n"
          + "      \"localizedName\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"Screen 3 (repeated from 1)\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"localizedDescription\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"Screen 3 description\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"repeaterId\" : 1,\n"
          + "      \"hidePredicate\" : null,\n"
          + "      \"optionalPredicate\" : null,\n"
          + "      \"questionDefinitions\" : [ {\n"
          + "        \"id\" : 94,\n"
          + "        \"optional\" : false,\n"
          + "        \"addressCorrectionEnabled\" : false\n"
          + "      } ]\n"
          + "    }, {\n"
          + "      \"id\" : 4,\n"
          + "      \"name\" : \"Screen 4 (repeated from 3)\",\n"
          + "      \"description\" : \"Screen 4 description\",\n"
          + "      \"localizedName\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"Screen 4 (repeated from 3)\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"localizedDescription\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"Screen 4 description\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"repeaterId\" : 3,\n"
          + "      \"hidePredicate\" : null,\n"
          + "      \"optionalPredicate\" : null,\n"
          + "      \"questionDefinitions\" : [ {\n"
          + "        \"id\" : 95,\n"
          + "        \"optional\" : false,\n"
          + "        \"addressCorrectionEnabled\" : false\n"
          + "      } ]\n"
          + "    } ],\n"
          + "    \"programType\" : \"DEFAULT\",\n"
          + "    \"eligibilityIsGating\" : true,\n"
          + "    \"acls\" : {\n"
          + "      \"tiProgramViewAcls\" : [ ]\n"
          + "    },\n"
          + "    \"localizedSummaryImageDescription\" : null,\n"
          + "    \"categories\" : [ ]\n"
          + "  },\n"
          + "  \"questions\" : [ {\n"
          + "    \"type\" : \"name\",\n"
          + "    \"config\" : {\n"
          + "      \"name\" : \"Sample Name Question\",\n"
          + "      \"description\" : \"description\",\n"
          + "      \"questionText\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"What is your name?\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"questionHelpText\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"help text\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"validationPredicates\" : {\n"
          + "        \"type\" : \"name\"\n"
          + "      },\n"
          + "      \"id\" : 13,\n"
          + "      \"universal\" : true,\n"
          + "      \"primaryApplicantInfoTags\" : [ \"APPLICANT_NAME\" ]\n"
          + "    }\n"
          + "  }, {\n"
          + "    \"type\" : \"enumerator\",\n"
          + "    \"config\" : {\n"
          + "      \"name\" : \"Sample Enumerator Question\",\n"
          + "      \"description\" : \"description\",\n"
          + "      \"questionText\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"List all members of your household.\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"questionHelpText\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"help text\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"validationPredicates\" : {\n"
          + "        \"type\" : \"enumerator\",\n"
          + "        \"minEntities\" : null,\n"
          + "        \"maxEntities\" : null\n"
          + "      },\n"
          + "      \"id\" : 10,\n"
          + "      \"universal\" : false,\n"
          + "      \"primaryApplicantInfoTags\" : [ ]\n"
          + "    },\n"
          + "    \"entityType\" : {\n"
          + "      \"translations\" : {\n"
          + "        \"en_US\" : \"household member\"\n"
          + "      },\n"
          + "      \"isRequired\" : true\n"
          + "    }\n"
          + "  }, {\n"
          + "    \"type\" : \"enumerator\",\n"
          + "    \"config\" : {\n"
          + "      \"name\" : \"cats\",\n"
          + "      \"description\" : \"\",\n"
          + "      \"questionText\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"Please list each cat owned by $this\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"questionHelpText\" : {\n"
          + "        \"translations\" : { },\n"
          + "        \"isRequired\" : false\n"
          + "      },\n"
          + "      \"validationPredicates\" : {\n"
          + "        \"type\" : \"enumerator\",\n"
          + "        \"minEntities\" : null,\n"
          + "        \"maxEntities\" : null\n"
          + "      },\n"
          + "      \"id\" : 94,\n"
          + "      \"enumeratorId\" : 10,\n"
          + "      \"universal\" : false,\n"
          + "      \"primaryApplicantInfoTags\" : [ ]\n"
          + "    },\n"
          + "    \"entityType\" : {\n"
          + "      \"translations\" : {\n"
          + "        \"en_US\" : \"cat\"\n"
          + "      },\n"
          + "      \"isRequired\" : true\n"
          + "    }\n"
          + "  }, {\n"
          + "    \"type\" : \"text\",\n"
          + "    \"config\" : {\n"
          + "      \"name\" : \"cat-color\",\n"
          + "      \"description\" : \"\",\n"
          + "      \"questionText\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"What color is $this?\"\n"
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
          + "      \"id\" : 95,\n"
          + "      \"enumeratorId\" : 94,\n"
          + "      \"universal\" : false,\n"
          + "      \"primaryApplicantInfoTags\" : [ ]\n"
          + "    }\n"
          + "  } ]\n"
          + "}";
  public static final String PROGRAM_JSON_WITH_PREDICATES =
      "{\n"
          + "  \"program\" : {\n"
          + "    \"id\" : 6,\n"
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
          + "        \"rootNode\" : { \n"
          + "          \"node\" : { \n"
          + "            \"type\" : \"or\", \n"
          + "            \"children\" : [ { \n"
          + "              \"node\" : { \n"
          + "                \"type\" : \"and\", \n"
          + "                \"children\" : [ { \n"
          + "                  \"node\" : { \n"
          + "                    \"type\" : \"leaf\", \n"
          + "                    \"questionId\" : 11, \n"
          + "                    \"scalar\" : \"SELECTIONS\", \n"
          + "                    \"operator\" : \"ANY_OF\", \n"
          + "                    \"value\" : { \n"
          + "                      \"value\" : \"[\\\"0\\\", \\\"9\\\", \\\"11\\\"]\", \n"
          + "                      \"type\" : \"LIST_OF_STRINGS\" \n"
          + "                    } \n"
          + "                  } \n"
          + "                }, { \n"
          + "                  \"node\" : { \n"
          + "                    \"type\" : \"leaf\", \n"
          + "                    \"questionId\" : 11, \n"
          + "                    \"scalar\" : \"SELECTION\", \n"
          + "                    \"operator\" : \"IN\", \n"
          + "                    \"value\" : { \n"
          + "                      \"value\" : \"[\\\"1\\\"]\", \n"
          + "                      \"type\" : \"LIST_OF_STRINGS\" \n"
          + "                    } \n"
          + "                  } \n"
          + "                } ] \n"
          + "              } \n"
          + "            } ] \n"
          + "          } \n"
          + "        }, \n"
          + "          \"action\" : \"ELIGIBLE_BLOCK\"\n"
          + "        }\n"
          + "      },\n"
          + "      \"optionalPredicate\" : null,\n"
          + "      \"questionDefinitions\" : [ {\n"
          + "        \"id\" : 11,\n"
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
          + "            \"questionId\" : 11,\n"
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
          + "        \"id\" : 12,\n"
          + "        \"optional\" : false,\n"
          + "        \"addressCorrectionEnabled\" : false\n"
          + "      } ]\n"
          + "    } ],\n"
          + "    \"statusDefinitions\" : {\n"
          + "      \"statuses\" : [ ]\n"
          + "    },\n"
          + "    \"programType\" : \"DEFAULT\",\n"
          + "    \"eligibilityIsGating\" : true,\n"
          + "    \"acls\" : {\n"
          + "      \"tiProgramViewAcls\" : [ ]\n"
          + "    },\n"
          + "    \"categories\" : [ ], \n"
          + "    \"localizedSummaryImageDescription\" : null\n"
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
          + "      \"id\" : 11,\n"
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
          + "      \"id\" : 12,\n"
          + "      \"universal\" : false,\n"
          + "      \"primaryApplicantInfoTags\" : [ ]\n"
          + "    }\n"
          + "  } ]\n"
          + "}";
  public static final String PROGRAM_JSON_WITH_PAI_TAGS =
      "{\n"
          + "  \"program\" : {\n"
          + "    \"id\" : 2,\n"
          + "    \"adminName\" : \"pai-program\",\n"
          + "    \"adminDescription\" : \"admin description\",\n"
          + "    \"externalLink\" : \"https:usa.gov\",\n"
          + "    \"displayMode\" : \"PUBLIC\",\n"
          + "    \"notificationPreferences\" : [ ],\n"
          + "    \"localizedName\" : {\n"
          + "      \"translations\" : {\n"
          + "        \"en_US\" : \"PAI Program\"\n"
          + "      },\n"
          + "      \"isRequired\" : true\n"
          + "    },\n"
          + "    \"localizedDescription\" : {\n"
          + "      \"translations\" : {\n"
          + "        \"en_US\" : \"program description\"\n"
          + "      },\n"
          + "      \"isRequired\" : true\n"
          + "    },\n"
          + "    \"localizedConfirmationMessage\" : {\n"
          + "      \"translations\" : {\n"
          + "        \"en_US\" : \"This is the _custom confirmation message_ with markdown\\r\\n"
          + "[This is a link](https:www.example.com)\\r\\n"
          + "This is a list:\\r\\n"
          + "* Item 1\\r\\n"
          + "* Item 2\\r\\n"
          + "\\r\\n"
          + "There are some empty lines below this that should be preserved\\r\\n"
          + "\\r\\n"
          + "\\r\\n"
          + "This link should be autodetected: https:www.example.com\\r\\n"
          + "\"\n"
          + "      },\n"
          + "      \"isRequired\" : true\n"
          + "    },\n"
          + "    \"blockDefinitions\" : [ {\n"
          + "      \"id\" : 1,\n"
          + "      \"name\" : \"Screen 1\",\n"
          + "      \"description\" : \"dummy description\",\n"
          + "      \"localizedName\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"Screen 1\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"localizedDescription\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"dummy description\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"repeaterId\" : null,\n"
          + "      \"hidePredicate\" : null,\n"
          + "      \"optionalPredicate\" : null,\n"
          + "      \"questionDefinitions\" : [ {\n"
          + "        \"id\" : 3,\n"
          + "        \"optional\" : false,\n"
          + "        \"addressCorrectionEnabled\" : false\n"
          + "      }, {\n"
          + "        \"id\" : 4,\n"
          + "        \"optional\" : false,\n"
          + "        \"addressCorrectionEnabled\" : false\n"
          + "      }, {\n"
          + "        \"id\" : 5,\n"
          + "        \"optional\" : false,\n"
          + "        \"addressCorrectionEnabled\" : false\n"
          + "      }, {\n"
          + "        \"id\" : 6,\n"
          + "        \"optional\" : false,\n"
          + "        \"addressCorrectionEnabled\" : false\n"
          + "      } ]\n"
          + "    } ],\n"
          + "    \"statusDefinitions\" : {\n"
          + "      \"statuses\" : [ ]\n"
          + "    },\n"
          + "    \"programType\" : \"DEFAULT\",\n"
          + "    \"eligibilityIsGating\" : true,\n"
          + "    \"acls\" : {\n"
          + "      \"tiProgramViewAcls\" : [ ]\n"
          + "    },\n"
          + "    \"categories\" : [ ], \n"
          + "    \"localizedSummaryImageDescription\" : null\n"
          + "  },\n"
          + "  \"questions\" : [ {\n"
          + "    \"type\" : \"date\",\n"
          + "    \"config\" : {\n"
          + "      \"name\" : \"dob\",\n"
          + "      \"description\" : \"date description\",\n"
          + "      \"questionText\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"Date of birth\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"questionHelpText\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"date question help text\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"validationPredicates\" : {\n"
          + "        \"type\" : \"date\"\n"
          + "      },\n"
          + "      \"id\" : 3,\n"
          + "      \"universal\" : true,\n"
          + "      \"primaryApplicantInfoTags\" : [ ]\n"
          + "    }\n"
          + "  }, {\n"
          + "    \"type\" : \"name\",\n"
          + "    \"config\" : {\n"
          + "      \"name\" : \"name\",\n"
          + "      \"description\" : \"name description\",\n"
          + "      \"questionText\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"Name\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"questionHelpText\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"name question help text\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"validationPredicates\" : {\n"
          + "        \"type\" : \"name\"\n"
          + "      },\n"
          + "      \"id\" : 4,\n"
          + "      \"universal\" : true,\n"
          + "      \"primaryApplicantInfoTags\" : [ ]\n"
          + "    }\n"
          + "  }, {\n"
          + "    \"type\" : \"phone\",\n"
          + "    \"config\" : {\n"
          + "      \"name\" : \"phone\",\n"
          + "      \"description\" : \"Phone description\",\n"
          + "      \"questionText\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"Phone\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"questionHelpText\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"Phone question help text\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"validationPredicates\" : {\n"
          + "        \"type\" : \"phone\"\n"
          + "      },\n"
          + "      \"id\" : 5,\n"
          + "      \"universal\" : true,\n"
          + "      \"primaryApplicantInfoTags\" : [ ]\n"
          + "    }\n"
          + "  }, {\n"
          + "    \"type\" : \"email\",\n"
          + "    \"config\" : {\n"
          + "      \"name\" : \"email\",\n"
          + "      \"description\" : \"email description\",\n"
          + "      \"questionText\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"Email\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"questionHelpText\" : {\n"
          + "        \"translations\" : {\n"
          + "          \"en_US\" : \"email question help text\"\n"
          + "        },\n"
          + "        \"isRequired\" : true\n"
          + "      },\n"
          + "      \"validationPredicates\" : {\n"
          + "        \"type\" : \"email\"\n"
          + "      },\n"
          + "      \"id\" : 6,\n"
          + "      \"universal\" : true,\n"
          + "      \"primaryApplicantInfoTags\" : [ ]\n"
          + "    }\n"
          + "  } ]\n"
          + "}";
}
