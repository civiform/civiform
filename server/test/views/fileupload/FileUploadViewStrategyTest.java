package views.fileupload;

import static org.assertj.core.api.Assertions.assertThat;
import static views.fileupload.FileUploadViewStrategy.FILE_INPUT_HINT_ID_PREFIX;

import com.google.common.collect.ImmutableList;
import j2html.tags.specialized.DivTag;
import org.junit.Test;

public class FileUploadViewStrategyTest {
  private static final int FILE_LIMIT_MB = 4;

  @Test
  public void createUswdsFileInputFormElement_hasAriaDescribedByLabels() {
    DivTag uswdsForm =
        FileUploadViewStrategy.createUswdsFileInputFormElement(
            /* id= */ "fakeId",
            /* acceptedMimeTypes= */ "image/*",
            /* hints= */ ImmutableList.of("hint0", "hint1", "hint2"),
            /* disabled= */ false,
            FILE_LIMIT_MB);

    String expectedAriaDescribedBy =
        FILE_INPUT_HINT_ID_PREFIX
            + "0 "
            + FILE_INPUT_HINT_ID_PREFIX
            + "1 "
            + FILE_INPUT_HINT_ID_PREFIX
            + "2";
    assertThat(uswdsForm.render())
        .containsPattern("<input type=\"file\".*aria-describedby=\"" + expectedAriaDescribedBy);
  }

  @Test
  public void createUswdsFileInputFormElement_hasFileTooLargeError() {
    DivTag uswdsForm =
        FileUploadViewStrategy.createUswdsFileInputFormElement(
            /* id= */ "fakeId",
            /* acceptedMimeTypes= */ "image/*",
            /* hints= */ ImmutableList.of("hint0", "hint1", "hint2"),
            /* disabled= */ false,
            FILE_LIMIT_MB);

    assertThat(uswdsForm.render()).contains("<p id=\"file-too-large-error\"");
  }

  @Test
  public void createUswdsFileInputFormElement_inputHasFileLimitAsAttr() {
    DivTag uswdsForm =
        FileUploadViewStrategy.createUswdsFileInputFormElement(
            /* id= */ "fakeId",
            /* acceptedMimeTypes= */ "image/*",
            /* hints= */ ImmutableList.of("hint0", "hint1", "hint2"),
            /* disabled= */ false,
            /* fileLimitMb= */ 5);

    assertThat(uswdsForm.render())
        .containsPattern("<input type=\"file\".*data-file-limit-mb=\"5\"");
  }
}
