package views.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static support.FakeRequestBuilder.fakeRequest;

import auth.ProfileUtils;
import controllers.WithMockedProfiles;
import modules.ThymeleafModule;
import org.junit.Before;
import org.junit.Test;
import org.thymeleaf.TemplateEngine;
import services.ViteService;
import services.settings.SettingsManifest;

public class AdminLayoutBaseViewTest extends WithMockedProfiles {
  private record CustomViewModel() implements BaseViewModel {}

  public static class CustomView extends AdminLayoutBaseView<CustomViewModel> {
    private final String pageTemplate;

    public CustomView(
        TemplateEngine templateEngine,
        ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
        SettingsManifest settingsManifest,
        ViteService viteService,
        ProfileUtils profileUtils,
        String pageTemplate) {
      super(
          templateEngine, playThymeleafContextFactory, settingsManifest, viteService, profileUtils);
      this.pageTemplate = pageTemplate;
    }

    @Override
    protected String pageTitle() {
      return "page-title-1";
    }

    @Override
    protected String pageTemplate() {
      return pageTemplate;
    }
  }

  private CustomView createView(String pageTemplate) {
    return new CustomView(
        instanceOf(TemplateEngine.class),
        instanceOf(ThymeleafModule.PlayThymeleafContextFactory.class),
        instanceOf(SettingsManifest.class),
        instanceOf(ViteService.class),
        instanceOf(ProfileUtils.class),
        pageTemplate);
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
    assertThat(result).contains("-uswds.min.css");
    assertThat(result).contains("-uswds-init.min.js");
    assertThat(result).contains("-uswds.bundle.js");
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
    assertThat(result).contains("-uswds.min.css");
    assertThat(result).contains("-uswds-init.min.js");
    assertThat(result).contains("-uswds.bundle.js");
    assertThat(result).contains("-admin.bundle.js");
    assertThat(result).contains("page-title-1");
    assertThat(result).contains("page-content-1");
    assertThat(result).doesNotContain("page-scripts-1");
  }
}
