package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import controllers.WithMockedProfiles;
import java.util.Optional;
import models.QuestionModel;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.VersionRepository;
import services.LocalizedStrings;
import services.cloud.PublicFileNameFormatter;
import services.question.QuestionService;
import services.question.types.QuestionDefinition;

public class QuestionPreviewControllerTest extends WithMockedProfiles {
  private QuestionPreviewController controller;
  private QuestionService questionService;
  private VersionRepository versionRepository;

  @Before
  public void setup() {
    resetDatabase();
    controller = instanceOf(QuestionPreviewController.class);
    questionService = instanceOf(QuestionService.class);
    versionRepository = instanceOf(VersionRepository.class);
    createGlobalAdminWithMockedProfile();
  }

  @Test
  public void sampleQuestion_staticQuestionWithImage_rendersAndUpdatesPreviewWhenChanged()
      throws Exception {
    QuestionModel questionModel = testQuestionBank().staticContent();
    String fileKey1 =
        PublicFileNameFormatter.formatPublicQuestionImageFileKey(
            questionModel.id, "first-image.png");
    QuestionDefinition draftQd =
        questionService.setImageFileKeyAndDescription(
            questionModel.id,
            Optional.of(fileKey1),
            LocalizedStrings.DEFAULT_LOCALE,
            "first alt text");

    Request request =
        fakeRequestBuilder()
            .addCiviFormSetting("IMAGES_IN_QUESTION_FEATURE_ENABLED", "true")
            .call(
                routes.QuestionPreviewController.sampleQuestion(
                    "Static Text", Optional.of(questionModel.id)))
            .build();

    // Verify initial image and alt text are rendered in preview
    Result initialResult =
        controller.sampleQuestion(request, "Static Text", Optional.of(questionModel.id));
    assertThat(initialResult.status()).isEqualTo(OK);
    String initialHtml = contentAsString(initialResult);
    assertThat(initialHtml).contains("data-testid=\"question-image-container\"");
    assertThat(initialHtml).contains("first-image.png");
    assertThat(initialHtml).contains("alt=\"first alt text\"");

    // Update image and alt text using the draft question ID
    String fileKey2 =
        PublicFileNameFormatter.formatPublicQuestionImageFileKey(
            draftQd.getId(), "second-image.png");
    questionService.setImageFileKeyAndDescription(
        draftQd.getId(),
        Optional.of(fileKey2),
        LocalizedStrings.DEFAULT_LOCALE,
        "updated alt text");

    // Verify preview reflects updated image and alt text
    Result updatedResult =
        controller.sampleQuestion(request, "Static Text", Optional.of(questionModel.id));
    assertThat(updatedResult.status()).isEqualTo(OK);
    String updatedHtml = contentAsString(updatedResult);
    assertThat(updatedHtml).doesNotContain("first-image.png");
    assertThat(updatedHtml).doesNotContain("alt=\"first alt text\"");
    assertThat(updatedHtml).contains("second-image.png");
    assertThat(updatedHtml).contains("alt=\"updated alt text\"");
  }

  @Test
  public void sampleQuestion_activeQuestionWithDraftImage_rendersDraftImageInPreview()
      throws Exception {
    QuestionDefinition activeQd = testQuestionBank().staticContent().getQuestionDefinition();
    versionRepository.publishNewSynchronizedVersion();

    Request request =
        fakeRequestBuilder()
            .addCiviFormSetting("IMAGES_IN_QUESTION_FEATURE_ENABLED", "true")
            .call(
                routes.QuestionPreviewController.sampleQuestion(
                    "Static Text", Optional.of(activeQd.getId())))
            .build();

    // Active question has no image initially
    Result initialResult =
        controller.sampleQuestion(request, "Static Text", Optional.of(activeQd.getId()));
    assertThat(contentAsString(initialResult))
        .doesNotContain("data-testid=\"question-image-container\"");

    // Add image to the active question, which creates/updates draft
    String draftFileKey =
        PublicFileNameFormatter.formatPublicQuestionImageFileKey(
            activeQd.getId(), "draft-image.png");
    questionService.setImageFileKeyAndDescription(
        activeQd.getId(),
        Optional.of(draftFileKey),
        LocalizedStrings.DEFAULT_LOCALE,
        "Draft image on active question");

    // Request preview using the active question's ID
    Result draftResult =
        controller.sampleQuestion(request, "Static Text", Optional.of(activeQd.getId()));
    assertThat(draftResult.status()).isEqualTo(OK);
    String html = contentAsString(draftResult);
    assertThat(html).contains("data-testid=\"question-image-container\"");
    assertThat(html).contains("draft-image.png");
    assertThat(html).contains("alt=\"Draft image on active question\"");
  }

  @Test
  public void sampleQuestion_featureFlagDisabled_doesNotRenderImageContainer() throws Exception {
    QuestionModel questionModel = testQuestionBank().staticContent();
    String fileKey =
        PublicFileNameFormatter.formatPublicQuestionImageFileKey(questionModel.id, "my-image.png");
    questionService.setImageFileKeyAndDescription(
        questionModel.id, Optional.of(fileKey), LocalizedStrings.DEFAULT_LOCALE, "sample alt text");

    Request request =
        fakeRequestBuilder()
            .addCiviFormSetting("IMAGES_IN_QUESTION_FEATURE_ENABLED", "false")
            .call(
                routes.QuestionPreviewController.sampleQuestion(
                    "Static Text", Optional.of(questionModel.id)))
            .build();

    Result result =
        controller.sampleQuestion(request, "Static Text", Optional.of(questionModel.id));
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).doesNotContain("data-testid=\"question-image-container\"");
  }
}
