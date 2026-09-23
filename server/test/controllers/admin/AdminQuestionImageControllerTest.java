package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import forms.questions.QuestionImageDescriptionForm;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import models.ConcurrentUpdateException;
import models.QuestionModel;
import models.VersionModel;
import org.apache.commons.text.StringEscapeUtils;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.mvc.Result;
import repository.ResetPostgres;
import repository.VersionRepository;
import services.LocalizedStrings;
import services.cloud.PublicFileNameFormatter;
import services.question.QuestionService;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.QuestionType;
import services.question.types.StaticContentQuestionDefinition;
import services.settings.SettingsManifest;
import support.FakeRequestBuilder;

public class AdminQuestionImageControllerTest extends ResetPostgres {
  private AdminQuestionImageController controller;
  private QuestionService questionService;
  private Messages messages;
  private VersionModel draftVersion;

  @Before
  public void setUp() {
    controller = instanceOf(AdminQuestionImageController.class);
    questionService = instanceOf(QuestionService.class);
    MessagesApi messagesApi = instanceOf(MessagesApi.class);
    messages = messagesApi.preferred(ImmutableList.of(Lang.defaultLang()));
    VersionRepository versionRepository = instanceOf(VersionRepository.class);
    draftVersion = versionRepository.getDraftVersionOrCreate();
  }

  @Test
  public void hxUploadQuestionImage_featureFlagDisabled_returnsNotFound() {
    QuestionModel question = testQuestionBank.nameApplicantName();

    Result result =
        controller.hxUploadQuestionImage(
            fakeRequestBuilder()
                .addCiviFormSetting("IMAGES_IN_QUESTION_FEATURE_ENABLED", "false")
                .method("POST")
                .build(),
            question.id);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void hxUploadQuestionImage_withFileAndDescription_setsKeyAndRedirects() throws Exception {
    QuestionModel question = createDraftQuestion();
    long id = question.id;
    String fileKey = PublicFileNameFormatter.formatPublicQuestionImageFileKey(id, "myImage.png");

    Result result =
        controller.hxUploadQuestionImage(createUploadRequest(fileKey, "Alt text description"), id);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    String htmlContent = contentAsString(result);
    assertThat(htmlContent).contains("hx-swap-oob=\"true\"");
    assertThat(htmlContent).contains("id=\"question-image-file-input-errors\"");
    assertThat(htmlContent).contains("hidden");

    QuestionDefinition updatedQuestion = questionService.getQuestionDefinition(id);
    assertThat(updatedQuestion.getImageFileKey()).contains(fileKey);
    assertThat(updatedQuestion.getLocalizedImageDescription())
        .map(LocalizedStrings::getDefault)
        .contains("Alt text description");
  }

  @Test
  public void hxUploadQuestionImage_descriptionOnly_updatesDescription() throws Exception {
    QuestionModel question = createDraftQuestion();
    long id = question.id;
    Result result =
        controller.hxUploadQuestionImage(
            createUploadRequest(/* fileKey= */ null, "Description only"), id);
    assertThat(result.status()).isEqualTo(OK);
    QuestionDefinition updatedQuestion = questionService.getQuestionDefinition(id);
    assertThat(updatedQuestion.getImageFileKey()).isEmpty();
    assertThat(
            updatedQuestion
                .getLocalizedImageDescription()
                .get()
                .get(LocalizedStrings.DEFAULT_LOCALE))
        .contains("Description only");
  }

  @Test
  public void hxUploadQuestionImage_blankDescriptionWithFile_doesNotSaveImage_redirectsWithError()
      throws Exception {
    QuestionDefinition question = testQuestionBank.staticContent().getQuestionDefinition();
    long id = question.getId();
    String fileKey = "question-image/question-" + id + "/myImage.png";

    Result result = controller.hxUploadQuestionImage(createUploadRequest(fileKey, ""), id);

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
    String htmlContent = contentAsString(result);
    assertThat(htmlContent).contains("id=\"question-image-file-input-errors\"");
    assertThat(htmlContent).contains(messages.at("validation.adminQuestionImage.altTextRequired"));
    assertThat(htmlContent).doesNotContain("hidden");

    QuestionDefinition currentQuestion = questionService.getQuestionDefinition(id);
    assertThat(currentQuestion.getImageFileKey()).isEmpty();
  }

  @Test
  public void hxUploadQuestionImage_missingQuestion_returnsNotFound() {
    Result result =
        controller.hxUploadQuestionImage(
            createUploadRequest(/* fileKey= */ null, "fake description"),
            /* questionId= */ Long.MAX_VALUE);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void hxUploadQuestionImage_nullBody_returnsBadRequest() {
    QuestionModel question = testQuestionBank.staticContent();

    Result result =
        controller.hxUploadQuestionImage(
            fakeRequestBuilder()
                .addCiviFormSetting("IMAGES_IN_QUESTION_FEATURE_ENABLED", "true")
                .method("POST")
                .build(),
            question.id);

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  private Http.Request createUploadRequest(String fileKey, String description) {
    FakeRequestBuilder requestBuilder =
        fakeRequestBuilder().addCiviFormSetting("IMAGES_IN_QUESTION_FEATURE_ENABLED", "true");
    if (fileKey == null) {
      return requestBuilder
          .method("POST")
          .bodyMultipart(
              Map.of(
                  QuestionImageDescriptionForm.QUESTION_IMAGE_DESCRIPTION,
                  new String[] {description}),
              List.of())
          .build();
    }
    return requestBuilder
        .method("POST")
        .bodyMultipart(
            Map.of(
                QuestionImageDescriptionForm.QUESTION_IMAGE_DESCRIPTION,
                new String[] {description}),
            List.of(
                new Http.MultipartFormData.FilePart<>(
                    "questionImage", "myImage.png", "image/png", fileKey)))
        .build();
  }

  private QuestionModel createDraftQuestion() {
    QuestionDefinition definition =
        new StaticContentQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("static-question-" + UUID.randomUUID())
                .setDescription("static content description")
                .setQuestionText(LocalizedStrings.withDefaultValue("Static content text"))
                .setQuestionHelpText(LocalizedStrings.withDefaultValue("Static content help text"))
                .build());
    QuestionModel question = new QuestionModel(definition);
    question.addVersion(draftVersion);
    question.save();
    return question;
  }

  private AdminQuestionImageController createControllerWithCustomService(
      QuestionService customQuestionService) {
    return new AdminQuestionImageController(
        instanceOf(ProfileUtils.class),
        instanceOf(VersionRepository.class),
        instanceOf(SettingsManifest.class),
        customQuestionService,
        instanceOf(MessagesApi.class));
  }

  @Test
  public void hxUploadQuestionImage_invalidFileKeyFormat_returnsBadRequest() {
    QuestionModel question = createDraftQuestion();
    long id = question.id;

    Result result =
        controller.hxUploadQuestionImage(
            createUploadRequest("invalid-file-key", "Valid alt text"), id);

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
    String htmlContent = contentAsString(result);
    assertThat(htmlContent).contains("id=\"question-image-file-input-errors\"");
    assertThat(htmlContent).contains("Key incorrectly formatted for question image file.");
  }

  @Test
  public void hxUploadQuestionImage_descriptionNotRemovable_returnsBadRequest() throws Exception {
    QuestionModel question = createDraftQuestion();
    long id = question.id;
    String fileKey = PublicFileNameFormatter.formatPublicQuestionImageFileKey(id, "myImage.png");

    // First upload sets both file key and description
    Result firstUpload =
        controller.hxUploadQuestionImage(createUploadRequest(fileKey, "Initial description"), id);
    assertThat(firstUpload.status()).isEqualTo(OK);

    // Attempt to clear description while an image is attached (fileKey is null in request)
    Result result =
        controller.hxUploadQuestionImage(createUploadRequest(/* fileKey= */ null, ""), id);

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
    String htmlContent = contentAsString(result);
    assertThat(htmlContent).contains("id=\"question-image-file-input-errors\"");
    assertThat(StringEscapeUtils.unescapeHtml4(htmlContent))
        .contains(messages.at("validation.adminQuestionImage.descriptionNotRemovable"));
  }

  @Test
  public void hxUploadQuestionImage_unsupportedQuestionType_returnsBadRequest() throws Exception {
    QuestionService spyService = spy(questionService);
    doThrow(new UnsupportedQuestionTypeException(QuestionType.TEXT))
        .when(spyService)
        .setImageFileKeyAndDescription(anyLong(), any(), any(), any());

    long id = 1L;
    String fileKey = PublicFileNameFormatter.formatPublicQuestionImageFileKey(id, "myImage.png");
    AdminQuestionImageController customController = createControllerWithCustomService(spyService);
    Result result =
        customController.hxUploadQuestionImage(createUploadRequest(fileKey, "description"), id);

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
    String htmlContent = contentAsString(result);
    assertThat(htmlContent).contains("id=\"question-image-file-input-errors\"");
    assertThat(htmlContent).contains("Unsupported question type");
  }

  @Test
  public void hxUploadQuestionImage_concurrentUpdateException_returnsBadRequest() throws Exception {
    QuestionService spyService = spy(questionService);
    doThrow(new ConcurrentUpdateException("concurrent update occurred"))
        .when(spyService)
        .setImageFileKeyAndDescription(anyLong(), any(), any(), any());

    long id = 1L;
    String fileKey = PublicFileNameFormatter.formatPublicQuestionImageFileKey(id, "myImage.png");
    AdminQuestionImageController customController = createControllerWithCustomService(spyService);
    Result result =
        customController.hxUploadQuestionImage(createUploadRequest(fileKey, "description"), id);

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
    String htmlContent = contentAsString(result);
    assertThat(htmlContent).contains("id=\"question-image-file-input-errors\"");
    assertThat(htmlContent).contains("Please try your edits again");
  }

  @Test
  public void hxDeleteQuestionImage_featureFlagDisabled_returnsNotFound() {
    QuestionModel question = testQuestionBank.staticContent();

    Result result =
        controller.hxDeleteQuestionImage(
            fakeRequestBuilder()
                .addCiviFormSetting("IMAGES_IN_QUESTION_FEATURE_ENABLED", "false")
                .method("POST")
                .build(),
            question.id);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void hxDeleteQuestionImage_missingQuestion_returnsNotFound() {
    Result result =
        controller.hxDeleteQuestionImage(
            fakeRequestBuilder()
                .addCiviFormSetting("IMAGES_IN_QUESTION_FEATURE_ENABLED", "true")
                .method("POST")
                .build(),
            /* questionId= */ Long.MAX_VALUE);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void hxDeleteQuestionImage_unsupportedQuestionType_returnsBadRequest() throws Exception {
    QuestionService spyService = spy(questionService);
    doThrow(new UnsupportedQuestionTypeException(QuestionType.TEXT))
        .when(spyService)
        .deleteImageFromQuestion(anyLong());

    AdminQuestionImageController customController = createControllerWithCustomService(spyService);
    Result result =
        customController.hxDeleteQuestionImage(
            fakeRequestBuilder()
                .addCiviFormSetting("IMAGES_IN_QUESTION_FEATURE_ENABLED", "true")
                .method("POST")
                .build(),
            1L);

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
    String htmlContent = contentAsString(result);
    assertThat(htmlContent).contains("id=\"question-image-file-input-errors\"");
    assertThat(htmlContent).contains("Unsupported question type");
  }

  @Test
  public void hxDeleteQuestionImage_success_clearsImageAndReturnsOkWithTrigger() throws Exception {
    QuestionModel question = createDraftQuestion();
    long id = question.id;
    String fileKey = PublicFileNameFormatter.formatPublicQuestionImageFileKey(id, "myImage.png");

    // Upload an image first
    controller.hxUploadQuestionImage(createUploadRequest(fileKey, "Initial description"), id);
    assertThat(questionService.getQuestionDefinition(id).getImageFileKey()).isPresent();

    // Now delete the image
    Result result =
        controller.hxDeleteQuestionImage(
            fakeRequestBuilder()
                .addCiviFormSetting("IMAGES_IN_QUESTION_FEATURE_ENABLED", "true")
                .method("POST")
                .build(),
            id);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.headers()).containsEntry("HX-Trigger", "question-image-deleted");
    String htmlContent = contentAsString(result);
    assertThat(htmlContent).contains("hx-swap-oob=\"true\"");
    assertThat(htmlContent).contains("id=\"concurrencyToken\"");

    // Verify in database that image and description were removed
    QuestionDefinition updated = questionService.getQuestionDefinition(id);
    assertThat(updated.getImageFileKey()).isEmpty();
    assertThat(updated.getLocalizedImageDescription()).isEmpty();
  }
}
