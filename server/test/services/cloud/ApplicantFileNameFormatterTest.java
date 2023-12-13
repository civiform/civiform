package services.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ApplicantFileNameFormatterTest {

  @Test
  public void formatFileUploadQuestionFilename() {
    assertThat(ApplicantFileNameFormatter.formatFileUploadQuestionFilename(1L, 2L, "3-4"))
        .isEqualTo("applicant-1/program-2/block-3-4/${filename}");
  }
}
