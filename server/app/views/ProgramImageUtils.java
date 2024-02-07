package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.rawHtml;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import j2html.tags.DomContent;
import java.util.Locale;
import java.util.Optional;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.mvc.Http;
import services.cloud.PublicFileNameFormatter;
import services.cloud.PublicStorageClient;
import services.program.ProgramDefinition;
import services.settings.SettingsManifest;

/** Utility class for rendering program images. */
public final class ProgramImageUtils {
  private final PublicStorageClient publicStorageClient;

  private final SettingsManifest settingsManifest;
  private final TemplateEngine templateEngine;
  private final ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory;

  @Inject
  public ProgramImageUtils(
      PublicStorageClient publicStorageClient,
      SettingsManifest settingsManifest,
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory) {
    this.publicStorageClient = checkNotNull(publicStorageClient);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.templateEngine = checkNotNull(templateEngine);
    this.playThymeleafContextFactory = checkNotNull(playThymeleafContextFactory);
  }

  /**
   * Returns an <img> tag with the program image if the feature is enabled and the program image
   * data is valid. Returns an empty optional otherwise.
   *
   * @param isWithinProgramCard true if this image will be shown within the context of the program
   *     card we show to applicants and false if this image will be shown on its own.
   */
  public Optional<DomContent> createProgramImage(
      Http.Request request,
      ProgramDefinition program,
      Locale preferredLocale,
      boolean isWithinProgramCard) {
    if (!settingsManifest.getProgramCardImages(request)) {
      return Optional.empty();
    }
    if (program.summaryImageFileKey().isEmpty()) {
      return Optional.empty();
    }
    String summaryImageFileKey = program.summaryImageFileKey().get();
    if (!PublicFileNameFormatter.isFileKeyForPublicProgramImage(summaryImageFileKey)) {
      return Optional.empty();
    }

    ThymeleafModule.PlayThymeleafContext context = playThymeleafContextFactory.create(request);
    context.setVariable(
        "src", publicStorageClient.getPublicDisplayUrl(program.summaryImageFileKey().get()));
    context.setVariable("altText", getProgramImageAltText(program, preferredLocale));
    context.setVariable("isWithinProgramCard", isWithinProgramCard);

    return Optional.of(
        rawHtml(templateEngine.process("ProgramImageFragment", ImmutableSet.of("image"), context)));
  }

  private static String getProgramImageAltText(ProgramDefinition program, Locale preferredLocale) {
    if (program.localizedSummaryImageDescription().isPresent()
        && program.localizedSummaryImageDescription().get().hasTranslationFor(preferredLocale)) {
      return program.localizedSummaryImageDescription().get().getOrDefault(preferredLocale);
    } else {
      // Fall back to the program name if the description hasn't been set.
      return program.localizedName().getOrDefault(preferredLocale);
    }
  }
}
