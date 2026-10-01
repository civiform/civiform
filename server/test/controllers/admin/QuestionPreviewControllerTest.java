package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.contentAsString;
import static support.FakeRequestBuilder.fakeRequest;

import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.ResetPostgres;

public class QuestionPreviewControllerTest extends ResetPostgres {
  private QuestionPreviewController controller;

  @Before
  public void setup() {
    controller = instanceOf(QuestionPreviewController.class);
  }

  @Test
  public void sampleQuestion_staticQuestionWithImage_succeeds() {
    Request request =
        fakeRequest(
            routes.QuestionPreviewController.sampleQuestion("Static Text").url()
                + "?imageFileKey=question-image/question-333/my-image.png&imageAltText=altText");
    Result result = controller.sampleQuestion(request, "Static Text");
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("my-image.png");
    assertThat(contentAsString(result)).contains("altText");
  }
}
