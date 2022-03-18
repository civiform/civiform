package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import forms.DropdownQuestionForm;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import models.LifecycleStage;
import models.Question;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import play.test.Helpers;
import repository.QuestionRepository;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.question.QuestionOption;
import services.question.types.DropdownQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import views.html.helper.CSRF;

public class AdminQuestionControllerTest extends ResetPostgres {
  private QuestionRepository questionRepo;
  private AdminQuestionController controller;

  @Before
  public void setup() {
    questionRepo = instanceOf(QuestionRepository.class);
    controller = instanceOf(AdminQuestionController.class);
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

    assertThat(result.redirectLocation()).hasValue(routes.AdminQuestionController.index().url());
    assertThat(result.flash().get("message").get()).contains("created");
  }

  @Test
  public void create_repeatedQuestion_redirectsOnSuccess() {
    Question enumeratorQuestion = testQuestionBank.applicantHouseholdMembers();
    ImmutableMap.Builder<String, String> formData = ImmutableMap.builder();
    formData
        .put("questionName", "name")
        .put("questionDescription", "desc")
        .put("enumeratorId", String.valueOf(enumeratorQuestion.id))
        .put("questionType", "TEXT")
        .put("questionText", "$this is required")
        .put("questionHelpText", "$this is also required");
    RequestBuilder requestBuilder = Helpers.fakeRequest().bodyForm(formData.build());

    Result result = controller.create(requestBuilder.build(), "text");

    assertThat(result.redirectLocation()).hasValue(routes.AdminQuestionController.index().url());
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
    assertThat(contentAsString(result)).contains("Description cannot be blank");
    assertThat(contentAsString(result)).contains("Question text cannot be blank");
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
  public void edit_repeatedQuestion_hasEnumeratorName() {
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
        .put("questionHelpText", "a new help text")
        .put("questionExportState", "NON_DEMOGRAPHIC");
    RequestBuilder requestBuilder = addCSRFToken(Helpers.fakeRequest().bodyForm(formData.build()));

    Result result =
        controller.update(
            requestBuilder.build(),
            nameQuestion.getId(),
            nameQuestion.getQuestionType().toString());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue(routes.AdminQuestionController.index().url());
    assertThat(result.flash().get("message").get()).contains("updated");
  }

  @Test
  public void update_setsIdsAsExpected() {
    DropdownQuestionDefinition definition =
        new DropdownQuestionDefinition(
            /* name= */ "applicant ice cream",
            /* enumeratorId= */ Optional.empty(),
            /* description= */ "Select your favorite ice cream flavor",
            LocalizedStrings.of(Locale.US, "Ice cream?", Locale.FRENCH, "crème glacée?"),
            LocalizedStrings.of(Locale.US, "help", Locale.FRENCH, "aider"),
            ImmutableList.of(
                QuestionOption.create(
                    1L, LocalizedStrings.of(Locale.US, "chocolate", Locale.FRENCH, "chocolat")),
                QuestionOption.create(
                    2L, LocalizedStrings.of(Locale.US, "strawberry", Locale.FRENCH, "fraise")),
                QuestionOption.create(
                    3L, LocalizedStrings.of(Locale.US, "vanilla", Locale.FRENCH, "vanille")),
                QuestionOption.create(
                    4L, LocalizedStrings.of(Locale.US, "coffee", Locale.FRENCH, "café"))));
    // We can only update draft questions, so save this in the DRAFT version.
    testQuestionBank.maybeSave(definition, LifecycleStage.DRAFT);

    DropdownQuestionForm questionForm = new DropdownQuestionForm(definition);
    questionForm.setNewOptions(ImmutableList.of("cookie", "mint", "pistachio"));

    questionForm.getBuilder();

    assertThat(questionForm.getNextAvailableId()).isPresent();
    assertThat(questionForm.getNextAvailableId()).isEqualTo(OptionalLong.of(8));
  }

  @Test
  public void update_mergesTranslations() {
    QuestionDefinition definition =
        new DropdownQuestionDefinition(
            "applicant ice cream",
            Optional.empty(),
            "Select your favorite ice cream flavor",
            LocalizedStrings.of(Locale.US, "Ice cream?", Locale.FRENCH, "crème glacée?"),
            LocalizedStrings.of(Locale.US, "help", Locale.FRENCH, "aider"),
            ImmutableList.of(
                QuestionOption.create(
                    1L, LocalizedStrings.of(Locale.US, "chocolate", Locale.FRENCH, "chocolat")),
                QuestionOption.create(
                    2L, LocalizedStrings.of(Locale.US, "strawberry", Locale.FRENCH, "fraise")),
                QuestionOption.create(
                    3L, LocalizedStrings.of(Locale.US, "vanilla", Locale.FRENCH, "vanille")),
                QuestionOption.create(
                    4L, LocalizedStrings.of(Locale.US, "coffee", Locale.FRENCH, "café"))));
    // We can only update draft questions, so save this in the DRAFT version.
    Question question = testQuestionBank.maybeSave(definition, LifecycleStage.DRAFT);

    ImmutableMap<String, String> formData =
        ImmutableMap.<String, String>builder()
            .put("questionName", definition.getName())
            .put("questionDescription", definition.getDescription())
            .put("questionType", definition.getQuestionType().name())
            .put("questionText", "new question text")
            .put("questionHelpText", "new help text")
            .put("options[0]", "coffee") // Unchanged but out of order
            .put("options[1]", "vanilla") // Unchanged and in order
            .put("newOptions[0]", "lavender") // New flavor
            .put("optionIds[0]", "4")
            .put("optionIds[1]", "3")
            .put("nextAvailableId", "5")
            .put("questionExportState", "NON_DEMOGRAPHIC")
            // Has one fewer than the original question
            .build();
    RequestBuilder requestBuilder = addCSRFToken(Helpers.fakeRequest().bodyForm(formData));

    Result result =
        controller.update(
            requestBuilder.build(), question.id, definition.getQuestionType().toString());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    Question found = questionRepo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition().getQuestionText().translations())
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap.of(Locale.US, "new question text", Locale.FRENCH, "crème glacée?"));
    assertThat(found.getQuestionDefinition().getQuestionHelpText().translations())
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap.of(Locale.US, "new help text", Locale.FRENCH, "aider"));

    ImmutableList<QuestionOption> expectedOptions =
        ImmutableList.of(
            QuestionOption.create(
                4, 0, LocalizedStrings.of(Locale.US, "coffee", Locale.FRENCH, "café")),
            QuestionOption.create(
                3, 1, LocalizedStrings.of(Locale.US, "vanilla", Locale.FRENCH, "vanille")),
            QuestionOption.create(5, 2, LocalizedStrings.withDefaultValue("lavender")));
    assertThat(((MultiOptionQuestionDefinition) found.getQuestionDefinition()).getOptions())
        .isEqualTo(expectedOptions);
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
    assertThat(contentAsString(result)).contains("Description cannot be blank");
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
