package views.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableList;
import controllers.AssetsFinder;
import controllers.admin.routes;
import j2html.tags.specialized.ButtonTag;
import java.util.Locale;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.twirl.api.Content;
import repository.ResetPostgres;
import services.DeploymentType;
import services.TranslationLocales;
import services.settings.SettingsManifest;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.JsBundle;
import views.ViewUtils;
import views.components.SessionTimeoutModalsTest;

public class AdminLayoutTest extends ResetPostgres {
  private SettingsManifest settingsManifest;
  private TranslationLocales translationLocales;
  private AdminLayout adminLayout;

  @Before
  public void setUp() {
    settingsManifest = mock(SettingsManifest.class);
    when(settingsManifest.getCiviformImageTag()).thenReturn(Optional.of("fake-image-tag"));
    when(settingsManifest.getFaviconUrl()).thenReturn(Optional.of("favicon-url"));

    translationLocales = mock(TranslationLocales.class);
    when(translationLocales.translatableLocales()).thenReturn(ImmutableList.of(Locale.CHINESE));

    adminLayout =
        new AdminLayout(
            instanceOf(ViewUtils.class),
            AdminLayout.NavPage.PROGRAMS,
            settingsManifest,
            translationLocales,
            instanceOf(DeploymentType.class),
            instanceOf(AssetsFinder.class),
            instanceOf(MessagesApi.class));
  }

  @Test
  public void createManageTranslationsButton_noTranslatableLocales_returnsEmpty() {
    when(translationLocales.translatableLocales()).thenReturn(ImmutableList.of());

    Optional<ButtonTag> button =
        adminLayout.createManageTranslationsButton(
            "programAdminName", Optional.of("buttonId"), "buttonStyles");

    assertThat(button).isEmpty();
  }

  @Test
  public void createManageTranslationsButton_hasTranslatableLocales_returnsButton() {
    when(translationLocales.translatableLocales()).thenReturn(ImmutableList.of(Locale.CHINESE));

    Optional<ButtonTag> button =
        adminLayout.createManageTranslationsButton(
            "programAdminName", Optional.of("buttonId"), "buttonStyles");

    assertThat(button).isPresent();
  }

  @Test
  public void createManageTranslationsButton_redirectsToTranslationsPage() {
    Optional<ButtonTag> button =
        adminLayout.createManageTranslationsButton(
            "programAdminName", Optional.of("buttonId"), "buttonStyles");

    assertThat(button).isPresent();
    String expectedRedirectUrl =
        routes.AdminProgramTranslationsController.redirectToFirstLocale("programAdminName").url();
    assertThat(button.get().render())
        .contains(String.format("data-redirect-to=\"%s\"", expectedRedirectUrl));
  }

  @Test
  public void createManageTranslationsButton_noIdProvided_noIdSet() {
    Optional<ButtonTag> button =
        adminLayout.createManageTranslationsButton(
            "programAdminName", Optional.empty(), "buttonStyles");

    assertThat(button).isPresent();
    assertThat(button.get().render()).doesNotContain("id=");
  }

  @Test
  public void createManageTranslationsButton_idProvided_idSet() {
    Optional<ButtonTag> button =
        adminLayout.createManageTranslationsButton(
            "programAdminName", Optional.of("testProgramId"), "buttonStyles");

    assertThat(button).isPresent();
    assertThat(button.get().render()).contains("id=\"testProgramId\"");
  }

  @Test
  public void render_includesSessionTimeoutModals_whenEnabled() {
    Messages messages = mock(Messages.class);
    SessionTimeoutModalsTest.mockMessages(messages);

    MessagesApi messagesApi = mock(MessagesApi.class);
    when(messagesApi.preferred(any(Http.RequestHeader.class))).thenReturn(messages);

    adminLayout =
        new AdminLayout(
            instanceOf(ViewUtils.class),
            AdminLayout.NavPage.PROGRAMS,
            settingsManifest,
            translationLocales,
            instanceOf(DeploymentType.class),
            instanceOf(AssetsFinder.class),
            messagesApi);

    // Create bundle with the request
    Http.Request request = fakeRequestBuilder().build();
    when(settingsManifest.getSessionTimeoutEnabled(request)).thenReturn(true);

    HtmlBundle bundle = new HtmlBundle(request, instanceOf(ViewUtils.class));
    bundle.setJsBundle(JsBundle.ADMIN);

    // Render the admin layout
    Content content = adminLayout.render(bundle);
    String html = content.body();
    SessionTimeoutModalsTest.assertSessionTimeoutModalStructure(
        html, BaseHtmlView.getCsrfToken(request));
  }

  @Test
  public void render_doesNotIncludeSessionTimeoutModals_whenDisabled() {
    Http.Request request = fakeRequestBuilder().build();
    when(settingsManifest.getSessionTimeoutEnabled(request)).thenReturn(false);

    HtmlBundle bundle = new HtmlBundle(request, instanceOf(ViewUtils.class));
    bundle.setJsBundle(JsBundle.APPLICANT);

    Content content = adminLayout.render(bundle);
    String html = content.body();

    assertThat(html).doesNotContain("session-timeout-container");
    assertThat(html).doesNotContain("session-inactivity-warning-modal");
    assertThat(html).doesNotContain("session-length-warning-modal");
  }
}
