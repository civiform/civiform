package views.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static support.FakeRequestBuilder.fakeRequest;

import auth.CiviFormProfile;
import auth.ProfileFactory;
import auth.ProfileUtils;
import controllers.WithMockedProfiles;
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
    SettingsManifest mockSettingsManifest = spy(instanceOf(SettingsManifest.class));
    doReturn(Optional.of("demo")).when(mockSettingsManifest).getBannerType(any());
    doReturn(Optional.of("TestCity"))
        .when(mockSettingsManifest)
        .getWhitelabelCivicEntityShortName(any());

    BaseViewDeps baseViewDeps =
        new BaseViewDeps(
            instanceOf(TemplateEngine.class),
            instanceOf(ThymeleafModule.PlayThymeleafContextFactory.class),
            mockSettingsManifest,
            instanceOf(MessagesApi.class),
            instanceOf(Environment.class));
    LayoutDeps layoutDeps =
        new LayoutDeps(
            baseViewDeps,
            instanceOf(BundledAssetsFinder.class),
            instanceOf(ProfileUtils.class),
            new DeploymentType(/* isDev= */ true, /* isStaging= */ false));

    CustomView customView =
        new CustomView(layoutDeps, "../../test/views/admin/testTemplateWithoutScriptBlock.html");
    String result = customView.render(fakeRequest(), new CustomViewModel());

    assertThat(result).contains("cf-demo-banner");
    assertThat(result).contains("Demo: TestCity");
    assertThat(result).contains("View as:");
    assertThat(result)
        .containsPattern(
            "<a\\s+href=\"/logout\"\\s+class=\"usa-button"
                + " usa-button--outline\"\\s+id=\"resident\"\\s*>Resident</a");
    assertThat(result).containsPattern("<div\\s+class=\"usa-button\"\\s+id=\"admin\">");
    assertThat(result)
        .containsPattern(
            "class=\"usa-button usa-button--outline\"\\s+id=\"program-admin\"\\s*>Program"
                + " Admin</a");
    assertThat(result)
        .containsPattern(
            "class=\"usa-button"
                + " usa-button--outline\"\\s+id=\"trusted-intermediary\"\\s*>Trusted"
                + " Intermediary</a");
  }

  @Test
  public void render_demo_banner_highlights_active_program_admin_role() {
    ProfileFactory profileFactory = instanceOf(ProfileFactory.class);
    CiviFormProfile programAdminProfile =
        profileFactory.wrapProfileData(profileFactory.createNewProgramAdmin());
    ProfileUtils mockProfileUtils = instanceOf(ProfileUtils.class);
    when(mockProfileUtils.currentUserProfile(any())).thenReturn(programAdminProfile);

    SettingsManifest mockSettingsManifest = spy(instanceOf(SettingsManifest.class));
    doReturn(Optional.of("demo")).when(mockSettingsManifest).getBannerType(any());

    BaseViewDeps baseViewDeps =
        new BaseViewDeps(
            instanceOf(TemplateEngine.class),
            instanceOf(ThymeleafModule.PlayThymeleafContextFactory.class),
            mockSettingsManifest,
            instanceOf(MessagesApi.class),
            instanceOf(Environment.class));
    LayoutDeps layoutDeps =
        new LayoutDeps(
            baseViewDeps,
            instanceOf(BundledAssetsFinder.class),
            mockProfileUtils,
            new DeploymentType(/* isDev= */ true, /* isStaging= */ false));

    CustomView customView =
        new CustomView(layoutDeps, "../../test/views/admin/testTemplateWithoutScriptBlock.html");
    String result = customView.render(fakeRequest(), new CustomViewModel());

    assertThat(result).contains("cf-demo-banner");
    assertThat(result)
        .containsPattern(
            "<a\\s+href=\"/logout\"\\s+class=\"usa-button"
                + " usa-button--outline\"\\s+id=\"resident\"\\s*>Resident</a");
    assertThat(result)
        .containsPattern(
            "class=\"usa-button usa-button--outline\"\\s+id=\"admin\"\\s*>Civiform Admin</a");
    assertThat(result).containsPattern("<div\\s+class=\"usa-button\"\\s+id=\"program-admin\"\\s*>");
  }
}
