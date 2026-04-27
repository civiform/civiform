package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.VersionRepository;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantPersonalInfo.Representation;
import services.question.QuestionService;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import views.admin.questions.QuestionPreview;

/** Controller for rendering inputs for questions. */
public final class QuestionPreviewController extends CiviFormController {
  private final QuestionPreview questionPreview;
  private final QuestionService questionService;
  private final Messages messages;

  @Inject
  public QuestionPreviewController(
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      QuestionPreview questionPreview,
      QuestionService questionService,
      MessagesApi messagesApi) {
    super(profileUtils, versionRepository);
    this.questionPreview = checkNotNull(questionPreview);
    this.questionService = checkNotNull(questionService);
    this.messages = messagesApi.preferred(ImmutableList.of(Lang.defaultLang()));
  }

  @Secure
  public Result sampleQuestion(Request request, String questionType) {
    Representation representation = Representation.builder().build();
    ApplicantPersonalInfo api = ApplicantPersonalInfo.ofGuestUser(representation);
    CiviFormProfile profile = profileUtils.currentUserProfile(request);

    QuestionType questionTypeEnum;
    try {
      questionTypeEnum = QuestionType.fromLabel(questionType);
    } catch (InvalidQuestionTypeException e) {
      return badRequest("Invalid question type: " + questionType);
    }

    // For enumerator questions, look up the initial question so the preview can render
    // the correct input type instead of the legacy text input.
    Optional<QuestionDefinition> initialQuestionDefinition = Optional.empty();
    Optional<String> questionIdParam = request.queryString("questionId");
    if (questionTypeEnum == QuestionType.ENUMERATOR && questionIdParam.isPresent()) {
      try {
        QuestionDefinition enumeratorDef =
            questionService
                .getReadOnlyQuestionServiceSync()
                .getQuestionDefinition(Long.parseLong(questionIdParam.get()));
        if (enumeratorDef instanceof EnumeratorQuestionDefinition enumerator) {
          Optional<Long> initialQuestionId = enumerator.getInitialQuestionId();
          if (initialQuestionId.isPresent()) {
            initialQuestionDefinition =
                Optional.of(
                    questionService
                        .getReadOnlyQuestionServiceSync()
                        .getQuestionDefinition(initialQuestionId.get()));
          }
        }
      } catch (NumberFormatException | QuestionNotFoundException e) {
        // Fall back to no initial question.
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
            .setInitialQuestionDefinition(initialQuestionDefinition)
            .build();
    String content = questionPreview.render(params);
    return ok(content).as(Http.MimeTypes.HTML);
  }
}
