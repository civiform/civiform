package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.inject.Inject;
import models.QuestionModel;
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
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import services.settings.SettingsManifest;
import views.admin.questions.QuestionPreview;

/** Controller for rendering inputs for questions. */
public final class QuestionPreviewController extends CiviFormController {
  // The element id the enumerator fragment generates: "id-" and 8 alphanumeric characters.
  private static final Pattern ENUMERATOR_QUESTION_ID_PATTERN =
      Pattern.compile("id-[A-Za-z0-9]{8}");

  private final QuestionPreview questionPreview;
  private final Messages messages;
  private final PublicStorageClient publicStorageClient;
  private final QuestionRepository questionRepository;
  private final SettingsManifest settingsManifest;

  @Inject
  public QuestionPreviewController(
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      QuestionPreview questionPreview,
      MessagesApi messagesApi,
      PublicStorageClient publicStorageClient,
      QuestionRepository questionRepository,
      SettingsManifest settingsManifest) {
    super(profileUtils, versionRepository);
    this.questionPreview = checkNotNull(questionPreview);
    this.messages = messagesApi.preferred(ImmutableList.of(Lang.defaultLang()));
    this.publicStorageClient = checkNotNull(publicStorageClient);
    this.questionRepository = checkNotNull(questionRepository);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
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
      Optional<QuestionModel> latestQuestion = latestQuestionModel(questionId.get());
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
            .setInitialQuestionDefinition(
                maybeQuestionDefinition.flatMap(this::initialQuestionDefinition))
            .build();
    String content = questionPreview.render(params);
    return ok(content).as(Http.MimeTypes.HTML);
  }

  /**
   * Renders one new row for the initial question of an enumerator question, for the Add button in
   * the question preview. Nothing is saved: the preview has no applicant, so the row is only
   * rendered. The client sends {@code entityCount}, the number of rows it already has.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result hxAddEnumeratorEntity(Request request, long questionId) {
    if (!settingsManifest.getEnumeratorImprovementsEnabled(request)) {
      return notFound();
    }
    Optional<QuestionDefinition> maybeEnumerator =
        latestQuestionModel(questionId).map(questionRepository::getQuestionDefinition);
    if (maybeEnumerator.isEmpty()
        || !(maybeEnumerator.get() instanceof EnumeratorQuestionDefinition enumeratorDefinition)) {
      return notFound();
    }
    Optional<QuestionDefinition> maybeInitial = initialQuestionDefinition(enumeratorDefinition);
    if (maybeInitial.isEmpty()) {
      return notFound();
    }

    Map<String, String[]> form = request.body().asFormUrlEncoded();
    int entityIndex = entityCount(form);
    String html =
        questionPreview.renderNewEnumeratorEntityRow(
            request,
            messages,
            enumeratorDefinition,
            maybeInitial.get(),
            entityIndex,
            enumeratorQuestionId(form));
    return ok(html).as(Http.MimeTypes.HTML);
  }

  /** The number of rows the client already has, which is the index for the new row. */
  private static int entityCount(Map<String, String[]> form) {
    String[] values = form == null ? null : form.get("entityCount");
    if (values == null || values.length == 0) {
      return 0;
    }
    try {
      return Math.max(0, Integer.parseInt(values[0]));
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  /**
   * The enumerator question's element id sent by the client, used for the first row's aria
   * references. Anything that doesn't look like one is dropped, since it is echoed into the HTML.
   */
  private static String enumeratorQuestionId(Map<String, String[]> form) {
    String[] values = form == null ? null : form.get("enumeratorQuestionId");
    if (values == null
        || values.length == 0
        || !ENUMERATOR_QUESTION_ID_PATTERN.matcher(values[0]).matches()) {
      return "";
    }
    return values[0];
  }

  private Optional<QuestionDefinition> initialQuestionDefinition(
      QuestionDefinition enumeratorDefinition) {
    return enumeratorDefinition
        .getEnumeratorInitialQuestionId()
        .flatMap(id -> questionRepository.lookupQuestion(id).toCompletableFuture().join())
        .map(questionRepository::getQuestionDefinition);
  }

  /** The newest version of the question: the draft if there is one, else the active one. */
  private Optional<QuestionModel> latestQuestionModel(long questionId) {
    Optional<QuestionModel> questionModel =
        questionRepository.lookupQuestion(questionId).toCompletableFuture().join();
    if (questionModel.isEmpty()) {
      return Optional.empty();
    }
    String questionName = questionRepository.getQuestionDefinition(questionModel.get()).getName();
    return versionRepository
        .getDraftVersion()
        .flatMap(draft -> versionRepository.getQuestionByNameForVersion(questionName, draft))
        .or(
            () ->
                versionRepository.getQuestionByNameForVersion(
                    questionName, versionRepository.getActiveVersion()));
  }
}
