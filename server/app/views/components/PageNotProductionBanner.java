package views.components;

import static com.google.gdata.util.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h4;
import static j2html.TagCreator.join;
import static j2html.TagCreator.p;

import com.google.inject.Inject;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;
import services.DeploymentType;
import services.settings.SettingsManifest;

/**
 * This will build a sticky banner that follow the user as they scroll. It will show only on a
 * staging site that has the CIVIC_ENTITY_PRODUCTION_URL setting configured.
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

  public Optional<DivTag> render(Http.Request request) {
    if (deploymentType.isDev()) {
      logger.debug("Don't show site wide emergency banner on dev");
      return Optional.empty();
    }

    if (!deploymentType.isStaging()) {
      logger.debug("Don't show site wide emergency banner on prod");
      return Optional.empty();
    }

    // Don't show without a production url
    Optional<String> productionUrl = settingsManifest.getCivicEntityProductionUrl(request);

    if (productionUrl.isEmpty()) {
      logger.debug("CIVIC_ENTITY_PRODUCTION_URL setting is not set");
      return Optional.empty();
    }

    String messageForTestingOnly =
        "This site is for testing purposes only. Do not enter personal information.";

    ATag link =
        a(settingsManifest.getWhitelabelCivicEntityShortName(request).orElse("") + " CiviForm")
            .withHref(productionUrl.get())
            .withClasses("font-bold", "underline", "hover:no-underline");

    return Optional.of(
        div()
            .with(
                div()
                    .with(
                        h4(messageForTestingOnly).withClasses("text-xl", "mb-4", "font-bold"),
                        p(join("To apply to a program or service go to ", link, ".")))
                    .withClasses(
                        "flex", "flex-col", "justify-center", "items-center", "text-center"))
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
                "shadow-slate-900")
            .attr("role", "alert")
            .attr("aria-label", messageForTestingOnly));
  }
}
