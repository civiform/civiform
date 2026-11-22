package views.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static support.FakeRequestBuilder.fakeRequest;

import auth.CiviFormProfile;
import auth.ProfileFactory;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.WithMockedProfiles;
import java.time.Clock;
import java.util.Optional;
import modules.ThymeleafModule;
import org.junit.Test;
import org.thymeleaf.TemplateEngine;
import play.Environment;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import services.BundledAssetsFinder;
import services.DeploymentType;
import services.settings.SettingsManifest;
import views.BaseViewModel;
import views.dev.DevToolsPageView;
import views.dev.DevToolsPageViewModel;
import views.support.UnsupportedBrowserPageView;

public class UnauthenticatedLayoutBaseViewTest extends WithMockedProfiles {
  protected record CustomViewModel() implements BaseViewModel {}

  public static class CustomView extends UnauthenticatedLayoutBaseView<CustomViewModel> {
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
    doReturn(Optional.of("demo")).when(mockSettingsManifest).getBannerType();
    doReturn(Optional.of("TestCity"))
        .when(mockSettingsManifest)
        .getWhitelabelCivicEntityShortName();
    return mockSettingsManifest;
  }

  private ProfileUtils createProfileUtils(Optional<CiviFormProfile> profile) {
    ProfileUtils mockProfileUtils = instanceOf(ProfileUtils.class);
    when(mockProfileUtils.optionalCurrentUserProfile(any(play.mvc.Http.RequestHeader.class)))
        .thenReturn(profile);
    return mockProfileUtils;
  }

  private CustomView createView(String pageTemplate, boolean isDev) {
    LayoutDeps layoutDeps =
        createLayoutDeps(
            instanceOf(SettingsManifest.class), createProfileUtils(Optional.empty()), isDev);
    return new CustomView(layoutDeps, pageTemplate);
  }

  @Test
  public void render_page_with_script_block_smoke_test() {
    CustomView customView =
        createView("../../test/views/admin/testTemplateWithScriptBlock.html", true);

    String result = customView.render(fakeRequest(), new CustomViewModel());

    assertThat(result).contains("-northstar_css.min.css");
    assertThat(result).doesNotContain("-uswds_css.min.css");
    assertThat(result).contains("-uswdsinit_js.bundle.js");
    assertThat(result).contains("-uswds.min.js");
    assertThat(result).contains("-applicant.bundle.js");
    assertThat(result).doesNotContain("-admin.bundle.js");
    assertThat(result).contains("page-title-1 - CiviForm");
    assertThat(result).contains("page-content-1");
    assertThat(result).contains("page-scripts-1");
    assertThat(result).contains("id=\"modal-container\"");
  }

  @Test
  public void render_page_without_script_block_smoke_test() {
    CustomView customView =
        createView("../../test/views/admin/testTemplateWithoutScriptBlock.html", true);

    String result = customView.render(fakeRequest(), new CustomViewModel());

    assertThat(result).contains("page-content-1");
    assertThat(result).doesNotContain("page-scripts-1");
  }

  @Test
  public void render_has_no_navigation_or_footer() {
    CustomView customView =
        createView("../../test/views/admin/testTemplateWithoutScriptBlock.html", true);

    String result = customView.render(fakeRequest(), new CustomViewModel());

    assertThat(result).doesNotContain("usa-banner");
    assertThat(result).doesNotContain("usa-nav");
    assertThat(result).doesNotContain("usa-footer");
    assertThat(result).doesNotContain("id=\"sidebar\"");
  }

  @Test
  public void render_shows_warning_toast_in_dev_or_staging() {
    CustomView customView =
        createView("../../test/views/admin/testTemplateWithoutScriptBlock.html", true);

    String result = customView.render(fakeRequest(), new CustomViewModel());

    assertThat(result).contains("id=\"warning-message\"");
  }

  @Test
  public void render_hides_warning_toast_in_prod() {
    CustomView customView =
        createView("../../test/views/admin/testTemplateWithoutScriptBlock.html", false);

    String result = customView.render(fakeRequest(), new CustomViewModel());

    assertThat(result).doesNotContain("id=\"warning-message\"");
  }

  @Test
  public void render_demo_banner_on_dev_tools_page_unauthenticated() {
    LayoutDeps layoutDeps =
        createLayoutDeps(createDemoSettingsManifest(), createProfileUtils(Optional.empty()), true);
    DevToolsPageView devToolsPageView = new DevToolsPageView(layoutDeps);

    String result =
        devToolsPageView.render(
            fakeRequest(),
            DevToolsPageViewModel.builder().durableJobOptions(ImmutableList.of()).build());

    assertThat(result).contains("cf-demo-banner");
    assertThat(result).doesNotContain("usa-footer");
    assertThat(result)
        .containsPattern(
            "<div\\s+class=\"usa-button\"\\s+id=\"resident\"\\s*>\\s*<span>Resident</span\\s*>"
                + "<span\\s+class=\"usa-sr-only\">");
  }

  @Test
  public void render_demo_banner_highlights_signed_in_role() {
    ProfileFactory profileFactory = instanceOf(ProfileFactory.class);
    CiviFormProfile programAdminProfile =
        profileFactory.wrapProfileData(profileFactory.createNewProgramAdmin());
    LayoutDeps layoutDeps =
        createLayoutDeps(
            createDemoSettingsManifest(),
            createProfileUtils(Optional.of(programAdminProfile)),
            true);
    DevToolsPageView devToolsPageView = new DevToolsPageView(layoutDeps);

    String result =
        devToolsPageView.render(
            fakeRequest(),
            DevToolsPageViewModel.builder().durableJobOptions(ImmutableList.of()).build());

    assertThat(result)
        .containsPattern(
            "<div\\s+class=\"usa-button\"\\s+id=\"program-admin\"\\s*>\\s*<span>Program"
                + " Admin</span\\s*><span\\s+class=\"usa-sr-only\">");
  }

  @Test
  public void render_unsupported_browser_page() {
    LayoutDeps layoutDeps =
        createLayoutDeps(
            instanceOf(SettingsManifest.class), createProfileUtils(Optional.empty()), false);
    UnsupportedBrowserPageView view = new UnsupportedBrowserPageView(layoutDeps);

    String result = view.render(fakeRequest(), new UnsupportedBrowserPageView.Model());

    assertThat(result).contains("The browser you are currently using is not supported - CiviForm");
    assertThat(result).containsPattern("<h1[^>]*>Unsupported browser</h1>");
    assertThat(result).contains("Microsoft Edge logo");
    assertThat(result).contains("-applicant.bundle.js");
    assertThat(result).doesNotContain("tailwind.css");
  }
}
