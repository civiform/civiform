package views;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import controllers.AssetsFinder;
import j2html.tags.Tag;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import play.libs.crypto.DefaultCSRFTokenSigner;

public class ViewUtilsTest {

  @Rule public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Mock public DefaultCSRFTokenSigner tokenSigner;
  @Mock public AssetsFinder assetsFinder;

  public ViewUtils viewUtils;

  @Before
  public void setUp() {
    viewUtils = new ViewUtils(tokenSigner, assetsFinder);
  }

  @Test
  public void makeCsrfTokenInputTag_createsAHiddenFormInput() {
    when(tokenSigner.generateSignedToken()).thenReturn("signed-token");
    Tag result = viewUtils.makeCsrfTokenInputTag();

    assertThat(result.render())
        .isEqualTo("<input hidden value=\"signed-token\" name=\"csrfToken\">");
  }

  @Test
  public void makeLocalJsTag_createsAScriptTagWithTheJs() {
    when(assetsFinder.path("javascripts/hello.js")).thenReturn("/full/asset/path.js");
    Tag result = viewUtils.makeLocalJsTag("hello");

    assertThat(result.render())
        .isEqualTo("<script src=\"/full/asset/path.js\" type=\"text/javascript\"></script>");
  }
}
