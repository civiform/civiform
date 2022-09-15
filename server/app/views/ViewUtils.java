package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.button;
import static j2html.TagCreator.img;
import static j2html.TagCreator.link;
import static j2html.TagCreator.p;
import static j2html.TagCreator.script;
import static j2html.TagCreator.span;

import com.google.common.base.Joiner;
import controllers.AssetsFinder;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.ImgTag;
import j2html.tags.specialized.LinkTag;
import j2html.tags.specialized.PTag;
import j2html.tags.specialized.ScriptTag;
import javax.inject.Inject;
import views.components.Icons;
import views.style.BaseStyles;
import views.style.Styles;

/** Utility class for accessing stateful view dependencies. */
public final class ViewUtils {
  private final AssetsFinder assetsFinder;

  @Inject
  ViewUtils(AssetsFinder assetsFinder) {
    this.assetsFinder = checkNotNull(assetsFinder);
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
                .withClasses(Styles.ML_1, Styles.INLINE_BLOCK, Styles.FLEX_SHRINK_0)
                // Can't set 18px using Tailwind CSS classes.
                .withStyle("width: 18px; height: 18px;"),
            span(buttonText).withClass(Styles.TEXT_LEFT));
  }

  public static enum BadgeStatus {
    ACTIVE,
    DRAFT;
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
  public static PTag makeBadge(BadgeStatus status, String... extraClasses) {
    String badgeText = status == BadgeStatus.ACTIVE ? "Active" : "Draft";
    String badgeBGColor =
        status == BadgeStatus.ACTIVE
            ? BaseStyles.BG_CIVIFORM_GREEN_LIGHT
            : BaseStyles.BG_CIVIFORM_PURPLE_LIGHT;
    String badgeFillColor =
        status == BadgeStatus.ACTIVE
            ? BaseStyles.TEXT_CIVIFORM_GREEN
            : BaseStyles.TEXT_CIVIFORM_PURPLE;
    return p().withClasses(
            badgeBGColor,
            badgeFillColor,
            Styles.FONT_MEDIUM,
            Styles.ROUNDED_FULL,
            Styles.FLEX,
            Styles.FLEX_ROW,
            Styles.GAP_X_2,
            Styles.PLACE_ITEMS_CENTER,
            Styles.JUSTIFY_CENTER,
            Styles.H_10,
            Joiner.on(" ").join(extraClasses))
        .withStyle("width: 100px")
        .with(
            Icons.svg(Icons.NOISE_CONTROL_OFF).withClasses(Styles.INLINE_BLOCK, Styles.ML_3_5),
            span(badgeText).withClass(Styles.MR_4));
  }
}
