package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.img;

import com.google.inject.Inject;
import j2html.tags.specialized.ImgTag;
import java.util.Locale;
import java.util.Optional;
import services.cloud.PublicFileNameFormatter;
import services.cloud.PublicStorageClient;
import services.program.ProgramDefinition;
import views.style.StyleUtils;

/** Utility class for rendering program images. */
public final class ProgramImageUtils {
  private final PublicStorageClient publicStorageClient;

  @Inject
  public ProgramImageUtils(PublicStorageClient publicStorageClient) {
    this.publicStorageClient = checkNotNull(publicStorageClient);
  }

  /**
   * Returns an <img> tag with the program image if the feature is enabled and the program image
   * data is valid. Returns an empty optional otherwise.
   *
   * @param isWithinProgramCard true if this image will be shown within the context of the program
   *     card we show to applicants and false if this image will be shown on its own.
   */
  public Optional<ImgTag> createProgramImage(
      ProgramDefinition program,
      Locale preferredLocale,
      boolean isWithinProgramCard,
      boolean isProgramFilteringEnabled) {
    if (program.summaryImageFileKey().isEmpty()) {
      return Optional.empty();
    }
    String summaryImageFileKey = program.summaryImageFileKey().get();
    if (!PublicFileNameFormatter.isFileKeyForPublicProgramImage(summaryImageFileKey)) {
      return Optional.empty();
    }

    String styleClasses = StyleUtils.joinStyles("w-full", "aspect-video", "object-cover");
    if (isWithinProgramCard) {
      if (!isProgramFilteringEnabled) {
        // Only round the bottom corners when showing the image in context of a program card.
        styleClasses = StyleUtils.joinStyles(styleClasses, "rounded-b-lg");
      } else {
        // Round all corners when showing the image in context of a program card with filtering.
        styleClasses = StyleUtils.joinStyles(styleClasses, "rounded-lg");
      }
    }

    return Optional.of(
        img()
            .withSrc(publicStorageClient.getPublicDisplayUrl(program.summaryImageFileKey().get()))
            .withAlt(getProgramImageAltText(program, preferredLocale))
            .withClasses(styleClasses));
  }

  public static String getProgramImageAltText(ProgramDefinition program, Locale preferredLocale) {
    if (program.localizedSummaryImageDescription().isPresent()
        && program.localizedSummaryImageDescription().get().hasTranslationFor(preferredLocale)) {
      return program.localizedSummaryImageDescription().get().getOrDefault(preferredLocale);
    } else {
      // Fall back to the program name if the description hasn't been set.
      return program.localizedName().getOrDefault(preferredLocale);
    }
  }
}
