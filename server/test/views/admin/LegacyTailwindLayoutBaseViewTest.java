package views.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static support.FakeRequestBuilder.fakeRequest;

import auth.ProfileUtils;
import controllers.WithMockedProfiles;
import java.time.Clock;
import java.util.Optional;
import modules.ThymeleafModule;
import org.junit.Before;
import org.junit.Test;
import org.thymeleaf.TemplateEngine;
import play.Environment;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import services.BundledAssetsFinder;
import services.DeploymentType;
import services.settings.SettingsManifest;
import views.BaseViewModel;
import views.shared.BaseViewDeps;
import views.shared.LayoutDeps;

public class LegacyTailwindLayoutBaseViewTest extends WithMockedProfiles {
  protected record CustomViewModel() implements BaseViewModel {}

  public static class CustomView extends LegacyTailwindLayoutBaseView<CustomViewModel> {
    public CustomView(LayoutDeps layoutDeps) {
      super(layoutDeps);
    }

    @Override
    protected String pageTitle(CustomViewModel model, Messages messages) {
      return "legacy-tailwind-page-title";
    }

    @Override
    protected String pageTemplate() {
      return "../../test/views/admin/testTemplateWithoutScriptBlock.html";
    }
  }

  private LayoutDeps createLayoutDeps(
      SettingsManifest settingsManifest, ProfileUtils profileUtils, boolean isDev) {
    BaseViewDeps baseViewDeps =
        new BaseViewDeps(
            instanceOf(TemplateEngine.class),
            instanceOf(ThymeleafModule.PlayThymeleafContextFactory.class),
            settingsManifest,
            instanceOf(MessagesApi.class),
            instanceOf(Environment.class));
    return new LayoutDeps(
        baseViewDeps,
        instanceOf(BundledAssetsFinder.class),
        profileUtils,
        new DeploymentType(/* isDev= */ isDev, /* isStaging= */ false),
        instanceOf(Clock.class));
  }

  private SettingsManifest createDemoSettingsManifest() {
    SettingsManifest mockSettingsManifest = spy(instanceOf(SettingsManifest.class));
    doReturn(Optional.of("demo")).when(mockSettingsManifest).getBannerType(any());
    doReturn(Optional.of("TestCity"))
        .when(mockSettingsManifest)
        .getWhitelabelCivicEntityShortName(any());
    return mockSettingsManifest;
  }

  @Before
  public void setup() {
    createGlobalAdminWithMockedProfile();
  }

  @Test
  public void render_whenDemoBannerDisabled_usesFixedHeaderAndLegacyMargins() {
    CustomView customView = new CustomView(instanceOf(LayoutDeps.class));

    String result = customView.render(fakeRequest(), new CustomViewModel());

    assertThat(result).doesNotContain("cf-demo-banner");
    assertThat(result).contains("position-fixed top-0");
    assertThat(result).contains("mt-5");
    assertThat(result).contains("mt-12 admin-responsive-main-padding");
    assertThat(result).doesNotContain("flex-col");
  }

  @Test
  public void render_whenDemoBannerEnabled_rendersBannerAndAdjustsFlexAndModalClasses() {
    LayoutDeps layoutDeps =
        createLayoutDeps(createDemoSettingsManifest(), instanceOf(ProfileUtils.class), true);
    CustomView customView = new CustomView(layoutDeps);

    String result = customView.render(fakeRequest(), new CustomViewModel());

    assertThat(result).containsOnlyOnce("cf-alert cf-demo-banner");
    assertThat(result).contains("Demo: TestCity");
    assertThat(result).contains("cf-transitional-gov-banner");
    assertThat(result).contains("cf-transitional-sticky-nav");
    assertThat(result).doesNotContain("position-fixed top-0");
    assertThat(result).contains("flex-col");
    assertThat(result).doesNotContain("mt-12 admin-responsive-main-padding");
    assertThat(result).contains("top-0 left-0");
    assertThat(result)
        .containsPattern(
            "<div\\s+class=\"usa-button\"\\s+id=\"admin\"\\s*>\\s*<span>Civiform"
                + " Admin</span\\s*><span\\s+class=\"usa-sr-only\">");
  }
}
