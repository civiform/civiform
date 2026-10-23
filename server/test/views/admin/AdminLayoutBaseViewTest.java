package views.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static support.FakeRequestBuilder.fakeRequest;

import controllers.WithMockedProfiles;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Messages;
import views.BaseViewModel;
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
  public void render_demo_banner_highlights_active_civiform_admin_role() {
    services.settings.SettingsManifest mockSettingsManifest =
        org.mockito.Mockito.spy(instanceOf(services.settings.SettingsManifest.class));
    org.mockito.Mockito.when(mockSettingsManifest.getBannerType(org.mockito.ArgumentMatchers.any()))
        .thenReturn(java.util.Optional.of("demo"));
    org.mockito.Mockito.when(
            mockSettingsManifest.getWhitelabelCivicEntityShortName(
                org.mockito.ArgumentMatchers.any()))
        .thenReturn(java.util.Optional.of("TestCity"));

    views.shared.BaseViewDeps baseViewDeps =
        new views.shared.BaseViewDeps(
            instanceOf(org.thymeleaf.TemplateEngine.class),
            instanceOf(modules.ThymeleafModule.PlayThymeleafContextFactory.class),
            mockSettingsManifest,
            instanceOf(play.i18n.MessagesApi.class),
            instanceOf(play.Environment.class));
    LayoutDeps layoutDeps =
        new LayoutDeps(
            baseViewDeps,
            instanceOf(services.BundledAssetsFinder.class),
            instanceOf(auth.ProfileUtils.class),
            new services.DeploymentType(/* isDev= */ true, /* isStaging= */ false));

    CustomView customView =
        new CustomView(layoutDeps, "../../test/views/admin/testTemplateWithoutScriptBlock.html");
    String result = customView.render(fakeRequest(), new CustomViewModel());

    assertThat(result).contains("cf-demo-banner");
    assertThat(result).contains("DEMO: TestCity");
    assertThat(result).contains("View as:");
    assertThat(result)
        .contains(
            "<a href=\"/logout\" class=\"usa-button usa-button--outline\""
                + " id=\"resident\">Resident</a>");
    assertThat(result)
        .containsPattern(
            "<button\\s+type=\"button\"\\s+class=\"usa-button\"\\s+id=\"admin\"\\s+disabled>");
    assertThat(result)
        .contains(
            "class=\"usa-button usa-button--outline\" id=\"program-admin\">Program Admin</a>");
    assertThat(result)
        .contains(
            "class=\"usa-button usa-button--outline\" id=\"trusted-intermediary\">Trusted"
                + " Intermediary</a>");
  }

  @Test
  public void render_demo_banner_highlights_active_program_admin_role() {
    models.ProgramModel program = resourceCreator().insertActiveProgram("test-program");
    createProgramAdminWithMockedProfile(program);

    services.settings.SettingsManifest mockSettingsManifest =
        org.mockito.Mockito.spy(instanceOf(services.settings.SettingsManifest.class));
    org.mockito.Mockito.when(mockSettingsManifest.getBannerType(org.mockito.ArgumentMatchers.any()))
        .thenReturn(java.util.Optional.of("demo"));

    views.shared.BaseViewDeps baseViewDeps =
        new views.shared.BaseViewDeps(
            instanceOf(org.thymeleaf.TemplateEngine.class),
            instanceOf(modules.ThymeleafModule.PlayThymeleafContextFactory.class),
            mockSettingsManifest,
            instanceOf(play.i18n.MessagesApi.class),
            instanceOf(play.Environment.class));
    LayoutDeps layoutDeps =
        new LayoutDeps(
            baseViewDeps,
            instanceOf(services.BundledAssetsFinder.class),
            instanceOf(auth.ProfileUtils.class),
            new services.DeploymentType(/* isDev= */ true, /* isStaging= */ false));

    CustomView customView =
        new CustomView(layoutDeps, "../../test/views/admin/testTemplateWithoutScriptBlock.html");
    String result = customView.render(fakeRequest(), new CustomViewModel());

    assertThat(result).contains("cf-demo-banner");
    assertThat(result)
        .contains(
            "<a href=\"/logout\" class=\"usa-button usa-button--outline\""
                + " id=\"resident\">Resident</a>");
    assertThat(result)
        .contains("class=\"usa-button usa-button--outline\" id=\"admin\">Civiform Admin</a>");
    assertThat(result)
        .containsPattern(
            "<button\\s+type=\"button\"\\s+class=\"usa-button\"\\s+id=\"program-admin\"\\s+disabled>");
  }

  @Test
  public void render_not_production_banner() {
    services.settings.SettingsManifest mockSettingsManifest =
        org.mockito.Mockito.spy(instanceOf(services.settings.SettingsManifest.class));
    org.mockito.Mockito.when(mockSettingsManifest.getBannerType(org.mockito.ArgumentMatchers.any()))
        .thenReturn(java.util.Optional.of("not_production"));

    views.shared.BaseViewDeps baseViewDeps =
        new views.shared.BaseViewDeps(
            instanceOf(org.thymeleaf.TemplateEngine.class),
            instanceOf(modules.ThymeleafModule.PlayThymeleafContextFactory.class),
            mockSettingsManifest,
            instanceOf(play.i18n.MessagesApi.class),
            instanceOf(play.Environment.class));
    LayoutDeps layoutDeps =
        new LayoutDeps(
            baseViewDeps,
            instanceOf(services.BundledAssetsFinder.class),
            instanceOf(auth.ProfileUtils.class),
            new services.DeploymentType(/* isDev= */ true, /* isStaging= */ false));

    CustomView customView =
        new CustomView(layoutDeps, "../../test/views/admin/testTemplateWithoutScriptBlock.html");
    String result = customView.render(fakeRequest(), new CustomViewModel());

    assertThat(result).contains("usa-alert--emergency");
    assertThat(result).contains("This site is for testing purposes only.");
  }
}
