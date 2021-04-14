package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import controllers.CiviFormController;
import forms.AddressQuestionForm;
import forms.CheckboxQuestionForm;
import forms.DropdownQuestionForm;
import forms.NumberQuestionForm;
import forms.QuestionForm;
import forms.RadioButtonQuestionForm;
import forms.TextQuestionForm;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.LifecycleStage;
import org.pac4j.play.java.Secure;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.CiviFormError;
import services.ErrorAnd;
import services.question.QuestionService;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.InvalidUpdateException;
import services.question.exceptions.QuestionNotFoundException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import views.admin.questions.QuestionEditView;
import views.admin.questions.QuestionsListView;

public class QuestionController extends CiviFormController {
  private static final long NEW_VERSION = 1L;
  private static final long VERSION_PLACEHOLDER = 1L;

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
            readOnlyService -> ok(listView.render(readOnlyService.getAllQuestions(), maybeFlash)),
            httpExecutionContext.current());
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public CompletionStage<Result> show(Request request, Long id) {
    return service
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            readOnlyService -> {
              try {
                QuestionDefinition definition = readOnlyService.getQuestionDefinition(id);
                return ok(editView.renderViewQuestionForm(request, definition));
              } catch (QuestionNotFoundException e) {
                return badRequest(e.toString());
              }
            },
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
    QuestionForm questionForm;
    try {
      questionForm = createQuestionForm(request, questionType);
    } catch (InvalidQuestionTypeException e) {
      // Invalid question type.
      return badRequest(e.toString());
    }

    QuestionDefinition questionDefinition;
    try {
      questionDefinition =
          questionForm
              .getBuilder()
              .setVersion(NEW_VERSION)
              .setLifecycleStage(LifecycleStage.DRAFT)
              .build();
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
    QuestionForm questionForm;
    try {
      questionForm = createQuestionForm(request, questionType);
    } catch (InvalidQuestionTypeException e) {
      // Invalid question type.
      return badRequest(e.toString());
    }

    QuestionDefinition questionDefinition;
    try {
      questionDefinition =
          questionForm
              .getBuilder()
              .setId(id)
              // Version is needed for building a question definition.
              // This value is overwritten when updating the question.
              .setVersion(VERSION_PLACEHOLDER)
              .setLifecycleStage(LifecycleStage.DRAFT)
              .build();
    } catch (UnsupportedQuestionTypeException e) {
      throw new RuntimeException(
          "Failed while trying to update a question that was already created for question type "
              + questionForm.getQuestionType());
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

  private QuestionForm createQuestionForm(Request request, String type)
      throws InvalidQuestionTypeException {
    QuestionType questionType;
    try {
      questionType = QuestionType.of(type);
    } catch (InvalidQuestionTypeException e) {
      throw new InvalidQuestionTypeException(
          String.format(
              "unrecognized question type: '%s', accepted values include: %s",
              type.toUpperCase(), Arrays.toString(QuestionType.values())));
    }

    switch (questionType) {
      case ADDRESS:
        return formFactory.form(AddressQuestionForm.class).bindFromRequest(request).get();
      case CHECKBOX:
        return formFactory.form(CheckboxQuestionForm.class).bindFromRequest(request).get();
      case DROPDOWN:
        return formFactory.form(DropdownQuestionForm.class).bindFromRequest(request).get();
      case NUMBER:
        return formFactory.form(NumberQuestionForm.class).bindFromRequest(request).get();
      case RADIO_BUTTON:
        return formFactory.form(RadioButtonQuestionForm.class).bindFromRequest(request).get();
      case TEXT:
        return formFactory.form(TextQuestionForm.class).bindFromRequest(request).get();
      default:
        // TODO(#589): Once QuestionForm is abstract, the default case should throw.
        return formFactory.form(QuestionForm.class).bindFromRequest(request).get();
    }
  }
}
