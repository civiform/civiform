package services.cloud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

public class ApplicantFileNameFormatterTest {
  private static final String FILE_NAME =
      ApplicantFileNameFormatter.formatFileUploadQuestionFilename(
          /* applicantId= */ 1L, /* programId= */ 2L, /* blockId= */ "3-4");

  @Test
  public void formatFileUploadQuestionFilename() {
    assertThat(FILE_NAME).isEqualTo("applicant-1/program-2/block-3-4/${filename}");
  }

  @Test
  public void isApplicantOwnedFileKey_applicantIdMatches_isTrue() {
    assertThat(ApplicantFileNameFormatter.isApplicantOwnedFileKey(FILE_NAME, /* applicantId= */ 1L))
        .isTrue();
  }

  @Test
  public void isApplicantOwnedFileKey_applicantIdDoesNotMatch_isFalse() {
    assertThat(ApplicantFileNameFormatter.isApplicantOwnedFileKey(FILE_NAME, /* applicantId= */ 2L))
        .isFalse();
  }

  @Test
  public void isApplicantOwnedFileKey_applicantIdInFileNameOnly_isFalse() {
    // Must match at the start.
    assertThat(
            ApplicantFileNameFormatter.isApplicantOwnedFileKey(
                "applicant-9/program-2/block-3-4/applicant-1", /* applicantId= */ 1L))
        .isFalse();
  }

  @Test
  public void isApplicantOwnedFileKey_applicantIdPartialMatch_isFalse() {
    // applicant-1 is a substring of applicant-100 and shouldn't match.
    assertThat(
            ApplicantFileNameFormatter.isApplicantOwnedFileKey(
                "applicant-100/program-2/block-3-4/", /* applicantId= */ 1L))
        .isFalse();
  }

  @Test
  public void isApplicantOwnedFileKey_badFileKey() {
    assertThatThrownBy(
            () ->
                ApplicantFileNameFormatter.isApplicantOwnedFileKey(
                    /* fileKey= */ "", /* applicantId= */ 1L))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void isApplicantOwnedFileKey_badApplicantId() {
    assertThatThrownBy(
            () ->
                ApplicantFileNameFormatter.isApplicantOwnedFileKey(
                    "applicant-1/program-2/block-3-4/${filename}", /* applicantId= */ 0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
