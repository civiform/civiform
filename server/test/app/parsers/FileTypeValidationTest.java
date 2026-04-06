package parsers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.pekko.util.ByteString;
import org.junit.Test;

public class FileTypeValidationTest {
  private static final com.google.common.collect.ImmutableList<String> ALLOWED_SPECIFIERS =
      FileTypeValidation.parseSpecifiers("image/*,.pdf");

  @Test
  public void detectType_unknown_returnsNull() {
    byte[] unknownHeader = {
      0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
      0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F
    };
    assertThat(FileTypeValidation.detectTypes(ByteString.fromArray(unknownHeader))).isEmpty();
  }

  @Test
  public void validateHeaderBytes_pdfDeclaredAsImage_throws() {
    byte[] pdfHeader = {
      0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
    assertThatThrownBy(
            () ->
                new FileTypeValidation()
                    .validateHeaderBytes(
                        ByteString.fromArray(pdfHeader),
                        "image/png",
                        "fake.png",
                        ALLOWED_SPECIFIERS))
        .isInstanceOf(FileUploadTypeException.class)
        .hasMessageContaining("does not match detected type");
  }

  @Test
  public void validateHeaderBytes_imageDeclaredAsPdf_throws() {
    byte[] pngHeader = {
      (byte) 0x89,
      0x50,
      0x4E,
      0x47,
      0x0D,
      0x0A,
      0x1A,
      0x0A,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00
    };
    assertThatThrownBy(
            () ->
                new FileTypeValidation()
                    .validateHeaderBytes(
                        ByteString.fromArray(pngHeader),
                        "application/pdf",
                        "fake.pdf",
                        ALLOWED_SPECIFIERS))
        .isInstanceOf(FileUploadTypeException.class)
        .hasMessageContaining("does not match detected type");
  }

  @Test
  public void validateHeaderBytes_disallowedDeclaredType_unknownBytes_throws() {
    byte[] unknownHeader = {
      0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
      0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F
    };
    assertThatThrownBy(
            () ->
                new FileTypeValidation()
                    .validateHeaderBytes(
                        ByteString.fromArray(unknownHeader),
                        "application/x-executable",
                        "malware.exe",
                        ALLOWED_SPECIFIERS))
        .isInstanceOf(FileUploadTypeException.class)
        .hasMessageContaining("not an allowed upload type");
  }

  @Test
  public void validateHeaderBytes_allowedDeclaredType_unknownBytes_throws() {
    byte[] unknownHeader = {
      0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
      0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F
    };
    assertThatThrownBy(
            () ->
                new FileTypeValidation()
                    .validateHeaderBytes(
                        ByteString.fromArray(unknownHeader),
                        "image/svg+xml",
                        "drawing.svg",
                        ALLOWED_SPECIFIERS))
        .isInstanceOf(FileUploadTypeException.class)
        .hasMessageContaining("could not verify file type");
  }
}
