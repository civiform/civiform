package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
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
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.types.QuestionType;
import views.admin.questions.QuestionPreview;

/** Controller for rendering inputs for questions. */
public final class QuestionPreviewController extends CiviFormController {
  private final QuestionPreview questionPreview;
  private final Messages messages;

  @Inject
  public QuestionPreviewController(
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      QuestionPreview questionPreview,
      MessagesApi messagesApi) {
    super(profileUtils, versionRepository);
    this.questionPreview = checkNotNull(questionPreview);
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

    QuestionPreview.Params params =
        QuestionPreview.Params.builder()
            .setRequest(request)
            .setApplicantId(0l)
            .setApplicantPersonalInfo(api)
            .setProfile(profile)
            .setType(questionTypeEnum)
            .setMessages(messages)
            .build();
    String content = questionPreview.render(params);
    return ok(content).as(Http.MimeTypes.HTML);
  }
}
