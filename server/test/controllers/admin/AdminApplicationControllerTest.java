package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.mvc.Http.Status.UNAUTHORIZED;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileFactory;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.util.Providers;
import controllers.FlashKey;
import controllers.admin.AdminApplicationControllerTest.ProfileUtilsNoOpTester.ProfileTester;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import models.AccountModel;
import models.ApplicantModel;
import models.ApplicationEventModel;
import models.ApplicationModel;
import models.LifecycleStage;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.core.context.session.SessionStore;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.AccountRepository;
import repository.ApplicationStatusesRepository;
import repository.DatabaseExecutionContext;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.DateConverter;
import services.LocalizedStrings;
import services.applicant.ApplicantService;
import services.application.ApplicationEventDetails;
import services.application.ApplicationEventDetails.StatusEvent;
import services.applications.PdfExporterService;
import services.applications.ProgramAdminApplicationService;
import services.export.CsvExporterService;
import services.export.JsonExporterService;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import services.statuses.StatusDefinitions;
import services.statuses.StatusDefinitions.Status;
import services.statuses.StatusNotFoundException;
import services.statuses.StatusService;
import support.ProgramBuilder;
import views.admin.programs.ProgramApplicationListView;
import views.admin.programs.ProgramApplicationTableView;
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
  private ApplicationStatusesRepository repo;
  private SettingsManifest settingsManifestMock;
  private ProfileFactory profileFactory;

  @Before
  public void setupController() {
    controller = instanceOf(AdminApplicationController.class);
    programAdminApplicationService = instanceOf(ProgramAdminApplicationService.class);
    repo = instanceOf(ApplicationStatusesRepository.class);
    settingsManifestMock = mock();
    profileFactory = instanceOf(ProfileFactory.class);
  }

  @Test
  public void index_noUser_errors() throws Exception {
    long programId = ProgramBuilder.newActiveProgram().buildDefinition().id();
    Result result =
        controller.index(
            fakeRequest(),
            programId,
            /* search= */ Optional.empty(),
            /* page= */ Optional.of(1), // Needed to skip redirect.
            /* fromDate= */ Optional.empty(),
            /* untilDate= */ Optional.empty(),
            /* applicationStatus= */ Optional.empty(),
            /* selectedApplicationUri= */ Optional.empty(),
            /* showDownloadModal= */ Optional.empty(),
            /* message= */ Optional.empty());
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void index() throws Exception {

    controller = makeNoOpProfileController(/* adminAccount= */ Optional.empty());
    ProgramModel program = ProgramBuilder.newActiveProgram().build();
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    applicant.refresh();
    ApplicationModel application =
        ApplicationModel.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();
    application.refresh();
    Result result =
        controller.index(
            fakeRequest(),
            program.id,
            /* search= */ Optional.empty(),
            /* page= */ Optional.of(1), // Needed to skip redirect.
            /* fromDate= */ Optional.empty(),
            /* untilDate= */ Optional.empty(),
            /* applicationStatus= */ Optional.empty(),
            /* selectedApplicationUri= */ Optional.empty(),
            /* showDownloadModal= */ Optional.empty(),
            /* message= */ Optional.empty());
    assertThat(result.status()).isEqualTo(OK);
  }

  @Test
  public void updateStatus_programNotFound() {
    ProgramModel program = ProgramBuilder.newActiveProgram("test name", "test description").build();
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ApplicationModel application =
        ApplicationModel.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();
    assertThatThrownBy(() -> controller.updateStatus(fakeRequest(), Long.MAX_VALUE, application.id))
        .isInstanceOf(ProgramNotFoundException.class);
  }

  @Test
  public void updateStatuses_programNotFound() {
    ProgramModel program = ProgramBuilder.newActiveProgram("test name", "test description").build();
    List<String> appIdList = createApplicationList(3, program);
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "applicationsIds[0]",
                    appIdList.get(0),
                    "applicationsIds[1]",
                    appIdList.get(1),
                    "statusText",
                    "approved",
                    "maybeSendEmail",
                    "false"))
            .build();
    assertThatThrownBy(() -> controller.updateStatuses(request, Long.MAX_VALUE))
        .isInstanceOf(ProgramNotFoundException.class);
  }

  @Test
  public void updateStatuses_notAdmin() throws Exception {
    ProgramModel program = ProgramBuilder.newActiveProgram("test name", "test description").build();
    List<String> appIdList = createApplicationList(3, program);
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "applicationsIds[0]",
                    appIdList.get(0),
                    "applicationsIds[1]",
                    appIdList.get(1),
                    "statusText",
                    "approved",
                    "maybeSendEmail",
                    "false"))
            .build();
    Result result = controller.updateStatuses(request, program.id);
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void updateStatus_notAdmin() throws Exception {
    ProgramModel program = ProgramBuilder.newActiveProgram("test name", "test description").build();
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ApplicationModel application =
        ApplicationModel.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    Result result = controller.updateStatus(fakeRequest(), program.id, application.id);
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void updateStatuses_invalidNewStatus_fails() throws Exception {
    // Setup
    AccountModel adminAccount = resourceCreator.insertAccount();
    controller = makeNoOpProfileController(Optional.of(adminAccount));
    ProgramModel program = ProgramBuilder.newActiveProgram("test name", "test description").build();
    repo.createOrUpdateStatusDefinitions(
        program.getProgramDefinition().adminName(), new StatusDefinitions(ORIGINAL_STATUSES));
    List<String> appIdList = createApplicationList(2, program);
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "applicationsIds[0]",
                    appIdList.get(0),
                    "applicationsIds[1]",
                    appIdList.get(1),
                    "statusText",
                    "approved",
                    "maybeSendEmail",
                    "false"))
            .build();

    // Execute
    assertThatThrownBy(() -> controller.updateStatuses(request, program.id))
        .isInstanceOf(StatusNotFoundException.class);
  }

  @Test
  public void updateStatus_invalidNewStatus_fails() throws Exception {
    // Setup
    AccountModel adminAccount = resourceCreator.insertAccount();
    controller = makeNoOpProfileController(Optional.of(adminAccount));
    ProgramModel program = ProgramBuilder.newActiveProgram("test name", "test description").build();
    repo.createOrUpdateStatusDefinitions(
        program.getProgramDefinition().adminName(), new StatusDefinitions(ORIGINAL_STATUSES));
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ApplicationModel application =
        ApplicationModel.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    Request request =
        fakeRequestBuilder()
            .bodyForm(
                Map.of(
                    "redirectUri",
                    "/",
                    "sendEmail",
                    "",
                    "currentStatus",
                    UNSET_STATUS_TEXT,
                    "newStatus",
                    "NOT A REAL STATUS"))
            .build();

    // Execute
    assertThatThrownBy(() -> controller.updateStatus(request, program.id, application.id))
        .isInstanceOf(StatusNotFoundException.class);
  }

  @Test
  public void updateStatus_invalidCurrentStatus_fails() throws Exception {
    // Setup
    AccountModel adminAccount = resourceCreator.insertAccount();
    controller = makeNoOpProfileController(Optional.of(adminAccount));
    ProgramModel program = ProgramBuilder.newActiveProgram("test name", "test description").build();
    repo.createOrUpdateStatusDefinitions(
        program.getProgramDefinition().adminName(), new StatusDefinitions(ORIGINAL_STATUSES));
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ApplicationModel application =
        ApplicationModel.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    Request request =
        fakeRequestBuilder()
            .bodyForm(
                Map.of(
                    "redirectUri",
                    "/",
                    "sendEmail",
                    "",
                    "currentStatus",
                    "unset shouldn't have a value",
                    "newStatus",
                    APPROVED_STATUS.statusText()))
            .build();

    // Execute
    Result result = controller.updateStatus(request, program.id, application.id);

    // Evaluate
    assertThat(result.status()).isEqualTo(BAD_REQUEST);
    assertThat(contentAsString(result)).contains("field should be empty");
  }

  @Test
  public void updateStatuses_noNewStatus_fails() throws Exception {
    // Setup
    AccountModel adminAccount = resourceCreator.insertAccount();
    controller = makeNoOpProfileController(Optional.of(adminAccount));
    ProgramModel program = ProgramBuilder.newActiveProgram("test name", "test description").build();
    repo.createOrUpdateStatusDefinitions(
        program.getProgramDefinition().adminName(), new StatusDefinitions(ORIGINAL_STATUSES));
    List<String> appIdList = createApplicationList(2, program);
    Request request =
        fakeRequestBuilder()
            .bodyForm(
                ImmutableMap.of(
                    "applicationsIds[0]",
                    appIdList.get(0),
                    "applicationsIds[1]",
                    appIdList.get(1),
                    "statusText",
                    UNSET_STATUS_TEXT,
                    "maybeSendEmail",
                    "false"))
            .build();

    assertThatThrownBy(() -> controller.updateStatuses(request, program.id))
        .isInstanceOf(StatusNotFoundException.class);
  }

  @Test
  public void updateStatus_noNewStatus_fails() throws Exception {
    // Setup
    controller = makeNoOpProfileController(/* adminAccount= */ Optional.empty());
    ProgramModel program = ProgramBuilder.newActiveProgram("test name", "test description").build();
    repo.createOrUpdateStatusDefinitions(
        program.getProgramDefinition().adminName(), new StatusDefinitions(ORIGINAL_STATUSES));
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ApplicationModel application =
        ApplicationModel.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    Request request =
        fakeRequestBuilder()
            .bodyForm(
                Map.of("redirectUri", "/", "sendEmail", "", "currentStatus", UNSET_STATUS_TEXT))
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
    ProgramModel program = ProgramBuilder.newActiveProgram("test name", "test description").build();
    repo.createOrUpdateStatusDefinitions(
        program.getProgramDefinition().adminName(), new StatusDefinitions(ORIGINAL_STATUSES));
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ApplicationModel application =
        ApplicationModel.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    Request request =
        fakeRequestBuilder()
            .bodyForm(
                Map.of(
                    "redirectUri", "/", "sendEmail", "", "newStatus", APPROVED_STATUS.statusText()))
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
    ProgramModel program = ProgramBuilder.newActiveProgram("test name", "test description").build();
    repo.createOrUpdateStatusDefinitions(
        program.getProgramDefinition().adminName(), new StatusDefinitions(ORIGINAL_STATUSES));
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ApplicationModel application =
        ApplicationModel.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    Request request =
        fakeRequestBuilder()
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
                    "false"))
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
    AccountModel adminAccount = resourceCreator.insertAccount();
    controller = makeNoOpProfileController(Optional.of(adminAccount));
    ProgramModel program = ProgramBuilder.newActiveProgram("test name", "test description").build();
    repo.createOrUpdateStatusDefinitions(
        program.getProgramDefinition().adminName(), new StatusDefinitions(ORIGINAL_STATUSES));
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ApplicationModel application =
        ApplicationModel.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();
    programAdminApplicationService.setStatus(
        application,
        StatusEvent.builder()
            .setStatusText(APPROVED_STATUS.statusText())
            .setEmailSent(false)
            .build(),
        adminAccount);

    Request request =
        fakeRequestBuilder()
            .bodyForm(
                Map.of(
                    "redirectUri",
                    "/",
                    "sendEmail",
                    "",
                    "currentStatus",
                    UNSET_STATUS_TEXT,
                    "newStatus",
                    APPROVED_STATUS.statusText()))
            .build();

    // Execute
    Result result = controller.updateStatus(request, program.id, application.id);

    // Evaluate
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().get(FlashKey.ERROR)).isPresent();
    assertThat(result.flash().get(FlashKey.ERROR).get()).contains("application state has changed");
  }

  @Test
  public void updateStatus_succeeds() throws Exception {
    // Setup
    Instant start = Instant.now();
    AccountModel adminAccount = resourceCreator.insertAccount();
    controller = makeNoOpProfileController(Optional.of(adminAccount));
    ProgramModel program = ProgramBuilder.newActiveProgram("test name", "test description").build();
    repo.createOrUpdateStatusDefinitions(
        program.getProgramDefinition().adminName(), new StatusDefinitions(ORIGINAL_STATUSES));
    ApplicantModel applicant =
        resourceCreator.insertApplicantWithAccount(Optional.of("user@example.com"));
    ApplicationModel application =
        ApplicationModel.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    Request request =
        fakeRequestBuilder()
            .bodyForm(
                Map.of(
                    "redirectUri",
                    "/",
                    "currentStatus",
                    UNSET_STATUS_TEXT,
                    "newStatus",
                    APPROVED_STATUS.statusText(),
                    "sendEmail",
                    "on"))
            .build();

    // Execute
    Result result = controller.updateStatus(request, program.id, application.id);

    // Evaluate
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    application.refresh();
    assertThat(application.getApplicationEvents()).hasSize(1);
    ApplicationEventModel gotEvent = application.getApplicationEvents().get(0);
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
    AccountModel adminAccount = resourceCreator.insertAccount();
    controller = makeNoOpProfileController(Optional.of(adminAccount));
    ProgramModel program = ProgramBuilder.newActiveProgram("test name", "test description").build();
    repo.createOrUpdateStatusDefinitions(
        program.getProgramDefinition().adminName(), new StatusDefinitions(ORIGINAL_STATUSES));
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ApplicationModel application =
        ApplicationModel.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    Request request =
        fakeRequestBuilder()
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
                    APPROVED_STATUS.statusText()))
            .build();

    // Execute
    Result result = controller.updateStatus(request, program.id, application.id);

    // Evaluate
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    application.refresh();
    assertThat(application.getApplicationEvents()).hasSize(1);
    ApplicationEventModel gotEvent = application.getApplicationEvents().get(0);
    assertThat(gotEvent.getDetails().statusEvent()).isPresent();
    assertThat(gotEvent.getDetails().statusEvent().get().emailSent()).isFalse();
  }

  @Test
  public void updateNote_programNotFound() {
    ProgramModel program = ProgramBuilder.newDraftProgram("test name", "test description").build();
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ApplicationModel application =
        ApplicationModel.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    assertThatThrownBy(() -> controller.updateNote(fakeRequest(), Long.MAX_VALUE, application.id))
        .isInstanceOf(ProgramNotFoundException.class);
  }

  @Test
  public void updateNote_notAdmin() throws Exception {
    ProgramModel program = ProgramBuilder.newDraftProgram("test name", "test description").build();
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ApplicationModel application =
        ApplicationModel.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    Result result = controller.updateNote(fakeRequest(), program.id, application.id);
    assertThat(result.status()).isEqualTo(UNAUTHORIZED);
  }

  @Test
  public void updateNote_noNote_fails() throws Exception {
    // Setup.
    AccountModel adminAccount = resourceCreator.insertAccount();
    controller = makeNoOpProfileController(Optional.of(adminAccount));
    ProgramModel program = ProgramBuilder.newDraftProgram("test name", "test description").build();
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ApplicationModel application =
        ApplicationModel.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    // Execute.
    Result result = controller.updateNote(fakeRequest(), program.id, application.id);

    // Verify.
    assertThat(result.status()).isEqualTo(BAD_REQUEST);
    assertThat(contentAsString(result)).contains("A note is not present");
  }

  @Test
  public void updateNote_succeeds() throws Exception {
    // Setup.
    Instant start = Instant.now();
    String noteText = "Test note content.";
    AccountModel adminAccount = resourceCreator.insertAccount();
    controller = makeNoOpProfileController(Optional.of(adminAccount));
    ProgramModel program = ProgramBuilder.newDraftProgram("test name", "test description").build();
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ApplicationModel application =
        ApplicationModel.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    Request request =
        fakeRequestBuilder().bodyForm(Map.of("redirectUri", "/", "note", noteText)).build();

    // Execute.
    Result result = controller.updateNote(request, program.id, application.id);

    // Verify.
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    application.refresh();
    assertThat(application.getApplicationEvents()).hasSize(1);
    ApplicationEventModel gotEvent = application.getApplicationEvents().get(0);
    assertThat(gotEvent.getEventType()).isEqualTo(ApplicationEventDetails.Type.NOTE_CHANGE);
    assertThat(gotEvent.getDetails().noteEvent()).isPresent();
    assertThat(gotEvent.getDetails().noteEvent().get().note()).isEqualTo(noteText);
    assertThat(gotEvent.getCreator()).isEqualTo(Optional.of(adminAccount));
    assertThat(gotEvent.getCreateTime()).isAfter(start);
    // verify application has the latest note
    assertThat(application.getLatestNote().get()).isEqualTo(noteText);
  }

  @Test
  public void updateNote_emptyNote_succeeds() throws Exception {
    // Setup.
    String noteText = "";
    AccountModel adminAccount = resourceCreator.insertAccount();
    controller = makeNoOpProfileController(Optional.of(adminAccount));
    ProgramModel program = ProgramBuilder.newDraftProgram("test name", "test description").build();
    ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
    ApplicationModel application =
        ApplicationModel.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();

    Request request =
        fakeRequestBuilder().bodyForm(Map.of("redirectUri", "/", "note", noteText)).build();

    // Execute.
    Result result = controller.updateNote(request, program.id, application.id);

    // Verify.
    assertThat(result.status()).isEqualTo(SEE_OTHER);
    application.refresh();
    assertThat(application.getApplicationEvents()).hasSize(1);
    ApplicationEventModel gotEvent = application.getApplicationEvents().get(0);
    assertThat(gotEvent.getDetails().noteEvent()).isPresent();
    assertThat(gotEvent.getDetails().noteEvent().get().note()).isEqualTo(noteText);
    assertThat(application.getLatestNote().get()).isEqualTo(noteText);
  }

  // Returns a controller with a faked ProfileUtils to bypass acl checks.
  AdminApplicationController makeNoOpProfileController(Optional<AccountModel> adminAccount) {
    ProfileTester profileTester =
        new ProfileTester(
            instanceOf(DatabaseExecutionContext.class),
            instanceOf(ClassLoaderExecutionContext.class),
            instanceOf(CiviFormProfileData.class),
            instanceOf(SettingsManifest.class),
            adminAccount,
            instanceOf(AccountRepository.class));
    ProfileUtils profileUtilsNoOpTester =
        new ProfileUtilsNoOpTester(
            instanceOf(SessionStore.class), instanceOf(ProfileFactory.class), profileTester);
    return new AdminApplicationController(
        instanceOf(ProgramService.class),
        instanceOf(ApplicantService.class),
        instanceOf(CsvExporterService.class),
        instanceOf(FormFactory.class),
        instanceOf(JsonExporterService.class),
        instanceOf(PdfExporterService.class),
        instanceOf(ProgramApplicationListView.class),
        instanceOf(ProgramApplicationView.class),
        instanceOf(ProgramAdminApplicationService.class),
        profileUtilsNoOpTester,
        instanceOf(MessagesApi.class),
        instanceOf(DateConverter.class),
        Providers.of(LocalDateTime.now(ZoneId.systemDefault())),
        instanceOf(VersionRepository.class),
        instanceOf(StatusService.class),
        settingsManifestMock,
        instanceOf(ProgramApplicationTableView.class));
  }

  private List<String> createApplicationList(int count, ProgramModel program) {
    List<String> returnList = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      ApplicantModel applicant = resourceCreator.insertApplicantWithAccount();
      ApplicationModel application =
          ApplicationModel.create(applicant, program, LifecycleStage.ACTIVE).setSubmitTimeToNow();
      returnList.add(String.valueOf(application.id));
    }
    return returnList;
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
    public Optional<CiviFormProfile> optionalCurrentUserProfile(Http.RequestHeader request) {
      return Optional.of(profileTester);
    }

    @Override
    public CiviFormProfile currentUserProfile(Http.RequestHeader request) {
      return profileTester;
    }

    // A test version of CiviFormProfile that disable functionality that is hard
    // to otherwise test around.
    public static class ProfileTester extends CiviFormProfile {
      Optional<AccountModel> adminAccount;

      public ProfileTester(
          DatabaseExecutionContext dbContext,
          ClassLoaderExecutionContext classLoaderExecutionContext,
          CiviFormProfileData profileData,
          SettingsManifest settingsManifest,
          Optional<AccountModel> adminAccount,
          AccountRepository accountRepository) {
        super(
            dbContext,
            classLoaderExecutionContext,
            profileData,
            settingsManifest,
            accountRepository);
        this.adminAccount = adminAccount;
      }

      // Always passes and does no checks.
      @Override
      public CompletableFuture<Void> checkProgramAuthorization(
          String programName, Request request) {
        return CompletableFuture.completedFuture(null);
      }

      @Override
      public CompletableFuture<AccountModel> getAccount() {
        return CompletableFuture.completedFuture(adminAccount.get());
      }
    }
  }
}
