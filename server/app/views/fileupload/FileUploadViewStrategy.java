package views.fileupload;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static j2html.TagCreator.form;

import com.google.common.collect.ImmutableList;
import j2html.TagCreator;
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
   * Creates a list of all the input tags required for the file upload form to correctly connect to
   * and authenticate with the cloud storage provider.
   *
   * @param fileInputId an ID associates the file <input> field. Can be used to associate custom
   *     screen reader functionality with the file input.
   */
  public abstract ImmutableList<InputTag> fileUploadFormInputs(
      Optional<StorageUploadRequest> request,
      String acceptedMimeTypes,
      String fileInputId,
      ImmutableList<String> ariaDescribedByIds,
      boolean hasErrors);

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
