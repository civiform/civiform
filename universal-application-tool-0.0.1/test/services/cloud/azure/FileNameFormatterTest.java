package services.cloud.azure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class FileNameFormatterTest {

  @Test
  public void getPrefixedOriginalFileName_devUpload() {
    String fileName = "applicant-1/program-2/block-3/24fc5062-ce90-43d3-9f82-21a2816a0304";
    String originalFileName = "drivers-license.jpg";

    String prefixedOriginalFileName =
        FileNameFormatter.getPrefixedOriginalFileName(fileName, originalFileName);

    assertThat(prefixedOriginalFileName)
        .isEqualTo("applicant-1/program-2/block-3/drivers-license.jpg");
  }
}
