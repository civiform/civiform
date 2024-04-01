package views.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.fakeRequest;

import com.google.common.collect.ImmutableList;
import controllers.AssetsFinder;
import controllers.admin.routes;
import j2html.tags.specialized.ButtonTag;
import java.util.Locale;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import play.twirl.api.Content;
import repository.ResetPostgres;
import services.DeploymentType;
import services.TranslationLocales;
import services.settings.SettingsManifest;
import views.HtmlBundle;
import views.ViewUtils;

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
            instanceOf(AssetsFinder.class));
  }

  @Test
  public void getBundle_programMigrationNotEnabled_noExportTabInNav() {
    when(settingsManifest.getProgramMigration(any())).thenReturn(false);

    HtmlBundle bundle =
        adminLayout.getBundle(new HtmlBundle(fakeRequest().build(), instanceOf(ViewUtils.class)));

    Content content = bundle.render();
    assertThat(contentAsString(content)).doesNotContain("Export");
  }

  @Test
  public void getBundle_programMigrationEnabled_hasExportTabInNav() {
    when(settingsManifest.getProgramMigration(any())).thenReturn(true);

    HtmlBundle bundle =
        adminLayout.getBundle(new HtmlBundle(fakeRequest().build(), instanceOf(ViewUtils.class)));

    Content content = bundle.render();
    assertThat(contentAsString(content)).contains("Export");
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
}
