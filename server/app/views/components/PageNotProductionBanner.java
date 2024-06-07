package views.components;

import static com.google.gdata.util.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h4;
import static j2html.TagCreator.p;
import static j2html.TagCreator.rawHtml;

import com.google.inject.Inject;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.Messages;
import play.mvc.Http;
import services.DeploymentType;
import services.MessageKey;
import services.settings.SettingsManifest;

/**
 * This will build a sticky banner that follows the user as they scroll. Enable the
 * SHOW_NOT_PRODUCTION_BANNER_ENABLED setting to add this banner to the page. It will not show on a
 * production site even if the SHOW_NOT_PRODUCTION_BANNER_ENABLED setting is enabled.
 *
 * <p>Set the CIVIC_ENTITY_PRODUCTION_URL to include an optional link to the production site.
 *
 * <p>The purpose is to make it extremely clear to people loading a page if the site is Production
 * or not.
 */
public final class PageNotProductionBanner {

  private static final Logger logger = LoggerFactory.getLogger(PageNotProductionBanner.class);
  private final SettingsManifest settingsManifest;
  private final DeploymentType deploymentType;

  @Inject
  public PageNotProductionBanner(SettingsManifest settingsManifest, DeploymentType deploymentType) {
    this.settingsManifest = checkNotNull(settingsManifest);
    this.deploymentType = checkNotNull(deploymentType);
  }

  public Optional<DivTag> render(Http.Request request, Messages messages) {
    if (!settingsManifest.getShowNotProductionBannerEnabled(request)) {
      return Optional.empty();
    }

    if (!deploymentType.isDevOrStaging()) {
      logger.debug("Don't show this banner on production");
      return Optional.empty();
    }

    String productionUrl = settingsManifest.getCivicEntityProductionUrl(request).orElse("");

    ATag link =
        a(settingsManifest.getWhitelabelCivicEntityShortName(request).orElse("") + " CiviForm")
            .withHref(productionUrl)
            .withClasses("font-bold", "underline", "hover:no-underline");

    String notForProductionBannerLine1 =
        messages.at(MessageKey.NOT_FOR_PRODUCTION_BANNER_LINE_1.getKeyName());
    String notForProductionBannerLine2 =
        messages.at(MessageKey.NOT_FOR_PRODUCTION_BANNER_LINE_2.getKeyName(), link);

    return Optional.of(
        div()
            .with(h4(notForProductionBannerLine1).withClasses("text-xl", "font-bold"))
            .condWith(
                !productionUrl.isBlank(),
                p(rawHtml(notForProductionBannerLine2)).withClasses("mt-4"))
            .withClasses(
                "sticky",
                "width-full",
                "top-0",
                "left-0",
                "z-50",
                "text-white",
                "bg-red-600",
                "p-6",
                "shadow-xl",
                "shadow-slate-900",
                "flex",
                "flex-col",
                "justify-center",
                "items-center",
                "text-center")
            .attr("role", "alert")
            .attr("aria-label", notForProductionBannerLine1));
  }
}
