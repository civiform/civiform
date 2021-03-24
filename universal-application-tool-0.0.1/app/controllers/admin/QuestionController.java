package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import controllers.CiviFormController;
import forms.QuestionForm;
import forms.TextQuestionForm;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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
  final Logger LOG = LoggerFactory.getLogger(this.getClass());

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
  public CompletionStage<Result> create(Request request, String type) {
    QuestionType questionType;
    try {
      questionType = QuestionType.valueOf(type.toUpperCase());
    } catch (IllegalArgumentException e) {
      return CompletableFuture.completedFuture(
          badRequest(
              String.format(
                  "unrecognized question type: '%s', accepted values include: %s",
                  type.toUpperCase(), Arrays.toString(QuestionType.values()))));
    }

    QuestionForm questionForm;
    switch (questionType) {
      case TEXT:
        {
          Form<TextQuestionForm> form = formFactory.form(TextQuestionForm.class);
          questionForm = form.bindFromRequest(request).get();
          break;
        }
      default:
        {
          Form<QuestionForm> form = formFactory.form(QuestionForm.class);
          questionForm = form.bindFromRequest(request).get();
        }
    }

    return service
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            readOnlyService -> {
              try {
                try {
                  QuestionDefinition questionDefinition =
                      questionForm.getBuilder().setVersion(1L).build();
                  ErrorAnd<QuestionDefinition, CiviFormError> result =
                      service.create(questionDefinition);
                  if (result.isError()) {
                    String errorMessage = joinErrors(result.getErrors());
                    return ok(
                        editView.renderNewQuestionForm(
                            request, questionType, questionForm, errorMessage));
                  }
                } catch (UnsupportedQuestionTypeException e) {
                  // These are valid question types, but are not fully supported yet.
                  String errorMessage = e.toString();
                  return ok(
                      editView.renderNewQuestionForm(
                          request, questionType, questionForm, errorMessage));
                }
              } catch (InvalidQuestionTypeException e) {
                // These are unrecognized invalid question types.
                return badRequest(e.toString());
              }
              String successMessage =
                  String.format("question %s created", questionForm.getQuestionPath().path());
              return withMessage(redirect(routes.QuestionController.index()), successMessage);
            },
            httpExecutionContext.current());
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
  public Result newOne(Request request, String type) {
    try {
      QuestionType questionType = QuestionType.valueOf(type.toUpperCase());
      return ok(editView.renderNewQuestionForm(request, questionType));
    } catch (IllegalArgumentException e) {
      return badRequest(
          String.format(
              "unrecognized question type: '%s', accepted values include: %s",
              type.toUpperCase(), Arrays.toString(QuestionType.values())));
    }
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public CompletionStage<Result> index(Request request) {
    Optional<String> maybeFlash = request.flash().get("message");
    return service
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            readOnlyService -> {
              return ok(listView.render(readOnlyService.getAllQuestions(), maybeFlash));
            },
            httpExecutionContext.current());
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result update(Request request, Long id) {
    // TODO: Need to get the question here so we can get the question type and use the appropriate
    //  form.
    Form<QuestionForm> form = formFactory.form(QuestionForm.class);
    QuestionForm questionForm = form.bindFromRequest(request).get();
    try {
      try {
        QuestionDefinition questionDefinition =
            questionForm.getBuilder().setId(id).setVersion(1L).build();
        ErrorAnd<QuestionDefinition, CiviFormError> result = service.update(questionDefinition);
        if (result.isError()) {
          String errorMessage = joinErrors(result.getErrors());
          return ok(editView.renderEditQuestionForm(request, id, questionForm, errorMessage));
        }
      } catch (UnsupportedQuestionTypeException e) {
        // These are valid question types, but are not fully supported yet.
        String errorMessage = e.toString();
        return ok(editView.renderEditQuestionForm(request, id, questionForm, errorMessage));
      } catch (InvalidUpdateException e) {
        // Ill-formed update request
        return badRequest(e.toString());
      }
    } catch (InvalidQuestionTypeException e) {
      // These are unrecognized invalid question types.
      return badRequest(e.toString());
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
}
