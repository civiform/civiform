package controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static play.test.Helpers.fakeRequest;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import java.util.Locale;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import play.i18n.Lang;
import play.i18n.Langs;
import play.i18n.MessagesApi;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Result;
import repository.UserRepository;
import views.LoginForm;

public class HomeControllerWithProfileTest extends WithMockedProfiles {

  @Before
  public void setUp() {
    resetDatabase();
  }

  @Test
  public void testLanguageSelectorShown() {
    Applicant applicant = createApplicantWithMockedProfile();
    HomeController controller = instanceOf(HomeController.class);
    Result result = controller.index(fakeRequest().build()).toCompletableFuture().join();
    ;
    assertThat(result.redirectLocation())
        .contains(
            controllers.applicant.routes.ApplicantInformationController.edit(applicant.id).url());
  }

  @Test
  public void testLanguageSelectorNotShownOneLanguage() {
    Applicant applicant = createApplicantWithMockedProfile();
    Langs mockLangs = Mockito.mock(Langs.class);
    when(mockLangs.availables()).thenReturn(ImmutableList.of(Lang.forCode("en-US")));
    LanguageUtils languageUtils = new LanguageUtils(instanceOf(UserRepository.class), mockLangs);

    HomeController controller =
        new HomeController(
            instanceOf(Config.class),
            instanceOf(LoginForm.class),
            instanceOf(ProfileUtils.class),
            instanceOf(MessagesApi.class),
            instanceOf(HttpExecutionContext.class),
            languageUtils);
    Result result = controller.index(fakeRequest().build()).toCompletableFuture().join();
    assertThat(result.redirectLocation())
        .contains(
            controllers.applicant.routes.ApplicantProgramsController.index(applicant.id).url());
  }
}
