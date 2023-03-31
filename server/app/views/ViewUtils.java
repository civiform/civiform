package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.img;
import static j2html.TagCreator.link;
import static j2html.TagCreator.p;
import static j2html.TagCreator.script;
import static j2html.TagCreator.span;

import com.google.common.base.Joiner;
import controllers.AssetsFinder;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.ImgTag;
import j2html.tags.specialized.LinkTag;
import j2html.tags.specialized.PTag;
import j2html.tags.specialized.ScriptTag;
import java.time.Instant;
import java.util.Optional;
import javax.inject.Inject;
import services.DateConverter;
import views.components.Icons;
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
   * Generates an HTML script tag for loading the javascript file found at
   * public/javascripts/[filename].js.
   */
  public ScriptTag makeLocalJsTag(String filename) {
    return script()
        .withSrc(assetsFinder.path("javascripts/" + filename + ".js"))
        .withType("text/javascript");
  }

  /**
   * Generates a script tag for loading a javascript asset that is provided by a web JAR and found
   * at the given asset route.
   */
  public ScriptTag makeWebJarsTag(String assetsRoute) {
    return script().withSrc(assetsFinder.path(assetsRoute));
  }

  /**
   * Generates an HTML link tag for loading the CSS file found at public/stylesheets/[filename].css.
   */
  LinkTag makeLocalCssTag(String filename) {
    return link()
        .withHref(assetsFinder.path("stylesheets/" + filename + ".css"))
        .withRel("stylesheet");
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
    DRAFT
  }

  public static DivTag makeSvgToolTipRightAnchored(String toolTipText, Icons icon) {
    return makeSvgToolTip(toolTipText, icon, Optional.of("right-1/2"));
  }

  public static DivTag makeSvgToolTip(String toolTipText, Icons icon) {
    return makeSvgToolTip(toolTipText, icon, /* classes= */ Optional.empty());
  }

  public static DivTag makeSvgToolTip(String toolTipText, Icons icon, Optional<String> classes) {
    return div()
        .withClasses("group inline")
        .with(
            Icons.svg(icon).withClasses("inline-block", "w-5", "relative"),
            span(toolTipText)
                .withClasses(
                    "hidden",
                    "group-hover:block",
                    "bg-white",
                    "rounded-3xl",
                    "p-2",
                    "px-4",
                    "text-black",
                    "absolute",
                    "mt-2.5",
                    "border-gray-200",
                    "border",
                    "text-left",
                    "w-96",
                    "text-sm",
                    "font-normal",
                    "whitespace-normal",
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
  public static PTag makeBadge(ProgramDisplayType status, String... extraClasses) {
    String badgeText = status == ProgramDisplayType.ACTIVE ? "Active" : "Draft";
    String badgeBGColor =
        status == ProgramDisplayType.ACTIVE
            ? BaseStyles.BG_CIVIFORM_GREEN_LIGHT
            : BaseStyles.BG_CIVIFORM_PURPLE_LIGHT;
    String badgeFillColor =
        status == ProgramDisplayType.ACTIVE
            ? BaseStyles.TEXT_CIVIFORM_GREEN
            : BaseStyles.TEXT_CIVIFORM_PURPLE;
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
        .withStyle("width: 100px")
        .with(
            Icons.svg(Icons.NOISE_CONTROL_OFF).withClasses("inline-block", "ml-3.5", "w-5", "h-5"),
            span(badgeText).withClass("mr-4"));
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
}
