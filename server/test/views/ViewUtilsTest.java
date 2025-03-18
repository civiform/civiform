package views;

import static j2html.TagCreator.p;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import controllers.AssetsFinder;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FieldsetTag;
import j2html.tags.specialized.LinkTag;
import j2html.tags.specialized.ScriptTag;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import services.DateConverter;

@RunWith(JUnitParamsRunner.class)
public class ViewUtilsTest {

  @Rule public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Mock public AssetsFinder assetsFinder;

  @Mock public DateConverter dateConverter;

  public ViewUtils viewUtils;

  @Before
  public void setUp() {
    viewUtils = new ViewUtils(assetsFinder, dateConverter);
  }

  @Test
  public void makeLocalJsTag_createsAScriptTagWithTheJs() {
    when(assetsFinder.path("hello.js")).thenReturn("/full/asset/path.js");
    ScriptTag result = viewUtils.makeLocalJsTag("hello");

    assertThat(result.render())
        .isEqualTo("<script src=\"/full/asset/path.js\" type=\"text/javascript\"></script>");
  }

  @Test
  public void makeLocalCssTag_createsALinkTagWithTheCss() {
    when(assetsFinder.path("stylesheets/hello.css")).thenReturn("/full/asset/path.css");
    LinkTag result = viewUtils.makeLocalCssTag("stylesheets/hello");

    assertThat(result.render())
        .isEqualTo("<link href=\"/full/asset/path.css\" rel=\"stylesheet\">");
  }

  @Test
  public void makeUswdsModal_doesNotIncludeFooterIfHasFooterIsFalse() {
    DivTag modal =
        ViewUtils.makeUswdsModal(
            p("Welcome to the test modal!"), "test-modal", "header", "Button text", false, "", "");
    assertThat(modal.render()).doesNotContain("usa-modal__footer");
  }

  @Test
  public void makeUswdsModal_includesCorrectDataAttributes() {
    String elementIdPrefix = "test-uswds-modal";
    DivTag modal =
        ViewUtils.makeUswdsModal(
            p("Test modal content"),
            elementIdPrefix,
            "Test Header",
            "Open Modal Button Text",
            true,
            "Primary Button Text",
            "Secondary Button Text");
    String rendered = modal.render();

    // Check modal container has correct type attribute
    assertThat(rendered)
        .containsPattern("class=\"usa-modal\"[^>]*data-modal-type=\"" + elementIdPrefix + "\"");

    // Check primary button has all required attributes in that order(regex is attribute order
    // sensitive).
    assertThat(rendered)
        .containsPattern(
            "button[^>]*class=\"usa-button\"[^>]*data-close-modal[^>]*data-modal-primary[^>]*data-modal-type=\""
                + elementIdPrefix
                + "\"");

    // Check secondary button has all required attributes in that order.
    assertThat(rendered)
        .containsPattern(
            "button[^>]*class=\"usa-button usa-button--unstyled padding-105"
                + " text-center\"[^>]*data-close-modal[^>]*data-modal-secondary[^>]*data-modal-type=\""
                + elementIdPrefix
                + "\"");

    // Verify total number of modal type attributes
    String targetString = "data-modal-type=\"" + elementIdPrefix + "\"";
    int count = StringUtils.countMatches(rendered, targetString);
    assertThat(count).isEqualTo(5);
  }

  @Test
  public void makeMemorableDate_createsDateComponentWithCorrectFieldNames() {
    FieldsetTag dateComponent =
        ViewUtils.makeMemorableDate("", "", "", "Test DOB", false, Optional.empty());
    String rendered = dateComponent.render();
    assertThat(rendered)
        .contains("<input class=\"usa-input\" id=\"date_of_birth_day\" name=\"dayQuery\"");
    assertThat(rendered)
        .contains("<input class=\"usa-input\" id=\"date_of_birth_year\" name=\"yearQuery\"");
    assertThat(rendered)
        .contains("<select class=\"usa-select\" id=\"date_of_birth_month\" name=\"monthQuery\"");
  }

  @Test
  public void makeMemorableDate_showsErrorWhenShowErrorIsTrue() {
    FieldsetTag dateComponent =
        ViewUtils.makeMemorableDate("", "", "", "Test DOB", true, Optional.empty());
    String rendered = dateComponent.render();
    assertThat(rendered)
        .contains("<div class=\"text-red-600 text-xs\" id=\"memorable_date_error\"><span>Error:");

    assertThat(rendered).contains("<select class=\"usa-input--error");
  }

  @Test
  public void makeMemorableDate_doesNotShowErrorWhenShowErrorIsFalse() {
    FieldsetTag dateComponent =
        ViewUtils.makeMemorableDate("04", "04", "", "Test DOB", false, Optional.empty());
    String rendered = dateComponent.render();
    assertThat(rendered).doesNotContain("<div class=\"text-red-600 text-xs\"><span>Error:");

    assertThat(rendered).doesNotContain("usa-input--error");
  }
}
