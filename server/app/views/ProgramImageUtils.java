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

/** TODO */
public final class ProgramImageUtils {
  private final PublicStorageClient publicStorageClient;

  private final SettingsManifest settingsManifest;

  @Inject
  public ProgramImageUtils(
      PublicStorageClient publicStorageClient, SettingsManifest settingsManifest) {
    this.publicStorageClient = checkNotNull(publicStorageClient);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  /** TODO */
  public Optional<ImgTag> createProgramImage(
      Http.Request request, ProgramDefinition program, Locale preferredLocale) {
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
    // TODO(#5676): Can we detect if the image URL is invalid and then not show it?
    // TODO(#5676): Include a placeholder while the image is loading.

    return Optional.of(
        img()
            .withSrc(publicStorageClient.getPublicDisplayUrl(program.summaryImageFileKey().get()))
            .withAlt(getProgramImageAltText(program, preferredLocale))
            .withClasses("w-full", "aspect-video", "object-cover", "rounded-b-lg"));
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
