package controllers.applicant;

import static controllers.CallbackController.REDIRECT_TO_SESSION_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static support.CfTestHelpers.requestBuilderWithSettings;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.LanguageUtils;
import controllers.WithMockedProfiles;
import java.util.Locale;
import models.Applicant;
import models.Application;
import models.LifecycleStage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import play.i18n.Lang;
import play.i18n.Langs;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Result;
import repository.UserRepository;
import repository.VersionRepository;
import services.applicant.ApplicantService;
import services.program.ProgramDefinition;
import services.program.ProgramService;
import support.ProgramBuilder;

public class DeepLinkControllerTest extends WithMockedProfiles {

  @Before
  public void setUp() {
    resetDatabase();
  }

  @Test
  public void programBySlug_redirectsToPreviousProgramVersionForExistingApplications() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    VersionRepository versionRepository = instanceOf(VersionRepository.class);
    Applicant applicant = createApplicantWithMockedProfile();
    applicant.getApplicantData().setPreferredLocale(Locale.ENGLISH);
    applicant.save();
    Application app =
        new Application(applicant, programDefinition.toProgram(), LifecycleStage.DRAFT);
    app.save();
    resourceCreator().insertDraftProgram(programDefinition.adminName());
    versionRepository.publishNewSynchronizedVersion();

    Result result =
        instanceOf(DeepLinkController.class)
            .programBySlug(
                addCSRFToken(requestBuilderWithSettings()).build(), programDefinition.slug())
            .toCompletableFuture()
            .join();

    assertThat(result.redirectLocation())
        .contains(
            controllers.applicant.routes.ApplicantProgramReviewController.review(
                    applicant.id, programDefinition.id())
                .url());
  }

  @Test
  public void programBySlug_clearsOutRedirectSessionKey_existingProgram() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    VersionRepository versionRepository = instanceOf(VersionRepository.class);
    Applicant applicant = createApplicantWithMockedProfile();
    applicant.getApplicantData().setPreferredLocale(Locale.ENGLISH);
    applicant.save();
    resourceCreator().insertDraftProgram(programDefinition.adminName());
    versionRepository.publishNewSynchronizedVersion();

    Result result =
        instanceOf(DeepLinkController.class)
            .programBySlug(
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
    Applicant applicant = createApplicantWithMockedProfile();
    applicant.getApplicantData().setPreferredLocale(Locale.ENGLISH);
    applicant.save();

    Result result =
        instanceOf(DeepLinkController.class)
            .programBySlug(
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
    Applicant applicant = createApplicantWithMockedProfile();
    DeepLinkController controller = instanceOf(DeepLinkController.class);
    Result result =
        controller
            .programBySlug(
                addCSRFToken(requestBuilderWithSettings()).build(), programDefinition.slug())
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
    Applicant applicant = createApplicantWithMockedProfile();
    Langs mockLangs = Mockito.mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of(Lang.forCode("en-US")));
    LanguageUtils languageUtils = new LanguageUtils(instanceOf(UserRepository.class), mockLangs);

    DeepLinkController controller =
        new DeepLinkController(
            instanceOf(HttpExecutionContext.class),
            instanceOf(ApplicantService.class),
            instanceOf(ProfileUtils.class),
            instanceOf(ProgramService.class),
            instanceOf(VersionRepository.class),
            languageUtils,
            instanceOf(MessagesApi.class));
    Result result =
        controller
            .programBySlug(
                addCSRFToken(requestBuilderWithSettings()).build(), programDefinition.slug())
            .toCompletableFuture()
            .join();
    assertThat(result.redirectLocation())
        .contains(
            controllers.applicant.routes.ApplicantProgramReviewController.review(
                    applicant.id, programDefinition.id())
                .url());
  }

  @Test
  public void programBySlug_testLanguageSelectorNotShownNoLanguage() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
    Applicant applicant = createApplicantWithMockedProfile();
    Langs mockLangs = Mockito.mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of());
    LanguageUtils languageUtils = new LanguageUtils(instanceOf(UserRepository.class), mockLangs);

    DeepLinkController controller =
        new DeepLinkController(
            instanceOf(HttpExecutionContext.class),
            instanceOf(ApplicantService.class),
            instanceOf(ProfileUtils.class),
            instanceOf(ProgramService.class),
            instanceOf(VersionRepository.class),
            languageUtils,
            instanceOf(MessagesApi.class));
    Result result =
        controller
            .programBySlug(
                addCSRFToken(requestBuilderWithSettings()).build(), programDefinition.slug())
            .toCompletableFuture()
            .join();
    assertThat(result.redirectLocation())
        .contains(
            controllers.applicant.routes.ApplicantProgramReviewController.review(
                    applicant.id, programDefinition.id())
                .url());
  }
}
