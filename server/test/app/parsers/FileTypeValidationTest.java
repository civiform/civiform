package parsers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.pekko.util.ByteString;
import org.junit.Test;

public class FileTypeValidationTest {

  @Test
  public void detectType_png() {
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
    assertThat(FileTypeValidation.detectType(ByteString.fromArray(pngHeader)))
        .isEqualTo("image/png");
  }

  @Test
  public void detectType_jpeg() {
    byte[] jpegHeader = {
      (byte) 0xFF,
      (byte) 0xD8,
      (byte) 0xFF,
      (byte) 0xE0,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00
    };
    assertThat(FileTypeValidation.detectType(ByteString.fromArray(jpegHeader)))
        .isEqualTo("image/jpeg");
  }

  @Test
  public void detectType_gif87a() {
    byte[] gifHeader = {
      0x47, 0x49, 0x46, 0x38, 0x37, 0x61,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00
    };
    assertThat(FileTypeValidation.detectType(ByteString.fromArray(gifHeader)))
        .isEqualTo("image/gif");
  }

  @Test
  public void detectType_gif89a() {
    byte[] gifHeader = {
      0x47, 0x49, 0x46, 0x38, 0x39, 0x61,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00
    };
    assertThat(FileTypeValidation.detectType(ByteString.fromArray(gifHeader)))
        .isEqualTo("image/gif");
  }

  @Test
  public void detectType_pdf() {
    byte[] pdfHeader = {
      0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
    assertThat(FileTypeValidation.detectType(ByteString.fromArray(pdfHeader)))
        .isEqualTo("application/pdf");
  }

  @Test
  public void detectType_bmp() {
    byte[] bmpHeader = {
      0x42, 0x4D, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
    assertThat(FileTypeValidation.detectType(ByteString.fromArray(bmpHeader)))
        .isEqualTo("image/bmp");
  }

  @Test
  public void detectType_webp() {
    byte[] webpHeader = {
      0x52, 0x49, 0x46, 0x46, // RIFF
      0x00, 0x00, 0x00, 0x00, // file size
      0x57, 0x45, 0x42, 0x50, // WEBP
      0x00, 0x00, 0x00, 0x00
    };
    assertThat(FileTypeValidation.detectType(ByteString.fromArray(webpHeader)))
        .isEqualTo("image/webp");
  }

  @Test
  public void detectType_tiffLittleEndian() {
    byte[] tiffHeader = {
      0x49, 0x49, 0x2A, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
    assertThat(FileTypeValidation.detectType(ByteString.fromArray(tiffHeader)))
        .isEqualTo("image/tiff");
  }

  @Test
  public void detectType_tiffBigEndian() {
    byte[] tiffHeader = {
      0x4D, 0x4D, 0x00, 0x2A, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
    assertThat(FileTypeValidation.detectType(ByteString.fromArray(tiffHeader)))
        .isEqualTo("image/tiff");
  }

  @Test
  public void detectType_unknown_returnsNull() {
    byte[] unknownHeader = {
      0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
      0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F
    };
    assertThat(FileTypeValidation.detectType(ByteString.fromArray(unknownHeader))).isNull();
  }

  @Test
  public void validateHeaderBytes_matchingPng_succeeds() {
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
    // Should not throw
    FileTypeValidation.validateHeaderBytes(
        ByteString.fromArray(pngHeader), "image/png", "test.png");
  }

  @Test
  public void validateHeaderBytes_imageFamilyMatch_succeeds() {
    // PNG bytes declared as image/x-png - same family
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
    FileTypeValidation.validateHeaderBytes(
        ByteString.fromArray(pngHeader), "image/x-png", "test.png");
  }

  @Test
  public void validateHeaderBytes_pdfDeclaredAsImage_throws() {
    byte[] pdfHeader = {
      0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
    assertThatThrownBy(
            () ->
                FileTypeValidation.validateHeaderBytes(
                    ByteString.fromArray(pdfHeader), "image/png", "fake.png"))
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
                FileTypeValidation.validateHeaderBytes(
                    ByteString.fromArray(pngHeader), "application/pdf", "fake.pdf"))
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
                FileTypeValidation.validateHeaderBytes(
                    ByteString.fromArray(unknownHeader), "application/x-executable", "malware.exe"))
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
                FileTypeValidation.validateHeaderBytes(
                    ByteString.fromArray(unknownHeader), "image/svg+xml", "drawing.svg"))
        .isInstanceOf(FileUploadTypeException.class)
        .hasMessageContaining("could not verify file type");
  }

  @Test
  public void validateHeaderBytes_matchingPdf_succeeds() {
    byte[] pdfHeader = {
      0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
    FileTypeValidation.validateHeaderBytes(
        ByteString.fromArray(pdfHeader), "application/pdf", "doc.pdf");
  }
}
