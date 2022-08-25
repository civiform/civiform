package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.UNAUTHORIZED;

import featureflags.FeatureFlags;
import java.util.Optional;
import models.Applicant;
import models.Application;
import models.LifecycleStage;
import models.Program;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.test.Helpers;
import repository.ResetPostgres;
import services.program.ProgramNotFoundException;
import support.ProgramBuilder;

public class AdminApplicationControllerTest extends ResetPostgres {
  // NOTE: the controller asserts the user is valid on the program that applications are requested
  // for. However, we currently have no pattern for setting a profile in a test request, so we can't
  // make affirmative tests.
  private AdminApplicationController controller;

  @Before
  public void setupController() {
    controller = instanceOf(AdminApplicationController.class);
  }

  @Test
  public void index_noUser_errors() throws Exception {
    long programId = ProgramBuilder.newActiveProgram().buildDefinition().id();
    Request request = addCSRFToken(Helpers.fakeRequest()).build();
    Result result =
        controller.index(
            request,
            programId,
            /* search= */ Optional.empty(),
            /* page= */ Optional.of(1), // Needed to skip redirect.
            /* fromDate= */ Optional.empty(),
            /* untilDate= */ Optional.empty());
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void updateStatus_flagDisabled() throws Exception {
    Program program = ProgramBuilder.newDraftProgram("test name", "test description").build();
    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    Application application =
        Application.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    Request request =
        addCSRFToken(
                Helpers.fakeRequest()
                    .session(FeatureFlags.APPLICATION_STATUS_TRACKING_ENABLED, "false"))
            .build();
    Result result = controller.updateStatus(request, program.id, application.id);
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void updateStatus_programNotFound() {
    Program program = ProgramBuilder.newDraftProgram("test name", "test description").build();
    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    Application application =
        Application.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    Request request = addCSRFToken(Helpers.fakeRequest()).build();
    assertThatThrownBy(() -> controller.updateStatus(request, Long.MAX_VALUE, application.id))
        .isInstanceOf(ProgramNotFoundException.class);
  }

  @Test
  public void updateStatus_notAdmin() throws Exception {
    Program program = ProgramBuilder.newDraftProgram("test name", "test description").build();
    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    Application application =
        Application.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    Request request = addCSRFToken(Helpers.fakeRequest()).build();
    Result result = controller.updateStatus(request, program.id, application.id);
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void updateNote_flagDisabled() throws Exception {
    Program program = ProgramBuilder.newDraftProgram("test name", "test description").build();
    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    Application application =
        Application.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    // Initialize the controller explicitly to override status tracking enablement.
    controller =
        new AdminApplicationController(
            instanceOf(ProgramService.class),
            instanceOf(ApplicantService.class),
            instanceOf(ExporterService.class),
            instanceOf(JsonExporter.class),
            instanceOf(PdfExporter.class),
            instanceOf(ProgramApplicationListView.class),
            instanceOf(ProgramApplicationView.class),
            instanceOf(ProgramAdminApplicationService.class),
            instanceOf(ProfileUtils.class),
            instanceOf(MessagesApi.class),
            instanceOf(DateConverter.class),
            /* nowProvider= */ new Provider<LocalDateTime>() {
              @Override
              public LocalDateTime get() {
                return LocalDateTime.now(ZoneId.systemDefault());
              }
            },
            /* statusTrackingEnabled= */ new Provider<Boolean>() {
              @Override
              public Boolean get() {
                return false;
              }
            });

    Request request = addCSRFToken(Helpers.fakeRequest()).build();
    Result result = controller.updateNote(request, program.id, application.id);
    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void updateNote_programNotFound() {
    Program program = ProgramBuilder.newDraftProgram("test name", "test description").build();
    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    Application application =
        Application.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    Request request = addCSRFToken(Helpers.fakeRequest()).build();
    assertThatThrownBy(() -> controller.updateNote(request, Long.MAX_VALUE, application.id))
        .isInstanceOf(ProgramNotFoundException.class);
  }

  @Test
  public void updateNote_notAdmin() throws Exception {
    Program program = ProgramBuilder.newDraftProgram("test name", "test description").build();
    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    Application application =
        Application.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    Request request = addCSRFToken(Helpers.fakeRequest()).build();
    Result result = controller.updateNote(request, program.id, application.id);
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }
}
