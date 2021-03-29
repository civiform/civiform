package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import controllers.CiviFormController;
import forms.QuestionForm;
import forms.TextQuestionForm;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.Form;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.CiviFormError;
import services.ErrorAnd;
import services.question.InvalidQuestionTypeException;
import services.question.InvalidUpdateException;
import services.question.QuestionDefinition;
import services.question.QuestionNotFoundException;
import services.question.QuestionService;
import services.question.QuestionType;
import services.question.UnsupportedQuestionTypeException;
import views.admin.questions.QuestionEditView;
import views.admin.questions.QuestionsListView;

public class QuestionController extends CiviFormController {
  private final QuestionService service;
  private final QuestionsListView listView;
  private final QuestionEditView editView;
  private final FormFactory formFactory;
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  public QuestionController(
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
              ok(listView.render(readOnlyService.getAllQuestions(), maybeFlash))
            ,
            httpExecutionContext.current());
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result newOne(Request request, String type) {
    String upperType = type.toUpperCase();
    try {
      QuestionType questionType = QuestionType.valueOf(upperType.toUpperCase());
      return ok(editView.renderNewQuestionForm(request, questionType));
    } catch (IllegalArgumentException e) {
      return badRequest(
          String.format(
              "unrecognized question type: '%s', accepted values include: %s",
              upperType, Arrays.toString(QuestionType.values())));
    }
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result create(Request request, String questionType) {
    ErrorAnd<QuestionForm, String> errorAndQuestionForm = createQuestionForm(request, questionType);
    if (errorAndQuestionForm.isError()) {
      // Invalid question type.
      return badRequest(Iterables.getOnlyElement(errorAndQuestionForm.getErrors()));
    }

    QuestionForm questionForm = errorAndQuestionForm.getResult();

    QuestionDefinition questionDefinition;
    try {
      questionDefinition = questionForm.getBuilder().setVersion(1L).build();
    } catch (UnsupportedQuestionTypeException e) {
      // Valid question type that is not yet fully supported.
      return badRequest(e.toString());
    }

    ErrorAnd<QuestionDefinition, CiviFormError> result = service.create(questionDefinition);
    if (result.isError()) {
      String errorMessage = joinErrors(result.getErrors());
      return ok(editView.renderNewQuestionForm(request, questionForm, errorMessage));
    }

    String successMessage =
        String.format("question %s created", questionForm.getQuestionPath().path());
    return withMessage(redirect(routes.QuestionController.index()), successMessage);
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public CompletionStage<Result> edit(Request request, Long id) {
    return service
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            readOnlyService -> {
              try {
                QuestionDefinition questionDefinition = readOnlyService.getQuestionDefinition(id);
                return ok(editView.renderEditQuestionForm(request, questionDefinition));
              } catch (QuestionNotFoundException e) {
                return badRequest(e.toString());
              }
            },
            httpExecutionContext.current());
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result update(Request request, Long id, String questionType) {
    ErrorAnd<QuestionForm, String> errorAndQuestionForm = createQuestionForm(request, questionType);
    if (errorAndQuestionForm.isError()) {
      // Invalid question type.
      return badRequest(Iterables.getOnlyElement(errorAndQuestionForm.getErrors()));
    }

    QuestionForm questionForm = errorAndQuestionForm.getResult();

    QuestionDefinition questionDefinition;
    try {
      questionDefinition = questionForm.getBuilder().setId(id).setVersion(1L).build();
    } catch (UnsupportedQuestionTypeException e) {
      // Valid question type that is not yet fully supported.
      String errorMessage = e.toString();
      return ok(editView.renderEditQuestionForm(request, id, questionForm, errorMessage));
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
      return ok(editView.renderEditQuestionForm(request, id, questionForm, errorMessage));
    }

    String successMessage =
        String.format("question %s updated", questionForm.getQuestionPath().path());
    return withMessage(redirect(routes.QuestionController.index()), successMessage);
  }

  private Result withMessage(Result result, String message) {
    if (!message.isEmpty()) {
      return result.flashing("message", message);
    }
    return result;
  }

  private ErrorAnd<QuestionForm, String> createQuestionForm(Request request, String type) {
    QuestionType questionType;
    try {
      questionType = QuestionType.of(type);
    } catch (InvalidQuestionTypeException e) {
      // TODO: Return ErrorAnd of InvalidQuestionTypeException instead of String.
      return ErrorAnd.error(
          ImmutableSet.of(
              String.format(
                  "unrecognized question type: '%s', accepted values include: %s",
                  type.toUpperCase(), Arrays.toString(QuestionType.values()))));
    }

    switch (questionType) {
      case TEXT:
        {
          Form<TextQuestionForm> form = formFactory.form(TextQuestionForm.class);
          return ErrorAnd.of(form.bindFromRequest(request).get());
        }
      default:
        {
          Form<QuestionForm> form = formFactory.form(QuestionForm.class);
          return ErrorAnd.of(form.bindFromRequest(request).get());
        }
    }
  }
}
