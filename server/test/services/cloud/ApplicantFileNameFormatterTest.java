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
                "applicant-9/program-2/block-3-4/applicant-1/", /* applicantId= */ 1L))
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

  @Test
  public void formatFilenameApplicantLookupPrefixString() {
    String prefix =
        ApplicantFileNameFormatter.formatFilenameApplicantLookupPrefixString(/* applicantId= */ 1L);
    assertThat(FILE_NAME).startsWith(prefix);
  }

  @Test
  public void formatFileUploadQuestionFilenameWithUuid() {
    String result =
        ApplicantFileNameFormatter.formatFileUploadQuestionFilenameWithUuid(
            /* applicantId= */ 1L, /* programId= */ 2L, /* blockId= */ "3-4", "report.pdf");
    assertThat(result).startsWith("applicant-1/program-2/block-3-4/");
    assertThat(result).endsWith(".pdf");
    assertThat(ApplicantFileNameFormatter.isApplicantOwnedFileKey(result, /* applicantId= */ 1L))
        .isTrue();
    // Each call generates a unique key.
    String result2 =
        ApplicantFileNameFormatter.formatFileUploadQuestionFilenameWithUuid(
            /* applicantId= */ 1L, /* programId= */ 2L, /* blockId= */ "3-4", "report.pdf");
    assertThat(result).isNotEqualTo(result2);
  }

  @Test
  public void buildResponseContentDisposition_macScreenshotName() {
    // macOS uses narrow no-break space (U+202F) around "at" in screenshot names.
    String macName = "Screenshot 2026-05-26\u202Fat\u202F10.30.45 AM.png";
    String disposition = ApplicantFileNameFormatter.buildResponseContentDisposition(macName);

    assertThat(disposition).startsWith("inline; filename*=UTF-8''");
    assertThat(disposition).contains("%E2%80%AF");
    assertThat(disposition).doesNotContain("\u202F");
    assertThat(disposition).doesNotContain("filename=\"");
  }

  @Test
  public void buildResponseContentDisposition_unicodeFileName() {
    String disposition = ApplicantFileNameFormatter.buildResponseContentDisposition("고양이.jpg");

    assertThat(disposition).isEqualTo("inline; filename*=UTF-8''%EA%B3%A0%EC%96%91%EC%9D%B4.jpg");
  }

  @Test
  public void buildResponseContentDisposition_encodesQuotesInFilename() {
    String disposition = ApplicantFileNameFormatter.buildResponseContentDisposition("my\"file.pdf");

    assertThat(disposition).isEqualTo("inline; filename*=UTF-8''my%22file.pdf");
  }
}
