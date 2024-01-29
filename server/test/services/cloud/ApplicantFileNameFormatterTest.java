package services.cloud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

public class ApplicantFileNameFormatterTest {

  @Test
  public void formatFileUploadQuestionFilename() {
    assertThat(ApplicantFileNameFormatter.formatFileUploadQuestionFilename(1L, 2L, "3-4"))
        .isEqualTo("applicant-1/program-2/block-3-4/${filename}");
  }

  @Test
  public void isApplicantOwnedFileKey_applicantIdMatches_isTrue() {
    assertThat(
            ApplicantFileNameFormatter.isApplicantOwnedFileKey(
                "applicant-1/program-2/block-3-4/${filename}", 1L))
        .isEqualTo(true);
  }

  @Test
  public void isApplicantOwnedFileKey_applicantIdDoesNotMatch_isFalse() {
    assertThat(
            ApplicantFileNameFormatter.isApplicantOwnedFileKey(
                "applicant-1/program-2/block-3-4/${filename}", 2L))
        .isEqualTo(false);
  }

  @Test
  public void isApplicantOwnedFileKey_badFileKey() {
    assertThatThrownBy(() -> ApplicantFileNameFormatter.isApplicantOwnedFileKey("", 1L))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void isApplicantOwnedFileKey_badApplicantId() {
    assertThatThrownBy(
            () ->
                ApplicantFileNameFormatter.isApplicantOwnedFileKey(
                    "applicant-1/program-2/block-3-4/${filename}", 0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
