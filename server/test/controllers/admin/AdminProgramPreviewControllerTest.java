package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import controllers.WithMockedProfiles;
import models.AccountModel;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.program.ProgramNotFoundException;

public class AdminProgramPreviewControllerTest extends WithMockedProfiles {

  private AdminProgramPreviewController controller;

  @Before
  public void setup() {
    resetDatabase();
    controller = instanceOf(AdminProgramPreviewController.class);
  }

  @Test
  public void preview_redirectsToProgramReviewPage() {
    ProgramModel program = resourceCreator().insertActiveProgram("test-slug");
    AccountModel adminAccount = createGlobalAdminWithMockedProfile();

    String programSlug = "test-slug";
    Request request =
        fakeRequestBuilder().addCiviFormSetting("NORTH_STAR_APPLICANT_UI", "false").build();
    Result result = controller.preview(request, programSlug).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(
            controllers.applicant.routes.ApplicantProgramReviewController.reviewWithApplicantId(
                    adminAccount.ownedApplicantIds().get(0),
                    Long.toString(program.id),
                    /* isFromUrlCall= */ false)
                .url());
  }

  @Test
  public void back_draftProgram_redirectsToProgramEditView() {
    ProgramModel program = resourceCreator().insertDraftProgram("some program");
    Result result = controller.back(fakeRequest(), program.id).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(controllers.admin.routes.AdminProgramBlocksController.index(program.id).url());
  }

  @Test
  public void back_nonDraftProgram_redirectsToProgramReadOnlyView() {
    ProgramModel program = resourceCreator().insertActiveProgram("another program");
    Result result = controller.back(fakeRequest(), program.id).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(
            controllers.admin.routes.AdminProgramBlocksController.readOnlyIndex(program.id).url());
  }

  @Test
  public void pdfPreview_draft_okAndHasPdf() throws ProgramNotFoundException {
    ProgramModel program = resourceCreator().insertDraftProgram("draft program");

    Result result = controller.pdfPreview(fakeRequest(), program.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType().get()).isEqualTo("application/pdf");
    assertThat(result.header("Content-Disposition")).isPresent();
    assertThat(result.header("Content-Disposition").get())
        .startsWith("attachment; filename=\"draft program");
  }

  @Test
  public void pdfPreview_active_okAndHasPdf() throws ProgramNotFoundException {
    ProgramModel program = resourceCreator().insertActiveProgram("active program");

    Result result = controller.pdfPreview(fakeRequest(), program.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType().get()).isEqualTo("application/pdf");
    assertThat(result.header("Content-Disposition")).isPresent();
    assertThat(result.header("Content-Disposition").get())
        .startsWith("attachment; filename=\"active program");
  }
}
