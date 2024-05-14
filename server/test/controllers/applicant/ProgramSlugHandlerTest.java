package controllers.applicant;

import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static support.CfTestHelpers.requestBuilderWithSettings;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import controllers.LanguageUtils;
import controllers.WithMockedProfiles;
import java.util.Locale;
import models.ApplicantModel;
import models.ApplicationModel;
import models.LifecycleStage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import play.i18n.MessagesApi;
import play.i18n.Lang;
import play.i18n.Langs;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Result;
import repository.AccountRepository;
import repository.VersionRepository;
import services.applicant.ApplicantService;
import services.program.ProgramDefinition;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import support.ProgramBuilder;

public class ProgramSlugHandlerTest extends WithMockedProfiles {

  @Before
  public void setUp() {
    resetDatabase();
  }

  @Test
  public void programBySlug_redirectsToPreviousProgramVersionForExistingApplications() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    VersionRepository versionRepository = instanceOf(VersionRepository.class);
    ApplicantModel applicant = createApplicantWithMockedProfile();
    applicant.getApplicantData().setPreferredLocale(Locale.ENGLISH);
    applicant.save();
    ApplicationModel app =
        new ApplicationModel(applicant, programDefinition.toProgram(), LifecycleStage.DRAFT);
    app.save();
    resourceCreator().insertDraftProgram(programDefinition.adminName());
    versionRepository.publishNewSynchronizedVersion();

    CiviFormController controller = instanceOf(CiviFormController.class);
    Result result =
        instanceOf(ProgramSlugHandler.class)
            .showProgram(
                controller,
                addCSRFToken(requestBuilderWithSettings()).build(),
                programDefinition.slug())
            .toCompletableFuture()
            .join();

    assertThat(result.redirectLocation())
        .contains(
            controllers.applicant.routes.ApplicantProgramReviewController.review(
                    programDefinition.id())
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
                addCSRFToken(
                        requestBuilderWithSettings()
                            .session(REDIRECT_TO_SESSION_KEY, "redirect-url"))
                    .build(),
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
                addCSRFToken(
                        requestBuilderWithSettings()
                            .session(REDIRECT_TO_SESSION_KEY, "redirect-url"))
                    .build(),
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
            .showProgram(
                controller,
                addCSRFToken(requestBuilderWithSettings()).build(),
                programDefinition.slug())
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
    Langs mockLangs = Mockito.mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of(Lang.forCode("en-US")));
    SettingsManifest mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    LanguageUtils languageUtils =
        new LanguageUtils(instanceOf(AccountRepository.class), mockLangs, mockSettingsManifest, instanceOf(MessagesApi.class));
    CiviFormController controller = instanceOf(CiviFormController.class);
    ApplicantRoutes applicantRoutes = instanceOf(ApplicantRoutes.class);

    ProgramSlugHandler handler =
        new ProgramSlugHandler(
            instanceOf(ClassLoaderExecutionContext.class),
            instanceOf(ApplicantService.class),
            instanceOf(ProfileUtils.class),
            instanceOf(ProgramService.class),
            languageUtils,
            applicantRoutes);
    Result result =
        handler
            .showProgram(
                controller,
                addCSRFToken(requestBuilderWithSettings()).build(),
                programDefinition.slug())
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
    Langs mockLangs = Mockito.mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of());
    SettingsManifest mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    LanguageUtils languageUtils =
        new LanguageUtils(instanceOf(AccountRepository.class), mockLangs, mockSettingsManifest, instanceOf(MessagesApi.class));
    CiviFormController controller = instanceOf(CiviFormController.class);
    ApplicantRoutes applicantRoutes = instanceOf(ApplicantRoutes.class);

    ProgramSlugHandler handler =
        new ProgramSlugHandler(
            instanceOf(ClassLoaderExecutionContext.class),
            instanceOf(ApplicantService.class),
            instanceOf(ProfileUtils.class),
            instanceOf(ProgramService.class),
            languageUtils,
            applicantRoutes);
    Result result =
        handler
            .showProgram(
                controller,
                addCSRFToken(requestBuilderWithSettings()).build(),
                programDefinition.slug())
            .toCompletableFuture()
            .join();
    assertThat(result.redirectLocation())
        .contains(
            controllers.applicant.routes.ApplicantProgramReviewController.review(
                    programDefinition.id())
                .url());
  }
}
