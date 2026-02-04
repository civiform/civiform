package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.img;

import com.google.inject.Inject;
import j2html.tags.specialized.ImgTag;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.springframework.beans.factory.aot.AotServices;
import scala.concurrent.impl.FutureConvertersImpl;
import services.cloud.PublicFileNameFormatter;
import services.cloud.PublicStorageClient;
import services.program.ProgramDefinition;
import software.amazon.awssdk.services.s3.model.S3Object;
import views.style.StyleUtils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;

import javax.imageio.ImageIO;

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
      ProgramDefinition program, Locale preferredLocale, boolean isWithinProgramCard)  {
    if (program.summaryImageFileKey().isEmpty()) {
      return Optional.empty();
    }
    String summaryImageFileKey = program.summaryImageFileKey().get();

    if (!PublicFileNameFormatter.isFileKeyForPublicProgramImage(summaryImageFileKey)) {
      return Optional.empty();
    }


    String styleClasses = StyleUtils.joinStyles("w-full, ");
    if (isWithinProgramCard) {
      // Round all corners when showing the image in context of a program card with filtering.
      styleClasses = StyleUtils.joinStyles(styleClasses, "rounded-lg");
      //String imagePath = "./image10.png";
    }


    //String imageSource;
    String url = publicStorageClient.getPublicDisplayUrl(program.summaryImageFileKey().get());
    try (InputStream is = new URL(url).openStream()) {
      // PDFBox 3.x requires wrapping InputStreams into a RandomAccessReadBuffer
      try (PDDocument document = Loader.loadPDF(RandomAccessReadBuffer.createBufferFromStream(is))) {
        System.out.println("Successfully loaded PDF from S3. Pages: " + document.getNumberOfPages());

        PDFRenderer renderer = new PDFRenderer(document);

        // 2. Render first page (index 0) at 300 DPI for quality
        BufferedImage image = renderer.renderImageWithDPI(0, 300);

        // 3. Convert image to Byte Array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        // 4. Encode to Base64
        String base64String = Base64.getEncoder().encodeToString(imageBytes);

        // 5. Generate the HTML Tag
        return Optional.of(img().withSrc("data:image/png;base64," + base64String).withClasses(styleClasses));
        // Proceed to generate HTML image tag as discussed earlier
      }
    } catch (Exception e) {
      System.err.println("Failed to read from S3: " + e.getMessage());
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
