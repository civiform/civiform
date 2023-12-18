package views.fileupload;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;
import static j2html.TagCreator.span;

import com.google.common.collect.ImmutableList;
import j2html.TagCreator;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FooterTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.ScriptTag;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
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
   * <p>Important: This specifically does *not* include the required <input type="file"> element;
   * use {@link #applicationFileInputElement} or {@link #adminProgramImageFileInputElement} to get
   * the <input type="file"> element.
   */
  public abstract ImmutableList<InputTag> additionalFileUploadFormInputs(
      Optional<StorageUploadRequest> request);

  /**
   * Creates the <input type="file"> element needed for applicants filling out applications.
   *
   * <p>Note: This likely could be migrated to use the USWDS component in {@link
   * #adminProgramImageFileInputElement} instead. That hasn't been done yet to keep the program
   * image work scoped to just the new image feature.
   *
   * @param fileInputId an ID associates the file <input> field. Can be used to associate custom
   *     screen reader functionality with the file input.
   */
  public InputTag applicationFileInputElement(
      String acceptedMimeTypes,
      String fileInputId,
      ImmutableList<String> ariaDescribedByIds,
      boolean hasErrors) {
    return input()
        .withId(fileInputId)
        .condAttr(hasErrors, "aria-invalid", "true")
        .condAttr(
            !ariaDescribedByIds.isEmpty(),
            "aria-describedby",
            StringUtils.join(ariaDescribedByIds, " "))
        .withType("file")
        .withName("file")
        .withClass("hidden")
        .withAccept(acceptedMimeTypes);
  }

  /** Creates the <input type="file"> element needed for admins to upload program images. */
  public DivTag adminProgramImageFileInputElement(String acceptedMimeTypes) {
    return div()
        .withClasses("usa-form-group", "mb-2")
        .with(
            span("File size must be at most 500 KB.")
                .withId("file-input-size-hint")
                .withClass("usa-hint"))
        .with(
            input()
                .attr("aria-describedby", "file-input-size-hint")
                .withType("file")
                .withName("file")
                .withClasses("usa-file-input", "w-full")
                .withAccept(acceptedMimeTypes));
  }

  /** Creates a list of footer tags needed on a page rendering a file upload form. */
  public ImmutableList<FooterTag> footerTags() {
    return extraScriptTags().stream().map(TagCreator::footer).collect(toImmutableList());
  }

  /**
   * Returns strategy-specific class to add to the <form> element. It helps to distinguish
   * client-side different strategies (AWS or Azure).
   */
  protected abstract String getUploadFormClass();

  protected abstract ImmutableList<ScriptTag> extraScriptTags();
}
