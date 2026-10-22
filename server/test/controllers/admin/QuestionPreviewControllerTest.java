package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.Helpers.testServerPort;
import static play.mvc.Http.Status.NOT_FOUND;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.route;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import com.google.common.collect.ImmutableMap;
import controllers.WithMockedProfiles;
import java.util.Optional;
import models.QuestionModel;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
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
    questionService.setImageFileKeyAndDescription(
        questionModel.id, Optional.of(fileKey1), LocalizedStrings.DEFAULT_LOCALE, "first alt text");

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

    // Update image and alt text using questionModel.id (automatically resolves to the existing
    // draft)
    String fileKey2 =
        PublicFileNameFormatter.formatPublicQuestionImageFileKey(
            questionModel.id, "second-image.png");
    questionService.setImageFileKeyAndDescription(
        questionModel.id,
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
  public void sampleQuestion_enumeratorWithInitialQuestion_rendersTheInitialQuestionAndAddButton()
      throws Exception {
    QuestionModel enumerator =
        testQuestionBank().enumeratorWithInitialQuestionApplicantHouseholdPlantNames();
    Request request =
        fakeRequestBuilder()
            .addCiviFormSetting("ENUMERATOR_IMPROVEMENTS_ENABLED", "true")
            .call(
                routes.QuestionPreviewController.sampleQuestion(
                    "Enumerator", Optional.of(enumerator.id)))
            .build();

    Result result = controller.sampleQuestion(request, "Enumerator", Optional.of(enumerator.id));

    assertThat(result.status()).isEqualTo(OK);
    String html = contentAsString(result);
    // The Add button posts to the server for a new row, as it does for an applicant, instead of
    // cloning the legacy free-text template.
    assertThat(html).contains("hx-post");
    assertThat(html).doesNotContain("enumerator-field-template");
    // The first row has the initial question's input for the first entity.
    assertThat(html).contains("[0]");
  }

  @Test
  public void sampleQuestion_enumeratorWithoutInitialQuestion_keepsTheLegacyTextInput()
      throws Exception {
    QuestionModel enumerator = testQuestionBank().enumeratorApplicantHouseholdMembers();
    Request request =
        fakeRequestBuilder()
            .addCiviFormSetting("ENUMERATOR_IMPROVEMENTS_ENABLED", "true")
            .call(
                routes.QuestionPreviewController.sampleQuestion(
                    "Enumerator", Optional.of(enumerator.id)))
            .build();

    Result result = controller.sampleQuestion(request, "Enumerator", Optional.of(enumerator.id));

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("enumerator-field-template");
  }

  @Test
  public void hxAddEnumeratorEntity_returnsARowForTheNextEntity() throws Exception {
    QuestionModel enumerator =
        testQuestionBank().enumeratorWithInitialQuestionApplicantHouseholdPlantNames();

    Result result =
        controller.hxAddEnumeratorEntity(
            addEntityRequest(ImmutableMap.of("entityCount", "2"), /* flagOn= */ true),
            enumerator.id);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("[2]");
  }

  @Test
  public void hxAddEnumeratorEntity_withFlagOff_isNotFound() throws Exception {
    QuestionModel enumerator =
        testQuestionBank().enumeratorWithInitialQuestionApplicantHouseholdPlantNames();

    Result result =
        controller.hxAddEnumeratorEntity(
            addEntityRequest(ImmutableMap.of("entityCount", "1"), /* flagOn= */ false),
            enumerator.id);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void hxAddEnumeratorEntity_withoutInitialQuestion_isNotFound() throws Exception {
    QuestionModel enumerator = testQuestionBank().enumeratorApplicantHouseholdMembers();

    Result result =
        controller.hxAddEnumeratorEntity(
            addEntityRequest(ImmutableMap.of("entityCount", "1"), /* flagOn= */ true),
            enumerator.id);

    assertThat(result.status()).isEqualTo(NOT_FOUND);
  }

  @Test
  public void hxAddEnumeratorEntity_withUnexpectedEnumeratorQuestionId_doesNotEchoIt()
      throws Exception {
    QuestionModel enumerator =
        testQuestionBank().enumeratorWithInitialQuestionApplicantHouseholdPlantNames();

    Result result =
        controller.hxAddEnumeratorEntity(
            addEntityRequest(
                ImmutableMap.of(
                    "entityCount", "1", "enumeratorQuestionId", "\"><script>alert(1)</script>"),
                /* flagOn= */ true),
            enumerator.id);

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).doesNotContain("<script>alert(1)");
  }

  @Test
  public void hxAddEnumeratorEntity_applicantProfile_rejected() {
    createApplicantWithMockedProfile();
    QuestionModel enumerator =
        testQuestionBank().enumeratorWithInitialQuestionApplicantHouseholdPlantNames();

    Http.RequestBuilder request =
        fakeRequestBuilder()
            .call(routes.QuestionPreviewController.hxAddEnumeratorEntity(enumerator.id))
            .header(Http.HeaderNames.HOST, "localhost:" + testServerPort());

    Result result = route(app, request);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue(controllers.routes.HomeController.index().url());
  }

  private Request addEntityRequest(ImmutableMap<String, String> form, boolean flagOn) {
    return fakeRequestBuilder()
        .addCiviFormSetting("ENUMERATOR_IMPROVEMENTS_ENABLED", String.valueOf(flagOn))
        .bodyForm(form)
        .build();
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

  @Test
  public void sampleQuestion_applicantProfileWithQuestionId_rejected() {
    createApplicantWithMockedProfile();
    QuestionModel questionModel = testQuestionBank().staticContent();

    Http.RequestBuilder request =
        fakeRequestBuilder()
            .call(
                routes.QuestionPreviewController.sampleQuestion(
                    "Static Text", Optional.of(questionModel.id)))
            .header(Http.HeaderNames.HOST, "localhost:" + testServerPort());

    Result result = route(app, request);

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue(controllers.routes.HomeController.index().url());
  }
}
