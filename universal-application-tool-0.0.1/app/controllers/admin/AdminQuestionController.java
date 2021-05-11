package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import forms.QuestionForm;
import forms.QuestionFormBuilder;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.CiviFormError;
import services.ErrorAnd;
import services.LocalizedStrings;
import services.Path;
import services.question.QuestionService;
import services.question.ReadOnlyQuestionService;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.InvalidUpdateException;
import services.question.exceptions.QuestionNotFoundException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;
import views.admin.questions.QuestionEditView;
import views.admin.questions.QuestionsListView;

public class AdminQuestionController extends CiviFormController {
  private final QuestionService service;
  private final QuestionsListView listView;
  private final QuestionEditView editView;
  private final FormFactory formFactory;
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  public AdminQuestionController(
      QuestionService service,
      QuestionsListView listView,
      QuestionEditView editView,
      FormFactory formFactory,
      HttpExecutionContext httpExecutionContext) {
    this.service = checkNotNull(service);
    this.listView = checkNotNull(listView);
    this.editView = checkNotNull(editView);
    this.formFactory = checkNotNull(formFactory);
    this.httpExecutionContext = checkNotNull(httpExecutionContext);
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public CompletionStage<Result> index(Request request) {
    Optional<String> maybeFlash = request.flash().get("message");
    return service
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            readOnlyService ->
                ok(listView.render(readOnlyService.getActiveAndDraftQuestions(), maybeFlash)),
            httpExecutionContext.current());
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public CompletionStage<Result> show(long id) {
    return service
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            readOnlyService -> {
              QuestionDefinition questionDefinition;
              try {
                questionDefinition = readOnlyService.getQuestionDefinition(id);
              } catch (QuestionNotFoundException e) {
                return badRequest(e.toString());
              }

              Optional<QuestionDefinition> maybeEnumerationQuestion =
                  maybeGetEnumerationQuestion(readOnlyService, questionDefinition);
              try {
                return ok(
                    editView.renderViewQuestionForm(questionDefinition, maybeEnumerationQuestion));
              } catch (InvalidQuestionTypeException e) {
                return badRequest(
                    invalidQuestionTypeMessage(questionDefinition.getQuestionType().toString()));
              }
            },
            httpExecutionContext.current());
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result newOne(Request request, String type) {
    QuestionType questionType;
    try {
      questionType = QuestionType.of(type);
    } catch (InvalidQuestionTypeException e) {
      return badRequest(invalidQuestionTypeMessage(type));
    }

    ImmutableList<EnumeratorQuestionDefinition> enumeratorQuestionDefinitions =
        service
            .getReadOnlyQuestionService()
            .toCompletableFuture()
            .join()
            .getUpToDateEnumeratorQuestions();

    try {
      return ok(
          editView.renderNewQuestionForm(request, questionType, enumeratorQuestionDefinitions));
    } catch (UnsupportedQuestionTypeException e) {
      return badRequest(e.getMessage());
    }
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result create(Request request, String questionType) {
    QuestionForm questionForm;
    try {
      questionForm =
          QuestionFormBuilder.createFromRequest(
              request, formFactory, QuestionType.of(questionType));
    } catch (InvalidQuestionTypeException e) {
      return badRequest(invalidQuestionTypeMessage(questionType));
    }

    ReadOnlyQuestionService roService =
        service.getReadOnlyQuestionService().toCompletableFuture().join();

    QuestionDefinition questionDefinition;
    try {
      questionDefinition =
          getBuilderWithQuestionPath(roService, Optional.empty(), questionForm).build();
    } catch (UnsupportedQuestionTypeException e) {
      // Valid question type that is not yet fully supported.
      return badRequest(e.getMessage());
    }

    ErrorAnd<QuestionDefinition, CiviFormError> result = service.create(questionDefinition);
    if (result.isError()) {
      String errorMessage = joinErrors(result.getErrors());
      ImmutableList<EnumeratorQuestionDefinition> enumeratorQuestionDefinitions =
          roService.getUpToDateEnumeratorQuestions();
      return ok(
          editView.renderNewQuestionForm(
              request, questionForm, enumeratorQuestionDefinitions, errorMessage));
    }

    String successMessage = String.format("question %s created", questionForm.getQuestionName());
    return withMessage(redirect(routes.AdminQuestionController.index()), successMessage);
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public CompletionStage<Result> edit(Request request, Long id) {
    return service
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            readOnlyService -> {
              QuestionDefinition questionDefinition;
              try {
                questionDefinition = readOnlyService.getQuestionDefinition(id);
              } catch (QuestionNotFoundException e) {
                return badRequest(e.toString());
              }

              Optional<QuestionDefinition> maybeEnumerationQuestion =
                  maybeGetEnumerationQuestion(readOnlyService, questionDefinition);
              try {
                return ok(
                    editView.renderEditQuestionForm(
                        request, questionDefinition, maybeEnumerationQuestion));
              } catch (InvalidQuestionTypeException e) {
                return badRequest(
                    invalidQuestionTypeMessage(questionDefinition.getQuestionType().toString()));
              }
            },
            httpExecutionContext.current());
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result update(Request request, Long id, String questionType) {
    QuestionForm questionForm;
    try {
      questionForm =
          QuestionFormBuilder.createFromRequest(
              request, formFactory, QuestionType.of(questionType));
    } catch (InvalidQuestionTypeException e) {
      return badRequest(invalidQuestionTypeMessage(questionType));
    }

    ReadOnlyQuestionService roService =
        service.getReadOnlyQuestionService().toCompletableFuture().join();

    Optional<QuestionDefinition> maybeExisting;
    try {
      maybeExisting = Optional.of(roService.getQuestionDefinition(id));
    } catch (QuestionNotFoundException e) {
      maybeExisting = Optional.empty();
    }

    QuestionDefinition questionDefinition;
    try {
      questionDefinition =
          getBuilderWithQuestionPath(roService, maybeExisting, questionForm).setId(id).build();
    } catch (UnsupportedQuestionTypeException e) {
      // Failed while trying to update a question that was already created for the given question
      // type
      return badRequest(e.getMessage());
    }

    ErrorAnd<QuestionDefinition, CiviFormError> errorAndUpdatedQuestionDefinition;
    try {
      errorAndUpdatedQuestionDefinition = service.update(questionDefinition);
    } catch (InvalidUpdateException e) {
      // Ill-formed update request.
      return badRequest(e.toString());
    }

    if (errorAndUpdatedQuestionDefinition.isError()) {
      String errorMessage = joinErrors(errorAndUpdatedQuestionDefinition.getErrors());
      Optional<QuestionDefinition> maybeEnumerationQuestion =
          maybeGetEnumerationQuestion(roService, questionDefinition);
      return ok(
          editView.renderEditQuestionForm(
              request, id, questionForm, maybeEnumerationQuestion, errorMessage));
    }

    String successMessage = String.format("question %s updated", questionForm.getQuestionName());
    return withMessage(redirect(routes.AdminQuestionController.index()), successMessage);
  }

  private Result withMessage(Result result, String message) {
    if (!message.isEmpty()) {
      return result.flashing("message", message);
    }
    return result;
  }

  private QuestionDefinitionBuilder getBuilderWithQuestionPath(
      ReadOnlyQuestionService roService,
      Optional<QuestionDefinition> existing,
      QuestionForm questionForm) {
    try {
      Path path =
          roService.makePath(
              questionForm.getEnumeratorId(),
              questionForm.getQuestionName(),
              questionForm.getQuestionType().equals(QuestionType.ENUMERATOR));
      QuestionDefinitionBuilder updated = questionForm.getBuilder(path);

      if (existing.isPresent()) {
        updated = mergeLocalizations(existing.get(), updated, questionForm);
      }

      return updated;
    } catch (QuestionNotFoundException | InvalidQuestionTypeException e) {
      throw new RuntimeException(
          String.format(
              "Failed to create a question definition builder because of invalid enumerator id"
                  + " reference: %s",
              questionForm),
          e);
    }
  }

  private QuestionDefinitionBuilder mergeLocalizations(
      QuestionDefinition existing, QuestionDefinitionBuilder updated, QuestionForm questionForm) {
    // Instead of overwriting all localizations, we just want to overwrite the one
    // for the default locale (the only one possible to change in the edit form).
    updated.setQuestionText(
        existing
            .getQuestionText()
            .updateTranslation(LocalizedStrings.DEFAULT_LOCALE, questionForm.getQuestionText()));
    updated.setQuestionHelpText(
        existing
            .getQuestionHelpText()
            .updateTranslation(
                LocalizedStrings.DEFAULT_LOCALE, questionForm.getQuestionHelpText()));

    return updated;
  }

  private String invalidQuestionTypeMessage(String questionType) {
    return String.format(
        "unrecognized question type: '%s', accepted values include: %s",
        questionType.toUpperCase(), Arrays.toString(QuestionType.values()));
  }

  /**
   * Maybe return the name of the question definition's enumerator question, if it is a repeated
   * question definition.
   */
  private Optional<QuestionDefinition> maybeGetEnumerationQuestion(
      ReadOnlyQuestionService readOnlyQuestionService, QuestionDefinition questionDefinition) {
    return questionDefinition
        .getEnumeratorId()
        .flatMap(
            enumeratorId -> {
              try {
                return Optional.of(readOnlyQuestionService.getQuestionDefinition(enumeratorId));
              } catch (QuestionNotFoundException e) {
                throw new RuntimeException(
                    "This repeated question's enumerator id reference does not refer to a real"
                        + " question!");
              }
            });
  }
}
