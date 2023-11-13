package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.img;
import static j2html.TagCreator.input;
import static j2html.TagCreator.link;
import static j2html.TagCreator.p;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.script;
import static j2html.TagCreator.span;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import controllers.AssetsFinder;
import j2html.TagCreator;
import j2html.tags.ContainerTag;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.ImgTag;
import j2html.tags.specialized.LinkTag;
import j2html.tags.specialized.PTag;
import j2html.tags.specialized.ScriptTag;
import j2html.tags.specialized.SpanTag;
import java.time.Instant;
import java.util.Optional;
import javax.inject.Inject;
import services.DateConverter;
import services.question.types.QuestionDefinition;
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
   * Generates an HTML script tag for loading the Azure Blob Storage client library from the
   * jsdelivr.net CDN. TOOD(https://github.com/seattle-uat/civiform/issues/2349): Stop using this.
   */
  public ScriptTag makeAzureBlobStoreScriptTag() {
    return script()
        .withSrc("https://cdn.jsdelivr.net/npm/@azure/storage-blob@10.5.0")
        .withType("text/javascript")
        .attr("crossorigin", "anonymous")
        .attr("integrity", "sha256-VFdCcG0JBuOTN0p15rwVT5EIuL7bzWMYi4aD6KeDqus=");
  }

  /**
   * Generates an HTML script tag for loading the javascript file found at public/main/[path].js.
   */
  public ScriptTag makeLocalJsTag(String path) {
    return script().withSrc(assetsFinder.path(path + ".js")).withType("text/javascript");
  }

  /**
   * Generates a script tag for loading a javascript asset that is provided by a web JAR and found
   * at the given asset route.
   */
  public ScriptTag makeWebJarsTag(String assetsRoute) {
    return script().withSrc(assetsFinder.path(assetsRoute));
  }

  /** Generates an HTML link tag for loading the CSS file found at public/main/[filePath].css. */
  LinkTag makeLocalCssTag(String filePath) {
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
            Joiner.on(" ").join(extraClasses))
        .withStyle("width: 115px")
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
    String formattedUpdateTime = time.map(dateConverter::renderDateTime).orElse("unknown");
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
    return span(rawHtml("&nbsp;*")).withClasses("text-red-600", "font-semibold");
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
  public static ButtonTag makeToggleButton(
      String fieldName, boolean enabled, Optional<String> idPrefix, Optional<String> text) {
    String buttonId = idPrefix.map((v) -> v + "-toggle").orElse("");
    String inputId = idPrefix.map((v) -> v + "-toggle-input").orElse("");
    String backgroundId = idPrefix.map((v) -> v + "-toggle-background").orElse("");
    String nubId = idPrefix.map((v) -> v + "-toggle-nub").orElse("");

    boolean idPresent = idPrefix.isPresent();
    ButtonTag button =
        TagCreator.button()
            .withClasses(
                "cf-toggle",
                "flex",
                "px-0",
                "gap-2",
                "items-center",
                "text-black",
                "font-normal",
                "bg-transparent",
                "rounded-full",
                StyleUtils.hover("bg-transparent"))
            .withType("button")
            .withCondId(idPresent, buttonId)
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
    return button;
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
        String.format("Universal %s Question", questionDefinition.getQuestionType().getLabel()),
        Lists.asList("cf-universal-badge", classes).toArray(new String[0]));
  }
}
