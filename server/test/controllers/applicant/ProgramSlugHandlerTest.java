package controllers.applicant;

import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequest;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import controllers.LanguageUtils;
import controllers.WithMockedProfiles;
import java.util.Locale;
import models.ApplicantModel;
import models.ApplicationModel;
import models.DisplayMode;
import models.LifecycleStage;
import models.ProgramModel;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Lang;
import play.i18n.Langs;
import play.i18n.MessagesApi;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Result;
import repository.AccountRepository;
import repository.VersionRepository;
import services.applicant.ApplicantService;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.program.ProgramType;
import services.settings.SettingsManifest;
import support.ProgramBuilder;
import views.applicant.NorthStarProgramOverviewView;

public class ProgramSlugHandlerTest extends WithMockedProfiles {

  @Before
  public void setUp() {
    resetDatabase();
  }

  @Test
  public void programBySlug_redirectsToActiveProgramVersionForExistingApplications() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    VersionRepository versionRepository = instanceOf(VersionRepository.class);

    ApplicantModel applicant = createApplicantWithMockedProfile();
    applicant.getApplicantData().setPreferredLocale(Locale.ENGLISH);
    applicant.save();

    ApplicationModel app =
        new ApplicationModel(applicant, programDefinition.toProgram(), LifecycleStage.DRAFT);
    app.save();

    ProgramModel programModelV2 =
        resourceCreator().insertDraftProgram(programDefinition.adminName());
    versionRepository.publishNewSynchronizedVersion();

    CiviFormController controller = instanceOf(CiviFormController.class);

    Result result =
        instanceOf(ProgramSlugHandler.class)
            .showProgram(controller, fakeRequest(), programDefinition.slug())
            .toCompletableFuture()
            .join();

    assertThat(result.redirectLocation())
        .contains(
            controllers.applicant.routes.ApplicantProgramReviewController.review(programModelV2.id)
                .url());
  }

  @Test
  public void programBySlug_clearsOutRedirectSessionKey_existingProgram() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    VersionRepository versionRepository = instanceOf(VersionRepository.class);
    ApplicantModel applicant = createApplicantWithMockedProfile();
    applicant.getApplicantData().setPreferredLocale(Locale.ENGLISH);
    applicant.save();
    resourceCreator().insertDraftProgram(programDefinition.adminName());
    versionRepository.publishNewSynchronizedVersion();

    CiviFormController controller = instanceOf(CiviFormController.class);

    Result result =
        instanceOf(ProgramSlugHandler.class)
            .showProgram(
                controller,
                fakeRequestBuilder().session(REDIRECT_TO_SESSION_KEY, "redirect-url").build(),
                programDefinition.slug())
            .toCompletableFuture()
            .join();

    assertThat(result.session().get(REDIRECT_TO_SESSION_KEY)).isNotPresent();
  }

  @Test
  public void programBySlug_clearsOutRedirectSessionKey_nonExistingProgram() {
    ApplicantModel applicant = createApplicantWithMockedProfile();
    applicant.getApplicantData().setPreferredLocale(Locale.ENGLISH);
    applicant.save();

    CiviFormController controller = instanceOf(CiviFormController.class);

    Result result =
        instanceOf(ProgramSlugHandler.class)
            .showProgram(
                controller,
                fakeRequestBuilder().session(REDIRECT_TO_SESSION_KEY, "redirect-url").build(),
                "non-existing-program-slug")
            .toCompletableFuture()
            .join();

    assertThat(result.session().get(REDIRECT_TO_SESSION_KEY)).isNotPresent();
  }

  @Test
  public void programBySlug_testLanguageSelectorShown() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    ApplicantModel applicant = createApplicantWithMockedProfile();
    CiviFormController controller = instanceOf(CiviFormController.class);

    ProgramSlugHandler handler = instanceOf(ProgramSlugHandler.class);

    Result result =
        handler
            .showProgram(controller, fakeRequest(), programDefinition.slug())
            .toCompletableFuture()
            .join();

    assertThat(result.redirectLocation())
        .contains(
            controllers.applicant.routes.ApplicantInformationController.setLangFromBrowser(
                    applicant.id)
                .url());
  }

  @Test
  public void programBySlug_testLanguageSelectorNotShownOneLanguage() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    createApplicantWithMockedProfile();
    Langs mockLangs = mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of(Lang.forCode("en-US")));
    SettingsManifest mockSettingsManifest = mock(SettingsManifest.class);
    LanguageUtils languageUtils =
        new LanguageUtils(
            instanceOf(AccountRepository.class),
            mockLangs,
            mockSettingsManifest,
            instanceOf(MessagesApi.class));
    CiviFormController controller = instanceOf(CiviFormController.class);
    ApplicantRoutes applicantRoutes = instanceOf(ApplicantRoutes.class);

    ProgramSlugHandler handler =
        new ProgramSlugHandler(
            instanceOf(ClassLoaderExecutionContext.class),
            instanceOf(ApplicantService.class),
            instanceOf(ProfileUtils.class),
            instanceOf(ProgramService.class),
            languageUtils,
            applicantRoutes,
            mockSettingsManifest,
            instanceOf(NorthStarProgramOverviewView.class),
            instanceOf(MessagesApi.class));
    Result result =
        handler
            .showProgram(controller, fakeRequest(), programDefinition.slug())
            .toCompletableFuture()
            .join();
    assertThat(result.redirectLocation())
        .contains(
            controllers.applicant.routes.ApplicantProgramReviewController.review(
                    programDefinition.id())
                .url());
  }

  @Test
  public void programBySlug_testLanguageSelectorNotShownNoLanguage() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    createApplicantWithMockedProfile();
    Langs mockLangs = mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of());
    SettingsManifest mockSettingsManifest = mock(SettingsManifest.class);
    LanguageUtils languageUtils =
        new LanguageUtils(
            instanceOf(AccountRepository.class),
            mockLangs,
            mockSettingsManifest,
            instanceOf(MessagesApi.class));
    CiviFormController controller = instanceOf(CiviFormController.class);
    ApplicantRoutes applicantRoutes = instanceOf(ApplicantRoutes.class);

    ProgramSlugHandler handler =
        new ProgramSlugHandler(
            instanceOf(ClassLoaderExecutionContext.class),
            instanceOf(ApplicantService.class),
            instanceOf(ProfileUtils.class),
            instanceOf(ProgramService.class),
            languageUtils,
            applicantRoutes,
            mockSettingsManifest,
            instanceOf(NorthStarProgramOverviewView.class),
            instanceOf(MessagesApi.class));
    Result result =
        handler
            .showProgram(controller, fakeRequest(), programDefinition.slug())
            .toCompletableFuture()
            .join();
    assertThat(result.redirectLocation())
        .contains(
            controllers.applicant.routes.ApplicantProgramReviewController.review(
                    programDefinition.id())
                .url());
  }

  @Test
  public void showProgram_withNorthStarEnabled_whenApplicationStarted_loadsProgramOverview() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();

    ApplicantModel applicant = createApplicantWithMockedProfile();

    Langs mockLangs = mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of(Lang.forCode("en-US")));

    SettingsManifest mockSettingsManifest = mock(SettingsManifest.class);
    when(mockSettingsManifest.getNorthStarApplicantUi(any())).thenReturn(true);

    ApplicationModel app =
        new ApplicationModel(applicant, programDefinition.toProgram(), LifecycleStage.DRAFT);
    app.save();

    LanguageUtils languageUtils =
        new LanguageUtils(
            instanceOf(AccountRepository.class),
            mockLangs,
            mockSettingsManifest,
            instanceOf(MessagesApi.class));
    CiviFormController controller = instanceOf(CiviFormController.class);
    ApplicantRoutes applicantRoutes = instanceOf(ApplicantRoutes.class);

    ProgramSlugHandler handler =
        new ProgramSlugHandler(
            instanceOf(ClassLoaderExecutionContext.class),
            instanceOf(ApplicantService.class),
            instanceOf(ProfileUtils.class),
            instanceOf(ProgramService.class),
            languageUtils,
            applicantRoutes,
            mockSettingsManifest,
            instanceOf(NorthStarProgramOverviewView.class),
            instanceOf(MessagesApi.class));
    Result result =
        handler
            .showProgram(controller, fakeRequest(), programDefinition.slug())
            .toCompletableFuture()
            .join();

    String content = contentAsString(result);
    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    assertThat(content).contains("<title>test program - Program Overview</title>");
  }

  @Test
  public void showProgram_withNorthStarEnabled_whenNoApplication_loadsProgramOverview() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    createApplicantWithMockedProfile();

    Langs mockLangs = mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of(Lang.forCode("en-US")));

    SettingsManifest mockSettingsManifest = mock(SettingsManifest.class);
    when(mockSettingsManifest.getNorthStarApplicantUi(any())).thenReturn(true);

    LanguageUtils languageUtils =
        new LanguageUtils(
            instanceOf(AccountRepository.class),
            mockLangs,
            mockSettingsManifest,
            instanceOf(MessagesApi.class));
    CiviFormController controller = instanceOf(CiviFormController.class);
    ApplicantRoutes applicantRoutes = instanceOf(ApplicantRoutes.class);

    ProgramSlugHandler handler =
        new ProgramSlugHandler(
            instanceOf(ClassLoaderExecutionContext.class),
            instanceOf(ApplicantService.class),
            instanceOf(ProfileUtils.class),
            instanceOf(ProgramService.class),
            languageUtils,
            applicantRoutes,
            mockSettingsManifest,
            instanceOf(NorthStarProgramOverviewView.class),
            instanceOf(MessagesApi.class));
    Result result =
        handler
            .showProgram(controller, fakeRequest(), programDefinition.slug())
            .toCompletableFuture()
            .join();

    String content = contentAsString(result);
    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    assertThat(content).contains("<title>test program - Program Overview</title>");
  }

  @Test
  public void showProgram_withNorthStarEnabled_whenApplicationSubmitted_loadsProgramOverview() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();

    ApplicantModel applicant = createApplicantWithMockedProfile();

    Langs mockLangs = mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of(Lang.forCode("en-US")));

    SettingsManifest mockSettingsManifest = mock(SettingsManifest.class);
    when(mockSettingsManifest.getNorthStarApplicantUi(any())).thenReturn(true);

    ApplicationModel app =
        new ApplicationModel(applicant, programDefinition.toProgram(), LifecycleStage.ACTIVE);
    app.save();

    LanguageUtils languageUtils =
        new LanguageUtils(
            instanceOf(AccountRepository.class),
            mockLangs,
            mockSettingsManifest,
            instanceOf(MessagesApi.class));
    CiviFormController controller = instanceOf(CiviFormController.class);
    ApplicantRoutes applicantRoutes = instanceOf(ApplicantRoutes.class);

    ProgramSlugHandler handler =
        new ProgramSlugHandler(
            instanceOf(ClassLoaderExecutionContext.class),
            instanceOf(ApplicantService.class),
            instanceOf(ProfileUtils.class),
            instanceOf(ProgramService.class),
            languageUtils,
            applicantRoutes,
            mockSettingsManifest,
            instanceOf(NorthStarProgramOverviewView.class),
            instanceOf(MessagesApi.class));
    Result result =
        handler
            .showProgram(controller, fakeRequest(), programDefinition.slug())
            .toCompletableFuture()
            .join();

    String content = contentAsString(result);
    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    assertThat(content).contains("<title>test program - Program Overview</title>");
  }

  @Test
  public void showProgramPreview_withNorthStarEnabled_loadsProgramOverview() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();

    ApplicantModel applicant = createApplicantWithMockedProfile();

    Langs mockLangs = mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of(Lang.forCode("en-US")));

    SettingsManifest mockSettingsManifest = mock(SettingsManifest.class);
    when(mockSettingsManifest.getNorthStarApplicantUi(any())).thenReturn(true);

    ApplicationModel app =
        new ApplicationModel(applicant, programDefinition.toProgram(), LifecycleStage.ACTIVE);
    app.save();

    LanguageUtils languageUtils =
        new LanguageUtils(
            instanceOf(AccountRepository.class),
            mockLangs,
            mockSettingsManifest,
            instanceOf(MessagesApi.class));
    CiviFormController controller = instanceOf(CiviFormController.class);
    ApplicantRoutes applicantRoutes = instanceOf(ApplicantRoutes.class);

    ProgramSlugHandler handler =
        new ProgramSlugHandler(
            instanceOf(ClassLoaderExecutionContext.class),
            instanceOf(ApplicantService.class),
            instanceOf(ProfileUtils.class),
            instanceOf(ProgramService.class),
            languageUtils,
            applicantRoutes,
            mockSettingsManifest,
            instanceOf(NorthStarProgramOverviewView.class),
            instanceOf(MessagesApi.class));
    Result result =
        handler
            .showProgramPreview(controller, fakeRequest(), programDefinition.slug())
            .toCompletableFuture()
            .join();

    String content = contentAsString(result);
    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    assertThat(content).contains("<title>test program - Program Overview</title>");
  }

  @Test
  public void showProgramPreview_withPreScreener_loadsFirstBlockPage() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveCommonIntakeForm("test pre-screener").buildDefinition();

    ApplicantModel applicant = createApplicantWithMockedProfile();

    Langs mockLangs = mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of(Lang.forCode("en-US")));

    SettingsManifest mockSettingsManifest = mock(SettingsManifest.class);

    ApplicationModel app =
        new ApplicationModel(applicant, programDefinition.toProgram(), LifecycleStage.ACTIVE);
    app.save();

    LanguageUtils languageUtils =
        new LanguageUtils(
            instanceOf(AccountRepository.class),
            mockLangs,
            mockSettingsManifest,
            instanceOf(MessagesApi.class));
    CiviFormController controller = instanceOf(CiviFormController.class);
    ApplicantRoutes applicantRoutes = instanceOf(ApplicantRoutes.class);

    ProgramSlugHandler handler =
        new ProgramSlugHandler(
            instanceOf(ClassLoaderExecutionContext.class),
            instanceOf(ApplicantService.class),
            instanceOf(ProfileUtils.class),
            instanceOf(ProgramService.class),
            languageUtils,
            applicantRoutes,
            mockSettingsManifest,
            instanceOf(NorthStarProgramOverviewView.class),
            instanceOf(MessagesApi.class));
    Result result =
        handler
            .showProgramPreview(controller, fakeRequest(), programDefinition.slug())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);

    assertThat(result.redirectLocation())
        .contains(
            controllers.applicant.routes.ApplicantProgramsController.edit(programDefinition.id())
                .url());
  }

  @Test
  public void showProgram_withExternalProgram_returnsBadRequest() {
    // Create an external program
    ProgramModel externalProgram =
        ProgramBuilder.newActiveProgram(
                "External Program",
                "External Program",
                "",
                DisplayMode.PUBLIC,
                ProgramType.EXTERNAL)
            .build();

    // Set up applicant
    ApplicantModel applicant = createApplicantWithMockedProfile();
    applicant.getApplicantData().setPreferredLocale(Locale.ENGLISH);
    applicant.save();
    CiviFormController controller = instanceOf(CiviFormController.class);

    // Call showProgram with the external program's slug
    Result result =
        instanceOf(ProgramSlugHandler.class)
            .showProgram(controller, fakeRequest(), externalProgram.getSlug())
            .toCompletableFuture()
            .join();

    // Verify the result is a bad request, since external programs don't support a program link
    assertThat(result.status()).isEqualTo(play.mvc.Http.Status.BAD_REQUEST);
    assertThat(contentAsString(result))
        .contains(new ProgramNotFoundException(externalProgram.getSlug()).getMessage());
  }
}
