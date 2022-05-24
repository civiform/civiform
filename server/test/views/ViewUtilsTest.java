package views;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import controllers.AssetsFinder;
import j2html.tags.specialized.ScriptTag;
import j2html.tags.specialized.LinkTag;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

public class ViewUtilsTest {

  @Rule public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Mock public AssetsFinder assetsFinder;

  public ViewUtils viewUtils;

  @Before
  public void setUp() {
    viewUtils = new ViewUtils(assetsFinder);
  }

  @Test
  public void makeLocalJsTag_createsAScriptTagWithTheJs() {
    when(assetsFinder.path("javascripts/hello.js")).thenReturn("/full/asset/path.js");
    ScriptTag result = viewUtils.makeLocalJsTag("hello");

    assertThat(result.render())
        .isEqualTo("<script src=\"/full/asset/path.js\" type=\"text/javascript\"></script>");
  }

  @Test
  public void makeLocalCssTag_createsALinkTagWithTheCss() {
    when(assetsFinder.path("stylesheets/hello.css")).thenReturn("/full/asset/path.css");
    LinkTag result = viewUtils.makeLocalCssTag("hello");

    assertThat(result.render())
        .isEqualTo("<link href=\"/full/asset/path.css\" rel=\"stylesheet\">");
  }
}
