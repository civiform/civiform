package views;

import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.script;
import static org.assertj.core.api.Assertions.assertThat;

import autovalue.shaded.com.google.common.collect.ImmutableList;
import j2html.tags.specialized.ScriptTag;
import java.util.List;
import junitparams.JUnitParamsRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.api.mvc.request.RequestAttrKey;
import play.mvc.Http.RequestImpl;
import play.test.Helpers;

@RunWith(JUnitParamsRunner.class)
public class CspUtilTest {

  @Test
  public void testGetNonce() {
    RequestImpl request =
        Helpers.fakeRequest().attr(RequestAttrKey.CSPNonce().asJava(), "nonce-value").build();

    String result = CspUtil.getNonce(request);

    assertThat(result).isEqualTo("nonce-value");
  }

  /** This covers both overloads of applyCsp because the List one calls the singular one */
  @Test
  public void testApplyCsp() {
    ImmutableList<ScriptTag> inputs =
        ImmutableList.of(
            script().withSrc("http://foo.com/bar").withType("text/javascript"),
            script().with(rawHtml("console.log(\"hello\")")));

    RequestImpl request =
        Helpers.fakeRequest().attr(RequestAttrKey.CSPNonce().asJava(), "nonce-value").build();

    List<ScriptTag> result = CspUtil.applyCsp(request, inputs);

    assertThat(result.stream().map(ScriptTag::render))
        .allMatch(s -> s.contains("nonce=\"nonce-value\""));
  }
}
