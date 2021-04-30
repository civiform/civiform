package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;

import com.google.common.collect.ImmutableMap;
import models.Question;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;
import repository.WithPostgresContainer;
import services.question.types.QuestionDefinition;
import views.html.helper.CSRF;

public class QuestionControllerTest extends WithPostgresContainer {
  private QuestionController controller;

  @Before
  public void setup() {
    controller = app.injector().instanceOf(QuestionController.class);
  }

  @Test
  public void create_redirectsOnSuccess() {
    ImmutableMap.Builder<String, String> formData = ImmutableMap.builder();
    formData
        .put("questionName", "name")
        .put("questionDescription", "desc")
        .put("questionType", "TEXT")
        .put("questionText", "Hi mom!")
        .put("questionHelpText", ":-)");
    RequestBuilder requestBuilder = Helpers.fakeRequest().bodyForm(formData.build());

    Result result = controller.create(requestBuilder.build(), "text");

    assertThat(result.redirectLocation()).hasValue(routes.QuestionController.index().url());
    assertThat(result.flash().get("message").get()).contains("created");
  }

  @Test
  public void create_repeatedQuestion_redirectsOnSuccess() {
    Question repeaterQuestion = testQuestionBank.applicantHouseholdMembers();
    ImmutableMap.Builder<String, String> formData = ImmutableMap.builder();
    formData
        .put("questionName", "name")
        .put("questionDescription", "desc")
        .put("repeaterId", String.valueOf(repeaterQuestion.id))
        .put("questionType", "TEXT")
        .put("questionText", "Hi mom!")
        .put("questionHelpText", ":-)");
    RequestBuilder requestBuilder = Helpers.fakeRequest().bodyForm(formData.build());

    Result result = controller.create(requestBuilder.build(), "text");

    assertThat(result.redirectLocation()).hasValue(routes.QuestionController.index().url());
    assertThat(result.flash().get("message").get()).contains("created");
  }

  @Test
  public void create_failsWithErrorMessageAndPopulatedFields() {
    ImmutableMap.Builder<String, String> formData = ImmutableMap.builder();
    formData.put("questionName", "name");
    Request request = addCSRFToken(Helpers.fakeRequest().bodyForm(formData.build())).build();

    Result result = controller.create(request, "text");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("New text question");
    assertThat(contentAsString(result)).contains(CSRF.getToken(request.asScala()).value());
    assertThat(contentAsString(result)).contains("blank description");
    assertThat(contentAsString(result)).contains("no question text");
    assertThat(contentAsString(result)).contains("name");
  }

  @Test
  public void create_failsWithInvalidQuestionType() {
    ImmutableMap.Builder<String, String> formData = ImmutableMap.builder();
    formData.put("questionName", "name").put("questionType", "INVALID_TYPE");
    RequestBuilder requestBuilder = Helpers.fakeRequest().bodyForm(formData.build());

    Result result = controller.create(requestBuilder.build(), "invalid_type");

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void edit_invalidIDReturnsBadRequest() {
    Request request = addCSRFToken(Helpers.fakeRequest()).build();
    controller
        .edit(request, 9999L)
        .thenAccept(
            result -> {
              assertThat(result.status()).isEqualTo(BAD_REQUEST);
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  public void edit_returnsPopulatedForm() {
    Question question = testQuestionBank.applicantName();
    Request request = addCSRFToken(Helpers.fakeRequest()).build();
    controller
        .edit(request, question.id)
        .thenAccept(
            result -> {
              assertThat(result.status()).isEqualTo(OK);
              assertThat(contentAsString(result)).contains("Edit name question");
              assertThat(contentAsString(result))
                  .contains(CSRF.getToken(request.asScala()).value());
              assertThat(contentAsString(result)).contains("Sample Question of type:");
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  public void edit_repeatedQuestion_hasRepeaterName() {
    Question repeatedQuestion = testQuestionBank.applicantHouseholdMemberName();
    Request request = addCSRFToken(Helpers.fakeRequest()).build();
    controller
        .edit(request, repeatedQuestion.id)
        .thenAccept(
            result -> {
              assertThat(result.status()).isEqualTo(OK);
              assertThat(contentAsString(result)).contains("Edit name question");
              assertThat(contentAsString(result)).contains("applicant household members");
              assertThat(contentAsString(result))
                  .contains(CSRF.getToken(request.asScala()).value());
              assertThat(contentAsString(result)).contains("Sample Question of type:");
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  public void index_returnsQuestions() {
    testQuestionBank.applicantAddress();
    testQuestionBank.applicantName();
    Request request = addCSRFToken(Helpers.fakeRequest()).build();
    controller
        .index(request)
        .thenAccept(
            result -> {
              assertThat(result.status()).isEqualTo(OK);
              assertThat(result.contentType()).hasValue("text/html");
              assertThat(result.charset()).hasValue("utf-8");
              assertThat(contentAsString(result)).contains("Total Questions: 2");
              assertThat(contentAsString(result)).contains("All Questions");
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  public void index_withNoQuestions() {
    Request request = addCSRFToken(Helpers.fakeRequest()).build();
    controller
        .index(request)
        .thenAccept(
            result -> {
              assertThat(result.status()).isEqualTo(OK);
              assertThat(result.contentType()).hasValue("text/html");
              assertThat(result.charset()).hasValue("utf-8");
              assertThat(contentAsString(result)).contains("Total Questions: 0");
              assertThat(contentAsString(result)).contains("All Questions");
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  public void index_showsMessageFlash() {
    Request request = addCSRFToken(Helpers.fakeRequest().flash("message", "has message")).build();
    controller
        .index(request)
        .thenAccept(
            result -> {
              assertThat(result.status()).isEqualTo(OK);
              assertThat(result.contentType()).hasValue("text/html");
              assertThat(result.charset()).hasValue("utf-8");
              assertThat(contentAsString(result)).contains("has message");
            })
        .toCompletableFuture()
        .join();
  }

  @Test
  public void newOne_returnsExpectedForm() {
    Request request = addCSRFToken(Helpers.fakeRequest()).build();
    Result result = controller.newOne(request, "text");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("New text question");
    assertThat(contentAsString(result)).contains(CSRF.getToken(request.asScala()).value());
    assertThat(contentAsString(result)).contains("Sample Question of type:");
  }

  @Test
  public void newOne_returnsFailureForInvalidQuestionType() {
    Request request = addCSRFToken(Helpers.fakeRequest()).build();
    Result result = controller.newOne(request, "nope");
    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void update_redirectsOnSuccess() {
    QuestionDefinition nameQuestion = testQuestionBank.applicantName().getQuestionDefinition();
    ImmutableMap.Builder<String, String> formData = ImmutableMap.builder();
    formData
        .put("questionName", nameQuestion.getName())
        .put("questionDescription", "a new description")
        .put("questionType", nameQuestion.getQuestionType().name())
        .put("questionText", "question text updated")
        .put("questionHelpText", "a new help text");
    RequestBuilder requestBuilder = addCSRFToken(Helpers.fakeRequest().bodyForm(formData.build()));

    Result result =
        controller.update(
            requestBuilder.build(),
            nameQuestion.getId(),
            nameQuestion.getQuestionType().toString());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue(routes.QuestionController.index().url());
    assertThat(result.flash().get("message").get()).contains("updated");
  }

  @Test
  public void update_failsWithErrorMessageAndPopulatedFields() {
    Question question = testQuestionBank.applicantFavoriteColor();
    ImmutableMap.Builder<String, String> formData = ImmutableMap.builder();
    formData
        .put("questionName", "favorite_color")
        .put("questionDescription", "")
        .put("questionText", "question text updated!");
    Request request = addCSRFToken(Helpers.fakeRequest().bodyForm(formData.build())).build();

    Result result = controller.update(request, question.id, "text");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Edit text question");
    assertThat(contentAsString(result)).contains(CSRF.getToken(request.asScala()).value());
    assertThat(contentAsString(result)).contains("blank description");
    assertThat(contentAsString(result)).contains("question text updated!");
  }

  @Test
  public void update_failsWithInvalidQuestionType() {
    Question question = testQuestionBank.applicantHouseholdMembers();
    ImmutableMap.Builder<String, String> formData = ImmutableMap.builder();
    formData.put("questionType", "INVALID_TYPE").put("questionText", "question text updated!");
    RequestBuilder requestBuilder = Helpers.fakeRequest().bodyForm(formData.build());

    Result result = controller.update(requestBuilder.build(), question.id, "invalid_type");

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }
}
