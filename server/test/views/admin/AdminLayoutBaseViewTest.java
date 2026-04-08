package views.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static support.FakeRequestBuilder.fakeRequest;

import controllers.WithMockedProfiles;
import org.junit.Before;
import org.junit.Test;
import views.shared.LayoutDeps;

public class AdminLayoutBaseViewTest extends WithMockedProfiles {
  private record CustomViewModel() implements BaseViewModel {}

  public static class CustomView extends AdminLayoutBaseView<CustomViewModel> {
    private final String pageTemplate;

    public CustomView(LayoutDeps layoutDeps, String pageTemplate) {
      super(layoutDeps);
      this.pageTemplate = pageTemplate;
    }

    @Override
    protected String pageTitle(CustomViewModel model) {
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
}
