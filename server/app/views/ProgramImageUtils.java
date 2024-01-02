package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.img;

import com.google.inject.Inject;
import j2html.tags.specialized.ImgTag;
import java.util.Locale;
import java.util.Optional;
import play.mvc.Http;
import services.cloud.PublicFileNameFormatter;
import services.cloud.PublicStorageClient;
import services.program.ProgramDefinition;
import services.settings.SettingsManifest;
import views.style.StyleUtils;

/** Utility class for rendering program images. */
public final class ProgramImageUtils {
  private final PublicStorageClient publicStorageClient;

  private final SettingsManifest settingsManifest;

  @Inject
  public ProgramImageUtils(
      PublicStorageClient publicStorageClient, SettingsManifest settingsManifest) {
    this.publicStorageClient = checkNotNull(publicStorageClient);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  /**
   * Returns an <img> tag with the program image if the feature is enabled and the program image
   * data is valid. Returns an empty optional otherwise.
   *
   * @param isWithinProgramCard true if this image will be shown within the context of the program
   *     card we show to applicants and false if this image will be shown on its own.
   */
  public Optional<ImgTag> createProgramImage(
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

    String classes = StyleUtils.joinStyles("w-full", "aspect-video", "object-cover");
    if (isWithinProgramCard) {
      // Only round the bottom corners when showing the image in context of a program card.
      classes = StyleUtils.joinStyles(classes, "rounded-b-lg");
    }

    return Optional.of(
        img()
            .withSrc(publicStorageClient.getPublicDisplayUrl(program.summaryImageFileKey().get()))
            .withAlt(getProgramImageAltText(program, preferredLocale))
            .withClasses(classes));
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
