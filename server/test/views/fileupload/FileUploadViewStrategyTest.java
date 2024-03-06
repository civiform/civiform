package views.fileupload;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.stubMessagesApi;
import static views.fileupload.FileUploadViewStrategy.FILE_INPUT_HINT_ID_PREFIX;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import org.junit.Test;
import play.i18n.Lang;
import play.i18n.Messages;
import views.style.ReferenceClasses;

public class FileUploadViewStrategyTest {
  private static final int FILE_LIMIT_MB = 4;

  private final Messages messages =
      stubMessagesApi().preferred(ImmutableSet.of(Lang.defaultLang()));

  @Test
  public void createUswdsFileInputFormElement_hasAriaDescribedByLabels() {
    DivTag uswdsForm =
        FileUploadViewStrategy.createUswdsFileInputFormElement(
            /* id= */ "fakeId",
            /* acceptedMimeTypes= */ "image/*",
            /* hints= */ ImmutableList.of("hint0", "hint1", "hint2"),
            /* disabled= */ false,
            FILE_LIMIT_MB,
            messages);

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
            FILE_LIMIT_MB,
            messages);

    assertThat(uswdsForm.render())
        .contains(String.format("id=\"%s\"", ReferenceClasses.FILEUPLOAD_TOO_LARGE_ERROR_ID));
  }

  @Test
  public void createUswdsFileInputFormElement_inputHasFileLimitAsAttr() {
    DivTag uswdsForm =
        FileUploadViewStrategy.createUswdsFileInputFormElement(
            /* id= */ "fakeId",
            /* acceptedMimeTypes= */ "image/*",
            /* hints= */ ImmutableList.of("hint0", "hint1", "hint2"),
            /* disabled= */ false,
            /* fileLimitMb= */ 5,
            messages);

    assertThat(uswdsForm.render())
        .containsPattern("<input type=\"file\".*data-file-limit-mb=\"5\"");
  }
}
