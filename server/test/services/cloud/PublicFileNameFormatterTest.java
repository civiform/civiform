package services.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class PublicFileNameFormatterTest {
  @Test
  public void formatPublicProgramImageFilename_isCorrectlyFormatted() {
    String filename = PublicFileNameFormatter.formatPublicProgramImageFilename(156);

    assertThat(filename).isEqualTo("program-summary-image/program-156/${filename}");
  }

  @Test
  public void isFileKeyForPublicProgramImage_isCorrect_true() {
    String filename = PublicFileNameFormatter.formatPublicProgramImageFilename(156);

    assertThat(PublicFileNameFormatter.isFileKeyForPublicProgramImage(filename)).isTrue();
  }

  @Test
  public void isFileKeyForPublicProgramImage_isApplicantFile_false() {
    assertThat(
            PublicFileNameFormatter.isFileKeyForPublicProgramImage(
                "applicant-10/program-2/block-3/myFile.png"))
        .isFalse();
  }

  @Test
  public void isFileKeyForPublicProgramImage_isEmpty_false() {
    assertThat(PublicFileNameFormatter.isFileKeyForPublicProgramImage("")).isFalse();
  }
}
