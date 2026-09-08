package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import controllers.CiviFormController;
import forms.questions.QuestionImageDescriptionForm;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import parsers.admin.QuestionImageStreamingMultipartBodyParser;
import play.mvc.BodyParser;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.LocalizedStrings;
import services.cloud.PublicFileNameFormatter;
import services.question.QuestionService;
import services.question.exceptions.QuestionNotFoundException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.settings.SettingsManifest;

public class AdminQuestionImageController extends CiviFormController {
  private final SettingsManifest settingsManifest;
  private final QuestionService questionService;

  @Inject
  public AdminQuestionImageController(
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      SettingsManifest settingsManifest, QuestionService questionService) {
    super(profileUtils, versionRepository);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.questionService=checkNotNull(questionService);
  }

  /** Uploads a question image and saves its alt text. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  @BodyParser.Of(QuestionImageStreamingMultipartBodyParser.class)
  public Result uploadQuestionImage(Http.Request request, long questionId) {
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
      try{
        questionService.setImageFileKey(questionId, fileKey);
      }
      catch (QuestionNotFoundException | UnsupportedQuestionTypeException e)
      {
        return notFound();
      }
      try{
        questionService.setImageFileDescription(questionId, LocalizedStrings.DEFAULT_LOCALE, newDescription);
      }
      catch (QuestionNotFoundException | UnsupportedQuestionTypeException e){
        return notFound();
      }
    }


    // 3. Return 200 OK to HTMX
    return ok();
  }
}
