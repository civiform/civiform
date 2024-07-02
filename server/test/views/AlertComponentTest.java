package views;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import j2html.tags.specialized.DivTag;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.AlertType;
import views.style.BaseStyles;

@RunWith(JUnitParamsRunner.class)
public class AlertComponentTest {
  Map<String, String> expectResultsMap =
      ImmutableMap.<String, String>builder()
          .put(
              "visibleWithTitleAndText",
              """
              <div class="usa-alert usa-alert--%s" aria-live="polite" role="alert">
                  <div class="usa-alert__body">
                      <h4 class="usa-alert__heading">
                          title
                      </h4>
                      <p class="usa-alert__text">
                          some text
                      </p>
                  </div>
              </div>
              """)
          .put(
              "hiddenWithTitleAndText",
              """
              <div hidden class="usa-alert usa-alert--%s" aria-live="polite" role="alert">
                  <div class="usa-alert__body">
                      <h4 class="usa-alert__heading">
                          title
                      </h4>
                      <p class="usa-alert__text">
                          some text
                      </p>
                  </div>
              </div>
              """)
          .put(
              "visibleWithTextOnly",
              """
              <div class="usa-alert usa-alert--%s" aria-live="polite" role="alert">
                  <div class="usa-alert__body">
                      <p class="usa-alert__text">
                          some text
                      </p>
                  </div>
              </div>
              """)
          .put(
              "hiddenWithTextOnly",
              """
              <div hidden class="usa-alert usa-alert--%s" aria-live="polite" role="alert">
                  <div class="usa-alert__body">
                      <p class="usa-alert__text">
                          some text
                      </p>
                  </div>
              </div>
              """)
          .build();

  record Param(
      AlertType alertType,
      String text,
      Optional<String> title,
      boolean hidden,
      String expectedResultKey) {}

  private Object[] alertTypeTestParameters() {
    String text = "some text";
    String title = "title";

    return new Object[] {
      // visible alert with title and text
      new Param(AlertType.ERROR, text, Optional.of(title), false, "visibleWithTitleAndText"),
      new Param(AlertType.INFO, text, Optional.of(title), false, "visibleWithTitleAndText"),
      new Param(AlertType.SUCCESS, text, Optional.of(title), false, "visibleWithTitleAndText"),
      new Param(AlertType.WARNING, text, Optional.of(title), false, "visibleWithTitleAndText"),

      // hidden alert with title and text
      new Param(AlertType.ERROR, text, Optional.of(title), true, "hiddenWithTitleAndText"),
      new Param(AlertType.INFO, text, Optional.of(title), true, "hiddenWithTitleAndText"),
      new Param(AlertType.SUCCESS, text, Optional.of(title), true, "hiddenWithTitleAndText"),
      new Param(AlertType.WARNING, text, Optional.of(title), true, "hiddenWithTitleAndText"),

      // visible alert with text only
      new Param(AlertType.ERROR, text, Optional.empty(), false, "visibleWithTextOnly"),
      new Param(AlertType.INFO, text, Optional.empty(), false, "visibleWithTextOnly"),
      new Param(AlertType.SUCCESS, text, Optional.empty(), false, "visibleWithTextOnly"),
      new Param(AlertType.WARNING, text, Optional.empty(), false, "visibleWithTextOnly"),

      // hidden alert with text only
      new Param(AlertType.ERROR, text, Optional.empty(), true, "hiddenWithTextOnly"),
      new Param(AlertType.INFO, text, Optional.empty(), true, "hiddenWithTextOnly"),
      new Param(AlertType.SUCCESS, text, Optional.empty(), true, "hiddenWithTextOnly"),
      new Param(AlertType.WARNING, text, Optional.empty(), true, "hiddenWithTextOnly"),
    };
  }

  @Test
  @Parameters(method = "alertTypeTestParameters")
  public void makeAlert_createsAlertComponentCorrectly(Param param) {
    DivTag alertComponent =
        AlertComponent.renderFullView(
            param.alertType(), param.text(), param.title(), param.hidden());
    assertThat(alertComponent.renderFormatted())
        .isEqualTo(
            expectResultsMap
                .get(param.expectedResultKey())
                .formatted(param.alertType().name().toLowerCase(Locale.ROOT)));
  }

  @Test
  @Parameters(method = "alertTypeTestParameters")
  public void makeAlertSlim_createsAlertComponentCorrectly(Param param) {
    // The only difference between these and makeAlert is the inclusion of the
    // class usa-alert--slim
    DivTag alertComponent =
        AlertComponent.renderSlimView(param.alertType(), param.text(), param.hidden());
    assertThat(alertComponent.render()).contains(BaseStyles.ALERT_SLIM);
  }

  private Object[] headingLevelParameters() {
    return new Object[] {
      AlertComponent.HeadingLevel.H2,
      AlertComponent.HeadingLevel.H3,
      AlertComponent.HeadingLevel.H4,
      AlertComponent.HeadingLevel.H5,
      AlertComponent.HeadingLevel.H6
    };
  }

  @Test
  @Parameters(method = "headingLevelParameters")
  public void makeAlert_usesTheCorrectHeadingLevel(AlertComponent.HeadingLevel headingLevel) {
    DivTag alertComponent =
        AlertComponent.renderFullView(
            AlertType.INFO, "some text", Optional.of("title"), true, headingLevel);
    assertThat(alertComponent.render()).contains(headingLevel.name().toLowerCase(Locale.ROOT));
  }
}
