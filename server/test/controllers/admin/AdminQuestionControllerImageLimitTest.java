package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.inject.Bindings.bind;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequestBuilder;

import controllers.WithMockedProfiles;
import models.QuestionModel;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.VersionRepository;
import services.LocalizedStrings;
import services.cloud.PublicStorageClient;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.StaticContentQuestionDefinition;
import support.cloud.FakePublicStorageClient;

/**
 * Checks the question image size limit on the question edit page. Uses {@link
 * FakePublicStorageClient}, whose 6 MB limit differs from the 1 MB default in cloud.conf.
 */
public class AdminQuestionControllerImageLimitTest extends WithMockedProfiles {

  private AdminQuestionController controller;

  @BeforeClass
  public static void setupInjectorWithFakePublicStorage() {
    setupInjectorWithExtraBinding(
        bind(PublicStorageClient.class).to(FakePublicStorageClient.class));
  }

  @Before
  public void setUp() {
    resetDatabase();
    createGlobalAdminWithMockedProfile();
    controller = instanceOf(AdminQuestionController.class);
  }

  @Test
  public void edit_staticQuestionWithImagesEnabled_showsConfiguredImageSizeLimit() {
    QuestionModel question =
        new QuestionModel(
            new StaticContentQuestionDefinition(
                QuestionDefinitionConfig.builder()
                    .setName("static-question")
                    .setDescription("static content description")
                    .setQuestionText(LocalizedStrings.withDefaultValue("Static content text"))
                    .build()));
    question.addVersion(instanceOf(VersionRepository.class).getDraftVersionOrCreate());
    question.save();
    Request request =
        fakeRequestBuilder()
            .addCSRFToken()
            .addCiviFormSetting("IMAGES_IN_QUESTION_FEATURE_ENABLED", "true")
            .build();

    Result result =
        controller.edit(request, question.id, /* redirectUrl= */ "").toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("The maximum size for image upload is 6MB.");
    assertThat(contentAsString(result)).contains("data-file-limit-mb=\"6\"");
    assertThat(contentAsString(result))
        .contains("Your file is too large. Please choose a file less than 6 MB.");
  }
}
