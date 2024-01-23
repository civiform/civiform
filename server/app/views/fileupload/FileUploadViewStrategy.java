package views.fileupload;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;
import static j2html.TagCreator.li;
import static j2html.TagCreator.ul;

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

  /** Creates a <input type="file"> element that uses the USWDS file input UI component. */
  public DivTag createUswdsFileInputFormElement(
      String acceptedMimeTypes, ImmutableList<String> hintTexts, boolean disabled) {
    return div()
        .withClasses("usa-form-group", "mb-2")
        .with(
            ul().withClasses("list-disc", "list-inside")
                .with(
                    each(
                        hintTexts,
                        hintText -> li(hintText).withId("file-input-hint").withClass("usa-hint"))))
        .with(
            input()
                .withType("file")
                .withName("file")
                .withClasses("usa-file-input", "w-full", "h-56")
                .attr("aria-describedby", "file-input-hint")
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
