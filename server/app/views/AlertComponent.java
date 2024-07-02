package views;

import static j2html.TagCreator.div;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.h4;
import static j2html.TagCreator.h5;
import static j2html.TagCreator.h6;
import static j2html.TagCreator.p;

import com.google.common.collect.Lists;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import services.AlertType;
import views.style.BaseStyles;

/**
 * Render a USWDS Alert Component
 *
 * @see <a
 *     href="https://designsystem.digital.gov/components/alert">https://designsystem.digital.gov/components/alert</a>
 */
public final class AlertComponent {
  /**
   * Heading levels that can be used for Alerts H1 tags are limited to a single instance per page
   * and and not allowed as headings.
   */
  public enum HeadingLevel {
    H2,
    H3,
    H4,
    H5,
    H6
  }

  /**
   * Makes a USWDS Alert component with the given text and optional title. Alert variant is
   * determined by the {@link AlertType} passed in.
   *
   * @param alertType The type of {@link AlertType} alert to show
   * @param text The text to include in the alert.
   * @param title An optional title to be included in the alert.
   * @param hidden Whether or not to set the hidden property on the component.
   * @param headingLevel Allow configuring the heading level between H2-H6. Default is H4
   * @param classes One or more additional classes to apply to the USWDS Alert component.
   * @return DivTag containing the alert.
   */
  public static DivTag renderFullView(
      AlertType alertType,
      String text,
      Optional<String> title,
      boolean hidden,
      HeadingLevel headingLevel,
      String... classes) {
    String alertTypeStyle =
        switch (alertType) {
          case INFO -> BaseStyles.ALERT_INFO;
          case ERROR -> BaseStyles.ALERT_ERROR;
          case SUCCESS -> BaseStyles.ALERT_SUCCESS;
          case WARNING -> BaseStyles.ALERT_WARNING;
          default -> "";
        };

    var headingTag =
        switch (headingLevel) {
          case H2 -> h2();
          case H3 -> h3();
          case H5 -> h5();
          case H6 -> h6();
          default -> h4();
        };

    return div()
        .withCondHidden(hidden)
        .withClasses("usa-alert", alertTypeStyle, String.join(" ", classes))
        // Notify screen readers to read the new text when the element changes
        .attr("aria-live", "polite")
        .attr("role", "alert")
        .with(
            div()
                .withClasses("usa-alert__body")
                .condWith(
                    title.isPresent(),
                    headingTag.withClass("usa-alert__heading").withText(title.orElse("")))
                .with(p().withClass("usa-alert__text").withText(text)));
  }

  /**
   * Makes a USWDS Alert component with the given text and optional title. Alert variant is
   * determined by the {@link AlertType} passed in. If using a title the element will be an H4 tag.
   *
   * @param alertType The type of {@link AlertType} alert to show
   * @param text The text to include in the alert.
   * @param title An optional title to be included in the alert.
   * @param hidden Whether or not to set the hidden property on the component.
   * @param classes One or more additional classes to apply to the USWDS Alert component.
   * @return DivTag containing the alert.
   */
  public static DivTag renderFullView(
      AlertType alertType, String text, Optional<String> title, boolean hidden, String... classes) {
    return renderFullView(alertType, text, title, hidden, HeadingLevel.H4, classes);
  }

  /**
   * Makes a slim version of a USWDS Alert component with the given text. Note that the slim version
   * has no title. Alert variant is determined by the classes passed in.
   * https://designsystem.digital.gov/components/alert/
   *
   * @param text The text to include in the alert.
   * @param hidden Whether or not to set the hidden property on the component.
   * @param classes One or more additional classes to apply to the USWDS Alert component.
   * @return DivTag containing the alert.
   */
  public static DivTag renderSlimView(
      AlertType alertType, String text, boolean hidden, String... classes) {
    return renderFullView(
        alertType,
        text,
        /* title= */ Optional.empty(),
        hidden,
        Lists.asList(BaseStyles.ALERT_SLIM, classes).toArray(new String[0]));
  }
}
