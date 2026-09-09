package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.SEE_OTHER;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableList;
import forms.questions.QuestionImageDescriptionForm;
import java.util.List;
import java.util.Map;
import models.QuestionModel;
import models.VersionModel;
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
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
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
  public void uploadQuestionImage_featureFlagDisabled_returnsNotFound() {
    QuestionModel question = testQuestionBank.nameApplicantName();

    Result result =
        controller.uploadQuestionImage(
            fakeRequestBuilder()
                .addCiviFormSetting("IMAGES_IN_QUESTION_FEATURE_ENABLED", "false")
                .method("POST")
                .build(),
            question.id);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void uploadQuestionImage_withFileAndDescription_setsKeyAndRedirects() throws Exception {
    QuestionDefinition question = createDraftQuestionEnglishOnly().getQuestionDefinition();
    long id = question.getId();
    String fileKey = PublicFileNameFormatter.formatPublicQuestionImageFileKey(id, "myImage.png");

    Result result =
        controller.uploadQuestionImage(createUploadRequest(fileKey, "Alt text description"), id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(controllers.admin.routes.AdminQuestionController.edit(id, "").url());

    QuestionDefinition updatedQuestion = questionService.getQuestionDefinition(id);
    assertThat(updatedQuestion.getImageFileKey()).contains(fileKey);
    assertThat(updatedQuestion.getLocalizedImageDescription())
        .map(LocalizedStrings::getDefault)
        .contains("Alt text description");
  }

  @Test
  public void uploadQuestionImage_blankDescriptionWithFile_doesNotSaveImage_redirectsWithError()
      throws Exception {
    QuestionDefinition question = testQuestionBank.staticContent().getQuestionDefinition();
    long id = question.getId();
    String fileKey = "question-image/question-" + id + "/myImage.png";

    Result result = controller.uploadQuestionImage(createUploadRequest(fileKey, ""), id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.flash().data().get("error"))
        .isEqualTo(messages.at("validation.adminQuestionImage.altTextRequired"));

    QuestionDefinition currentQuestion = questionService.getQuestionDefinition(id);
    assertThat(currentQuestion.getImageFileKey()).isEmpty();
  }

  @Test
  public void uploadQuestionImage_descriptionOnly_updatesDescription() throws Exception {
    QuestionModel questionModel = createDraftQuestionEnglishOnly();
    long id = questionModel.id;

    Result result =
        controller.uploadQuestionImage(
            createUploadRequest(/* fileKey= */ null, "Description only"), id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(controllers.admin.routes.AdminQuestionController.edit(id, "").url());
    // had to refresh the models as they were failing
    questionModel.refresh();
    assertThat(questionModel.getQuestionDefinition().getImageFileKey()).isEmpty();
    assertThat(
            questionModel
                .getQuestionDefinition()
                .getLocalizedImageDescription()
                .get()
                .get(LocalizedStrings.DEFAULT_LOCALE))
        .contains("Description only");
  }

  @Test
  public void uploadQuestionImage_missingQuestion_returnsNotFound() {
    Result result =
        controller.uploadQuestionImage(
            createUploadRequest(/* fileKey= */ null, "fake description"),
            /* questionId= */ Long.MAX_VALUE);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void uploadQuestionImage_nullBody_returnsBadRequest() {
    QuestionModel question = testQuestionBank.staticContent();

    Result result =
        controller.uploadQuestionImage(
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

  private QuestionModel createDraftQuestionEnglishOnly() {
    QuestionDefinition definition =
        new NameQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("applicant name")
                .setDescription("name of applicant")
                .setQuestionText(LocalizedStrings.withDefaultValue("Applicant name"))
                .setQuestionHelpText(LocalizedStrings.withDefaultValue("enter name"))
                .build());
    QuestionModel question = new QuestionModel(definition);
    // Only draft questions are editable.
    question.addVersion(draftVersion);
    question.save();
    return question;
  }
}
