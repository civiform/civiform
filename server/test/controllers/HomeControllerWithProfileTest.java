package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static play.test.Helpers.fakeRequest;

import auth.ProfileUtils;
import auth.controllers.MissingOptionalException;
import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import controllers.applicant.ApplicantRoutes;
import models.ApplicantModel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import play.i18n.Lang;
import play.i18n.Langs;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Result;
import repository.AccountRepository;
import services.settings.SettingsManifest;

public class HomeControllerWithProfileTest extends WithMockedProfiles {

  @Before
  public void setUp() {
    resetDatabase();
  }

  @Test
  public void testLanguageSelectorShown() {
    ApplicantModel applicant = createApplicantWithMockedProfile();
    HomeController controller = instanceOf(HomeController.class);
    Result result = controller.index(fakeRequest().build()).toCompletableFuture().join();
    assertThat(result.redirectLocation())
        .contains(
            controllers.applicant.routes.ApplicantInformationController.setLangFromBrowser(
                    applicant.id)
                .url());
  }

  @Test
  public void testLanguageSelectorNotShownOneLanguage() {
    createApplicantWithMockedProfile();
    Langs mockLangs = Mockito.mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of(Lang.forCode("en-US")));
    SettingsManifest mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    LanguageUtils languageUtils =
        new LanguageUtils(instanceOf(AccountRepository.class), mockLangs, mockSettingsManifest);

    HomeController controller =
        new HomeController(
            instanceOf(Config.class),
            instanceOf(ProfileUtils.class),
            instanceOf(MessagesApi.class),
            instanceOf(HttpExecutionContext.class),
            languageUtils,
            new ApplicantRoutes());
    Result result = controller.index(fakeRequest().build()).toCompletableFuture().join();
    assertThat(result.redirectLocation()).isNotEmpty();
    assertThat(
            result.redirectLocation().orElseThrow(() -> new MissingOptionalException(String.class)))
        .endsWith("/programs");
  }
}
