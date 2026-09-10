package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.input;

import auth.Authorizers;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import forms.questions.QuestionImageDescriptionForm;
import java.util.Optional;
import java.util.UUID;
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
import services.question.types.QuestionDefinition;
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
  public Result hxUploadQuestionImage(Http.Request request, long questionId) {
    if (!settingsManifest.getImagesInQuestionFeatureEnabled(request)) {
      return notFound();
    }

    Http.MultipartFormData<String> body = request.body().asMultipartFormData();
    if (body == null) {
      return renderError("Bad request: no upload body received.");
    }

    // 1. Extract alt text description
    String[] descriptionValues =
        body.asFormUrlEncoded().get(QuestionImageDescriptionForm.QUESTION_IMAGE_DESCRIPTION);
    String newDescription =
        (descriptionValues != null && descriptionValues.length > 0) ? descriptionValues[0] : "";

    // 2. Extract uploaded file part
    Http.MultipartFormData.FilePart<String> filePart = body.getFile("questionImage");

    // 3. Alt text is required if a file is uploaded
    Optional<String> maybeFileKey = Optional.empty();
    if (filePart != null && newDescription.isBlank()) {
      return renderError(messages.at("validation.adminQuestionImage.altTextRequired"));
    }

    // 4. Save image file key if a new file was uploaded
    if (filePart != null) {
      String fileKey = filePart.getRef();
      if (!PublicFileNameFormatter.isFileKeyForPublicQuestionImage(fileKey)) {
        return renderError("Key incorrectly formatted for question image file.");
      }
      maybeFileKey = Optional.of(fileKey);
    }
    QuestionDefinition updatedQuestion;
    try {
      // 5. Update the description and file key (used empty filekey if no file was uploaded)
      updatedQuestion =
          questionService.setImageFileKeyAndDescription(
              questionId, maybeFileKey, LocalizedStrings.DEFAULT_LOCALE, newDescription);
    } catch (QuestionNotFoundException e) {
      return notFound();
    } catch (UnsupportedQuestionTypeException e) {
      return renderError("Unsupported question type");
    } catch (ImageDescriptionNotRemovableException e) {
      return renderError(messages.at("toast.adminQuestionImage.descriptionNotRemovable"));
    }

    String newConcurrencyToken =
        updatedQuestion.getConcurrencyToken().map(UUID::toString).orElse("");

    // 6. On success: return 200 OK and clear out errors via OOB swap
    return ok(div(
                div()
                    .withClasses("text-red-600", "text-xs", "p-1", "hidden")
                    .withId("question-image-file-input-errors")
                    .attr("hx-swap-oob", "true"),
                input()
                    .withType("hidden")
                    .withId("concurrencyToken")
                    .withName("concurrencyToken")
                    .withValue(newConcurrencyToken)
                    .attr("hx-swap-oob", "true"))
            .render())
        .as(Http.MimeTypes.HTML);
  }

  private Result renderError(String errorMessage) {
    return badRequest(
            div(errorMessage)
                .withClasses("text-red-600", "text-xs", "p-1")
                .withId("question-image-file-input-errors")
                .toString())
        .as(Http.MimeTypes.HTML);
  }
}
