package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertThrows;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.contentAsString;

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
import services.application.ApplicationEventDetails.StatusEvent;
import services.applications.ProgramAdminApplicationService;
import services.export.CsvExporterService;
import services.export.JsonExporter;
import services.export.PdfExporter;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.program.StatusDefinitions;
import services.program.StatusDefinitions.Status;
import services.program.StatusNotFoundException;
import support.ProgramBuilder;
import views.admin.programs.ProgramApplicationListView;
import views.admin.programs.ProgramApplicationView;

public class AdminApplicationControllerTest extends ResetPostgres {
  private static final String UNSET_STATUS_TEXT = "";
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
  private ProgramAdminApplicationService programAdminApplicationService;

  @Before
  public void setupController() {
    controller = instanceOf(AdminApplicationController.class);
    programAdminApplicationService = instanceOf(ProgramAdminApplicationService.class);
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
            /* untilDate= */ Optional.empty(),
            /* applicationStatus= */ Optional.empty(),
            /* selectedApplicationUri= */ Optional.empty());
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
            /* untilDate= */ Optional.empty(),
            /* applicationStatus= */ Optional.empty(),
            /* selectedApplicationUri= */ Optional.empty());
    assertThat(result.status()).isEqualTo(OK);
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

  @Test
  public void updateStatus_invalidNewStatus_fails() throws Exception {
    // Setup
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
                Helpers.fakeRequest()
                    .bodyForm(
                        Map.of(
                            "redirectUri",
                            "/",
                            "sendEmail",
                            "",
                            "currentStatus",
                            UNSET_STATUS_TEXT,
                            "newStatus",
                            "NOT A REAL STATUS")))
            .build();

    // Execute
    assertThrows(
        StatusNotFoundException.class,
        () -> controller.updateStatus(request, program.id, application.id));
  }

  @Test
  public void updateStatus_invalidCurrentStatus_fails() throws Exception {
    // Setup
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
                Helpers.fakeRequest()
                    .bodyForm(
                        Map.of(
                            "redirectUri",
                            "/",
                            "sendEmail",
                            "",
                            "currentStatus",
                            "unset shouldn't have a value",
                            "newStatus",
                            APPROVED_STATUS.statusText())))
            .build();

    // Execute
    Result result = controller.updateStatus(request, program.id, application.id);

    // Evaluate
    assertThat(result.status()).isEqualTo(BAD_REQUEST);
    assertThat(contentAsString(result)).contains("field should be empty");
  }

  @Test
  public void updateStatus_noNewStatus_fails() throws Exception {
    // Setup
    controller = makeNoOpProfileController(/* adminAccount= */ Optional.empty());
    Program program =
        ProgramBuilder.newActiveProgram("test name", "test description")
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .build();
    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    Application application =
        Application.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    Request request =
        addCSRFToken(
                Helpers.fakeRequest()
                    .bodyForm(
                        Map.of(
                            "redirectUri",
                            "/",
                            "sendEmail",
                            "",
                            "currentStatus",
                            UNSET_STATUS_TEXT)))
            .build();

    // Execute
    Result result = controller.updateStatus(request, program.id, application.id);

    // Evaluate
    assertThat(result.status()).isEqualTo(BAD_REQUEST);
    assertThat(contentAsString(result)).contains("is not present");
  }

  @Test
  public void updateStatus_noCurrentStatus_fails() throws Exception {
    // Setup
    controller = makeNoOpProfileController(/* adminAccount= */ Optional.empty());
    Program program =
        ProgramBuilder.newActiveProgram("test name", "test description")
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .build();
    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    Application application =
        Application.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    Request request =
        addCSRFToken(
                Helpers.fakeRequest()
                    .bodyForm(
                        Map.of(
                            "redirectUri",
                            "/",
                            "sendEmail",
                            "",
                            "newStatus",
                            APPROVED_STATUS.statusText())))
            .build();

    // Execute
    Result result = controller.updateStatus(request, program.id, application.id);

    // Evaluate
    assertThat(result.status()).isEqualTo(BAD_REQUEST);
    assertThat(contentAsString(result)).contains("is not present");
  }

  @Test
  public void updateStatus_invalidSendEmail_fails() throws Exception {
    // Setup
    controller = makeNoOpProfileController(/* adminAccount= */ Optional.empty());
    Program program =
        ProgramBuilder.newActiveProgram("test name", "test description")
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .build();
    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    Application application =
        Application.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    Request request =
        addCSRFToken(
                Helpers.fakeRequest()
                    .bodyForm(
                        Map.of(
                            "redirectUri",
                            "/",
                            "currentStatus",
                            UNSET_STATUS_TEXT,
                            "newStatus",
                            APPROVED_STATUS.statusText(),
                            // Only "on" is a valid checkbox state.
                            "sendEmail",
                            "false")))
            .build();

    // Execute
    Result result = controller.updateStatus(request, program.id, application.id);

    // Evaluate
    assertThat(result.status()).isEqualTo(BAD_REQUEST);
    assertThat(contentAsString(result)).contains("sendEmail value is invalid");
  }

  @Test
  public void updateStatus_outOfDateCurrentStatus_fails() throws Exception {
    // Setup
    Account adminAccount = resourceCreator.insertAccount();
    controller = makeNoOpProfileController(Optional.of(adminAccount));
    Program program =
        ProgramBuilder.newActiveProgram("test name", "test description")
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .build();
    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    Application application =
        Application.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();
    programAdminApplicationService.setStatus(
        application,
        StatusEvent.builder()
            .setStatusText(APPROVED_STATUS.statusText())
            .setEmailSent(false)
            .build(),
        adminAccount);

    Request request =
        addCSRFToken(
                Helpers.fakeRequest()
                    .bodyForm(
                        Map.of(
                            "redirectUri",
                            "/",
                            "sendEmail",
                            "",
                            "currentStatus",
                            UNSET_STATUS_TEXT,
                            "newStatus",
                            APPROVED_STATUS.statusText())))
            .build();

    // Execute
    Result result = controller.updateStatus(request, program.id, application.id);

    // Evaluate
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().get("error")).isPresent();
    assertThat(result.flash().get("error").get()).contains("application state has changed");
  }

  @Test
  public void updateStatus_succeeds() throws Exception {
    // Setup
    Instant start = Instant.now();
    Account adminAccount = resourceCreator.insertAccount();
    controller = makeNoOpProfileController(Optional.of(adminAccount));
    Program program =
        ProgramBuilder.newActiveProgram("test name", "test description")
            .withStatusDefinitions(new StatusDefinitions(ORIGINAL_STATUSES))
            .build();
    Applicant applicant =
        resourceCreator.insertApplicantWithAccount(Optional.of("user@example.com"));
    Application application =
        Application.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    Request request =
        addCSRFToken(
                Helpers.fakeRequest()
                    .bodyForm(
                        Map.of(
                            "redirectUri",
                            "/",
                            "currentStatus",
                            UNSET_STATUS_TEXT,
                            "newStatus",
                            APPROVED_STATUS.statusText(),
                            "sendEmail",
                            "on")))
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
    assertThat(gotEvent.getDetails().statusEvent().get().emailSent()).isTrue();
    assertThat(gotEvent.getCreator()).isEqualTo(Optional.of(adminAccount));
    assertThat(gotEvent.getCreateTime()).isAfter(start);
  }

  @Test
  public void updateStatus_emptySendEmail_succeeds() throws Exception {
    // Setup
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
                Helpers.fakeRequest()
                    .bodyForm(
                        Map.of(
                            "redirectUri",
                            "/",
                            // Only "on" is a valid checkbox state.
                            "sendEmail",
                            "",
                            "currentStatus",
                            UNSET_STATUS_TEXT,
                            "newStatus",
                            APPROVED_STATUS.statusText())))
            .build();

    // Execute
    Result result = controller.updateStatus(request, program.id, application.id);

    // Evaluate
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    application.refresh();
    assertThat(application.getApplicationEvents()).hasSize(1);
    ApplicationEvent gotEvent = application.getApplicationEvents().get(0);
    assertThat(gotEvent.getDetails().statusEvent()).isPresent();
    assertThat(gotEvent.getDetails().statusEvent().get().emailSent()).isFalse();
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

  @Test
  public void updateNote_noNote_fails() throws Exception {
    // Setup.
    Account adminAccount = resourceCreator.insertAccount();
    controller = makeNoOpProfileController(Optional.of(adminAccount));
    Program program = ProgramBuilder.newDraftProgram("test name", "test description").build();
    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    Application application =
        Application.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    Request request = addCSRFToken(Helpers.fakeRequest()).build();
    // Execute.
    Result result = controller.updateNote(request, program.id, application.id);

    // Verify.
    assertThat(result.status()).isEqualTo(BAD_REQUEST);
    assertThat(contentAsString(result)).contains("A note is not present");
  }

  @Test
  public void updateNote_succeeds() throws Exception {
    // Setup.
    Instant start = Instant.now();
    String noteText = "Test note content.";
    Account adminAccount = resourceCreator.insertAccount();
    controller = makeNoOpProfileController(Optional.of(adminAccount));
    Program program = ProgramBuilder.newDraftProgram("test name", "test description").build();
    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    Application application =
        Application.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    Request request =
        addCSRFToken(Helpers.fakeRequest().bodyForm(Map.of("redirectUri", "/", "note", noteText)))
            .build();

    // Execute.
    Result result = controller.updateNote(request, program.id, application.id);

    // Verify.
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    application.refresh();
    assertThat(application.getApplicationEvents()).hasSize(1);
    ApplicationEvent gotEvent = application.getApplicationEvents().get(0);
    assertThat(gotEvent.getEventType()).isEqualTo(ApplicationEventDetails.Type.NOTE_CHANGE);
    assertThat(gotEvent.getDetails().noteEvent()).isPresent();
    assertThat(gotEvent.getDetails().noteEvent().get().note()).isEqualTo(noteText);
    assertThat(gotEvent.getCreator()).isEqualTo(Optional.of(adminAccount));
    assertThat(gotEvent.getCreateTime()).isAfter(start);
  }

  @Test
  public void updateNote_emptyNote_succeeds() throws Exception {
    // Setup.
    String noteText = "";
    Account adminAccount = resourceCreator.insertAccount();
    controller = makeNoOpProfileController(Optional.of(adminAccount));
    Program program = ProgramBuilder.newDraftProgram("test name", "test description").build();
    Applicant applicant = resourceCreator.insertApplicantWithAccount();
    Application application =
        Application.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    Request request =
        addCSRFToken(Helpers.fakeRequest().bodyForm(Map.of("redirectUri", "/", "note", noteText)))
            .build();

    // Execute.
    Result result = controller.updateNote(request, program.id, application.id);

    // Verify.
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    application.refresh();
    assertThat(application.getApplicationEvents()).hasSize(1);
    ApplicationEvent gotEvent = application.getApplicationEvents().get(0);
    assertThat(gotEvent.getDetails().noteEvent()).isPresent();
    assertThat(gotEvent.getDetails().noteEvent().get().note()).isEqualTo(noteText);
  }

  // Returns a controller with a faked ProfileUtils to bypass acl checks.
  AdminApplicationController makeNoOpProfileController(Optional<Account> adminAccount) {
    ProfileTester profileTester =
        new ProfileTester(
            instanceOf(DatabaseExecutionContext.class),
            instanceOf(HttpExecutionContext.class),
            instanceOf(CiviFormProfileData.class),
            instanceOf(FeatureFlags.class),
            adminAccount);
    ProfileUtils profileUtilsNoOpTester =
        new ProfileUtilsNoOpTester(
            instanceOf(SessionStore.class), instanceOf(ProfileFactory.class), profileTester);
    return new AdminApplicationController(
        instanceOf(ProgramService.class),
        instanceOf(ApplicantService.class),
        instanceOf(CsvExporterService.class),
        instanceOf(FormFactory.class),
        instanceOf(JsonExporter.class),
        instanceOf(PdfExporter.class),
        instanceOf(ProgramApplicationListView.class),
        instanceOf(ProgramApplicationView.class),
        instanceOf(ProgramAdminApplicationService.class),
        profileUtilsNoOpTester,
        instanceOf(MessagesApi.class),
        instanceOf(DateConverter.class),
        Providers.of(LocalDateTime.now(ZoneId.systemDefault())));
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
          FeatureFlags featureFlags,
          Optional<Account> adminAccount) {
        super(dbContext, httpContext, profileData, featureFlags);
        this.adminAccount = adminAccount;
      }

      // Always passes and does no checks.
      @Override
      public CompletableFuture<Void> checkProgramAuthorization(
          String programName, Request request) {
        return CompletableFuture.completedFuture(null);
      }

      @Override
      public CompletableFuture<Account> getAccount() {
        return CompletableFuture.completedFuture(adminAccount.get());
      }
    }
  }
}
