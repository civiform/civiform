package views.fileupload;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;
import static j2html.TagCreator.p;
import static views.style.BaseStyles.FORM_ERROR_TEXT_BASE;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import j2html.TagCreator;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FooterTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.ScriptTag;
import java.util.Optional;
import play.mvc.Http;
import services.cloud.StorageUploadRequest;
import views.applicant.ApplicantFileUploadRenderer;

/**
 * Class to render a <form> that supports file upload. Must be subclassed by each cloud storage
 * provider that CiviForm supports.
 *
 * <p>This class supports rendering file upload forms for both applicants *and* admins. See {@link
 * ApplicantFileUploadRenderer} for additional rendering for *applicant* file upload.
 */
public abstract class FileUploadViewStrategy {
  @VisibleForTesting static final String FILE_INPUT_HINT_ID_PREFIX = "file-input-hint-";

  /** Returns a top-level <form> element to use for file upload. */
  public FormTag renderFileUploadFormElement(StorageUploadRequest request) {
    return form()
        .withEnctype("multipart/form-data")
        .withMethod(Http.HttpVerbs.POST)
        .withClasses(getUploadFormClass());
  }

  /**
   * Creates a list of all the **additional** input tags required for the file upload form to
   * correctly connect to and authenticate with the cloud storage provider.
   *
   * <p>Important: This specifically does *not* include the required <input type="file"> element.
   */
  public abstract ImmutableList<InputTag> additionalFileUploadFormInputs(
      Optional<StorageUploadRequest> request);

  /** Creates a list of footer tags needed on a page rendering a file upload form. */
  public ImmutableList<FooterTag> footerTags() {
    return extraScriptTags().stream().map(TagCreator::footer).collect(toImmutableList());
  }

  /**
   * Creates a <input type="file"> element that uses the USWDS file input UI component.
   *
   * @param id the ID to apply to the outermost div.
   * @param hints a list of hints that should be displayed above the file input UI.
   * @param disabled true if the file input should be shown as disabled.
   * @param fileLimitMb the maximum file size in megabytes allowed for this file input element. Used
   *     to show an error client-side if the user uploads a file that's too big.
   */
  public static DivTag createUswdsFileInputFormElement(
      String id,
      String acceptedMimeTypes,
      ImmutableList<String> hints,
      boolean disabled,
      int fileLimitMb) {
    StringBuilder ariaDescribedByIds = new StringBuilder();
    for (int i = 0; i < hints.size(); i++) {
      ariaDescribedByIds.append(FILE_INPUT_HINT_ID_PREFIX);
      ariaDescribedByIds.append(i);
      ariaDescribedByIds.append(" ");
    }
    return div()
        .withId(id)
        .withClasses("usa-form-group", "mb-2")
        .with(
            each(
                hints,
                (index, hint) ->
                    p(hint)
                        .withId(FILE_INPUT_HINT_ID_PREFIX + index)
                        .withClasses("usa-hint", "mb-2")))
        .with(
            p("Error: The file you uploaded is too large. Please choose a smaller file.")
                .withId("file-too-large")
                // TypeScript will be responsible for showing/hiding the file-too-large error.
                .withClasses(FORM_ERROR_TEXT_BASE, "hidden"))
        .with(
            input()
                .withType("file")
                .withName("file")
                .withClasses("usa-file-input")
                .attr("aria-describedby", ariaDescribedByIds.toString())
                .attr("data-file-limit-mb", fileLimitMb)
                .withAccept(acceptedMimeTypes)
                .withCondDisabled(disabled));
  }

  /**
   * Returns strategy-specific class to add to the <form> element. It helps to distinguish
   * client-side different strategies (AWS or Azure).
   */
  protected abstract String getUploadFormClass();

  protected abstract ImmutableList<ScriptTag> extraScriptTags();
}
