package views;

import static j2html.TagCreator.button;
import static j2html.TagCreator.p;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import controllers.AssetsFinder;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FieldsetTag;
import j2html.tags.specialized.LinkTag;
import j2html.tags.specialized.ScriptTag;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import services.DateConverter;
import views.style.BaseStyles;

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
  public void makeAlert_createsAlertComponentWithTheCorrectClasses() {
    DivTag alertComponent =
        ViewUtils.makeAlert(
            "some text", false, Optional.of("title"), BaseStyles.ALERT_INFO, BaseStyles.ALERT_SLIM);
    assertThat(alertComponent.render())
        .isEqualTo(
            "<div class=\"usa-alert usa-alert--info usa-alert--slim\" aria-live=\"polite\""
                + " role=\"alert\"><div class=\"usa-alert__body\"><h4"
                + " class=\"usa-alert__heading\">title</h4><p class=\"usa-alert__text\">some"
                + " text</p></div></div>");
  }

  @Test
  public void makeAlert_doesNotIncludeTitleIfNoneIsPresent() {
    DivTag alertComponent =
        ViewUtils.makeAlert("some text", false, Optional.empty(), BaseStyles.ALERT_WARNING);
    assertThat(alertComponent.render())
        .isEqualTo(
            "<div class=\"usa-alert usa-alert--warning\" aria-live=\"polite\" role=\"alert\"><div"
                + " class=\"usa-alert__body\"><p class=\"usa-alert__text\">some"
                + " text</p></div></div>");
  }

  @Test
  public void makeUSWDSModal_doesNotIncludeFooterIfHasFooterIsFalse() {
    DivTag modal =
        ViewUtils.makeUSWDSModal(
            p("Welcome to the test modal!"),
            "test-modal",
            "header",
            false,
            button("Open the test modal"),
            "",
            "");
    assertThat(modal.render()).doesNotContain("usa-modal__footer");
  }

  @Test
  public void makeMemorableDate_createsDateComponentWithCorrectFieldNames() {
    FieldsetTag dateComponent = ViewUtils.makeMemorableDate("", "", "", "Test DOB", false);
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
    FieldsetTag dateComponent = ViewUtils.makeMemorableDate("", "", "", "Test DOB", true);
    String rendered = dateComponent.render();
    assertThat(rendered)
        .contains("<div class=\"text-red-600 text-xs\" id=\"memorable_date_error\"><span>Error:");

    assertThat(rendered).contains("<select class=\"usa-input--error");
  }

  @Test
  public void makeMemorableDate_doesNotShowErrorWhenShowErrorIsFalse() {
    FieldsetTag dateComponent = ViewUtils.makeMemorableDate("04", "04", "", "Test DOB", false);
    String rendered = dateComponent.render();
    assertThat(rendered).doesNotContain("<div class=\"text-red-600 text-xs\"><span>Error:");

    assertThat(rendered).doesNotContain("usa-input--error");
  }
}
