package views.tags;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.img;

import controllers.AssetsFinder;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;
import play.Environment;

/**
 * Thymeleaf custom tag used to render icons.
 *
 * <p>Usage examples:
 * <li>{@code <cf:icon type="some-name" />}
 * <li>{@code <cf:icon type="some-name" class="..." />}
 *
 *     <p>Optionally add {@code xmlns:cf="https://civiform.us/2024/html/icon"} to an html tag to
 *     prevent editor warnings.
 */
public final class IconElementTagProcessor extends AbstractElementTagProcessor {
  private static final String ASSETS_RELATIVE_PATH = "app/assets";
  private static final String USWDS_IMAGES_RELATIVE_PATH = "Images/uswds";

  private static final Logger logger = LoggerFactory.getLogger(IconElementTagProcessor.class);

  private final AssetsFinder assetsFinder;
  private final Environment environment;

  public IconElementTagProcessor(
      String dialectPrefix, AssetsFinder assetsFinder, Environment environment) {
    super(TemplateMode.HTML, dialectPrefix, "icon", true, null, false, 10);
    this.assetsFinder = checkNotNull(assetsFinder);
    this.environment = checkNotNull(environment);
  }

  @Override
  protected void doProcess(
      ITemplateContext context,
      IProcessableElementTag tag,
      IElementTagStructureHandler structureHandler) {
    try {
      String type = tag.getAttributeValue("type");

      Path assetPath = Paths.get(USWDS_IMAGES_RELATIVE_PATH, type + ".svg");
      verifyIconExists(assetPath);

      String cssClass = getAttributeValue(tag, "class");

      var imgTag =
          img()
              .withSrc(assetsFinder.path(assetPath.toString()))
              .attr("aria-hidden", true)
              .attr("role", "img")
              .condAttr(!cssClass.isBlank(), "class", cssClass)
              .withAlt("");

      structureHandler.replaceWith(imgTag.render(), false);
    } catch (RuntimeException ex) {
      logger.error(ex.getMessage(), ex);

      // Throw in dev/test environments to make it clear that the icon
      // can't be found. When in prod mode just log since there is no
      // reason to block the app from working if an icon can't be found.
      if (!environment.isProd()) {
        throw ex;
      }

      structureHandler.replaceWith("", false);
    }
  }

  private void verifyIconExists(Path assetPath) {
    File file = environment.getFile(Paths.get(ASSETS_RELATIVE_PATH).resolve(assetPath).toString());

    if (!file.exists()) {
      throw new RuntimeException(String.format("Can't find icon file at: %s", assetPath));
    }
  }

  /**
   * Returns the value of a specific attribute in the tag (or an empty string if it does not exist).
   *
   * @param completeName the complete name of the attribute that is being queried.
   * @return the value of the queried attribute, or an empty string if it does not exist.
   */
  private static String getAttributeValue(IProcessableElementTag tag, String completeName) {
    if (tag.hasAttribute(completeName)) {
      return tag.getAttributeValue(completeName);
    }

    return "";
  }
}
