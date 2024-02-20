package views.fileupload;

import static org.assertj.core.api.Assertions.assertThat;
import static views.fileupload.FileUploadViewStrategy.FILE_INPUT_HINT_ID_PREFIX;

import com.google.common.collect.ImmutableList;
import j2html.tags.specialized.DivTag;
import org.junit.Test;

public class FileUploadViewStrategyTest {

  @Test
  public void createUswdsFileInputFormElement_hasAriaDescribedByLabels() {
    DivTag uswdsForm =
        FileUploadViewStrategy.createUswdsFileInputFormElement(
            /* id= */ "fakeId",
            /* acceptedMimeTypes= */ "image/*",
            /* hints= */ ImmutableList.of("hint0", "hint1", "hint2"),
            /* disabled= */ false,
            /* fileLimitMb= */ 5);

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
  public void createUswdsFileInputFormElement_hasFileTooLargeErrorDiv() {
    DivTag uswdsForm =
        FileUploadViewStrategy.createUswdsFileInputFormElement(
            /* id= */ "fakeId",
            /* acceptedMimeTypes= */ "image/*",
            /* hints= */ ImmutableList.of("hint0", "hint1", "hint2"),
            /* disabled= */ false,
            /* fileLimitMb= */ 5);

    assertThat(uswdsForm.render()).contains("<p id=\"file-too-large\"");
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
