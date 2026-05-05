package parsers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.ByteString;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;

public class FileTypeValidationTest extends ResetPostgres {
  private static final ImmutableList<String> ALLOWED =
      FileTypeValidation.parseSpecifiers("image/*,.pdf");

  private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
  private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34};
  private static final byte[] UNKNOWN = {0x00, 0x01, 0x02, 0x03};
  private FileTypeValidation validator;

  @Before
  public void setup() {
    validator = instanceOf(FileTypeValidation.class);
  }

  @Test
  public void parseSpecifiers_translatesExtensionsAndPreservesWildcards() {
    assertThat(ALLOWED).containsExactly("image/*", "application/pdf");
  }

  @Test
  public void parseSpecifiers_dropsUnknownExtensions() {
    assertThat(FileTypeValidation.parseSpecifiers(".png,.xyz,.pdf"))
        .containsExactly("image/png", "application/pdf");
  }

  @Test
  public void validateHeaderBytes_realPng_passes() {
    validator.validateHeaderBytes(ByteString.fromArray(PNG_MAGIC), "photo.png", ALLOWED);
  }

  @Test
  public void validateHeaderBytes_pdfBytesNamedPng_throws() {
    assertThatThrownBy(
            () ->
                validator.validateHeaderBytes(ByteString.fromArray(PDF_MAGIC), "fake.png", ALLOWED))
        .isInstanceOf(FileUploadTypeException.class)
        .hasMessageContaining("do not match extension");
  }

  @Test
  public void validateHeaderBytes_pngBytesNamedPdf_throws() {
    assertThatThrownBy(
            () ->
                validator.validateHeaderBytes(ByteString.fromArray(PNG_MAGIC), "fake.pdf", ALLOWED))
        .isInstanceOf(FileUploadTypeException.class)
        .hasMessageContaining("do not match extension");
  }

  @Test
  public void validateHeaderBytes_unknownExtension_throws() {
    assertThatThrownBy(
            () ->
                validator.validateHeaderBytes(
                    ByteString.fromArray(UNKNOWN), "malware.exe", ALLOWED))
        .isInstanceOf(FileUploadTypeException.class)
        .hasMessageContaining("not a supported upload type");
  }

  @Test
  public void validateHeaderBytes_noExtension_throws() {
    assertThatThrownBy(
            () -> validator.validateHeaderBytes(ByteString.fromArray(PNG_MAGIC), "photo", ALLOWED))
        .isInstanceOf(FileUploadTypeException.class)
        .hasMessageContaining("not a supported upload type");
  }

  @Test
  public void validateHeaderBytes_extensionNotInAllowList_throws() {
    ImmutableList<String> imagesOnly = FileTypeValidation.parseSpecifiers("image/*");
    assertThatThrownBy(
            () ->
                validator.validateHeaderBytes(
                    ByteString.fromArray(PDF_MAGIC), "doc.pdf", imagesOnly))
        .isInstanceOf(FileUploadTypeException.class)
        .hasMessageContaining("not an allowed upload type");
  }

  @Test
  public void sniffingFlow_fileSmallerThanHeaderSize_throws() {
    ByteString tinyBytes = ByteString.fromArray(new byte[] {0x00, 0x01, 0x02, 0x03});
    AtomicReference<String> detectedRef = new AtomicReference<>(null);

    assertThatThrownBy(
            () ->
                Source.single(tinyBytes)
                    .via(validator.sniffingFlow("photo.png", detectedRef, ALLOWED))
                    .runWith(org.apache.pekko.stream.javadsl.Sink.ignore(), mat)
                    .toCompletableFuture()
                    .join())
        .hasCauseInstanceOf(FileUploadTypeException.class)
        .hasMessageContaining("too small");
    assertThat(detectedRef.get()).isNull();
  }

  @Test
  public void validateHeaderBytes_unknownBytesForKnownExtension_throws() {
    assertThatThrownBy(
            () ->
                validator.validateHeaderBytes(ByteString.fromArray(UNKNOWN), "photo.png", ALLOWED))
        .isInstanceOf(FileUploadTypeException.class)
        .hasMessageContaining("do not match extension");
  }
}
