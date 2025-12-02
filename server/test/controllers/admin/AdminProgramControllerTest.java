package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import models.ApplicationStep;
import models.DisplayMode;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import play.data.FormFactory;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.ProgramRepository;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.program.ProgramType;
import services.question.QuestionService;
import support.ProgramBuilder;
import views.admin.programs.ProgramEditStatus;
import views.admin.programs.ProgramIndexView;
import views.admin.programs.ProgramMetaDataEditView;
import views.admin.programs.ProgramNewOneView;
import views.html.helper.CSRF;

public class AdminProgramControllerTest extends ResetPostgres {

  private AdminProgramController controller;
  private ProgramRepository programRepository;
  private VersionRepository versionRepository;

  private static final String ADMIN_NAME = "internal-program-name";
  private static final String PROGRAM_NAME = "External program name";
  private static final String SHORT_DESCRIPTION = "External short program description";

  private static final ImmutableMap<String, String> DEFAULT_FORM_FIELDS =
      ImmutableMap.of(
          "adminName", ADMIN_NAME,
          "adminDescription", "Internal program description",
          "localizedDisplayName", PROGRAM_NAME,
          "localizedDisplayDescription", "External program description",
          "localizedShortDescription", SHORT_DESCRIPTION,
          "externalLink", "https://external.program.link",
          "displayMode", DisplayMode.PUBLIC.getValue(),
          "applicationSteps[0][title]", "step one title",
          "applicationSteps[0][description]", "step one description");

  private ProgramModel getDraftProgram() {
    return versionRepository.getDraftVersionOrCreate().getPrograms().get(0);
  }

  private void assertRedirectsToProgramImagePage(
      Result result, long programId, ProgramEditStatus status) {
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(routes.AdminProgramImageController.index(programId, status.name()).url());
  }

  @Before
  public void setup() {
    programRepository = instanceOf(ProgramRepository.class);
    versionRepository = instanceOf(VersionRepository.class);

    controller =
        new AdminProgramController(
            instanceOf(ProgramService.class),
            instanceOf(QuestionService.class),
            instanceOf(ProgramIndexView.class),
            instanceOf(ProgramNewOneView.class),
            instanceOf(ProgramMetaDataEditView.class),
            versionRepository,
            instanceOf(ProfileUtils.class),
            instanceOf(FormFactory.class),
            instanceOf(RequestChecker.class));
  }

  @Test
  public void index_withNoPrograms() {
    Result result = controller.index(fakeRequest());
    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    assertThat(result.charset()).hasValue("utf-8");
    assertThat(contentAsString(result)).contains("Programs");
  }

  @Test
  public void index_returnsPrograms() {
    ProgramBuilder.newDraftProgram("one").build();
    ProgramBuilder.newDraftProgram("two").build();

    Result result = controller.index(fakeRequest());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("one");
    assertThat(contentAsString(result)).contains("two");
  }

  @Test
  public void newOne_returnsExpectedForm() {
    Request request = fakeRequestBuilder().addCSRFToken().build();

    Result result = controller.newOne(request);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("New program");
    assertThat(contentAsString(result)).contains(CSRF.getToken(request.asScala()).value());
  }

  @Test
  public void create_returnsFormWithErrorMessage() {
    Request request =
        fakeRequestBuilder()
            .addCSRFToken()
            .bodyForm(ImmutableMap.of("name", "", "description", ""))
            .build();

    Result result = controller.create(request);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("A program URL is required");
    assertThat(contentAsString(result)).contains("New program");
    assertThat(contentAsString(result)).contains(CSRF.getToken(request.asScala()).value());
  }

  @Test
  public void create_northStar_showsNewProgramInListWithShortDescription() {
    controller.create(fakeRequestBuilder().bodyForm(DEFAULT_FORM_FIELDS).build());

    Result programDashboardResult = controller.index(fakeRequest());
    assertThat(contentAsString(programDashboardResult)).contains(PROGRAM_NAME);
    assertThat(contentAsString(programDashboardResult)).contains(SHORT_DESCRIPTION);
  }

  @Test
  public void create_redirectsToProgramImage() {
    Result result = controller.create(fakeRequestBuilder().bodyForm(DEFAULT_FORM_FIELDS).build());

    assertRedirectsToProgramImagePage(result, getDraftProgram().id, ProgramEditStatus.CREATION);
  }

  @Test
  public void create_northStar_returnsNewProgramWithAcls() {
    Map<String, String> formData = new HashMap<>(DEFAULT_FORM_FIELDS);
    formData.put("displayMode", DisplayMode.SELECT_TI.getValue());
    formData.put("tiGroups[]", "1");

    Result result = controller.create(fakeRequestBuilder().bodyForm(formData).build());

    assertThat(result.status()).isEqualTo(SEE_OTHER);

    Optional<ProgramModel> newProgram =
        versionRepository.getProgramByNameForVersion(
            ADMIN_NAME, versionRepository.getDraftVersionOrCreate());
    assertThat(newProgram).isPresent();
    assertThat(newProgram.get().getProgramDefinition().acls().getTiProgramViewAcls())
        .containsExactly(1L);

    Result programDashboard = controller.index(fakeRequest());
    assertThat(contentAsString(programDashboard)).contains(PROGRAM_NAME);
    assertThat(contentAsString(programDashboard)).contains(SHORT_DESCRIPTION);
  }

  @Test
  public void create_eligibilityIsGating_false() {
    Map<String, String> formData = new HashMap<>(DEFAULT_FORM_FIELDS);
    formData.put("eligibilityIsGating", "false");

    controller.create(fakeRequestBuilder().bodyForm(formData).build());

    ProgramModel updatedDraft = getDraftProgram();
    assertThat(updatedDraft.getProgramDefinition().eligibilityIsGating()).isFalse();
  }

  @Test
  public void create_eligibilityIsGating_true() {
    Map<String, String> formData = new HashMap<>(DEFAULT_FORM_FIELDS);
    formData.put("eligibilityIsGating", "true");

    controller.create(fakeRequestBuilder().bodyForm(formData).build());

    ProgramModel updatedDraft = getDraftProgram();
    assertThat(updatedDraft.getProgramDefinition().eligibilityIsGating()).isTrue();
  }

  @Test
  public void create_northStar_includesNewAndExistingProgramsInList() {
    ProgramBuilder.newActiveProgram("Existing One").build();

    Result result = controller.create(fakeRequestBuilder().bodyForm(DEFAULT_FORM_FIELDS).build());

    assertRedirectsToProgramImagePage(result, getDraftProgram().id, ProgramEditStatus.CREATION);

    Result programDashboard = controller.index(fakeRequest());
    assertThat(contentAsString(programDashboard)).contains("Existing One");
    assertThat(contentAsString(programDashboard)).contains(PROGRAM_NAME);
    assertThat(contentAsString(programDashboard)).contains(SHORT_DESCRIPTION);
  }

  @Test
  public void create_showsErrorsBeforePromptingUserToConfirmCommonIntakeChange() {
    ProgramBuilder.newActiveCommonIntakeForm("Old common intake").build();
    Map<String, String> formData = new HashMap<>(DEFAULT_FORM_FIELDS);
    formData.put("localizedDisplayName", ""); // Empty name to force validation error
    formData.put("programTypeValue", ProgramType.COMMON_INTAKE_FORM.getValue());
    formData.put("confirmedChangePreScreenerForm", "false");

    Result result = controller.create(fakeRequestBuilder().bodyForm(formData).build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains("A public display name for the program is required");
  }

  @Test
  public void create_promptsUserToConfirmCommonIntakeChange() {
    ProgramBuilder.newActiveCommonIntakeForm("Old common intake").build();
    Map<String, String> formData = new HashMap<>(DEFAULT_FORM_FIELDS);
    formData.put("programTypeValue", ProgramType.COMMON_INTAKE_FORM.getValue());
    formData.put("confirmedChangePreScreenerForm", "false");

    Result result = controller.create(fakeRequestBuilder().bodyForm(formData).build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("confirm-common-intake-change");
  }

  @Test
  public void create_northStar_doesNotPromptUserToConfirmCommonIntakeChangeIfNoneExists() {
    Map<String, String> formData = new HashMap<>(DEFAULT_FORM_FIELDS);
    formData.put("programTypeValue", ProgramType.COMMON_INTAKE_FORM.getValue());
    formData.put("confirmedChangePreScreenerForm", "false");

    Result result = controller.create(fakeRequestBuilder().bodyForm(formData).build());

    assertRedirectsToProgramImagePage(result, getDraftProgram().id, ProgramEditStatus.CREATION);

    Result programDashboard = controller.index(fakeRequest());
    assertThat(contentAsString(programDashboard)).contains(PROGRAM_NAME);
    assertThat(contentAsString(programDashboard)).contains(SHORT_DESCRIPTION);
  }

  @Test
  public void create_allowsChangingCommonIntakeAfterConfirming() {
    ProgramBuilder.newActiveCommonIntakeForm("Old common intake").build();

    Map<String, String> formData = new HashMap<>(DEFAULT_FORM_FIELDS);
    formData.put("programTypeValue", ProgramType.COMMON_INTAKE_FORM.getValue());
    formData.put("confirmedChangePreScreenerForm", "true");
    formData.put("tiGroups[]", "1");

    controller.create(fakeRequestBuilder().bodyForm(formData).build());

    Optional<ProgramModel> newProgram =
        versionRepository.getProgramByNameForVersion(
            ADMIN_NAME, versionRepository.getDraftVersionOrCreate());
    assertThat(newProgram).isPresent();
    assertThat(newProgram.get().getProgramDefinition().isCommonIntakeForm()).isTrue();

    Result programDashboard = controller.index(fakeRequest());
    assertThat(contentAsString(programDashboard)).contains(PROGRAM_NAME);
  }

  @Test
  public void edit_withInvalidProgram_throwsProgramNotFoundException() {
    Request request = fakeRequest();
    assertThatThrownBy(() -> controller.edit(request, 1L, ProgramEditStatus.EDIT.name()))
        .isInstanceOf(ProgramNotFoundException.class);
  }

  @Test
  public void edit_returnsExpectedForm() throws Exception {
    Request request = fakeRequestBuilder().addCSRFToken().build();
    ProgramModel program = ProgramBuilder.newDraftProgram("test program").build();

    Result result = controller.edit(request, program.id, ProgramEditStatus.EDIT.name());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("test program");
    assertThat(contentAsString(result)).contains("Edit program");
    assertThat(contentAsString(result)).contains(CSRF.getToken(request.asScala()).value());
  }

  @Test
  public void edit_withNonDraftProgram_throwsNotChangeableException() {
    ProgramModel program = ProgramBuilder.newActiveProgram("test program").build();

    assertThatThrownBy(
            () -> controller.edit(fakeRequest(), program.id, ProgramEditStatus.EDIT.name()))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void newVersionFrom_onlyActive_editActiveReturnsNewDraft() {
    // When there's a draft, editing the active one instead edits the existing draft.
    String programName = "test program";
    ProgramModel activeProgram =
        ProgramBuilder.newActiveProgram(programName, "active description").build();

    Result result = controller.newVersionFrom(fakeRequest(), activeProgram.id);
    Optional<ProgramModel> newDraft =
        versionRepository.getProgramByNameForVersion(
            programName, versionRepository.getDraftVersionOrCreate());

    // A new draft is made and redirected to.
    assertThat(newDraft).isPresent();
    assertThat(newDraft.get().getProgramDefinition().adminDescription())
        .isEqualTo("active description");

    // Redirect is to the blocks edit page.
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(
            controllers.admin.routes.AdminProgramBlocksController.index(newDraft.get().id).url());
  }

  @Test
  public void newVersionFrom_withDraft_editActiveReturnsDraft() {
    // When there's a draft, editing the active one instead edits the existing draft.
    String programName = "test program";
    ProgramModel activeProgram =
        ProgramBuilder.newActiveProgram(programName, "active description").build();
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram(programName, "draft description").build();

    Result result = controller.newVersionFrom(fakeRequest(), activeProgram.id);

    // Redirect is to the blocks edit page.
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(
            controllers.admin.routes.AdminProgramBlocksController.index(draftProgram.id).url());

    ProgramModel updatedDraft =
        programRepository.lookupProgram(draftProgram.id).toCompletableFuture().join().get();
    assertThat(updatedDraft.getProgramDefinition().adminDescription())
        .isEqualTo("draft description");
  }

  @Test
  public void update_invalidProgram_returnsNotFound() {
    Request request =
        fakeRequestBuilder()
            .bodyForm(ImmutableMap.of("name", "name", "description", "description"))
            .build();

    assertThatThrownBy(
            () -> controller.update(request, /* programId= */ 1L, ProgramEditStatus.EDIT.name()))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void update_nonDraftProgram_throwsException() throws Exception {
    ProgramModel activeProgram =
        ProgramBuilder.newActiveProgram("fakeName", "active description").build();
    Request request = fakeRequest();

    assertThatThrownBy(
            () ->
                controller.update(
                    request, /* programId= */ activeProgram.id, ProgramEditStatus.EDIT.name()))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void update_invalidInput_returnsFormWithErrors() throws Exception {
    ProgramModel program = ProgramBuilder.newDraftProgram("Existing One").build();
    Request request =
        fakeRequestBuilder()
            .addCSRFToken()
            .bodyForm(ImmutableMap.of("name", "", "description", ""))
            .build();

    Result result = controller.update(request, program.id, ProgramEditStatus.EDIT.name());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Edit program");
    assertThat(contentAsString(result)).contains(CSRF.getToken(request.asScala()).value());
  }

  @Test
  public void update_northStar_overwritesExistingProgram() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("Existing One", "old description").build();

    Map<String, String> formData = new HashMap<>(DEFAULT_FORM_FIELDS);
    formData.put("adminDescription", "New program slug");
    formData.put("localizedDisplayDescription", "New program description");
    formData.put("localizedShortDescription", "New external short program description");
    formData.put("programTypeValue", ProgramType.COMMON_INTAKE_FORM.getValue());
    formData.put("tiGroups[]", "1");

    controller.update(
        fakeRequestBuilder().bodyForm(formData).build(), program.id, ProgramEditStatus.EDIT.name());

    Result indexResult = controller.index(fakeRequestBuilder().build());
    String content = contentAsString(indexResult);
    assertThat(content)
        .contains("Create new program", PROGRAM_NAME, "New external short program description");
    assertThat(content).doesNotContain("Existing one", "short description");
  }

  @Test
  public void update_statusEdit_redirectsToProgramEditBlocks() throws ProgramNotFoundException {
    ProgramModel program = ProgramBuilder.newDraftProgram("Program", "description").build();
    Map<String, String> formData = new HashMap<>(DEFAULT_FORM_FIELDS);
    formData.put("localizedDisplayDescription", "description");
    formData.put("localizedShortDescription", "short description");
    formData.put("programTypeValue", ProgramType.DEFAULT.getValue());
    formData.put("tiGroups[]", "1");

    Result result =
        controller.update(
            fakeRequestBuilder().bodyForm(formData).build(),
            program.id,
            ProgramEditStatus.EDIT.name());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(routes.AdminProgramBlocksController.index(program.id).url());
  }

  @Test
  public void update_statusCreation_redirectsToProgramImage() throws ProgramNotFoundException {
    ProgramModel program = ProgramBuilder.newDraftProgram("Program", "description").build();
    Map<String, String> formData = new HashMap<>(DEFAULT_FORM_FIELDS);
    formData.put("localizedDisplayDescription", "description");
    formData.put("localizedShortDescription", "short description");
    formData.put("programTypeValue", ProgramType.DEFAULT.getValue());
    formData.put("tiGroups[]", "1");

    Result result =
        controller.update(
            fakeRequestBuilder().bodyForm(formData).build(),
            program.id,
            ProgramEditStatus.CREATION.name());

    assertRedirectsToProgramImagePage(result, program.id, ProgramEditStatus.CREATION);
  }

  @Test
  public void update_statusCreationEdit_redirectsToProgramImage() throws ProgramNotFoundException {
    ProgramModel program = ProgramBuilder.newDraftProgram("Program", "description").build();
    Map<String, String> formData = new HashMap<>(DEFAULT_FORM_FIELDS);
    formData.put("localizedDisplayDescription", "description");
    formData.put("localizedShortDescription", "short description");
    formData.put("programTypeValue", ProgramType.DEFAULT.getValue());
    formData.put("tiGroups[]", "1");

    Result result =
        controller.update(
            fakeRequestBuilder().bodyForm(formData).build(),
            program.id,
            ProgramEditStatus.CREATION_EDIT.name());

    assertRedirectsToProgramImagePage(result, program.id, ProgramEditStatus.CREATION_EDIT);
  }

  @Test
  public void update_showsErrorsBeforePromptingUserToConfirmCommonIntakeChange() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("Existing One", "old description").build();
    ProgramBuilder.newActiveCommonIntakeForm("Old common intake").build();

    Map<String, String> formData = new HashMap<>(DEFAULT_FORM_FIELDS);
    formData.put("localizedDisplayName", ""); // Empty name to force validation error
    formData.put("programTypeValue", ProgramType.COMMON_INTAKE_FORM.getValue());
    formData.put("confirmedChangePreScreenerForm", "false");

    Result result =
        controller.update(
            fakeRequestBuilder().bodyForm(formData).build(),
            program.id,
            ProgramEditStatus.EDIT.name());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains("A public display name for the program is required");
  }

  @Test
  public void update_promptsUserToConfirmCommonIntakeChange() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("Existing One", "old description").build();
    ProgramBuilder.newActiveCommonIntakeForm("Old common intake").build();

    Map<String, String> formData = new HashMap<>(DEFAULT_FORM_FIELDS);
    formData.put("programTypeValue", ProgramType.COMMON_INTAKE_FORM.getValue());
    formData.put("confirmedChangePreScreenerForm", "false");

    Result result =
        controller.update(
            fakeRequestBuilder().bodyForm(formData).build(),
            program.id,
            ProgramEditStatus.EDIT.name());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("confirm-common-intake-change");
  }

  @Test
  public void update_doesNotPromptUserToConfirmCommonIntakeChangeIfNoneExists() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("Existing One", "old description").build();

    Map<String, String> formData = new HashMap<>(DEFAULT_FORM_FIELDS);
    formData.put("programTypeValue", ProgramType.COMMON_INTAKE_FORM.getValue());
    formData.put("confirmedChangePreScreenerForm", "false");

    Result result =
        controller.update(
            fakeRequestBuilder().bodyForm(formData).build(),
            program.id,
            ProgramEditStatus.EDIT.name());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    long programId = getDraftProgram().id;
    assertThat(result.redirectLocation())
        .hasValue(routes.AdminProgramBlocksController.index(programId).url());

    Result redirectResult = controller.index(fakeRequest());
    assertThat(contentAsString(redirectResult)).contains(PROGRAM_NAME);
  }

  @Test
  public void update_northStar_allowsChangingCommonIntakeAfterConfirming() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("Existing One", "old description", "old short description")
            .build();
    ProgramBuilder.newActiveCommonIntakeForm("Old common intake").build();

    String newProgramShortDescription = "External program short description";

    Map<String, String> formData = new HashMap<>(DEFAULT_FORM_FIELDS);
    formData.put("localizedShortDescription", newProgramShortDescription);
    formData.put("programTypeValue", ProgramType.COMMON_INTAKE_FORM.getValue());
    formData.put("confirmedChangePreScreenerForm", "true");

    Result result =
        controller.update(
            fakeRequestBuilder().bodyForm(formData).build(),
            program.id,
            ProgramEditStatus.EDIT.name());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    Optional<ProgramModel> newProgram =
        versionRepository.getProgramByNameForVersion(
            "Existing One", versionRepository.getDraftVersionOrCreate());
    assertThat(newProgram).isPresent();
    assertThat(result.redirectLocation())
        .hasValue(routes.AdminProgramBlocksController.index(newProgram.get().id).url());

    Result redirectResult = controller.index(fakeRequest());
    assertThat(contentAsString(redirectResult)).contains(PROGRAM_NAME);
    assertThat(contentAsString(redirectResult)).contains(newProgramShortDescription);
  }

  @Test
  public void update_changesEligibilityIsGating() throws Exception {
    ProgramModel program = ProgramBuilder.newDraftProgram("one").build();
    assertThat(program.getProgramDefinition().eligibilityIsGating()).isTrue();

    Map<String, String> formData = new HashMap<>(DEFAULT_FORM_FIELDS);
    formData.put("localizedDisplayDescription", "description");
    formData.put("localizedShortDescription", "short description");
    formData.put("eligibilityIsGating", "false");

    Result result =
        controller.update(
            fakeRequestBuilder().bodyForm(formData).build(),
            program.id,
            ProgramEditStatus.EDIT.name());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(routes.AdminProgramBlocksController.index(program.id).url());

    ProgramModel updatedDraft =
        programRepository.lookupProgram(program.id).toCompletableFuture().join().get();
    assertThat(updatedDraft.getProgramDefinition().eligibilityIsGating()).isFalse();
  }

  @Test
  public void publishProgram() throws Exception {
    ProgramModel program = ProgramBuilder.newDraftProgram("one").build();
    Result result = controller.publishProgram(fakeRequest(), program.id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue(routes.AdminProgramController.index().url());

    assertThat(versionRepository.isDraft(program)).isFalse();
  }

  @Test
  public void publishProgram_nonDraftProgram_throwsException() throws Exception {
    ProgramModel program = ProgramBuilder.newActiveProgram("active").build();
    assertThatThrownBy(() -> controller.publishProgram(fakeRequest(), program.id))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void buildApplicationSteps_transformsDataIntoApplicationStepObjects() {
    List<Map<String, String>> applicationStepsData =
        List.of(
            Map.of("title", "title one", "description", "description one"),
            Map.of("title", "title two", "description", "description two"));
    ImmutableList<ApplicationStep> applicationSteps =
        controller.buildApplicationSteps(applicationStepsData);

    assertThat(applicationSteps.size()).isEqualTo(2);
    assertThat(applicationSteps.get(0).getTitle().getDefault()).isEqualTo("title one");
    assertThat(applicationSteps.get(0).getDescription().getDefault()).isEqualTo("description one");
    assertThat(applicationSteps.get(1).getTitle().getDefault()).isEqualTo("title two");
    assertThat(applicationSteps.get(1).getDescription().getDefault()).isEqualTo("description two");
  }

  @Test
  public void buildApplicationSteps_filtersStepsWithMissingKeysAndBlankSteps() {
    List<Map<String, String>> applicationStepsData =
        List.of(
            Map.of("title", "title one", "description", "description one"),
            Map.of("title", "title two"),
            Map.of("description", "description two"),
            Map.of("title", "", "description", ""));
    ImmutableList<ApplicationStep> applicationSteps =
        controller.buildApplicationSteps(applicationStepsData);

    assertThat(applicationSteps.size()).isEqualTo(1);
    assertThat(applicationSteps.get(0).getTitle().getDefault()).isEqualTo("title one");
    assertThat(applicationSteps.get(0).getDescription().getDefault()).isEqualTo("description one");
  }

  @Test
  public void buildApplicationSteps_includesStepsWithTitleOrDescription() {
    List<Map<String, String>> applicationStepsData =
        List.of(
            Map.of("title", "title one", "description", ""),
            Map.of("title", "", "description", "description two"));
    ImmutableList<ApplicationStep> applicationSteps =
        controller.buildApplicationSteps(applicationStepsData);

    assertThat(applicationSteps.size()).isEqualTo(2);
    assertThat(applicationSteps.get(0).getTitle().getDefault()).isEqualTo("title one");
    assertThat(applicationSteps.get(0).getDescription().getDefault()).isEqualTo("");
    assertThat(applicationSteps.get(1).getTitle().getDefault()).isEqualTo("");
    assertThat(applicationSteps.get(1).getDescription().getDefault()).isEqualTo("description two");
  }
}
