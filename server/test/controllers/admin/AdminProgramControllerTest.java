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
import play.mvc.Http.RequestBuilder;
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

  /**
   * @deprecated todo: Remove the test after NORTH_STAR_APPLICANT_UI is fully enabled
   */
  @Deprecated
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
                    "localizedShortDescription",
                    "External short program description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "applicationSteps[0][title]",
                    "step one title",
                    "applicationSteps[0][description]",
                    "step one description"));

    controller.create(requestBuilder.build());

    Request request =
        fakeRequestBuilder().addCiviFormSetting("NORTH_STAR_APPLICANT_UI", "false").build();
    Result programDashboardResult = controller.index(request);
    assertThat(contentAsString(programDashboardResult)).contains("External program name");
    assertThat(contentAsString(programDashboardResult)).contains("External program description");
  }

  @Test
  public void create_northStar_showsNewProgramInListWithShortDescription() {
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
                    "localizedShortDescription",
                    "External short program description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "applicationSteps[0][title]",
                    "step one title",
                    "applicationSteps[0][description]",
                    "step one description"));

    controller.create(requestBuilder.build());

    Request request = fakeRequestBuilder().build();
    Result programDashboardResult = controller.index(request);
    assertThat(contentAsString(programDashboardResult)).contains("External program name");
    assertThat(contentAsString(programDashboardResult))
        .contains("External short program description");
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
                    "localizedShortDescription",
                    "External short program description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "applicationSteps[0][title]",
                    "step one title",
                    "applicationSteps[0][description]",
                    "step one description"));

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

  /**
   * @deprecated todo: Remove the test after NORTH_STAR_APPLICANT_UI is fully enabled
   */
  @Deprecated
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
                    "localizedShortDescription",
                    "External short program description with acls",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.SELECT_TI.getValue(),
                    "tiGroups[]",
                    "1",
                    "applicationSteps[0][title]",
                    "step one title",
                    "applicationSteps[0][description]",
                    "step one description"));

    Result result = controller.create(requestBuilder.build());

    assertThat(result.status()).isEqualTo(SEE_OTHER);

    Optional<ProgramModel> newProgram =
        versionRepository.getProgramByNameForVersion(
            "internal-program-with-acls", versionRepository.getDraftVersionOrCreate());
    assertThat(newProgram).isPresent();
    assertThat(newProgram.get().getProgramDefinition().acls().getTiProgramViewAcls())
        .containsExactly(1L);

    Request request =
        fakeRequestBuilder().addCiviFormSetting("NORTH_STAR_APPLICANT_UI", "false").build();
    Result programDashboard = controller.index(request);
    assertThat(contentAsString(programDashboard)).contains("External program name with acls");
    assertThat(contentAsString(programDashboard))
        .contains("External program description with acls");
  }

  @Test
  public void create_northStar_returnsNewProgramWithAcls() {
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
                    "localizedShortDescription",
                    "External short program description with acls",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.SELECT_TI.getValue(),
                    "tiGroups[]",
                    "1",
                    "applicationSteps[0][title]",
                    "step one title",
                    "applicationSteps[0][description]",
                    "step one description"));

    Result result = controller.create(requestBuilder.build());

    assertThat(result.status()).isEqualTo(SEE_OTHER);

    Optional<ProgramModel> newProgram =
        versionRepository.getProgramByNameForVersion(
            "internal-program-with-acls", versionRepository.getDraftVersionOrCreate());
    assertThat(newProgram).isPresent();
    assertThat(newProgram.get().getProgramDefinition().acls().getTiProgramViewAcls())
        .containsExactly(1L);

    Request request = fakeRequestBuilder().build();
    Result programDashboard = controller.index(request);
    assertThat(contentAsString(programDashboard)).contains("External program name with acls");
    assertThat(contentAsString(programDashboard))
        .contains("External short program description with acls");
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
                    "localizedShortDescription",
                    "External short program description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "eligibilityIsGating",
                    "false",
                    "applicationSteps[0][title]",
                    "step one title",
                    "applicationSteps[0][description]",
                    "step one description"));

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
                    "localizedShortDescription",
                    "External short program description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "eligibilityIsGating",
                    "true",
                    "applicationSteps[0][title]",
                    "step one title",
                    "applicationSteps[0][description]",
                    "step one description"));

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

  /**
   * @deprecated todo: Remove the test after NORTH_STAR_APPLICANT_UI is fully enabled
   */
  @Deprecated
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
                    "localizedShortDescription",
                    "External short program description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "applicationSteps[0][title]",
                    "step one title",
                    "applicationSteps[0][description]",
                    "step one description"));

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

    Request request =
        fakeRequestBuilder().addCiviFormSetting("NORTH_STAR_APPLICANT_UI", "false").build();
    Result programDashboard = controller.index(request);
    assertThat(contentAsString(programDashboard)).contains("Existing One");
    assertThat(contentAsString(programDashboard)).contains("External program name");
    assertThat(contentAsString(programDashboard)).contains("External program description");
  }

  @Test
  public void create_northStar_includesNewAndExistingProgramsInList() {
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
                    "localizedShortDescription",
                    "External short program description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "applicationSteps[0][title]",
                    "step one title",
                    "applicationSteps[0][description]",
                    "step one description"));

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

    Request request = fakeRequestBuilder().build();
    Result programDashboard = controller.index(request);
    assertThat(contentAsString(programDashboard)).contains("Existing One");
    assertThat(contentAsString(programDashboard)).contains("External program name");
    assertThat(contentAsString(programDashboard)).contains("External short program description");
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
                    "localizedShortDescription",
                    "External short program description",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "programTypeValue",
                    ProgramType.COMMON_INTAKE_FORM.getValue(),
                    "confirmedChangeCommonIntakeForm",
                    "false",
                    "applicationSteps[0][title]",
                    "step one title",
                    "applicationSteps[0][description]",
                    "step one description"));

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
                    "localizedShortDescription",
                    "External short program description",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "programTypeValue",
                    ProgramType.COMMON_INTAKE_FORM.getValue(),
                    "confirmedChangeCommonIntakeForm",
                    "false",
                    "applicationSteps[0][title]",
                    "step one title",
                    "applicationSteps[0][description]",
                    "step one description"));

    Result result = controller.create(requestBuilder.build());

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("confirm-common-intake-change");
  }

  /**
   * @deprecated todo: Remove the test after NORTH_STAR_APPLICANT_UI is fully enabled
   */
  @Deprecated
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
                    "localizedShortDescription",
                    "External short program description",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "programTypeValue",
                    ProgramType.COMMON_INTAKE_FORM.getValue(),
                    "confirmedChangeCommonIntakeForm",
                    "false",
                    "applicationSteps[0][title]",
                    "step one title",
                    "applicationSteps[0][description]",
                    "step one description"));

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

    Request request =
        fakeRequestBuilder().addCiviFormSetting("NORTH_STAR_APPLICANT_UI", "false").build();
    Result programDashboard = controller.index(request);
    assertThat(contentAsString(programDashboard)).contains("External program name");
    assertThat(contentAsString(programDashboard)).contains("External program description");
  }

  @Test
  public void create_northStar_doesNotPromptUserToConfirmCommonIntakeChangeIfNoneExists() {
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
                    "localizedShortDescription",
                    "External short program description",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "programTypeValue",
                    ProgramType.COMMON_INTAKE_FORM.getValue(),
                    "confirmedChangeCommonIntakeForm",
                    "false",
                    "applicationSteps[0][title]",
                    "step one title",
                    "applicationSteps[0][description]",
                    "step one description"));

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

    Request request = fakeRequestBuilder().build();
    Result programDashboard = controller.index(request);
    assertThat(contentAsString(programDashboard)).contains("External program name");
    assertThat(contentAsString(programDashboard)).contains("External short program description");
  }

  @Test
  public void create_allowsChangingCommonIntakeAfterConfirming() {
    ProgramBuilder.newActiveCommonIntakeForm("Old common intake").build();

    String adminName = "internal-program-name";
    String programName = "External program name";
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
                    "localizedShortDescription",
                    "External short program description",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "programTypeValue",
                    ProgramType.COMMON_INTAKE_FORM.getValue(),
                    "confirmedChangeCommonIntakeForm",
                    "true",
                    "tiGroups[]",
                    "1",
                    "applicationSteps[0][title]",
                    "step one title",
                    "applicationSteps[0][description]",
                    "step one description"));

    controller.create(requestBuilder.build());

    Optional<ProgramModel> newProgram =
        versionRepository.getProgramByNameForVersion(
            adminName, versionRepository.getDraftVersionOrCreate());
    assertThat(newProgram).isPresent();
    assertThat(newProgram.get().getProgramDefinition().isCommonIntakeForm()).isTrue();

    Result programDashboard = controller.index(fakeRequest());
    assertThat(contentAsString(programDashboard)).contains(programName);
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

  /**
   * @deprecated todo: Remove the test after NORTH_STAR_APPLICANT_UI is fully enabled
   */
  @Deprecated
  @Test
  public void update_overwritesExistingProgram() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("Existing One", "old description").build();
    RequestBuilder requestBuilder =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "adminDescription",
                    "New program slug",
                    "localizedDisplayName",
                    "New program name",
                    "localizedDisplayDescription",
                    "New program description",
                    "localizedShortDescription",
                    "New external short program description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "programTypeValue",
                    ProgramType.COMMON_INTAKE_FORM.getValue(),
                    "tiGroups[]",
                    "1",
                    "applicationSteps[0][title]",
                    "step one title",
                    "applicationSteps[0][description]",
                    "step one description"));
    controller.update(requestBuilder.build(), program.id, ProgramEditStatus.EDIT.name());

    Request request =
        fakeRequestBuilder().addCiviFormSetting("NORTH_STAR_APPLICANT_UI", "false").build();
    Result indexResult = controller.index(request);
    assertThat(contentAsString(indexResult))
        .contains("Create new program", "New program name", "New program description");
    assertThat(contentAsString(indexResult)).doesNotContain("Existing one", "short description");
  }

  @Test
  public void update_northStar_overwritesExistingProgram() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("Existing One", "old description").build();
    RequestBuilder requestBuilder =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "adminDescription",
                    "New program slug",
                    "localizedDisplayName",
                    "New program name",
                    "localizedDisplayDescription",
                    "New program description",
                    "localizedShortDescription",
                    "New external short program description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "programTypeValue",
                    ProgramType.COMMON_INTAKE_FORM.getValue(),
                    "tiGroups[]",
                    "1",
                    "applicationSteps[0][title]",
                    "step one title",
                    "applicationSteps[0][description]",
                    "step one description"));
    controller.update(requestBuilder.build(), program.id, ProgramEditStatus.EDIT.name());

    Request request = fakeRequestBuilder().build();
    Result indexResult = controller.index(request);
    assertThat(contentAsString(indexResult))
        .contains(
            "Create new program", "New program name", "New external short program description");
    assertThat(contentAsString(indexResult)).doesNotContain("Existing one", "short description");
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
                    "localizedShortDescription",
                    "short description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "programTypeValue",
                    ProgramType.DEFAULT.getValue(),
                    "tiGroups[]",
                    "1",
                    "applicationSteps[0][title]",
                    "step one title",
                    "applicationSteps[0][description]",
                    "step one description"));

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
                    "localizedShortDescription",
                    "short description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "programTypeValue",
                    ProgramType.DEFAULT.getValue(),
                    "tiGroups[]",
                    "1",
                    "applicationSteps[0][title]",
                    "step one title",
                    "applicationSteps[0][description]",
                    "step one description"));

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
                    "localizedShortDescription",
                    "short description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "programTypeValue",
                    ProgramType.DEFAULT.getValue(),
                    "tiGroups[]",
                    "1",
                    "applicationSteps[0][title]",
                    "step one title",
                    "applicationSteps[0][description]",
                    "step one description"));

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
                    "New program slug",
                    "localizedDisplayName",
                    "",
                    "localizedDisplayDescription",
                    "New program description",
                    "localizedShortDescription",
                    "New short program description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "programTypeValue",
                    ProgramType.COMMON_INTAKE_FORM.getValue(),
                    "confirmedChangeCommonIntakeForm",
                    "false",
                    "applicationSteps[0][title]",
                    "step one title",
                    "applicationSteps[0][description]",
                    "step one description"));

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
                    "New program slug",
                    "localizedDisplayName",
                    "New program name",
                    "localizedDisplayDescription",
                    "New program description",
                    "localizedShortDescription",
                    "Short program description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "programTypeValue",
                    ProgramType.COMMON_INTAKE_FORM.getValue(),
                    "confirmedChangeCommonIntakeForm",
                    "false",
                    "applicationSteps[0][title]",
                    "step one title",
                    "applicationSteps[0][description]",
                    "step one description"));

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
                    "New program slug",
                    "localizedDisplayName",
                    "New program name",
                    "localizedDisplayDescription",
                    "New program description",
                    "localizedShortDescription",
                    "New short program description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "programTypeValue",
                    ProgramType.COMMON_INTAKE_FORM.getValue(),
                    "confirmedChangeCommonIntakeForm",
                    "false",
                    "applicationSteps[0][title]",
                    "step one title",
                    "applicationSteps[0][description]",
                    "step one description"));

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

    Result redirectResult = controller.index(fakeRequest());
    assertThat(contentAsString(redirectResult)).contains("New program name");
  }

  /**
   * @deprecated todo: Remove the test after NORTH_STAR_APPLICANT_UI is fully enabled
   */
  @Deprecated
  @Test
  public void update_allowsChangingCommonIntakeAfterConfirming() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("Existing One", "old description").build();
    ProgramBuilder.newActiveCommonIntakeForm("Old common intake").build();

    String newProgramName = "External program name";
    String newProgramDescription = "New program description";
    RequestBuilder requestBuilder =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "adminDescription",
                    "New program slug",
                    "localizedDisplayName",
                    newProgramName,
                    "localizedDisplayDescription",
                    "New program description",
                    "localizedShortDescription",
                    "External program short description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "programTypeValue",
                    ProgramType.COMMON_INTAKE_FORM.getValue(),
                    "confirmedChangeCommonIntakeForm",
                    "true",
                    "applicationSteps[0][title]",
                    "step one title",
                    "applicationSteps[0][description]",
                    "step one description"));

    Result result =
        controller.update(requestBuilder.build(), program.id, ProgramEditStatus.EDIT.name());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    Optional<ProgramModel> newProgram =
        versionRepository.getProgramByNameForVersion(
            "Existing One", versionRepository.getDraftVersionOrCreate());
    assertThat(newProgram).isPresent();
    assertThat(result.redirectLocation())
        .hasValue(routes.AdminProgramBlocksController.index(newProgram.get().id).url());

    Request request =
        fakeRequestBuilder().addCiviFormSetting("NORTH_STAR_APPLICANT_UI", "false").build();
    Result redirectResult = controller.index(request);
    assertThat(contentAsString(redirectResult)).contains(newProgramName);
    assertThat(contentAsString(redirectResult)).contains(newProgramDescription);
  }

  @Test
  public void update_northStar_allowsChangingCommonIntakeAfterConfirming() throws Exception {
    ProgramModel program =
        ProgramBuilder.newDraftProgram("Existing One", "old description", "old short description")
            .build();
    ProgramBuilder.newActiveCommonIntakeForm("Old common intake").build();

    String newProgramName = "External program name";
    String newProgramShortDescription = "External program short description";
    RequestBuilder requestBuilder =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "adminDescription",
                    "New program slug",
                    "localizedDisplayName",
                    newProgramName,
                    "localizedDisplayDescription",
                    "New program description",
                    "localizedShortDescription",
                    newProgramShortDescription,
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "programTypeValue",
                    ProgramType.COMMON_INTAKE_FORM.getValue(),
                    "confirmedChangeCommonIntakeForm",
                    "true",
                    "applicationSteps[0][title]",
                    "step one title",
                    "applicationSteps[0][description]",
                    "step one description"));

    Result result =
        controller.update(requestBuilder.build(), program.id, ProgramEditStatus.EDIT.name());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    Optional<ProgramModel> newProgram =
        versionRepository.getProgramByNameForVersion(
            "Existing One", versionRepository.getDraftVersionOrCreate());
    assertThat(newProgram).isPresent();
    assertThat(result.redirectLocation())
        .hasValue(routes.AdminProgramBlocksController.index(newProgram.get().id).url());

    Request request = fakeRequestBuilder().build();
    Result redirectResult = controller.index(request);
    assertThat(contentAsString(redirectResult)).contains(newProgramName);
    assertThat(contentAsString(redirectResult)).contains(newProgramShortDescription);
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
                    "localizedShortDescription",
                    "short description",
                    "externalLink",
                    "https://external.program.link",
                    "displayMode",
                    DisplayMode.PUBLIC.getValue(),
                    "eligibilityIsGating",
                    "false",
                    "applicationSteps[0][title]",
                    "step one title",
                    "applicationSteps[0][description]",
                    "step one description"));
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
