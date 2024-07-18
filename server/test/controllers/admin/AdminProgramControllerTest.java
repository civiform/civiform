package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import models.DisplayMode;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import play.data.FormFactory;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import repository.ProgramRepository;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.question.QuestionService;
import services.settings.SettingsManifest;
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
  private SettingsManifest mockSettingsManifest;

  @Before
  public void setup() {
    programRepository = instanceOf(ProgramRepository.class);
    versionRepository = instanceOf(VersionRepository.class);
    mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    when(mockSettingsManifest.getIntakeFormEnabled()).thenReturn(true);

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
            instanceOf(RequestChecker.class),
            mockSettingsManifest);
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

    Request request = fakeRequestBuilder().build();
    Result result = controller.index(request);

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
  public void create_showsNewProgramInList() {
    RequestBuilder requestBuilder =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "adminName",
                    "internal-program-name",
                    "adminDescription",
                    "Internal program description",
                    "localizedDisplayName",
                    "External program name",
                    "localizedDisplayDescription",
                    "External program description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue()));

    controller.create(requestBuilder.build());

    Result programDashboardResult = controller.index(fakeRequestBuilder().build());
    assertThat(contentAsString(programDashboardResult)).contains("External program name");
    assertThat(contentAsString(programDashboardResult)).contains("External program description");
  }

  @Test
  public void create_redirectsToProgramImage() {
    RequestBuilder requestBuilder =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "adminName",
                    "internal-program-name",
                    "adminDescription",
                    "Internal program description",
                    "localizedDisplayName",
                    "External program name",
                    "localizedDisplayDescription",
                    "External program description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue()));

    Result result = controller.create(requestBuilder.build());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    long programId =
        versionRepository
            .getDraftVersionOrCreate()
            .getPrograms()
            .get(0)
            .getProgramDefinition()
            .id();
    assertThat(result.redirectLocation())
        .hasValue(
            routes.AdminProgramImageController.index(programId, ProgramEditStatus.CREATION.name())
                .url());
  }

  @Test
  public void create_returnsNewProgramWithAcls() {
    RequestBuilder requestBuilder =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "adminName",
                    "internal-program-with-acls",
                    "adminDescription",
                    "Internal program description with acls",
                    "localizedDisplayName",
                    "External program name with acls",
                    "localizedDisplayDescription",
                    "External program description with acls",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.SELECT_TI.getValue(),
                    "tiGroups[]",
                    "1"));

    Result result = controller.create(requestBuilder.build());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    long programId =
        versionRepository
            .getDraftVersionOrCreate()
            .getPrograms()
            .get(0)
            .getProgramDefinition()
            .id();

    Optional<ProgramModel> newProgram =
        versionRepository.getProgramByNameForVersion(
            "internal-program-with-acls", versionRepository.getDraftVersionOrCreate());
    assertThat(newProgram).isPresent();
    assertThat(newProgram.get().getProgramDefinition().acls().getTiProgramViewAcls())
        .containsExactly(1L);

    Result programDashboard = controller.index(fakeRequestBuilder().build());
    assertThat(contentAsString(programDashboard)).contains("External program name with acls");
    assertThat(contentAsString(programDashboard))
        .contains("External program description with acls");
  }

  @Test
  public void create_eligibilityIsGating_false() {
    RequestBuilder requestBuilder =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "adminName",
                    "internal-program-name",
                    "adminDescription",
                    "Internal program description",
                    "localizedDisplayName",
                    "External program name",
                    "localizedDisplayDescription",
                    "External program description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "eligibilityIsGating",
                    "false"));

    controller.create(requestBuilder.build());

    long programId =
        versionRepository
            .getDraftVersionOrCreate()
            .getPrograms()
            .get(0)
            .getProgramDefinition()
            .id();
    ProgramModel updatedDraft =
        programRepository.lookupProgram(programId).toCompletableFuture().join().get();
    assertThat(updatedDraft.getProgramDefinition().eligibilityIsGating()).isFalse();
  }

  @Test
  public void create_eligibilityIsGating_true() {
    RequestBuilder requestBuilder =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "adminName",
                    "internal-program-name",
                    "adminDescription",
                    "Internal program description",
                    "localizedDisplayName",
                    "External program name",
                    "localizedDisplayDescription",
                    "External program description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "eligibilityIsGating",
                    "true"));

    controller.create(requestBuilder.build());

    long programId =
        versionRepository
            .getDraftVersionOrCreate()
            .getPrograms()
            .get(0)
            .getProgramDefinition()
            .id();
    ProgramModel updatedDraft =
        programRepository.lookupProgram(programId).toCompletableFuture().join().get();
    assertThat(updatedDraft.getProgramDefinition().eligibilityIsGating()).isTrue();
  }

  @Test
  public void create_includesNewAndExistingProgramsInList() {
    ProgramBuilder.newActiveProgram("Existing One").build();
    RequestBuilder requestBuilder =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "adminName",
                    "internal-program-name",
                    "adminDescription",
                    "Internal program description",
                    "localizedDisplayName",
                    "External program name",
                    "localizedDisplayDescription",
                    "External program description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue()));

    Result result = controller.create(requestBuilder.build());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    long programId =
        versionRepository
            .getDraftVersionOrCreate()
            .getPrograms()
            .get(0)
            .getProgramDefinition()
            .id();
    assertThat(result.redirectLocation())
        .hasValue(
            routes.AdminProgramImageController.index(programId, ProgramEditStatus.CREATION.name())
                .url());

    Result programDashboard = controller.index(fakeRequestBuilder().build());
    assertThat(contentAsString(programDashboard)).contains("Existing One");
    assertThat(contentAsString(programDashboard)).contains("External program name");
    assertThat(contentAsString(programDashboard)).contains("External program description");
  }

  @Test
  public void create_showsErrorsBeforePromptingUserToConfirmCommonIntakeChange() {
    ProgramBuilder.newActiveCommonIntakeForm("Old common intake").build();
    RequestBuilder requestBuilder =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "adminName",
                    "internal-program-name",
                    "adminDescription",
                    "Internal program description",
                    "localizedDisplayName",
                    "",
                    "localizedDisplayDescription",
                    "External program description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "isCommonIntakeForm",
                    "true",
                    "confirmedChangeCommonIntakeForm",
                    "false"));

    Result result = controller.create(requestBuilder.build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains("A public display name for the program is required");
  }

  @Test
  public void create_promptsUserToConfirmCommonIntakeChange() {
    ProgramBuilder.newActiveCommonIntakeForm("Old common intake").build();
    RequestBuilder requestBuilder =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "adminName",
                    "internal-program-name",
                    "adminDescription",
                    "Internal program description",
                    "localizedDisplayName",
                    "External program name",
                    "localizedDisplayDescription",
                    "External program description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "isCommonIntakeForm",
                    "true",
                    "confirmedChangeCommonIntakeForm",
                    "false"));

    Result result = controller.create(requestBuilder.build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("confirm-common-intake-change");
  }

  @Test
  public void create_doesNotPromptUserToConfirmCommonIntakeChangeIfNoneExists() {
    RequestBuilder requestBuilder =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "adminName",
                    "internal-program-name",
                    "adminDescription",
                    "Internal program description",
                    "localizedDisplayName",
                    "External program name",
                    "localizedDisplayDescription",
                    "External program description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "isCommonIntakeForm",
                    "true",
                    "confirmedChangeCommonIntakeForm",
                    "false"));

    Result result = controller.create(requestBuilder.build());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    long programId =
        versionRepository
            .getDraftVersionOrCreate()
            .getPrograms()
            .get(0)
            .getProgramDefinition()
            .id();
    assertThat(result.redirectLocation())
        .hasValue(
            routes.AdminProgramImageController.index(programId, ProgramEditStatus.CREATION.name())
                .url());

    Result programDashboard = controller.index(fakeRequestBuilder().build());
    assertThat(contentAsString(programDashboard)).contains("External program name");
    assertThat(contentAsString(programDashboard)).contains("External program description");
  }

  @Test
  public void create_allowsChangingCommonIntakeAfterConfirming() {
    ProgramBuilder.newActiveCommonIntakeForm("Old common intake").build();

    String adminName = "internal-program-name";
    String programName = "External program name";
    String programDescription = "External program description";
    RequestBuilder requestBuilder =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "adminName",
                    adminName,
                    "adminDescription",
                    "Internal program description",
                    "localizedDisplayName",
                    programName,
                    "localizedDisplayDescription",
                    programDescription,
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "isCommonIntakeForm",
                    "true",
                    "confirmedChangeCommonIntakeForm",
                    "true",
                    "tiGroups[]",
                    "1"));

    controller.create(requestBuilder.build());

    Optional<ProgramModel> newProgram =
        versionRepository.getProgramByNameForVersion(
            adminName, versionRepository.getDraftVersionOrCreate());
    assertThat(newProgram).isPresent();
    assertThat(newProgram.get().getProgramDefinition().isCommonIntakeForm()).isTrue();

    Result programDashboard = controller.index(fakeRequestBuilder().build());
    assertThat(contentAsString(programDashboard)).contains(programName);
    assertThat(contentAsString(programDashboard)).contains(programDescription);
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
    Request request = fakeRequestBuilder().build();
    ProgramModel program = ProgramBuilder.newActiveProgram("test program").build();

    assertThatThrownBy(() -> controller.edit(request, program.id, ProgramEditStatus.EDIT.name()))
        .isInstanceOf(NotChangeableException.class);
  }

  @Test
  public void newVersionFrom_onlyActive_editActiveReturnsNewDraft() {
    // When there's a draft, editing the active one instead edits the existing draft.
    String programName = "test program";
    Request request = fakeRequestBuilder().build();
    ProgramModel activeProgram =
        ProgramBuilder.newActiveProgram(programName, "active description").build();

    Result result = controller.newVersionFrom(request, activeProgram.id);
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
    Request request = fakeRequestBuilder().build();
    ProgramModel activeProgram =
        ProgramBuilder.newActiveProgram(programName, "active description").build();
    ProgramModel draftProgram =
        ProgramBuilder.newDraftProgram(programName, "draft description").build();

    Result result = controller.newVersionFrom(request, activeProgram.id);

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
  public void update_overwritesExistingProgram() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("Existing One", "old description").build();
    RequestBuilder requestBuilder =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "adminDescription",
                    "New internal program description",
                    "localizedDisplayName",
                    "New external program name",
                    "localizedDisplayDescription",
                    "New external program description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "isCommonIntakeForm",
                    "true",
                    "tiGroups[]",
                    "1"));
    controller.update(requestBuilder.build(), program.id, ProgramEditStatus.EDIT.name());

    Result indexResult = controller.index(fakeRequestBuilder().build());
    assertThat(contentAsString(indexResult))
        .contains(
            "Create new program", "New external program name", "New external program description");
    assertThat(contentAsString(indexResult)).doesNotContain("Existing one", "old description");
  }

  @Test
  public void update_statusEdit_redirectsToProgramEditBlocks() throws ProgramNotFoundException {
    ProgramModel program = ProgramBuilder.newDraftProgram("Program", "description").build();
    RequestBuilder requestBuilder =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "adminDescription",
                    "adminDescription",
                    "localizedDisplayName",
                    "Program",
                    "localizedDisplayDescription",
                    "description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "isCommonIntakeForm",
                    "false",
                    "tiGroups[]",
                    "1"));

    Result result =
        controller.update(requestBuilder.build(), program.id, ProgramEditStatus.EDIT.name());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(routes.AdminProgramBlocksController.index(program.id).url());
  }

  @Test
  public void update_statusCreation_redirectsToProgramImage() throws ProgramNotFoundException {
    ProgramModel program = ProgramBuilder.newDraftProgram("Program", "description").build();
    RequestBuilder requestBuilder =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "adminDescription",
                    "adminDescription",
                    "localizedDisplayName",
                    "Program",
                    "localizedDisplayDescription",
                    "description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "isCommonIntakeForm",
                    "false",
                    "tiGroups[]",
                    "1"));

    Result result =
        controller.update(requestBuilder.build(), program.id, ProgramEditStatus.CREATION.name());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.AdminProgramImageController.index(program.id, ProgramEditStatus.CREATION.name())
                .url());
  }

  @Test
  public void update_statusCreationEdit_redirectsToProgramImage() throws ProgramNotFoundException {
    ProgramModel program = ProgramBuilder.newDraftProgram("Program", "description").build();
    RequestBuilder requestBuilder =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "adminDescription",
                    "adminDescription",
                    "localizedDisplayName",
                    "Program",
                    "localizedDisplayDescription",
                    "description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "isCommonIntakeForm",
                    "false",
                    "tiGroups[]",
                    "1"));

    Result result =
        controller.update(
            requestBuilder.build(), program.id, ProgramEditStatus.CREATION_EDIT.name());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(
            routes.AdminProgramImageController.index(
                    program.id, ProgramEditStatus.CREATION_EDIT.name())
                .url());
  }

  @Test
  public void update_showsErrorsBeforePromptingUserToConfirmCommonIntakeChange() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("Existing One", "old description").build();
    ProgramBuilder.newActiveCommonIntakeForm("Old common intake").build();

    RequestBuilder requestBuilder =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "adminDescription",
                    "New internal program description",
                    "localizedDisplayName",
                    "",
                    "localizedDisplayDescription",
                    "New external program description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "isCommonIntakeForm",
                    "true",
                    "confirmedChangeCommonIntakeForm",
                    "false"));

    Result result =
        controller.update(requestBuilder.build(), program.id, ProgramEditStatus.EDIT.name());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result))
        .contains("A public display name for the program is required");
  }

  @Test
  public void update_promptsUserToConfirmCommonIntakeChange() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("Existing One", "old description").build();
    ProgramBuilder.newActiveCommonIntakeForm("Old common intake").build();

    RequestBuilder requestBuilder =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "adminDescription",
                    "New internal program description",
                    "localizedDisplayName",
                    "New external program name",
                    "localizedDisplayDescription",
                    "New external program description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "isCommonIntakeForm",
                    "true",
                    "confirmedChangeCommonIntakeForm",
                    "false"));

    Result result =
        controller.update(requestBuilder.build(), program.id, ProgramEditStatus.EDIT.name());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("confirm-common-intake-change");
  }

  @Test
  public void update_doesNotPromptUserToConfirmCommonIntakeChangeIfNoneExists() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("Existing One", "old description").build();

    RequestBuilder requestBuilder =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "adminDescription",
                    "New internal program description",
                    "localizedDisplayName",
                    "New external program name",
                    "localizedDisplayDescription",
                    "New external program description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "isCommonIntakeForm",
                    "true",
                    "confirmedChangeCommonIntakeForm",
                    "false"));

    Result result =
        controller.update(requestBuilder.build(), program.id, ProgramEditStatus.EDIT.name());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    long programId =
        versionRepository
            .getDraftVersionOrCreate()
            .getPrograms()
            .get(0)
            .getProgramDefinition()
            .id();
    assertThat(result.redirectLocation())
        .hasValue(routes.AdminProgramBlocksController.index(programId).url());

    Result redirectResult = controller.index(fakeRequestBuilder().build());
    assertThat(contentAsString(redirectResult)).contains("New external program name");
  }

  @Test
  public void update_allowsChangingCommonIntakeAfterConfirming() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("Existing One", "old description").build();
    ProgramBuilder.newActiveCommonIntakeForm("Old common intake").build();

    String newProgramName = "External program name";
    String newProgramDescription = "External program description";
    RequestBuilder requestBuilder =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "adminDescription",
                    "New internal program description",
                    "localizedDisplayName",
                    newProgramName,
                    "localizedDisplayDescription",
                    newProgramDescription,
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "isCommonIntakeForm",
                    "true",
                    "confirmedChangeCommonIntakeForm",
                    "true"));

    Result result =
        controller.update(requestBuilder.build(), program.id, ProgramEditStatus.EDIT.name());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    Optional<ProgramModel> newProgram =
        versionRepository.getProgramByNameForVersion(
            "Existing One", versionRepository.getDraftVersionOrCreate());
    assertThat(newProgram).isPresent();
    assertThat(result.redirectLocation())
        .hasValue(routes.AdminProgramBlocksController.index(newProgram.get().id).url());

    Result redirectResult = controller.index(fakeRequestBuilder().build());
    assertThat(contentAsString(redirectResult)).contains(newProgramName);
    assertThat(contentAsString(redirectResult)).contains(newProgramDescription);
  }

  @Test
  public void update_changesEligibilityIsGating() throws Exception {
    ProgramModel program = ProgramBuilder.newDraftProgram("one").build();
    assertThat(program.getProgramDefinition().eligibilityIsGating()).isTrue();

    RequestBuilder request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "adminDescription",
                    "adminDescription",
                    "localizedDisplayName",
                    "Program",
                    "localizedDisplayDescription",
                    "description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "eligibilityIsGating",
                    "false"));
    Result result = controller.update(request.build(), program.id, ProgramEditStatus.EDIT.name());

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
    Result result = controller.publishProgram(fakeRequestBuilder().build(), program.id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue(routes.AdminProgramController.index().url());

    assertThat(versionRepository.isDraft(program)).isFalse();
  }

  @Test
  public void publishProgram_nonDraftProgram_throwsException() throws Exception {
    ProgramModel program = ProgramBuilder.newActiveProgram("active").build();
    assertThatThrownBy(() -> controller.publishProgram(fakeRequestBuilder().build(), program.id))
        .isInstanceOf(NotChangeableException.class);
  }
}
