package controllers.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.api.test.CSRFTokenHelper.addCSRFToken;
import static play.mvc.Http.Status.BAD_REQUEST;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static support.CfTestHelpers.requestBuilderWithSettings;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import forms.DropdownQuestionForm;
import java.util.Locale;
import java.util.Optional;
import models.LifecycleStage;
import models.QuestionModel;
import models.QuestionTag;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import repository.QuestionRepository;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionQuestionType;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.QuestionType;
import views.html.helper.CSRF;

public class AdminQuestionControllerTest extends ResetPostgres {
  private QuestionRepository questionRepo;
  private AdminQuestionController controller;

  @Before
  public void setup() {
    questionRepo = instanceOf(QuestionRepository.class);
    controller = instanceOf(AdminQuestionController.class);
  }

  private ImmutableSet<Long> retrieveAllQuestionIds() {
    return questionRepo.listQuestions().toCompletableFuture().join().stream()
        .map(q -> q.getQuestionDefinition().getId())
        .collect(ImmutableSet.toImmutableSet());
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
    RequestBuilder requestBuilder = requestBuilderWithSettings().bodyForm(formData.build());

    ImmutableSet<Long> questionIdsBefore = retrieveAllQuestionIds();
    Result result = controller.create(requestBuilder.build(), "text");

    assertThat(result.redirectLocation()).hasValue(routes.AdminQuestionController.index().url());
    assertThat(result.flash().get("success").get()).contains("created");

    ImmutableSet<Long> questionIdsAfter = retrieveAllQuestionIds();
    assertThat(questionIdsAfter.size()).isEqualTo(questionIdsBefore.size() + 1);
    Long newQuestionId = Sets.difference(questionIdsAfter, questionIdsBefore).iterator().next();
    QuestionModel newQuestion =
        questionRepo.lookupQuestion(newQuestionId).toCompletableFuture().join().get();
    assertThat(newQuestion.getQuestionDefinition().getName()).isEqualTo("name");
    assertThat(newQuestion.getQuestionDefinition().getDescription()).isEqualTo("desc");
    assertThat(newQuestion.getQuestionDefinition().getEnumeratorId()).isEqualTo(Optional.empty());
    assertThat(newQuestion.getQuestionDefinition().getQuestionType()).isEqualTo(QuestionType.TEXT);
    assertThat(newQuestion.getQuestionTags())
        .isEqualTo(ImmutableList.of(QuestionTag.NON_DEMOGRAPHIC));
  }

  @Test
  public void create_selectedExportValue_redirectsOnSuccess() {
    ImmutableMap.Builder<String, String> formData = ImmutableMap.builder();
    formData
        .put("questionName", "name")
        .put("questionDescription", "desc")
        .put("questionType", "TEXT")
        .put("questionText", "Hi mom!")
        .put("questionHelpText", ":-)")
        .put("questionExportState", "DEMOGRAPHIC_PII");
    RequestBuilder requestBuilder = requestBuilderWithSettings().bodyForm(formData.build());

    ImmutableSet<Long> questionIdsBefore = retrieveAllQuestionIds();
    Result result = controller.create(requestBuilder.build(), "text");

    assertThat(result.redirectLocation()).hasValue(routes.AdminQuestionController.index().url());
    assertThat(result.flash().get("success").get()).contains("created");

    ImmutableSet<Long> questionIdsAfter = retrieveAllQuestionIds();
    assertThat(questionIdsAfter.size()).isEqualTo(questionIdsBefore.size() + 1);
    Long newQuestionId = Sets.difference(questionIdsAfter, questionIdsBefore).iterator().next();
    QuestionModel newQuestion =
        questionRepo.lookupQuestion(newQuestionId).toCompletableFuture().join().get();
    assertThat(newQuestion.getQuestionDefinition().getName()).isEqualTo("name");
    assertThat(newQuestion.getQuestionDefinition().getDescription()).isEqualTo("desc");
    assertThat(newQuestion.getQuestionDefinition().getEnumeratorId()).isEqualTo(Optional.empty());
    assertThat(newQuestion.getQuestionDefinition().getQuestionType()).isEqualTo(QuestionType.TEXT);
    assertThat(newQuestion.getQuestionTags())
        .isEqualTo(ImmutableList.of(QuestionTag.DEMOGRAPHIC_PII));
  }

  @Test
  public void create_repeatedQuestion_redirectsOnSuccess() {
    QuestionModel enumeratorQuestion = testQuestionBank.applicantHouseholdMembers();
    ImmutableMap.Builder<String, String> formData = ImmutableMap.builder();
    formData
        .put("questionName", "name")
        .put("questionDescription", "desc")
        .put("enumeratorId", String.valueOf(enumeratorQuestion.id))
        .put("questionType", "TEXT")
        .put("questionText", "$this is required")
        .put("questionHelpText", "$this is also required");
    RequestBuilder requestBuilder = requestBuilderWithSettings().bodyForm(formData.build());

    ImmutableSet<Long> questionIdsBefore = retrieveAllQuestionIds();
    Result result = controller.create(requestBuilder.build(), "text");

    assertThat(result.redirectLocation()).hasValue(routes.AdminQuestionController.index().url());
    assertThat(result.flash().get("success").get()).contains("created");

    ImmutableSet<Long> questionIdsAfter = retrieveAllQuestionIds();
    assertThat(questionIdsAfter.size()).isEqualTo(questionIdsBefore.size() + 1);
    Long newQuestionId = Sets.difference(questionIdsAfter, questionIdsBefore).iterator().next();
    QuestionModel newQuestion =
        questionRepo.lookupQuestion(newQuestionId).toCompletableFuture().join().get();
    assertThat(newQuestion.getQuestionDefinition().getName()).isEqualTo("name");
    assertThat(newQuestion.getQuestionDefinition().getDescription()).isEqualTo("desc");
    assertThat(newQuestion.getQuestionDefinition().getEnumeratorId())
        .isEqualTo(Optional.of(enumeratorQuestion.id));
    assertThat(newQuestion.getQuestionDefinition().getQuestionType()).isEqualTo(QuestionType.TEXT);
    assertThat(newQuestion.getQuestionTags())
        .isEqualTo(ImmutableList.of(QuestionTag.NON_DEMOGRAPHIC));
  }

  @Test
  public void create_failsWithErrorMessageAndPopulatedFields() {
    ImmutableMap.Builder<String, String> formData = ImmutableMap.builder();
    formData.put("questionName", "name");
    Request request = addCSRFToken(requestBuilderWithSettings().bodyForm(formData.build())).build();

    ImmutableSet<Long> questionIdsBefore = retrieveAllQuestionIds();
    Result result = controller.create(request, "text");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("New text question");
    assertThat(contentAsString(result)).contains(CSRF.getToken(request.asScala()).value());
    assertThat(contentAsString(result)).contains("Question text cannot be blank");
    assertThat(contentAsString(result)).contains("name");
    assertThat(retrieveAllQuestionIds().size()).isEqualTo(questionIdsBefore.size());
  }

  @Test
  public void create_failsWithInvalidQuestionType() {
    ImmutableMap.Builder<String, String> formData = ImmutableMap.builder();
    formData.put("questionName", "name").put("questionType", "INVALID_TYPE");
    RequestBuilder requestBuilder = requestBuilderWithSettings().bodyForm(formData.build());

    ImmutableSet<Long> questionIdsBefore = retrieveAllQuestionIds();
    Result result = controller.create(requestBuilder.build(), "invalid_type");

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
    assertThat(retrieveAllQuestionIds().size()).isEqualTo(questionIdsBefore.size());
  }

  @Test
  public void edit_invalidIDReturnsBadRequest() {
    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result = controller.edit(request, 9999L).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void edit_returnsRedirectWhenEditingQuestionWithExistingDraft() {
    QuestionModel publishedQuestion = testQuestionBank.applicantName();
    QuestionModel draftQuestion =
        testQuestionBank.maybeSave(
            this.createNameQuestionDuplicate(publishedQuestion), LifecycleStage.DRAFT);

    // sanity check that the new question has different id.
    assertThat(publishedQuestion.id).isNotEqualTo(draftQuestion.id);

    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result = controller.edit(request, publishedQuestion.id).toCompletableFuture().join();

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation())
        .hasValue(routes.AdminQuestionController.edit(draftQuestion.id).url());
  }

  @Test
  public void edit_returnsPopulatedForm() {
    QuestionModel question = testQuestionBank.applicantName();
    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result = controller.edit(request, question.id).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Edit name question");
    assertThat(contentAsString(result)).contains(CSRF.getToken(request.asScala()).value());
    assertThat(contentAsString(result)).contains("Sample question of type:");
  }

  @Test
  public void edit_repeatedQuestion_hasEnumeratorName() {
    QuestionModel repeatedQuestion = testQuestionBank.applicantHouseholdMemberName();
    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result = controller.edit(request, repeatedQuestion.id).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Edit name question");
    assertThat(contentAsString(result)).contains("applicant household members");
    assertThat(contentAsString(result)).contains(CSRF.getToken(request.asScala()).value());
    assertThat(contentAsString(result)).contains("Sample question of type:");
  }

  @Test
  public void index_returnsQuestions() throws Exception {
    testQuestionBank.applicantAddress();
    QuestionDefinition nameQuestion = testQuestionBank.applicantName().getQuestionDefinition();
    // Create a draft version of an already published question and ensure that it isn't
    // double-counted in the rendered total number of questions.
    QuestionDefinition updatedQuestion =
        new QuestionDefinitionBuilder(nameQuestion).clearId().build();
    testQuestionBank.maybeSave(updatedQuestion, LifecycleStage.DRAFT);
    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result = controller.index(request).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    assertThat(result.charset()).hasValue("utf-8");
    // We include the trailing "<" to ensure we don't partially match
    // 200 rather than 2.
    assertThat(contentAsString(result)).contains("Total questions: 2<");
    assertThat(contentAsString(result)).contains("All questions");

    // Now add a new draft question and ensure that it is included in the total.
    QuestionDefinition newDraftQuestion =
        new QuestionDefinitionBuilder(nameQuestion)
            .clearId()
            .setName(nameQuestion.getName() + "-new-question-name")
            .build();
    testQuestionBank.maybeSave(newDraftQuestion, LifecycleStage.DRAFT);

    result = controller.index(request).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    assertThat(result.charset()).hasValue("utf-8");
    // We include the trailing "<" to ensure we don't partially match
    // 300 rather than 3.
    assertThat(contentAsString(result)).contains("Total questions: 3<");
    assertThat(contentAsString(result)).contains("All questions");
  }

  @Test
  public void index_withNoQuestions() {
    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result = controller.index(request).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    assertThat(result.charset()).hasValue("utf-8");
    assertThat(contentAsString(result)).contains("Total questions: 0");
    assertThat(contentAsString(result)).contains("All questions");
  }

  @Test
  public void index_showsMessageFlash() {
    Request request =
        addCSRFToken(requestBuilderWithSettings().flash("success", "has message")).build();
    Result result = controller.index(request).toCompletableFuture().join();
    assertThat(result.status()).isEqualTo(OK);
    assertThat(result.contentType()).hasValue("text/html");
    assertThat(result.charset()).hasValue("utf-8");
    assertThat(contentAsString(result)).contains("has message");
  }

  @Test
  public void newOne_returnsExpectedForm() {
    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result = controller.newOne(request, "text", "/some/redirect/url");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("New text question");
    assertThat(contentAsString(result)).contains(CSRF.getToken(request.asScala()).value());
    assertThat(contentAsString(result)).contains("Sample question of type:");
    assertThat(contentAsString(result)).contains("/some/redirect/url");
  }

  @Test
  public void newOne_returnsFailureForInvalidQuestionType() {
    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    Result result = controller.newOne(request, "nope", "/some/redirect/url");
    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void newOne_absoluteRedirectUrl_throws() {
    Request request = addCSRFToken(requestBuilderWithSettings()).build();
    assertThatThrownBy(() -> controller.newOne(request, "text", "https://www.example.com"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContainingAll("Invalid absolute URL.");
  }

  @Test
  public void update_redirectsOnSuccessAndUpdatesQuestion() {
    // We can only update draft questions, so save this in the DRAFT version.
    QuestionModel originalNameQuestion =
        testQuestionBank.maybeSave(
            this.createNameQuestionDuplicate(testQuestionBank.applicantName()),
            LifecycleStage.DRAFT);
    assertThat(originalNameQuestion.getQuestionTags()).isEmpty();

    ImmutableMap.Builder<String, String> formData = ImmutableMap.builder();
    formData
        .put("questionName", originalNameQuestion.getQuestionDefinition().getName())
        .put("questionDescription", "a new description")
        .put("questionType", originalNameQuestion.getQuestionDefinition().getQuestionType().name())
        .put("questionText", "question text updated")
        .put("questionHelpText", "a new help text")
        .put("questionExportState", "DEMOGRAPHIC_PII");
    RequestBuilder requestBuilder =
        addCSRFToken(requestBuilderWithSettings().bodyForm(formData.build()));

    Result result =
        controller.update(
            requestBuilder.build(),
            originalNameQuestion.getQuestionDefinition().getId(),
            originalNameQuestion.getQuestionDefinition().getQuestionType().toString());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    assertThat(result.redirectLocation()).hasValue(routes.AdminQuestionController.index().url());
    assertThat(result.flash().get("success").get()).contains("updated");

    QuestionModel updatedNameQuestion =
        questionRepo
            .lookupQuestion(originalNameQuestion.getQuestionDefinition().getId())
            .toCompletableFuture()
            .join()
            .get();

    assertThat(updatedNameQuestion.getQuestionDefinition().getDescription())
        .isEqualTo("a new description");
    assertThat(updatedNameQuestion.getQuestionTags())
        .isEqualTo(ImmutableList.of(QuestionTag.DEMOGRAPHIC_PII));
  }

  @Test
  public void update_setsIdsAsExpected() throws Exception {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("applicant ice cream")
            .setDescription("Select your favorite ice cream flavor")
            .setQuestionText(
                LocalizedStrings.of(Locale.US, "Ice cream?", Locale.FRENCH, "crème glacée?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help", Locale.FRENCH, "aider"))
            .build();
    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(
                1L,
                "chocolate_admin",
                LocalizedStrings.of(Locale.US, "chocolate", Locale.FRENCH, "chocolat")),
            QuestionOption.create(
                2L,
                "strawberry_admin",
                LocalizedStrings.of(Locale.US, "strawberry", Locale.FRENCH, "fraise")),
            QuestionOption.create(
                3L,
                "vanilla_admin",
                LocalizedStrings.of(Locale.US, "vanilla", Locale.FRENCH, "vanille")),
            QuestionOption.create(
                4L,
                "coffee_admin",
                LocalizedStrings.of(Locale.US, "coffee", Locale.FRENCH, "café")));
    MultiOptionQuestionDefinition definition =
        new MultiOptionQuestionDefinition(
            config, questionOptions, MultiOptionQuestionType.DROPDOWN);

    // We can only update draft questions, so save this in the DRAFT version.
    testQuestionBank.maybeSave(definition, LifecycleStage.DRAFT);

    DropdownQuestionForm questionForm = new DropdownQuestionForm(definition);
    questionForm.setNewOptions(ImmutableList.of("cookie", "mint", "pistachio"));
    questionForm.setNewOptionAdminNames(
        ImmutableList.of("cookie_admin", "mint_admin", "pistachio_admin"));

    DropdownQuestionForm newQuestionForm =
        new DropdownQuestionForm((MultiOptionQuestionDefinition) questionForm.getBuilder().build());

    assertThat(newQuestionForm.getOptionIds().get(4)).isEqualTo(5L);
    assertThat(newQuestionForm.getOptionIds().get(5)).isEqualTo(6L);
    assertThat(newQuestionForm.getOptionIds().get(6)).isEqualTo(7L);
    assertThat(newQuestionForm.getNextAvailableId()).isPresent();
    assertThat(newQuestionForm.getNextAvailableId().getAsLong()).isEqualTo(8L);
  }

  @Test
  public void update_mergesTranslations() {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("applicant ice cream")
            .setDescription("Select your favorite ice cream flavor")
            .setQuestionText(
                LocalizedStrings.of(Locale.US, "Ice cream?", Locale.FRENCH, "crème glacée?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help", Locale.FRENCH, "aider"))
            .build();

    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(
                /* id= */ 1L,
                "chocolate_admin",
                LocalizedStrings.of(Locale.US, "chocolate", Locale.FRENCH, "chocolat")),
            QuestionOption.create(
                /* id= */ 2L,
                "strawberry_admin",
                LocalizedStrings.of(Locale.US, "strawberry", Locale.FRENCH, "fraise")),
            QuestionOption.create(
                /* id= */ 3L,
                "vanilla_admin",
                LocalizedStrings.of(Locale.US, "vanilla", Locale.FRENCH, "vanille")),
            QuestionOption.create(
                /* id= */ 4L,
                "coffee_admin",
                LocalizedStrings.of(Locale.US, "coffee", Locale.FRENCH, "café")));

    QuestionDefinition definition =
        new MultiOptionQuestionDefinition(
            config, questionOptions, MultiOptionQuestionType.DROPDOWN);

    // We can only update draft questions, so save this in the DRAFT version.
    QuestionModel question = testQuestionBank.maybeSave(definition, LifecycleStage.DRAFT);

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
            .put("optionAdminNames[0]", "coffee_admin")
            .put("optionAdminNames[1]", "vanilla_admin")
            .put("newOptionAdminNames[0]", "lavender_admin")
            .put("nextAvailableId", "5")
            .put("questionExportState", "NON_DEMOGRAPHIC")
            // Has one fewer than the original question
            .build();
    RequestBuilder requestBuilder = addCSRFToken(requestBuilderWithSettings().bodyForm(formData));

    Result result =
        controller.update(
            requestBuilder.build(), question.id, definition.getQuestionType().toString());

    assertThat(result.status()).isEqualTo(SEE_OTHER);
    QuestionModel found =
        questionRepo.lookupQuestion(question.id).toCompletableFuture().join().get();

    assertThat(found.getQuestionDefinition().getQuestionText().translations())
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap.of(Locale.US, "new question text", Locale.FRENCH, "crème glacée?"));
    assertThat(found.getQuestionDefinition().getQuestionHelpText().translations())
        .containsExactlyInAnyOrderEntriesOf(
            ImmutableMap.of(Locale.US, "new help text", Locale.FRENCH, "aider"));

    ImmutableList<QuestionOption> expectedOptions =
        ImmutableList.of(
            QuestionOption.create(
                /* id= */ 4,
                /* displayOrder= */ 0,
                "coffee_admin",
                LocalizedStrings.of(Locale.US, "coffee", Locale.FRENCH, "café")),
            QuestionOption.create(
                /* id= */ 3,
                /* displayOrder= */ 1,
                "vanilla_admin",
                LocalizedStrings.of(Locale.US, "vanilla", Locale.FRENCH, "vanille")),
            QuestionOption.create(
                /* id= */ 5,
                /* displayOrder= */ 2,
                "lavender_admin",
                LocalizedStrings.withDefaultValue("lavender")));
    assertThat(((MultiOptionQuestionDefinition) found.getQuestionDefinition()).getOptions())
        .isEqualTo(expectedOptions);

    DropdownQuestionForm questionForm =
        new DropdownQuestionForm((MultiOptionQuestionDefinition) definition);
    questionForm.getBuilder();

    assertThat(questionForm.getNextAvailableId()).isPresent();
    assertThat(questionForm.getNextAvailableId().getAsLong()).isEqualTo(5L);
  }

  @Test
  public void update_ignoresChangedAdminNameForExistingOptions() {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("applicant ice cream")
            .setDescription("Select your favorite ice cream flavor")
            .setQuestionText(
                LocalizedStrings.of(Locale.US, "Ice cream?", Locale.FRENCH, "crème glacée?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help", Locale.FRENCH, "aider"))
            .build();

    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(
                /* id= */ 1L,
                "chocolate_admin",
                LocalizedStrings.of(Locale.US, "chocolate", Locale.FRENCH, "chocolat")),
            QuestionOption.create(
                /* id= */ 2L,
                "strawberry_admin",
                LocalizedStrings.of(Locale.US, "strawberry", Locale.FRENCH, "fraise")));

    QuestionDefinition definition =
        new MultiOptionQuestionDefinition(
            config, questionOptions, MultiOptionQuestionType.DROPDOWN);

    // We can only update draft questions, so save this in the DRAFT version.
    QuestionModel question = testQuestionBank.maybeSave(definition, LifecycleStage.DRAFT);

    ImmutableMap<String, String> formData =
        ImmutableMap.<String, String>builder()
            .put("questionName", definition.getName())
            .put("questionDescription", definition.getDescription())
            .put("questionType", definition.getQuestionType().name())
            .put("questionText", "new question text")
            .put("questionHelpText", "new help text")
            .put("options[0]", "chocolate")
            .put("options[1]", "strawberry")
            .put("optionIds[0]", "1")
            .put("optionIds[1]", "2")
            .put("optionAdminNames[0]", "chocolate_admin")
            .put("optionAdminNames[1]", "strawberry_new_admin_name") // updated admin name
            .put("nextAvailableId", "3")
            .put("questionExportState", "NON_DEMOGRAPHIC")
            .build();
    RequestBuilder requestBuilder = addCSRFToken(requestBuilderWithSettings().bodyForm(formData));

    controller.update(requestBuilder.build(), question.id, definition.getQuestionType().toString());
    QuestionModel found =
        questionRepo.lookupQuestion(question.id).toCompletableFuture().join().get();

    ImmutableList<QuestionOption> expectedOptions =
        ImmutableList.of(
            QuestionOption.create(
                /* id= */ 1,
                /* displayOrder= */ 0,
                "chocolate_admin",
                LocalizedStrings.of(Locale.US, "chocolate", Locale.FRENCH, "chocolat")),
            QuestionOption.create(
                /* id= */ 2,
                /* displayOrder= */ 1,
                "strawberry_admin", // ignore changed admin name
                LocalizedStrings.of(Locale.US, "strawberry", Locale.FRENCH, "fraise")));
    assertThat(((MultiOptionQuestionDefinition) found.getQuestionDefinition()).getOptions())
        .isEqualTo(expectedOptions);
  }

  @Test
  public void update_clearsTranslationsAndKeepsAdminName_ifDefaultTextIsChanged() {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("applicant ice cream")
            .setDescription("Select your favorite ice cream flavor")
            .setQuestionText(
                LocalizedStrings.of(Locale.US, "Ice cream?", Locale.FRENCH, "crème glacée?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help", Locale.FRENCH, "aider"))
            .build();

    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(
                /* id= */ 1L,
                "chocolate_admin",
                LocalizedStrings.of(Locale.US, "chocolate", Locale.FRENCH, "chocolat")),
            QuestionOption.create(
                /* id= */ 2L,
                "strawberry_admin",
                LocalizedStrings.of(Locale.US, "strawberry", Locale.FRENCH, "fraise")));

    QuestionDefinition definition =
        new MultiOptionQuestionDefinition(
            config, questionOptions, MultiOptionQuestionType.DROPDOWN);

    // We can only update draft questions, so save this in the DRAFT version.
    QuestionModel question = testQuestionBank.maybeSave(definition, LifecycleStage.DRAFT);

    ImmutableMap<String, String> formData =
        ImmutableMap.<String, String>builder()
            .put("questionName", definition.getName())
            .put("questionDescription", definition.getDescription())
            .put("questionType", definition.getQuestionType().name())
            .put("questionText", "new question text")
            .put("questionHelpText", "new help text")
            .put("options[0]", "chocolate")
            .put("options[1]", "new ice cream name")
            .put("optionIds[0]", "1")
            .put("optionIds[1]", "2")
            .put("optionAdminNames[0]", "chocolate_admin")
            .put("optionAdminNames[1]", "new_admin_name") // updated admin name
            .put("nextAvailableId", "3")
            .put("questionExportState", "NON_DEMOGRAPHIC")
            .build();
    RequestBuilder requestBuilder = addCSRFToken(requestBuilderWithSettings().bodyForm(formData));

    controller.update(requestBuilder.build(), question.id, definition.getQuestionType().toString());
    QuestionModel found =
        questionRepo.lookupQuestion(question.id).toCompletableFuture().join().get();

    ImmutableList<QuestionOption> expectedOptions =
        ImmutableList.of(
            QuestionOption.create(
                /* id= */ 1,
                /* displayOrder= */ 0,
                "chocolate_admin",
                LocalizedStrings.of(Locale.US, "chocolate", Locale.FRENCH, "chocolat")),
            QuestionOption.create(
                /* id= */ 2,
                /* displayOrder= */ 1,
                "strawberry_admin", // ignore changed admin name
                LocalizedStrings.of(Locale.US, "new ice cream name"))); // clear other locales
    assertThat(((MultiOptionQuestionDefinition) found.getQuestionDefinition()).getOptions())
        .isEqualTo(expectedOptions);
  }

  @Test
  public void update_correctlyIncrementsId_ifOptionIsRemoved() {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("applicant ice cream")
            .setDescription("Select your favorite ice cream flavor")
            .setQuestionText(
                LocalizedStrings.of(Locale.US, "Ice cream?", Locale.FRENCH, "crème glacée?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help", Locale.FRENCH, "aider"))
            .build();

    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(
                /* id= */ 1L,
                "chocolate_admin",
                LocalizedStrings.of(Locale.US, "chocolate", Locale.FRENCH, "chocolat")),
            QuestionOption.create(
                /* id= */ 2L,
                "strawberry_admin",
                LocalizedStrings.of(Locale.US, "strawberry", Locale.FRENCH, "fraise")));

    QuestionDefinition definition =
        new MultiOptionQuestionDefinition(
            config, questionOptions, MultiOptionQuestionType.DROPDOWN);

    // We can only update draft questions, so save this in the DRAFT version.
    QuestionModel question = testQuestionBank.maybeSave(definition, LifecycleStage.DRAFT);

    ImmutableMap<String, String> formData =
        ImmutableMap.<String, String>builder()
            .put("questionName", definition.getName())
            .put("questionDescription", definition.getDescription())
            .put("questionType", definition.getQuestionType().name())
            .put("questionText", "new question text")
            .put("questionHelpText", "new help text")
            .put("options[0]", "chocolate")
            .put("optionIds[0]", "1")
            .put("optionAdminNames[0]", "chocolate_admin")
            // id 2 was deleted by the user, but 3 is still set as the next ID by the
            // QuestionConfig
            .put("nextAvailableId", "3")
            .put("newOptions[0]", "lavender") // New flavor
            .put("newOptionAdminNames[0]", "lavender_admin")
            .put("questionExportState", "NON_DEMOGRAPHIC")
            .build();
    RequestBuilder requestBuilder = addCSRFToken(requestBuilderWithSettings().bodyForm(formData));

    controller.update(requestBuilder.build(), question.id, definition.getQuestionType().toString());
    QuestionModel found =
        questionRepo.lookupQuestion(question.id).toCompletableFuture().join().get();

    ImmutableList<QuestionOption> expectedOptions =
        ImmutableList.of(
            QuestionOption.create(
                /* id= */ 1,
                /* displayOrder= */ 0,
                "chocolate_admin",
                LocalizedStrings.of(Locale.US, "chocolate", Locale.FRENCH, "chocolat")),
            QuestionOption.create(
                /* id= */ 3, // use nextAvailableId
                /* displayOrder= */ 1,
                "lavender_admin",
                LocalizedStrings.of(Locale.US, "lavender")));
    assertThat(((MultiOptionQuestionDefinition) found.getQuestionDefinition()).getOptions())
        .isEqualTo(expectedOptions);
  }

  @Test
  public void update_failsWithErrorMessageAndPopulatedFields() {
    QuestionModel question = testQuestionBank.applicantFavoriteColor();
    ImmutableMap.Builder<String, String> formData = ImmutableMap.builder();
    formData
        .put("questionName", "favorite_color")
        .put("questionDescription", "")
        .put("questionText", "question text updated!");
    Request request = addCSRFToken(requestBuilderWithSettings().bodyForm(formData.build())).build();

    Result result = controller.update(request, question.id, "text");

    assertThat(result.status()).isEqualTo(OK);
    assertThat(contentAsString(result)).contains("Edit text question");
    assertThat(contentAsString(result)).contains(CSRF.getToken(request.asScala()).value());
    assertThat(contentAsString(result)).contains("question text updated!");
  }

  @Test
  public void update_failsWithInvalidQuestionType() {
    QuestionModel question = testQuestionBank.applicantHouseholdMembers();
    ImmutableMap.Builder<String, String> formData = ImmutableMap.builder();
    formData.put("questionType", "INVALID_TYPE").put("questionText", "question text updated!");
    RequestBuilder requestBuilder = requestBuilderWithSettings().bodyForm(formData.build());

    Result result = controller.update(requestBuilder.build(), question.id, "invalid_type");

    assertThat(result.status()).isEqualTo(BAD_REQUEST);
  }

  private NameQuestionDefinition createNameQuestionDuplicate(QuestionModel question) {
    QuestionDefinition def = question.getQuestionDefinition();
    return new NameQuestionDefinition(
        QuestionDefinitionConfig.builder()
            .setName(def.getName())
            .setDescription(def.getDescription())
            .setQuestionText(def.getQuestionText())
            .setQuestionHelpText(def.getQuestionHelpText())
            .build());
  }
}
