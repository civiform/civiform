package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import controllers.FlashKey;
import forms.questions.QuestionImageDescriptionForm;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import parsers.admin.QuestionImageStreamingMultipartBodyParser;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
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
  private final Messages messages;

  @Inject
  public AdminQuestionImageController(
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      SettingsManifest settingsManifest,
      QuestionService questionService,
      MessagesApi messagesApi) {
    super(profileUtils, versionRepository);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.questionService = checkNotNull(questionService);
    this.messages = messagesApi.preferred(ImmutableList.of(Lang.defaultLang()));
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

    // 1. Extract alt text description
    String[] descriptionValues =
        body.asFormUrlEncoded().get(QuestionImageDescriptionForm.QUESTION_IMAGE_DESCRIPTION);
    String newDescription =
        (descriptionValues != null && descriptionValues.length > 0) ? descriptionValues[0] : "";

    final String editUrl =
        controllers.admin.routes.AdminQuestionController.edit(questionId, "").url();

    // 2. Extract uploaded file part
    Http.MultipartFormData.FilePart<String> filePart = body.getFile("questionImage");

    // 3. Alt text is required if a file is uploaded
    if (filePart != null && newDescription.isBlank()) {
      return redirect(editUrl)
          .flashing(FlashKey.ERROR, messages.at("validation.adminQuestionImage.altTextRequired"));
    }

    // 4. Save image file key if a new file was uploaded
    if (filePart != null) {
      String fileKey = filePart.getRef();
      if (!PublicFileNameFormatter.isFileKeyForPublicQuestionImage(fileKey)) {
        throw new IllegalArgumentException("Key incorrectly formatted for question image file");
      }
      try {
        questionService.setImageFileKey(questionId, fileKey);
      } catch (QuestionNotFoundException | UnsupportedQuestionTypeException e) {
        return notFound();
      }
    }

    // 5. Update description independently (even if no new file was uploaded)
    try {

      questionService.setImageFileDescription(
          questionId, LocalizedStrings.DEFAULT_LOCALE, newDescription);
    } catch (QuestionNotFoundException | UnsupportedQuestionTypeException e) {
      return notFound();
    } catch (ImageDescriptionNotRemovableException e) {
      return redirect(editUrl)
          .flashing(
              FlashKey.ERROR, messages.at("toast.adminQuestionImage.descriptionNotRemovable"));
    }

    // 6. Redirect back with flash success message
    String successMessage =
        filePart != null
            ? messages.at("toast.adminProgramImage.imageAndDescriptionSaved", newDescription)
            : messages.at("toast.adminProgramImage.descriptionSet", newDescription);
    return redirect(editUrl).flashing(FlashKey.SUCCESS, successMessage);
  }
}
