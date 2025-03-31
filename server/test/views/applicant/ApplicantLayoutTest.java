package views.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import controllers.AssetsFinder;
import controllers.LanguageUtils;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.twirl.api.Content;
import repository.ResetPostgres;
import services.DeploymentType;
import services.settings.SettingsManifest;
import views.BaseHtmlLayout;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.JsBundle;
import views.LanguageSelector;
import views.ViewUtils;
import views.components.PageNotProductionBanner;
import views.components.SessionTimeoutModalsTest;

public class ApplicantLayoutTest extends ResetPostgres {
  private ApplicantLayout applicantLayout;
  private SettingsManifest settingsManifest;
  private ProfileUtils profileUtils;
  private MessagesApi messagesApi;
  private CiviFormProfile profile;

  @Before
  public void setUp() {
    settingsManifest = mock(SettingsManifest.class);
    when(settingsManifest.getCiviformImageTag()).thenReturn(Optional.of("fake-image-tag"));
    when(settingsManifest.getFaviconUrl()).thenReturn(Optional.of("favicon-url"));

    profileUtils = mock(ProfileUtils.class);
    messagesApi = mock(MessagesApi.class);
    profile = mock(CiviFormProfile.class);

    applicantLayout =
        new ApplicantLayout(
            instanceOf(BaseHtmlLayout.class),
            instanceOf(ViewUtils.class),
            profileUtils,
            instanceOf(LanguageSelector.class),
            mock(LanguageUtils.class),
            settingsManifest,
            instanceOf(DeploymentType.class),
            instanceOf(AssetsFinder.class),
            instanceOf(PageNotProductionBanner.class),
            messagesApi);
  }

  @Test
  public void render_includesSessionTimeoutModals_whenEnabledAndProfilePresent() {
    Messages messages = mock(Messages.class);
    SessionTimeoutModalsTest.mockMessages(messages);
    when(messagesApi.preferred(any(Http.RequestHeader.class))).thenReturn(messages);

    Http.Request request = fakeRequestBuilder().build();
    when(settingsManifest.getSessionTimeoutEnabled(request)).thenReturn(true);
    when(profileUtils.optionalCurrentUserProfile(request)).thenReturn(Optional.of(profile));

    HtmlBundle bundle = new HtmlBundle(request, instanceOf(ViewUtils.class));
    bundle.setJsBundle(JsBundle.APPLICANT);

    Content content = applicantLayout.render(bundle);
    String html = content.body();

    SessionTimeoutModalsTest.assertSessionTimeoutModalStructure(
        html, BaseHtmlView.getCsrfToken(request));
  }

  @Test
  public void render_doesNotIncludeSessionTimeoutModals_whenDisabled() {
    Http.Request request = fakeRequestBuilder().build();
    when(settingsManifest.getSessionTimeoutEnabled(request)).thenReturn(false);
    when(profileUtils.optionalCurrentUserProfile(request)).thenReturn(Optional.of(profile));

    HtmlBundle bundle = new HtmlBundle(request, instanceOf(ViewUtils.class));
    bundle.setJsBundle(JsBundle.APPLICANT);
    Content content = applicantLayout.render(bundle);
    String html = content.body();

    assertThat(html).doesNotContain("session-timeout-container");
    assertThat(html).doesNotContain("session-inactivity-warning-modal");
    assertThat(html).doesNotContain("session-length-warning-modal");
  }

  @Test
  public void render_doesNotIncludeSessionTimeoutModals_whenNoProfile() {
    Http.Request request = fakeRequestBuilder().build();
    when(settingsManifest.getSessionTimeoutEnabled(request)).thenReturn(true);
    when(profileUtils.optionalCurrentUserProfile(request)).thenReturn(Optional.empty());

    HtmlBundle bundle = new HtmlBundle(request, instanceOf(ViewUtils.class));
    bundle.setJsBundle(JsBundle.APPLICANT);
    Content content = applicantLayout.render(bundle);
    String html = content.body();

    assertThat(html).doesNotContain("session-timeout-container");
    assertThat(html).doesNotContain("session-inactivity-warning-modal");
    assertThat(html).doesNotContain("session-length-warning-modal");
  }
}
