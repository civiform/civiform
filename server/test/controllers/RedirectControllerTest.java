package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.test.Helpers.fakeRequest;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.applicant.RedirectController;
import java.util.List;
import java.util.Locale;
import models.Applicant;
import models.Application;
import models.LifecycleStage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.direct.AnonymousClient;
import org.pac4j.core.client.finder.ClientFinder;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.engine.DefaultSecurityLogic;
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
import views.applicant.ApplicantUpsellCreateAccountView;

public class RedirectControllerTest extends WithMockedProfiles {
  private ProgramDefinition programDefinition;

  @Before
  public void setUp() {
    resetDatabase();
    // Get the config, and hack it so that all requests appear authorized.
    Config config = instanceOf(Config.class);
    AnonymousClient client = AnonymousClient.INSTANCE;
    config.setClients(new Clients(client));

    // The SecurityLogic wants to use some smarts to figure out which client to use, but
    // those smarts are never going to pick this client (since none of the endpoints are
    // configured to use it), so we implement an anonymous client finder which always returns
    // our client.
    DefaultSecurityLogic securityLogic = new DefaultSecurityLogic();
    securityLogic.setClientFinder(
        new ClientFinder() {
          @Override
          public List<Client> find(Clients clients, WebContext context, String clientNames) {
            return ImmutableList.of(client);
          }
        });
    config.setSecurityLogic(securityLogic);

    programDefinition = ProgramBuilder.newActiveProgram("test program", "desc").buildDefinition();
  }

  @Test
  public void programBySlug_redirectsToPreviousProgramVersionForExistingApplications() {
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
        instanceOf(RedirectController.class)
            .programBySlug(addCSRFToken(fakeRequest()).build(), programDefinition.slug())
            .toCompletableFuture()
            .join();

    assertThat(result.redirectLocation())
        .contains(
            controllers.applicant.routes.ApplicantProgramReviewController.review(
                    applicant.id, programDefinition.id())
                .url());
  }

  @Test
  public void programBySlug_testLanguageSelectorShown() {
    Applicant applicant = createApplicantWithMockedProfile();
    RedirectController controller = instanceOf(RedirectController.class);
    Result result =
        controller
            .programBySlug(addCSRFToken(fakeRequest()).build(), programDefinition.slug())
            .toCompletableFuture()
            .join();

    assertThat(result.redirectLocation())
        .contains(
            controllers.applicant.routes.ApplicantInformationController.edit(applicant.id).url());
  }

  @Test
  public void programBySlug_testLanguageSelectorNotShownOneLanguage() {
    Applicant applicant = createApplicantWithMockedProfile();
    Langs mockLangs = Mockito.mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of(Lang.forCode("en-US")));
    LanguageUtils languageUtils = new LanguageUtils(instanceOf(UserRepository.class), mockLangs);

    RedirectController controller =
        new RedirectController(
            instanceOf(HttpExecutionContext.class),
            instanceOf(ApplicantService.class),
            instanceOf(ProfileUtils.class),
            instanceOf(ProgramService.class),
            instanceOf(ApplicantUpsellCreateAccountView.class),
            instanceOf(MessagesApi.class),
            languageUtils);
    Result result =
        controller
            .programBySlug(addCSRFToken(fakeRequest()).build(), programDefinition.slug())
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
    Applicant applicant = createApplicantWithMockedProfile();
    Langs mockLangs = Mockito.mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of());
    LanguageUtils languageUtils = new LanguageUtils(instanceOf(UserRepository.class), mockLangs);

    RedirectController controller =
        new RedirectController(
            instanceOf(HttpExecutionContext.class),
            instanceOf(ApplicantService.class),
            instanceOf(ProfileUtils.class),
            instanceOf(ProgramService.class),
            instanceOf(ApplicantUpsellCreateAccountView.class),
            instanceOf(MessagesApi.class),
            languageUtils);
    Result result =
        controller
            .programBySlug(addCSRFToken(fakeRequest()).build(), programDefinition.slug())
            .toCompletableFuture()
            .join();
    assertThat(result.redirectLocation())
        .contains(
            controllers.applicant.routes.ApplicantProgramReviewController.review(
                    applicant.id, programDefinition.id())
                .url());
  }
}
