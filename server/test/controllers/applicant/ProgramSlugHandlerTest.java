package controllers.applicant;

import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import java.util.concurrent.CompletionStage;
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
import views.applicant.overview.ProgramOverviewView;

public class ProgramSlugHandlerTest extends WithMockedProfiles {

  @Before
  public void setUp() {
    resetDatabase();
  }

  @Test
  public void northStar_programBySlug_showsActiveProgramVersionForExistingApplications() {
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

    String content = contentAsString(result);
    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    assertThat(content).contains("<title>test program - Program Overview</title>");
    // Verify it's showing the newer version (programModelV2) by checking for its ID in the content
    assertThat(content).contains(String.valueOf(programModelV2.id));
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
            instanceOf(ProgramOverviewView.class),
            instanceOf(MessagesApi.class));
    Result result =
        handler
            .showProgram(controller, fakeRequest(), programDefinition.slug())
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    String content = contentAsString(result);
    assertThat(content).contains("<title>test program - Program Overview</title>");
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
            instanceOf(ProgramOverviewView.class),
            instanceOf(MessagesApi.class));
    Result result =
        handler
            .showProgram(controller, fakeRequest(), programDefinition.slug())
            .toCompletableFuture()
            .join();
    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    String content = contentAsString(result);
    assertThat(content).contains("<title>test program - Program Overview</title>");
  }

  @Test
  public void showProgram_whenApplicationStarted_loadsProgramOverview() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();

    ApplicantModel applicant = createApplicantWithMockedProfile();

    Langs mockLangs = mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of(Lang.forCode("en-US")));

    SettingsManifest mockSettingsManifest = mock(SettingsManifest.class);
    when(mockSettingsManifest.getNorthStarApplicantUi()).thenReturn(true);

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
            instanceOf(ProgramOverviewView.class),
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
  public void showProgram_whenNoApplication_loadsProgramOverview() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    createApplicantWithMockedProfile();

    Langs mockLangs = mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of(Lang.forCode("en-US")));

    SettingsManifest mockSettingsManifest = mock(SettingsManifest.class);
    when(mockSettingsManifest.getNorthStarApplicantUi()).thenReturn(true);

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
            instanceOf(ProgramOverviewView.class),
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
  public void showProgram_whenApplicationSubmitted_loadsProgramOverview() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();

    ApplicantModel applicant = createApplicantWithMockedProfile();

    Langs mockLangs = mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of(Lang.forCode("en-US")));

    SettingsManifest mockSettingsManifest = mock(SettingsManifest.class);
    when(mockSettingsManifest.getNorthStarApplicantUi()).thenReturn(true);

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
            instanceOf(ProgramOverviewView.class),
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
  public void showProgramPreview_loadsProgramOverview() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();

    ApplicantModel applicant = createApplicantWithMockedProfile();

    Langs mockLangs = mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of(Lang.forCode("en-US")));

    SettingsManifest mockSettingsManifest = mock(SettingsManifest.class);
    when(mockSettingsManifest.getNorthStarApplicantUi()).thenReturn(true);

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
            instanceOf(ProgramOverviewView.class),
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
            instanceOf(ProgramOverviewView.class),
            instanceOf(MessagesApi.class));
    Result result =
        handler
            .showProgramPreview(controller, fakeRequest(), programDefinition.slug())
            .toCompletableFuture()
            .join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);

    assertThat(result.redirectLocation())
        .contains(
            controllers.applicant.routes.ApplicantProgramsController.edit(
                    String.valueOf(programDefinition.id()), /* isFromUrlCall= */ false)
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

  @Test
  public void resolveProgramParam_whenSlugDisabledAndNotFromUrl_withId_success() throws Exception {
    String programId = "123";
    ApplicantModel applicant = createApplicantWithMockedProfile();

    CompletionStage<Long> result =
        instanceOf(ProgramSlugHandler.class)
            .resolveProgramParam(
                programId,
                applicant.id,
                /* isFromUrlCall= */ false,
                /* programSlugUrlEnabled= */ false);

    assertThat(result.toCompletableFuture().get()).isEqualTo(123L);
  }

  @Test
  public void resolveProgramParam_whenSlugDisabledAndNotFromUrl_withSlug_error() throws Exception {
    ProgramDefinition program = ProgramBuilder.newActiveProgram("test program").buildDefinition();
    ApplicantModel applicant = createApplicantWithMockedProfile();

    assertThatThrownBy(
            () ->
                instanceOf(ProgramSlugHandler.class)
                    .resolveProgramParam(
                        program.slug(),
                        applicant.id,
                        /* isFromUrlCall= */ false,
                        /* programSlugUrlEnabled= */ false))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Could not parse value from 'test-program' to a numeric value");
  }

  @Test
  public void resolveProgramParam_whenSlugDisabledAndFromUrl_withId_success() throws Exception {
    String programId = "123";
    ApplicantModel applicant = createApplicantWithMockedProfile();

    CompletionStage<Long> result =
        instanceOf(ProgramSlugHandler.class)
            .resolveProgramParam(
                programId,
                applicant.id,
                /* isFromUrlCall= */ true,
                /* programSlugUrlEnabled= */ false);

    assertThat(result.toCompletableFuture().get()).isEqualTo(123L);
  }

  @Test
  public void resolveProgramParam_whenSlugDisabledAndFromUrl_withSlug_error() throws Exception {
    ProgramDefinition program = ProgramBuilder.newActiveProgram("test program").buildDefinition();
    ApplicantModel applicant = createApplicantWithMockedProfile();

    assertThatThrownBy(
            () ->
                instanceOf(ProgramSlugHandler.class)
                    .resolveProgramParam(
                        program.slug(),
                        applicant.id,
                        /* isFromUrlCall= */ true,
                        /* programSlugUrlEnabled= */ false))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Could not parse value from 'test-program' to a numeric value");
  }

  @Test
  public void resolveProgramParam_whenSlugEnabledAndNotFromUrl_withId_success() throws Exception {
    String programId = "123";
    ApplicantModel applicant = createApplicantWithMockedProfile();

    CompletionStage<Long> result =
        instanceOf(ProgramSlugHandler.class)
            .resolveProgramParam(
                programId,
                applicant.id,
                /* isFromUrlCall= */ false,
                /* programSlugUrlEnabled= */ true);

    assertThat(result.toCompletableFuture().get()).isEqualTo(123L);
  }

  @Test
  public void resolveProgramParam_whenSlugEnabledAndNotFromUrl_withSlug_error() throws Exception {
    ProgramDefinition program = ProgramBuilder.newActiveProgram("test program").buildDefinition();
    ApplicantModel applicant = createApplicantWithMockedProfile();

    assertThatThrownBy(
            () ->
                instanceOf(ProgramSlugHandler.class)
                    .resolveProgramParam(
                        program.slug(),
                        applicant.id,
                        /* isFromUrlCall= */ false,
                        /* programSlugUrlEnabled= */ true))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Could not parse value from 'test-program' to a numeric value");
  }

  @Test
  public void resolveProgramParam_whenSlugEnabledAndFromUrl_withId_error() throws Exception {
    String programId = "123";
    ApplicantModel applicant = createApplicantWithMockedProfile();

    assertThatThrownBy(
            () ->
                instanceOf(ProgramSlugHandler.class)
                    .resolveProgramParam(
                        programId,
                        applicant.id,
                        /* isFromUrlCall= */ true,
                        /* programSlugUrlEnabled= */ true))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Numeric program parameter should have been handled by the caller");
  }

  @Test
  public void resolveProgramParam_whenSlugEnabledAndFromUrl_withSlug_success() throws Exception {
    ProgramDefinition program = ProgramBuilder.newActiveProgram("test program").buildDefinition();
    ApplicantModel applicant = createApplicantWithMockedProfile();

    CompletionStage<Long> result =
        instanceOf(ProgramSlugHandler.class)
            .resolveProgramParam(
                program.slug(),
                applicant.id,
                /* isFromUrlCall= */ true,
                /* programSlugUrlEnabled= */ true);

    assertThat(result.toCompletableFuture().get()).isEqualTo(program.id());
  }

  @Test
  public void getLatestProgramId_withExistingApplication() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program").buildDefinition();

    // Create an applicant and application for the program
    ApplicantModel applicant = createApplicantWithMockedProfile();
    ApplicationModel app =
        new ApplicationModel(applicant, programDefinition.toProgram(), LifecycleStage.DRAFT);
    app.save();

    long resultId =
        instanceOf(ProgramSlugHandler.class)
            .getLatestProgramId(programDefinition.slug(), applicant.id)
            .toCompletableFuture()
            .join();

    // Verify it returns the program ID from the application
    assertThat(resultId).isEqualTo(programDefinition.id());
  }

  @Test
  public void getLatestProgramId_withNoExistingApplication() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();

    // Create an applicant, but no application
    ApplicantModel applicant = createApplicantWithMockedProfile();

    long resultId =
        instanceOf(ProgramSlugHandler.class)
            .getLatestProgramId(programDefinition.slug(), applicant.id)
            .toCompletableFuture()
            .join();

    // Verify it returns the active program ID
    assertThat(resultId).isEqualTo(programDefinition.id());
  }
}
