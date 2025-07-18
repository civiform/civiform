package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.img;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.li;
import static j2html.TagCreator.link;
import static j2html.TagCreator.option;
import static j2html.TagCreator.p;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.script;
import static j2html.TagCreator.select;
import static j2html.TagCreator.span;
import static j2html.TagCreator.ul;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import controllers.AssetsFinder;
import j2html.TagCreator;
import j2html.tags.ContainerTag;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FieldsetTag;
import j2html.tags.specialized.ImgTag;
import j2html.tags.specialized.LinkTag;
import j2html.tags.specialized.PTag;
import j2html.tags.specialized.ScriptTag;
import j2html.tags.specialized.SpanTag;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Messages;
import services.DateConverter;
import services.MessageKey;
import services.question.types.QuestionDefinition;
import views.components.ButtonStyles;
import views.components.Icons;
import views.components.LinkElement;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Utility class for accessing stateful view dependencies. */
public final class ViewUtils {
  private final AssetsFinder assetsFinder;
  private final DateConverter dateConverter;

  @Inject
  ViewUtils(AssetsFinder assetsFinder, DateConverter dateConverter) {
    this.assetsFinder = checkNotNull(assetsFinder);
    this.dateConverter = checkNotNull(dateConverter);
  }

  /**
   * Generates an HTML script tag for loading the javascript file found at public/main/[path].js.
   */
  public ScriptTag makeLocalJsTag(String path) {
    return script().withSrc(assetsFinder.path(path + ".js")).withType("text/javascript");
  }

  /**
   * Generates a script tag for loading a javascript asset that is provided by a web JAR and found
   * at the given asset route. TODO(#2349): Start using this.
   */
  public ScriptTag makeWebJarsTag(String assetsRoute) {
    return script().withSrc(assetsFinder.path(assetsRoute));
  }

  /** Generates an HTML link tag for loading the CSS file found at public/main/[filePath].css. */
  public LinkTag makeLocalCssTag(String filePath) {
    return link().withHref(assetsFinder.path(filePath + ".css")).withRel("stylesheet");
  }

  public ImgTag makeLocalImageTag(String filename) {
    return img().withSrc(assetsFinder.path("Images/" + filename + ".png"));
  }

  public static ButtonTag makeSvgTextButton(String buttonText, Icons icon) {
    return button()
        .with(
            Icons.svg(icon)
                // 4.5 is 18px as defined in tailwind.config.js
                .withClasses("inline-block", "h-4.5", "w-4.5"),
            span(buttonText).withClass("text-left"));
  }

  /**
   * Used to indicate if a view that shows information about a program is displaying a draft (and
   * thus is editable) or an active program (not editable). Values here match the database statuses
   * but are limited to the statuses that are viewable for civiform admins.
   */
  public enum ProgramDisplayType {
    ACTIVE,
    DRAFT,
    PENDING_DELETION
  }

  public static DivTag makeSvgToolTipRightAnchored(String toolTipText, Icons icon) {
    return makeSvgToolTip(toolTipText, icon, Optional.of("right-1/2 mt-2.5"));
  }

  public static DivTag makeSvgToolTip(String toolTipText, Icons icon) {
    return makeSvgToolTip(toolTipText, icon, Optional.of("mt-2.5"));
  }

  public static DivTag makeSvgToolTipRightAnchoredWithLink(
      String toolTipText, Icons icon, String linkText, String linkRef) {
    ATag link =
        new LinkElement()
            .setStyles("mb-2", "text-sm", "underline")
            .setText(linkText)
            .setHref(linkRef)
            .opensInNewTab()
            .asAnchorText();
    // We must set mt-0 or else a user will be unable to move their mouse inside
    // the tooltip to be able to click on the link without the tooltip disappearing.
    return makeSvgToolTip(toolTipText, icon, Optional.of("right-1/2 mt-0"), link);
  }

  public static DivTag makeSvgToolTip(
      String toolTipText, Icons icon, Optional<String> classes, ContainerTag... otherElements) {
    return div()
        .withClasses("group inline")
        .with(
            Icons.svg(icon).withClasses("inline-block", "w-5", "relative"),
            span(toolTipText)
                .with(otherElements)
                .withClasses(
                    "hidden",
                    "z-50",
                    "group-hover:block",
                    "bg-white",
                    "rounded-3xl",
                    "p-2",
                    "px-4",
                    "text-black",
                    "absolute",
                    "border-gray-200",
                    "border",
                    "text-left",
                    "w-96",
                    "text-sm",
                    "font-normal",
                    "whitespace-normal",
                    "max-w-fit",
                    classes.orElse("")));
  }

  /**
   * Create green "active" or purple "draft" badge. It is used to indicate question or program
   * status in admin view.
   *
   * @param status Status to render.
   * @param extraClasses Additional classes to attach to the outer returned tag. It's useful to add
   *     margin classes to position badge.
   * @return Tag representing the badge. No classes should be added to the returned tag as it will
   *     overwrite existing classes due to how Jhtml works.
   */
  public static PTag makeLifecycleBadge(ProgramDisplayType status, String... extraClasses) {
    String badgeText = "", badgeBGColor = "", badgeFillColor = "";
    switch (status) {
      case ACTIVE:
        badgeText = "Active";
        badgeBGColor = BaseStyles.BG_CIVIFORM_GREEN_LIGHT;
        badgeFillColor = BaseStyles.TEXT_CIVIFORM_GREEN;
        break;
      case DRAFT:
        badgeText = "Draft";
        badgeBGColor = BaseStyles.BG_CIVIFORM_PURPLE_LIGHT;
        badgeFillColor = BaseStyles.TEXT_CIVIFORM_PURPLE;
        break;
      case PENDING_DELETION:
        badgeText = "Archived";
        badgeBGColor = BaseStyles.BG_CIVIFORM_YELLOW_LIGHT;
        badgeFillColor = BaseStyles.TEXT_CIVIFORM_YELLOW;
        break;
    }
    return p().withClasses(
            badgeBGColor,
            badgeFillColor,
            "font-medium",
            "rounded-full",
            "flex",
            "flex-row",
            "gap-x-2",
            "place-items-center",
            "justify-center",
            "h-10",
            "w-32",
            Joiner.on(" ").join(extraClasses))
        .with(
            Icons.svg(Icons.NOISE_CONTROL_OFF).withClasses("inline-block", "w-5", "h-5"),
            span(badgeText).withClass("mr-1"));
  }

  /**
   * Renders "Edited on YYYY/MM/DD" text for given instant. Provides responsive text that shows only
   * date on narrow screens and both date and time on wider screens.
   *
   * @param prefix Text to use before the rendered date. Examples: "Edited on " or "Published on ".
   * @param time Time to render. If time is missing then "unkown" will be rendered.
   * @return Tag containing rendered time.
   */
  public PTag renderEditOnText(String prefix, Optional<Instant> time) {
    String formattedUpdateTime =
        time.map(dateConverter::renderDateTimeHumanReadable).orElse("unknown");
    String formattedUpdateDate = time.map(dateConverter::renderDate).orElse("unknown");
    return p().with(
            span(prefix),
            span(formattedUpdateTime)
                .withClasses(
                    ReferenceClasses.BT_DATE,
                    "font-semibold",
                    "hidden",
                    StyleUtils.responsiveLarge("inline")),
            span(formattedUpdateDate)
                .withClasses(
                    ReferenceClasses.BT_DATE,
                    "font-semibold",
                    StyleUtils.responsiveLarge("hidden")));
  }

  public static SpanTag requiredQuestionIndicator() {
    return requiredQuestionIndicator(/* isVisible= */ true);
  }

  public static SpanTag requiredQuestionIndicator(Boolean isVisible) {
    return span(rawHtml("&nbsp;*"))
        .withClasses(
            "required-indicator", "text-red-600", "font-semibold", isVisible ? "" : "hidden")
        .attr("aria-hidden", true);
  }

  /**
   * Creates a toggle button whose state is toggled via app/assets/javascripts/toggle.ts. Behaves
   * much like a checkbox, and contains a hidden input field to record the state of the toggle, but
   * is a button, which is better for accessibility.
   *
   * @param fieldName The name of the hidden input field, for binding to the form this button is a
   *     part of.
   * @param enabled When true, the toggle renders initially as on.
   * @param idPrefix Optional text to use to set IDs for each component. These will be <id>-toggle
   *     for the top level button element, <id>-toggle-input for the hidden input field,
   *     <id>-toggle-background for the background of the toggle, and <id>-toggle-nub for the nub
   *     inside the toggle.
   * @param text Optional text label to include with the toggle.
   * @return ButtonTag containing the toggle.
   */
  public static DivTag makeToggleButton(
      String fieldName,
      boolean enabled,
      boolean hidden,
      Optional<String> idPrefix,
      Optional<String> text) {
    String buttonId = idPrefix.map((v) -> v + "-toggle").orElse("");
    String inputId = idPrefix.map((v) -> v + "-toggle-input").orElse("");
    String backgroundId = idPrefix.map((v) -> v + "-toggle-background").orElse("");
    String nubId = idPrefix.map((v) -> v + "-toggle-nub").orElse("");

    boolean idPresent = idPrefix.isPresent();
    ButtonTag button =
        TagCreator.button()
            .withCondId(idPresent, buttonId)
            .withClasses(
                "cf-toggle-button",
                "flex",
                "px-0",
                "gap-2",
                "items-center",
                "text-left",
                "text-black",
                "font-normal",
                "bg-transparent",
                "rounded-full",
                StyleUtils.hover("bg-transparent"),
                StyleUtils.focus("rounded"))
            .withType("button")
            .with(
                input()
                    .isHidden()
                    .withCondId(idPresent, inputId)
                    .withName(fieldName)
                    .withClass("cf-toggle-hidden-input")
                    .withValue(Boolean.valueOf(enabled).toString()))
            .with(
                div()
                    .withClasses("relative")
                    .with(
                        div()
                            .withClasses(
                                enabled ? "bg-blue-600" : "bg-gray-600",
                                "w-14",
                                "h-8",
                                "rounded-full",
                                "toggle",
                                "cf-toggle-background"))
                    .withCondId(idPresent, backgroundId)
                    .with(
                        div()
                            .withClasses(
                                "absolute",
                                "bg-white",
                                enabled ? "right-1" : "left-1",
                                "top-1",
                                "w-6",
                                "h-6",
                                "rounded-full",
                                "cf-toggle-nub")
                            .withCondId(idPresent, nubId)));
    text.ifPresent(button::withText);
    return div().withClass("cf-toggle-div").withCondHidden(hidden).with(button);
  }

  /**
   * Makes a badge with a civiform-teal background, white text, and the given icon.
   *
   * @param icon Icon to use in the badge, left of the text.
   * @param text Text for the badge.
   * @param classes Additional classes to apply to the badge.
   * @return DivTag containing the badge.
   */
  public static DivTag makeBadgeWithIcon(Icons icon, String text, String... classes) {
    return div()
        .withClasses(
            "rounded-lg",
            "flex",
            "max-w-fit",
            "px-2",
            "py-1",
            "space-x-1",
            "text-white",
            "bg-civiform-teal",
            String.join(" ", classes))
        .with(Icons.svg(icon).withClasses("flex", "h-6", "w-4"), span(text));
  }

  /**
   * Makes a universal question badge, with text denoting the question type, and a
   * "cf-universal-badge" class applied.
   *
   * @param questionDefinition The {@link QuestionDefinition} associated with the question this
   *     badge will be applied to.
   * @param classes Additional classes to apply to the badge.
   * @return DivTag containing the badge.
   */
  public static DivTag makeUniversalBadge(
      QuestionDefinition questionDefinition, String... classes) {

    return makeBadgeWithIcon(
        Icons.STAR,
        String.format(
            "Universal %s question",
            questionDefinition.getQuestionType().getLabel().toLowerCase(Locale.getDefault())),
        Lists.asList("cf-universal-badge", classes).toArray(new String[0]));
  }

  /**
   * Makes a USWDS Modal component that opens via a button click.
   * https://designsystem.digital.gov/components/modal/
   *
   * @param body The HTML element that will be the main content of the modal.
   * @param elementIdPrefix The prefix for the HTML element ids.
   * @param headerText The header text for the modal.
   * @param linkButtonText The text that will be on the button that opens the modal.
   * @param hasFooter A boolean value that determines whether to include a footer with action
   *     buttons. If the main content has a form, the buttons will already be included with the
   *     form, so no need for the footer.
   * @param firstButtonText Text for the first footer button.
   * @param secondButtonText Text for the second footer button.
   * @return DivTag containing the button that opens the modal and the modal itself.
   */
  public static DivTag makeUswdsModal(
      ContainerTag body,
      String elementIdPrefix,
      String headerText,
      String linkButtonText,
      boolean hasFooter,
      String firstButtonText,
      String secondButtonText) {
    // These are the html element ids
    String modalId = elementIdPrefix + "-modal";
    String headingId = elementIdPrefix + "-heading";
    String descriptionId = elementIdPrefix + "-description";

    DivTag modalContent =
        div()
            .withClass("usa-modal")
            .withId(modalId)
            .attr("aria-labelledby", headingId)
            .attr("aria-describedby", descriptionId)
            .attr("data-modal-type", elementIdPrefix)
            .with(
                div()
                    .withClass("usa-modal__content")
                    .with(
                        div()
                            .withClasses("mx-4", "usa-modal__main")
                            .with(h2(headerText).withClass("usa-modal__heading").withId(headingId))
                            .with(
                                div()
                                    .withClasses("my-6", "usa-prose")
                                    .with(body)
                                    .withId(descriptionId))
                            .condWith(
                                hasFooter,
                                div()
                                    .withClass("usa-modal__footer")
                                    .with(
                                        ul().withClass("usa-button-group")
                                            .with(
                                                li().withClass("usa-button-group__item")
                                                    .with(
                                                        button(firstButtonText)
                                                            .withType("button")
                                                            .withClass("usa-button")
                                                            .attr("data-close-modal")
                                                            .attr("data-modal-primary", "")
                                                            .attr(
                                                                "data-modal-type",
                                                                elementIdPrefix)))
                                            .with(
                                                li().withClass("usa-button-group__item")
                                                    .with(
                                                        button(secondButtonText)
                                                            .withType("button")
                                                            .withClass(
                                                                "usa-button usa-button--unstyled"
                                                                    + " padding-105 text-center")
                                                            .attr("data-close-modal")
                                                            .attr("data-modal-secondary", "")
                                                            .attr(
                                                                "data-modal-type",
                                                                elementIdPrefix))))))
                    .with(
                        BaseHtmlView.iconOnlyButton("Close this window")
                            .withClasses(
                                "usa-button usa-modal__close", ButtonStyles.CLEAR_WITH_ICON, "pt-4")
                            .attr("data-close-modal")
                            .attr("data-modal-type", elementIdPrefix)
                            .with(
                                Icons.svg(Icons.CLOSE)
                                    .withClasses("usa-icon")
                                    .attr("aria-hidden", "true")
                                    .attr("focusable", "false")
                                    .attr("role", "img"))));

    // This div has the button that opens the modal
    DivTag linkDiv =
        div()
            .withClass("margin-y-3")
            .with(
                a(linkButtonText)
                    .withHref("#" + modalId)
                    .withClasses("usa-button", "bg-blue-600")
                    .attr("aria-controls", modalId)
                    .attr("data-open-modal")
                    .attr("data-modal-type", elementIdPrefix))
            .with(modalContent);

    return linkDiv;
  }

  /**
   * Creates a USWDS Memorable Date component. This is to be used in place of a date picker anytime
   * that the date is well-defined, such as a date of birth.
   * https://designsystem.digital.gov/components/memorable-date/
   *
   * @param hideDateComponent Whether the date picker should be hidden
   * @param dayValue The default value which should appear in the "Day" input field
   * @param monthValue The default option which should be selected in the "Month" dropdown
   * @param yearValue The default value which should appear in the "Year" input field
   * @param idPrefix The prefix for the input field ids. Resulting ids will be in the format
   *     <idPrefix>-day, <idPrefix>-month, <idPrefix>-year
   * @param dayInputName The name attribute associated with the "Day" input field
   * @param monthInputName The name attribute associated with the "Month" input field
   * @param yearInputName The name attribute associated with the "Year" input field
   * @param showError Whether an error message should appear
   * @param showRequired Whether required indicators should appear
   * @return ContainerTag
   */
  public static FieldsetTag makeMemorableDate(
      boolean hideDateComponent,
      String dayValue,
      String monthValue,
      String yearValue,
      String idPrefix,
      String dayInputName,
      String monthInputName,
      String yearInputName,
      boolean showError,
      boolean showRequired,
      Optional<Messages> optionalMessages) {
    FieldsetTag dateFieldset =
        fieldset()
            .withId(idPrefix + "-fieldset")
            .withClass("usa-fieldset")
            .withCondHidden(hideDateComponent)
            .with(
                div()
                    .condWith(
                        showError,
                        span(
                            optionalMessages.isPresent()
                                ? optionalMessages
                                    .get()
                                    .at(MessageKey.ERROR_INCOMPLETE_DATE.getKeyName())
                                : "Error: Please enter month, day, and year."))
                    .withClasses("text-red-600 text-xs")
                    .withId("memorable_date_error"),
                div()
                    .withClass("usa-memorable-date")
                    .with(
                        getMonthSelectFormGroup(
                            monthValue,
                            monthInputName,
                            idPrefix,
                            showError && monthValue.isEmpty(),
                            showRequired,
                            optionalMessages),
                        getDayFormGroup(
                            dayValue,
                            dayInputName,
                            idPrefix,
                            showError && dayValue.isEmpty(),
                            showRequired,
                            optionalMessages),
                        getYearFormGroup(
                            yearValue,
                            yearInputName,
                            idPrefix,
                            showError && yearValue.isEmpty(),
                            showRequired,
                            optionalMessages)));
    return dateFieldset;
  }

  /* Helper function for the Memorable Date */
  private static DivTag getDayFormGroup(
      String dayValue,
      String dayInputName,
      String idPrefix,
      boolean hasError,
      boolean showRequired,
      Optional<Messages> optionalMessages) {
    return div()
        .withClass("usa-form-group usa-form-group--day")
        .with(
            label(
                    optionalMessages.isPresent()
                        ? optionalMessages.get().at(MessageKey.DAY_LABEL.getKeyName())
                        : "Day")
                .withClass("usa-label")
                .withFor(idPrefix + "-day")
                .with(requiredQuestionIndicator(showRequired)),
            input()
                .withClass("usa-input")
                .withCondClass(hasError, "usa-input--error mt-2.5")
                .withId(idPrefix + "-day")
                .withName(dayInputName)
                .attr("aria-describedby", "mdHint")
                .attr("inputmode", "numeric")
                .withMaxlength("2")
                .withPattern("[0-9]*")
                .withValue(dayValue));
  }

  /* Helper function for the Memorable Date */
  private static DivTag getYearFormGroup(
      String yearValue,
      String yearInputName,
      String idPrefix,
      boolean hasError,
      boolean showRequired,
      Optional<Messages> optionalMessages) {
    return div()
        .withClass("usa-form-group usa-form-group--year")
        .with(
            label(
                    optionalMessages.isPresent()
                        ? optionalMessages.get().at(MessageKey.YEAR_LABEL.getKeyName())
                        : "Year")
                .withClass("usa-label")
                .withFor(idPrefix + "-year")
                .with(requiredQuestionIndicator(showRequired)),
            input()
                .withClass("usa-input")
                .withCondClass(hasError, "usa-input--error mt-2.5")
                .withId(idPrefix + "-year")
                .withName(yearInputName)
                .attr("aria-describedby", "mdHint")
                .attr("minlength", "4")
                .attr("inputmode", "numeric")
                .withMaxlength("4")
                .withPattern("[0-9]*")
                .withValue(yearValue));
  }

  /* Helper function for the Memorable Date */
  private static DivTag getMonthSelectFormGroup(
      String monthValue,
      String monthInputName,
      String idPrefix,
      boolean hasError,
      boolean showRequired,
      Optional<Messages> optionalMessages) {
    return div()
        .withClass("usa-form-group usa-form-group--month usa-form-group--select")
        .with(
            label(
                    optionalMessages.isPresent()
                        ? optionalMessages.get().at(MessageKey.MONTH_LABEL.getKeyName())
                        : "Month")
                .withClass("usa-label")
                .withFor(idPrefix + "-month")
                .with(requiredQuestionIndicator(showRequired)),
            select()
                .withClass("usa-select")
                .withCondClass(hasError, "usa-input--error mt-2.5 py-1")
                .withId(idPrefix + "-month")
                .withName(monthInputName)
                .attr("aria-describedby", "mdHint")
                .with(
                    option()
                        .withValue("")
                        .withText(
                            "- "
                                + (optionalMessages.isPresent()
                                    ? optionalMessages
                                        .get()
                                        .at(MessageKey.MEMORABLE_DATE_PLACEHOLDER.getKeyName())
                                    : "Select")
                                + " -")
                        .withCondSelected(monthValue.equals("")),
                    option()
                        .withValue("1")
                        .withText(
                            optionalMessages.isPresent()
                                ? optionalMessages
                                    .get()
                                    .at(MessageKey.OPTION_MEMORABLE_DATE_JANUARY.getKeyName())
                                : "01 - January")
                        .withCondSelected(monthValue.equals("1")),
                    option()
                        .withValue("2")
                        .withText(
                            optionalMessages.isPresent()
                                ? optionalMessages
                                    .get()
                                    .at(MessageKey.OPTION_MEMORABLE_DATE_FEBRUARY.getKeyName())
                                : "02 - February")
                        .withCondSelected(monthValue.equals("2")),
                    option()
                        .withValue("3")
                        .withText(
                            optionalMessages.isPresent()
                                ? optionalMessages
                                    .get()
                                    .at(MessageKey.OPTION_MEMORABLE_DATE_MARCH.getKeyName())
                                : "03 - March")
                        .withCondSelected(monthValue.equals("3")),
                    option()
                        .withValue("4")
                        .withText(
                            optionalMessages.isPresent()
                                ? optionalMessages
                                    .get()
                                    .at(MessageKey.OPTION_MEMORABLE_DATE_APRIL.getKeyName())
                                : "04 - April")
                        .withCondSelected(monthValue.equals("4")),
                    option()
                        .withValue("5")
                        .withText(
                            optionalMessages.isPresent()
                                ? optionalMessages
                                    .get()
                                    .at(MessageKey.OPTION_MEMORABLE_DATE_MAY.getKeyName())
                                : "05 - May")
                        .withCondSelected(monthValue.equals("5")),
                    option()
                        .withValue("6")
                        .withText(
                            optionalMessages.isPresent()
                                ? optionalMessages
                                    .get()
                                    .at(MessageKey.OPTION_MEMORABLE_DATE_JUNE.getKeyName())
                                : "06 - June")
                        .withCondSelected(monthValue.equals("6")),
                    option()
                        .withValue("7")
                        .withText(
                            optionalMessages.isPresent()
                                ? optionalMessages
                                    .get()
                                    .at(MessageKey.OPTION_MEMORABLE_DATE_JULY.getKeyName())
                                : "07 - July")
                        .withCondSelected(monthValue.equals("7")),
                    option()
                        .withValue("8")
                        .withText(
                            optionalMessages.isPresent()
                                ? optionalMessages
                                    .get()
                                    .at(MessageKey.OPTION_MEMORABLE_DATE_AUGUST.getKeyName())
                                : "08 - August")
                        .withCondSelected(monthValue.equals("8")),
                    option()
                        .withValue("9")
                        .withText(
                            optionalMessages.isPresent()
                                ? optionalMessages
                                    .get()
                                    .at(MessageKey.OPTION_MEMORABLE_DATE_SEPTEMBER.getKeyName())
                                : "09 - September")
                        .withCondSelected(monthValue.equals("9")),
                    option()
                        .withValue("10")
                        .withText(
                            optionalMessages.isPresent()
                                ? optionalMessages
                                    .get()
                                    .at(MessageKey.OPTION_MEMORABLE_DATE_OCTOBER.getKeyName())
                                : "10 - October")
                        .withCondSelected(monthValue.equals("10")),
                    option()
                        .withValue("11")
                        .withText(
                            optionalMessages.isPresent()
                                ? optionalMessages
                                    .get()
                                    .at(MessageKey.OPTION_MEMORABLE_DATE_NOVEMBER.getKeyName())
                                : "11 - November")
                        .withCondSelected(monthValue.equals("11")),
                    option()
                        .withValue("12")
                        .withText(
                            optionalMessages.isPresent()
                                ? optionalMessages
                                    .get()
                                    .at(MessageKey.OPTION_MEMORABLE_DATE_DECEMBER.getKeyName())
                                : "12 - December")
                        .withCondSelected(monthValue.equals("12"))));
  }
}
