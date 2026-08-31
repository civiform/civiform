package views.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static support.FakeRequestBuilder.fakeRequest;

import controllers.WithMockedProfiles;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import play.i18n.Messages;
import services.settings.SettingsManifest;
import views.BaseViewModel;
import views.shared.BaseViewDeps;
import views.shared.LayoutDeps;

public class AdminLayoutBaseViewTest extends WithMockedProfiles {
  protected record CustomViewModel() implements BaseViewModel {}

  public static class CustomView extends AdminLayoutBaseView<CustomViewModel> {
    private final String pageTemplate;

    public CustomView(LayoutDeps layoutDeps, String pageTemplate) {
      super(layoutDeps);
      this.pageTemplate = pageTemplate;
    }

    @Override
    protected String pageTitle(CustomViewModel model, Messages messages) {
      return "page-title-1";
    }

    @Override
    protected String pageTemplate() {
      return pageTemplate;
    }
  }

  private CustomView createView(String pageTemplate) {
    return new CustomView(instanceOf(LayoutDeps.class), pageTemplate);
  }

  private CustomView createViewWithSettings(
      String pageTemplate, SettingsManifest settingsManifest) {
    LayoutDeps defaultDeps = instanceOf(LayoutDeps.class);
    BaseViewDeps baseViewDeps =
        new BaseViewDeps(
            defaultDeps.baseViewDeps().templateEngine(),
            defaultDeps.baseViewDeps().playThymeleafContextFactory(),
            settingsManifest,
            defaultDeps.baseViewDeps().messagesApi(),
            defaultDeps.baseViewDeps().environment());
    LayoutDeps layoutDeps =
        new LayoutDeps(baseViewDeps, defaultDeps.bundledAssetsFinder(), defaultDeps.profileUtils());
    return new CustomView(layoutDeps, pageTemplate);
  }

  @Before
  public void setup() {
    createGlobalAdminWithMockedProfile();
  }

  @Test
  public void render_page_with_script_block_smoke_test() {
    CustomView customView = createView("../../test/views/admin/testTemplateWithScriptBlock.html");
    assertThat(customView).isNotNull();

    String result = customView.render(fakeRequest(), new CustomViewModel());

    assertThat(result).isNotNull();
    assertThat(result.isBlank()).isFalse();
    assertThat(result).contains("-uswds_css.min.css");
    assertThat(result).contains("-uswdsinit_js.bundle.js");
    assertThat(result).contains("-uswds.min.js");
    assertThat(result).contains("-admin.bundle.js");
    assertThat(result).contains("page-title-1");
    assertThat(result).contains("page-content-1");
    assertThat(result).contains("page-scripts-1");
  }

  @Test
  public void render_page_without_script_block_smoke_test() {
    CustomView customView =
        createView("../../test/views/admin/testTemplateWithoutScriptBlock.html");
    assertThat(customView).isNotNull();

    String result = customView.render(fakeRequest(), new CustomViewModel());

    assertThat(result).isNotNull();
    assertThat(result.isBlank()).isFalse();
    assertThat(result).contains("-uswds_css.min.css");
    assertThat(result).contains("-uswdsinit_js.bundle.js");
    assertThat(result).contains("-uswds.min.js");
    assertThat(result).contains("-admin.bundle.js");
    assertThat(result).contains("page-title-1");
    assertThat(result).contains("page-content-1");
    assertThat(result).doesNotContain("page-scripts-1");
  }

  @Test
  public void render_demoBanner_whenDisabled_doesNotContainDemoBanner() {
    SettingsManifest settingsManifest = mock(SettingsManifest.class);
    when(settingsManifest.getCiviformImageTag()).thenReturn(Optional.of("civiform-image-tag"));
    when(settingsManifest.getFaviconUrl()).thenReturn(Optional.of("favicon-url"));
    when(settingsManifest.getDemoBannerEnabled(Mockito.any())).thenReturn(false);

    CustomView customView =
        createViewWithSettings(
            "../../test/views/admin/testTemplateWithoutScriptBlock.html", settingsManifest);

    String result = customView.render(fakeRequest(), new CustomViewModel());

    assertThat(result).doesNotContain("This is a demo site");
    assertThat(result).doesNotContain("Demo mode informational alert");
  }

  @Test
  public void render_demoBanner_whenEnabled_containsDemoBanner() {
    SettingsManifest settingsManifest = mock(SettingsManifest.class);
    when(settingsManifest.getCiviformImageTag()).thenReturn(Optional.of("civiform-image-tag"));
    when(settingsManifest.getFaviconUrl()).thenReturn(Optional.of("favicon-url"));
    when(settingsManifest.getDemoBannerEnabled(Mockito.any())).thenReturn(true);
    when(settingsManifest.getDemoBannerLearnMoreUrl(Mockito.any())).thenReturn(Optional.empty());

    CustomView customView =
        createViewWithSettings(
            "../../test/views/admin/testTemplateWithoutScriptBlock.html", settingsManifest);

    String result = customView.render(fakeRequest(), new CustomViewModel());

    assertThat(result).contains("This is a demo site");
    assertThat(result).contains("Demo mode informational alert");
    assertThat(result).contains("Do not enter actual or personal data in this demo site.");
  }

  @Test
  public void render_demoBanner_whenEnabledWithLearnMoreUrl_containsLearnMoreUrl() {
    SettingsManifest settingsManifest = mock(SettingsManifest.class);
    when(settingsManifest.getCiviformImageTag()).thenReturn(Optional.of("civiform-image-tag"));
    when(settingsManifest.getFaviconUrl()).thenReturn(Optional.of("favicon-url"));
    when(settingsManifest.getDemoBannerEnabled(Mockito.any())).thenReturn(true);
    when(settingsManifest.getDemoBannerLearnMoreUrl(Mockito.any()))
        .thenReturn(Optional.of("https://example.com/demo-learn-more"));

    CustomView customView =
        createViewWithSettings(
            "../../test/views/admin/testTemplateWithoutScriptBlock.html", settingsManifest);

    String result = customView.render(fakeRequest(), new CustomViewModel());

    assertThat(result).contains("This is a demo site");
    assertThat(result).contains("https://example.com/demo-learn-more");
    assertThat(result).contains("here");
  }
}
