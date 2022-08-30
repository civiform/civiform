package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.mvc.Http.Status.UNAUTHORIZED;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileFactory;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.util.Providers;
import controllers.admin.AdminApplicationControllerTest.ProfileUtilsNoOpTester.ProfileTester;
import featureflags.FeatureFlags;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import models.Account;
import models.Applicant;
import models.Application;
import models.ApplicationEvent;
import models.LifecycleStage;
import models.Program;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.core.context.session.SessionStore;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.test.Helpers;
import repository.DatabaseExecutionContext;
import repository.ResetPostgres;
import services.DateConverter;
import services.LocalizedStrings;
import services.applicant.ApplicantService;
import services.application.ApplicationEventDetails;
import services.applications.ProgramAdminApplicationService;
import services.export.ExporterService;
import services.export.JsonExporter;
import services.export.PdfExporter;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.program.StatusDefinitions;
import services.program.StatusDefinitions.Status;
import support.ProgramBuilder;
import views.admin.programs.ProgramApplicationListView;
import views.admin.programs.ProgramApplicationView;

public class AdminApplicationControllerTest extends ResetPostgres {
  private static final StatusDefinitions.Status APPROVED_STATUS =
      StatusDefinitions.Status.builder()
          .setStatusText("Approved")
          .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Approved"))
          .setLocalizedEmailBodyText(
              Optional.of(LocalizedStrings.withDefaultValue("Approved email body")))
          .build();

  private static final StatusDefinitions.Status REJECTED_STATUS =
      StatusDefinitions.Status.builder()
          .setStatusText("Rejected")
          .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Rejected"))
          .setLocalizedEmailBodyText(
              Optional.of(LocalizedStrings.withDefaultValue("Rejected email body")))
          .build();

  private static final StatusDefinitions.Status WITH_STATUS_TRANSLATIONS =
      StatusDefinitions.Status.builder()
          .setStatusText("With translations")
          .setLocalizedStatusText(
              LocalizedStrings.create(
                  ImmutableMap.of(
                      Locale.US, "With translations",
                      Locale.FRENCH, "With translations (French)")))
          .setLocalizedEmailBodyText(
              Optional.of(
                  LocalizedStrings.create(
                      ImmutableMap.of(
                          Locale.US, "A translatable email body",
                          Locale.FRENCH, "A translatable email body (French)"))))
          .build();

  private static final ImmutableList<Status> ORIGINAL_STATUSES =
      ImmutableList.of(APPROVED_STATUS, REJECTED_STATUS, WITH_STATUS_TRANSLATIONS);
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

    controller = makeNoOpProfileController(/* adminAccount= */ Optional.empty());
    Program program = ProgramBuilder.newActiveProgram().build();
    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    applicant.refresh();
    Application application =
        Application.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();
    application.refresh();
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
    Program program = ProgramBuilder.newActiveProgram("test name", "test description").build();
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
    Program program = ProgramBuilder.newActiveProgram("test name", "test description").build();
    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    Application application =
        Application.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    Request request = addCSRFToken(Helpers.fakeRequest()).build();
    assertThatThrownBy(() -> controller.updateStatus(request, Long.MAX_VALUE, application.id))
        .isInstanceOf(ProgramNotFoundException.class);
  }

  @Test
  public void updateStatus_notAdmin() throws Exception {
    Program program = ProgramBuilder.newActiveProgram("test name", "test description").build();
    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    Application application =
        Application.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    Request request = addCSRFToken(Helpers.fakeRequest()).build();
    Result result = controller.updateStatus(request, program.id, application.id);
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  // TODO(#3263): Add invalid data tests.
  // TODO(#3263): Add email set tests.
  @Test
  public void updateStatus() throws Exception {
    // Setup
    Instant start = Instant.now();
    Account adminAccount = resourceCreator.insertAccount();
    controller = makeNoOpProfileController(Optional.of(adminAccount));
    Program program =
        ProgramBuilder.newActiveProgram("test name", "test description")
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .build();
    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    Application application =
        Application.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    Request request =
        addCSRFToken(
                Helpers.fakeRequest().bodyForm(Map.of("newStatus", APPROVED_STATUS.statusText())))
            .build();

    // Execute
    Result result = controller.updateStatus(request, program.id, application.id);

    // Evaluate
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    application.refresh();
    assertThat(application.getApplicationEvents()).hasSize(1);
    ApplicationEvent gotEvent = application.getApplicationEvents().get(0);
    assertThat(gotEvent.getEventType()).isEqualTo(ApplicationEventDetails.Type.STATUS_CHANGE);
    assertThat(gotEvent.getDetails().statusEvent()).isPresent();
    assertThat(gotEvent.getDetails().statusEvent().get().statusText())
        .isEqualTo(APPROVED_STATUS.statusText());
    assertThat(gotEvent.getCreator()).isEqualTo(adminAccount);
    assertThat(gotEvent.getCreateTime()).isAfter(start);
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
  AdminApplicationController makeNoOpProfileController(Optional<Account> adminAccount) {
    ProfileTester profileTester =
        new ProfileTester(
            instanceOf(DatabaseExecutionContext.class),
            instanceOf(HttpExecutionContext.class),
            instanceOf(CiviFormProfileData.class),
            adminAccount);
    ProfileUtils profileUtilsNoOpTester =
        new ProfileUtilsNoOpTester(
            instanceOf(SessionStore.class), instanceOf(ProfileFactory.class), profileTester);
    return new AdminApplicationController(
        instanceOf(ProgramService.class),
        instanceOf(ApplicantService.class),
        instanceOf(ExporterService.class),
        instanceOf(FormFactory.class),
        instanceOf(JsonExporter.class),
        instanceOf(PdfExporter.class),
        instanceOf(ProgramApplicationListView.class),
        instanceOf(ProgramApplicationView.class),
        instanceOf(ProgramAdminApplicationService.class),
        profileUtilsNoOpTester,
        instanceOf(MessagesApi.class),
        instanceOf(DateConverter.class),
        Providers.of(LocalDateTime.now(ZoneId.systemDefault())),
        instanceOf(FeatureFlags.class));
  }

  // A test version of ProfileUtils that disable functionality that is hard
  // to otherwise test around.
  static class ProfileUtilsNoOpTester extends ProfileUtils {
    private final ProfileTester profileTester;

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
    public static class ProfileTester extends CiviFormProfile {
      Optional<Account> adminAccount;

      public ProfileTester(
          DatabaseExecutionContext dbContext,
          HttpExecutionContext httpContext,
          CiviFormProfileData profileData,
          Optional<Account> adminAccount) {
        super(dbContext, httpContext, profileData);
        this.adminAccount = adminAccount;
      }

      // Always passes and does no checks.
      @Override
      public CompletableFuture<Void> checkProgramAuthorization(String programName) {
        return CompletableFuture.completedFuture(null);
      }

      @Override
      public CompletableFuture<Account> getAccount() {
        return CompletableFuture.completedFuture(adminAccount.get());
      }
    }
  }
}
