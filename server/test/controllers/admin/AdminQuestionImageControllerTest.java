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
import org.junit.Before;
import org.junit.Test;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.mvc.Result;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.question.QuestionService;
import services.question.types.QuestionDefinition;
import support.FakeRequestBuilder;
import support.TestQuestionBank;

public class AdminQuestionImageControllerTest extends ResetPostgres {
  private TestQuestionBank testQuestionBank;
  private AdminQuestionImageController controller;
  private QuestionService questionService;
  private Messages messages;

  @Before
  public void setUp() {
    testQuestionBank = instanceOf(TestQuestionBank.class);
    controller = instanceOf(AdminQuestionImageController.class);
    questionService = instanceOf(QuestionService.class);
    MessagesApi messagesApi = instanceOf(MessagesApi.class);
    messages = messagesApi.preferred(ImmutableList.of(Lang.defaultLang()));
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
    QuestionDefinition question = testQuestionBank.staticContent().getQuestionDefinition();
    long id = question.getId();
    String fileKey = "question-image/question-" + id + "/myImage.png";

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
        .isEqualTo(messages.at("validation.adminProgramImage.altTextRequired"));

    QuestionDefinition currentQuestion = questionService.getQuestionDefinition(id);
    assertThat(currentQuestion.getImageFileKey()).isEmpty();
  }

  @Test
  public void uploadQuestionImage_descriptionOnly_updatesDescription() throws Exception {
    QuestionDefinition question = testQuestionBank.staticContent().getQuestionDefinition();
    long id = question.getId();

    Result result =
        controller.uploadQuestionImage(
            createUploadRequest(/* fileKey= */ null, "Description only"), id);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(controllers.admin.routes.AdminQuestionController.edit(id, "").url());

    QuestionDefinition updatedQuestion = questionService.getQuestionDefinition(id);
    assertThat(updatedQuestion.getImageFileKey()).isEmpty();
    assertThat(updatedQuestion.getLocalizedImageDescription())
        .map(LocalizedStrings::getDefault)
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
}
