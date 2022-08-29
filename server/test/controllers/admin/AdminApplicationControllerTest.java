package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.UNAUTHORIZED;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileFactory;
import auth.ProfileUtils;
import com.google.inject.util.Providers;
import featureflags.FeatureFlags;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import models.Applicant;
import models.Application;
import models.LifecycleStage;
import models.Program;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.core.context.session.SessionStore;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.test.Helpers;
import repository.DatabaseExecutionContext;
import repository.ResetPostgres;
import services.DateConverter;
import services.applicant.ApplicantService;
import services.applications.ProgramAdminApplicationService;
import services.export.ExporterService;
import services.export.JsonExporter;
import services.export.PdfExporter;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import support.ProgramBuilder;
import views.admin.programs.ProgramApplicationListView;
import views.admin.programs.ProgramApplicationView;

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
  public void index() throws Exception {

    controller = makeNoOpProfileController();
    Program program = ProgramBuilder.newActiveProgram().build();
    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    Application.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();
    Request request = addCSRFToken(Helpers.fakeRequest()).build();
    Result result =
        controller.index(
            request,
            program.id,
            /* search= */ Optional.empty(),
            /* page= */ Optional.of(1), // Needed to skip redirect.
            /* fromDate= */ Optional.empty(),
            /* untilDate= */ Optional.empty());
    assertThat(result.status()).isEqualTo(OK);
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

    Request request =
        addCSRFToken(
                Helpers.fakeRequest()
                    .session(FeatureFlags.APPLICATION_STATUS_TRACKING_ENABLED, "false"))
            .build();
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

  // Returns a controller with a faked ProfileUtils to bypass acl checks.
  AdminApplicationController makeNoOpProfileController() {
    return new AdminApplicationController(
        instanceOf(ProgramService.class),
        instanceOf(ApplicantService.class),
        instanceOf(ExporterService.class),
        instanceOf(JsonExporter.class),
        instanceOf(PdfExporter.class),
        instanceOf(ProgramApplicationListView.class),
        instanceOf(ProgramApplicationView.class),
        instanceOf(ProgramAdminApplicationService.class),
        instanceOf(ProfileUtilsNoOpTester.class),
        instanceOf(MessagesApi.class),
        instanceOf(DateConverter.class),
        Providers.of(LocalDateTime.now(ZoneId.systemDefault())),
        instanceOf(FeatureFlags.class));
  }

  // A test version of ProfileUtils that disable functionality that is hard
  // to otherwise test around.
  static class ProfileUtilsNoOpTester extends ProfileUtils {
    private final ProfileTester profileTester;

    @Inject
    public ProfileUtilsNoOpTester(
        SessionStore sessionStore, ProfileFactory profileFactory, ProfileTester profileTester) {
      super(sessionStore, profileFactory);
      this.profileTester = profileTester;
    }

    // Returns a Profile that will never fail auth checks.
    @Override
    public Optional<CiviFormProfile> currentUserProfile(Http.RequestHeader request) {
      return Optional.of(profileTester);
    }

    // A test version of CiviFormProfile that disable functionality that is hard
    // to otherwise test around.
    static class ProfileTester extends CiviFormProfile {

      @Inject
      public ProfileTester(
          DatabaseExecutionContext dbContext,
          HttpExecutionContext httpContext,
          CiviFormProfileData profileData) {
        super(dbContext, httpContext, profileData);
      }

      // Always passes and does no checks.
      @Override
      public CompletableFuture<Void> checkProgramAuthorization(String programName) {
        return CompletableFuture.completedFuture(null);
      }
    }
  }
}
