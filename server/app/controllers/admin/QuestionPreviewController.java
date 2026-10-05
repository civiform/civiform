package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import javax.inject.Inject;
import models.QuestionModel;
import org.pac4j.core.authorization.authorizer.DefaultAuthorizers;
import org.pac4j.play.java.Secure;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.QuestionRepository;
import repository.VersionRepository;
import services.LocalizedStrings;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantPersonalInfo.Representation;
import services.cloud.PublicFileNameFormatter;
import services.cloud.PublicStorageClient;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import views.admin.questions.QuestionPreview;

import java.util.Optional;

/** Controller for rendering inputs for questions. */
public final class QuestionPreviewController extends CiviFormController {
  private final QuestionPreview questionPreview;
  private final Messages messages;
  private final PublicStorageClient publicStorageClient;
  private final QuestionRepository questionRepository;

  @Inject
  public QuestionPreviewController(
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      QuestionPreview questionPreview,
      MessagesApi messagesApi,
      PublicStorageClient publicStorageClient,
      QuestionRepository questionRepository) {
    super(profileUtils, versionRepository);
    this.questionPreview = checkNotNull(questionPreview);
    this.messages = messagesApi.preferred(ImmutableList.of(Lang.defaultLang()));
    this.publicStorageClient = checkNotNull(publicStorageClient);
    this.questionRepository = checkNotNull(questionRepository);

  }

  @Secure(authorizers = DefaultAuthorizers.IS_AUTHENTICATED)
  public Result sampleQuestion(Request request, String questionType, Optional<Long> questionId) {
    Representation representation = Representation.builder().build();
    ApplicantPersonalInfo api = ApplicantPersonalInfo.ofGuestUser(representation);
    CiviFormProfile profile = profileUtils.currentUserProfile(request);

    QuestionType questionTypeEnum;
    try {
      questionTypeEnum = QuestionType.fromLabel(questionType);
    } catch (InvalidQuestionTypeException e) {
      return badRequest("Invalid question type: " + questionType);
    }

    Optional<String> imageUrl = Optional.empty();
    String imageAltText = "";
    Optional<QuestionDefinition> maybeQuestionDefinition = Optional.empty();
    if (questionId.isPresent()) {
      Optional<QuestionModel> questionModel =
          questionRepository.lookupQuestion(questionId.get()).toCompletableFuture().join();
      if (questionModel.isPresent()) {
        String questionName =
            questionRepository.getQuestionDefinition(questionModel.get()).getName();
        // Find latest QuestionModel for this name: draft version first, fallback to active
        Optional<QuestionModel> latestQuestion =
            versionRepository
                .getDraftVersion()
                .flatMap(
                    draft -> versionRepository.getQuestionByNameForVersion(questionName, draft))
                .or(
                    () ->
                        versionRepository.getQuestionByNameForVersion(
                            questionName, versionRepository.getActiveVersion()));
        if (latestQuestion.isPresent()) {
          QuestionModel qm = latestQuestion.get();
          maybeQuestionDefinition = Optional.of(questionRepository.getQuestionDefinition(qm));
          if (qm.getImageFileKey() != null
              && !qm.getImageFileKey().isBlank()
              && PublicFileNameFormatter.isFileKeyForPublicQuestionImage(qm.getImageFileKey())) {
            imageUrl = Optional.of(publicStorageClient.getPublicDisplayUrl(qm.getImageFileKey()));
            imageAltText =
                Optional.ofNullable(qm.getLocalizedImageDescription())
                    .map(LocalizedStrings::getDefault)
                    .orElse("");
          }
        }
      }
    }

    QuestionPreview.Params params =
        QuestionPreview.Params.builder()
            .setRequest(request)
            .setApplicantId(0l)
            .setApplicantPersonalInfo(api)
            .setProfile(profile)
            .setType(questionTypeEnum)
            .setMessages(messages)
            .setImageUrl(imageUrl)
            .setImageAltText(imageAltText)
            .setQuestionDefinition(maybeQuestionDefinition)
            .build();
    String content = questionPreview.render(params);
    return ok(content).as(Http.MimeTypes.HTML);
  }
}
