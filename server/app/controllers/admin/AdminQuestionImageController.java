package controllers.admin;

import static autovalue.shaded.com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import controllers.CiviFormController;
import forms.questions.QuestionImageDescriptionForm;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import parsers.admin.QuestionImageStreamingMultipartBodyParser;
import play.mvc.BodyParser;
import play.mvc.Http;
import play.mvc.Result;
import services.cloud.PublicFileNameFormatter;
import services.cloud.PublicStorageClient;
import services.settings.SettingsManifest;

public class AdminQuestionImageController extends CiviFormController {
  private final SettingsManifest settingsManifest;
  private final PublicStorageClient publicStorageClient;

  @Inject
  public AdminQuestionImageController(
      SettingsManifest settingsManifest, PublicStorageClient publicStorageClient) {
    this.settingsManifest = checkNotNull(settingsManifest);
    this.publicStorageClient = checkNotNull(publicStorageClient);
  }

  /** Uploads a question image and saves its alt text. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  @BodyParser.Of(QuestionImageStreamingMultipartBodyParser.class)
  public Result uploadQuestionImageImage(Http.Request request, long questionId) {
    if (!settingsManifest.getImagesInQuestionFeatureEnabled(request)) {
      return notFound();
    }

    Http.MultipartFormData<String> body = request.body().asMultipartFormData();
    if (body == null) {
      return badRequest();
    }

    // 1. Description grabbed via hx-include
    String[] descriptionValues =
        body.asFormUrlEncoded().get(QuestionImageDescriptionForm.QUESTION_IMAGE_DESCRIPTION);
    String newDescription =
        (descriptionValues != null && descriptionValues.length > 0) ? descriptionValues[0] : "";

    // 2. Uploaded file key
    Http.MultipartFormData.FilePart<String> filePart = body.getFile("questionImage");
    if (filePart != null) {
      String fileKey = filePart.getRef();
      if (!PublicFileNameFormatter.isFileKeyForPublicQuestionImage(fileKey)) {
        throw new IllegalArgumentException("Key incorrectly formatted for question image file");
      }
    }

    // 3. Return 200 OK to HTMX
    return ok();
  }
}
