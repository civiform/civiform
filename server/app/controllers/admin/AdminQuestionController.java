package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import controllers.FlashKey;
import forms.EnumeratorQuestionForm;
import forms.MultiOptionQuestionForm;
import forms.QuestionForm;
import forms.QuestionFormBuilder;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.ConcurrentUpdateException;
import org.pac4j.play.java.Secure;
import play.data.FormFactory;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.VersionRepository;
import services.CiviFormError;
import services.ErrorAnd;
import services.LocalizedStrings;
import services.question.QuestionOption;
import services.question.QuestionService;
import services.question.ReadOnlyQuestionService;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.InvalidUpdateException;
import services.question.exceptions.QuestionNotFoundException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;
import views.admin.questions.QuestionEditView;
import views.admin.questions.QuestionsListView;
import views.components.ToastMessage;

/** Controller for handling methods for admins managing questions. */
public final class AdminQuestionController extends CiviFormController {
  private final QuestionService service;
  private final QuestionsListView listView;
  private final QuestionEditView editView;
  private final FormFactory formFactory;
  private final ClassLoaderExecutionContext classLoaderExecutionContext;

  @Inject
  public AdminQuestionController(
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      QuestionService service,
      QuestionsListView listView,
      QuestionEditView editView,
      FormFactory formFactory,
      ClassLoaderExecutionContext classLoaderExecutionContext) {
    super(profileUtils, versionRepository);
    this.service = checkNotNull(service);
    this.listView = checkNotNull(listView);
    this.editView = checkNotNull(editView);
    this.formFactory = checkNotNull(formFactory);
    this.classLoaderExecutionContext = checkNotNull(classLoaderExecutionContext);
  }

  /**
   * Return a HTML page displaying all questions of the current live version and all questions of
   * the current draft version if any.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> index(Request request) {
    return service
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            readOnlyService ->
                ok(listView.render(readOnlyService.getActiveAndDraftQuestions(), request)),
            classLoaderExecutionContext.current());
  }

  /**
   * Return a HTML page displaying all configurations of a question without the ability to update
   * it.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> show(Request request, long id) {
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
                    editView.renderViewQuestionForm(
                        request, questionDefinition, maybeEnumerationQuestion));
              } catch (InvalidQuestionTypeException e) {
                return badRequest(
                    invalidQuestionTypeMessage(questionDefinition.getQuestionType().toString()));
              }
            },
            classLoaderExecutionContext.current());
  }

  /** Return a HTML page containing a form to create a new question in the draft version. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result newOne(Request request, String type, String redirectUrl) {
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
          editView.renderNewQuestionForm(
              request, questionType, enumeratorQuestionDefinitions, redirectUrl));
    } catch (UnsupportedQuestionTypeException e) {
      return badRequest(e.getMessage());
    }
  }

  /** POST endpoint for creating a new question in the draft version. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result create(Request request, String questionType) {
    QuestionForm questionForm;
    try {
      questionForm =
          QuestionFormBuilder.createFromRequest(
              request, formFactory, QuestionType.of(questionType));
    } catch (InvalidQuestionTypeException e) {
      return badRequest(invalidQuestionTypeMessage(questionType));
    }

    QuestionDefinition questionDefinition;
    try {
      questionDefinition = questionForm.getBuilder().build();
    } catch (UnsupportedQuestionTypeException e) {
      // Valid question type that is not yet fully supported.
      return badRequest(e.getMessage());
    }

    ErrorAnd<QuestionDefinition, CiviFormError> result = service.create(questionDefinition);
    if (result.isError()) {
      ToastMessage errorMessage = ToastMessage.errorNonLocalized(joinErrors(result.getErrors()));
      ReadOnlyQuestionService roService =
          service.getReadOnlyQuestionService().toCompletableFuture().join();
      ImmutableList<EnumeratorQuestionDefinition> enumeratorQuestionDefinitions =
          roService.getUpToDateEnumeratorQuestions();
      return ok(
          editView.renderNewQuestionForm(
              request, questionForm, enumeratorQuestionDefinitions, errorMessage));
    }

    try {
      service.setExportState(result.getResult(), questionForm.getQuestionExportStateTag());
    } catch (InvalidUpdateException | QuestionNotFoundException e) {
      return badRequest(e.toString());
    }

    String successMessage = String.format("question %s created", questionForm.getQuestionName());
    String redirectUrl =
        questionForm.getRedirectUrl().isEmpty()
            ? routes.AdminQuestionController.index().url()
            : questionForm.getRedirectUrl();
    return withSuccessMessage(redirect(redirectUrl), successMessage);
  }

  /** POST endpoint for un-archiving a question. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result restore(Request request, Long id) {
    try {
      service.restoreQuestion(id);
    } catch (InvalidUpdateException e) {
      return badRequest("Failed to restore question.");
    }
    return redirect(routes.AdminQuestionController.index());
  }

  /** POST endpoint for archiving a question so it will not be carried over to a new version. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result archive(Request request, Long id) {
    try {
      service.archiveQuestion(id);
    } catch (InvalidUpdateException e) {
      return badRequest("Failed to archive question.");
    }
    return redirect(routes.AdminQuestionController.index());
  }

  /** POST endpoint for discarding a draft for a question. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result discardDraft(Request request, Long id) {
    try {
      service.discardDraft(id);
    } catch (InvalidUpdateException e) {
      return badRequest("Failed to discard draft question.");
    }
    return redirect(routes.AdminQuestionController.index());
  }

  /**
   * Return a HTML page containing all configurations of a question in the draft version and forms
   * to edit them.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
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

              // Handle case someone tries to edit a live question that already has a draft version.
              // In this case we should redirect to the draft version.
              Optional<QuestionDefinition> possibleDraft =
                  readOnlyService
                      .getActiveAndDraftQuestions()
                      .getDraftQuestionDefinition(questionDefinition.getName());
              if (possibleDraft.isPresent() && possibleDraft.get().getId() != id) {
                return redirect(routes.AdminQuestionController.edit(possibleDraft.get().getId()))
                    .flashing(request.flash().data());
              }

              Optional<QuestionDefinition> maybeEnumerationQuestion =
                  maybeGetEnumerationQuestion(readOnlyService, questionDefinition);
              try {
                Optional<ToastMessage> message =
                    request
                        .flash()
                        .get(FlashKey.CONCURRENT_UPDATE)
                        .map(m -> ToastMessage.errorNonLocalized(m));
                return ok(
                    editView.renderEditQuestionForm(
                        request, questionDefinition, maybeEnumerationQuestion, message));
              } catch (InvalidQuestionTypeException e) {
                return badRequest(
                    invalidQuestionTypeMessage(questionDefinition.getQuestionType().toString()));
              }
            },
            classLoaderExecutionContext.current());
  }

  /** POST endpoint for updating a question in the draft version. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
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
      questionDefinition = getBuilder(maybeExisting, questionForm).setId(id).build();
    } catch (UnsupportedQuestionTypeException e) {
      // Failed while trying to update a question that was already created for the given question
      // type
      return badRequest(e.getMessage());
    }

    ErrorAnd<QuestionDefinition, CiviFormError> errorAndUpdatedQuestionDefinition;
    try {
      errorAndUpdatedQuestionDefinition = service.update(maybeExisting, questionDefinition);
    } catch (InvalidUpdateException e) {
      // Ill-formed update request.
      return badRequest(e.toString());
    } catch (ConcurrentUpdateException e) {
      // If there was a concurrent update, load a fresh edit form so the admin sees the concurrently
      // made changes.
      return redirect(routes.AdminQuestionController.edit(id))
          .flashing(
              FlashKey.CONCURRENT_UPDATE,
              "The question was updated by another user while the edit page was open in your"
                  + " browser. Please try your edits again.");
    }

    if (errorAndUpdatedQuestionDefinition.isError()) {
      ToastMessage errorMessage =
          ToastMessage.errorNonLocalized(joinErrors(errorAndUpdatedQuestionDefinition.getErrors()));
      Optional<QuestionDefinition> maybeEnumerationQuestion =
          maybeGetEnumerationQuestion(roService, questionDefinition);
      return ok(
          editView.renderEditQuestionForm(
              request, id, questionForm, maybeEnumerationQuestion, errorMessage));
    }
    try {
      service.setExportState(
          errorAndUpdatedQuestionDefinition.getResult(), questionForm.getQuestionExportStateTag());
    } catch (InvalidUpdateException | QuestionNotFoundException e) {
      return badRequest(e.toString());
    }

    String successMessage = String.format("question %s updated", questionForm.getQuestionName());
    return withSuccessMessage(redirect(routes.AdminQuestionController.index()), successMessage);
  }

  private Result withSuccessMessage(Result result, String message) {
    if (!message.isEmpty()) {
      return result.flashing(FlashKey.SUCCESS, message);
    }
    return result;
  }

  private QuestionDefinitionBuilder getBuilder(
      Optional<QuestionDefinition> existing, QuestionForm questionForm) {
    QuestionDefinitionBuilder updated = questionForm.getBuilder();

    if (existing.isPresent()) {
      updateDefaultLocalizations(existing.get(), updated, questionForm);
    }

    return updated;
  }

  /**
   * The edit form can change the default locale's text - we want to only change the default locale
   * text, instead of overwriting all localizations.
   */
  private void updateDefaultLocalizations(
      QuestionDefinition currentQuestionDefinition,
      QuestionDefinitionBuilder updatedQuestionDefinitionBuilder,
      QuestionForm questionForm) {
    // Instead of overwriting all localizations, we just want to overwrite the one
    // for the default locale (the only one possible to change in the edit form).
    updatedQuestionDefinitionBuilder.setQuestionText(
        currentQuestionDefinition
            .getQuestionText()
            .updateTranslation(LocalizedStrings.DEFAULT_LOCALE, questionForm.getQuestionText()));

    // Question help text is optional. If the admin submits an empty string, delete
    // all translations of it.
    if (questionForm.getQuestionHelpText().isBlank()) {
      updatedQuestionDefinitionBuilder.setQuestionHelpText(LocalizedStrings.empty());
    } else {
      updatedQuestionDefinitionBuilder.setQuestionHelpText(
          currentQuestionDefinition
              .getQuestionHelpText()
              .updateTranslation(
                  LocalizedStrings.DEFAULT_LOCALE, questionForm.getQuestionHelpText()));
    }

    if (currentQuestionDefinition.getQuestionType().equals(QuestionType.ENUMERATOR)) {
      updateDefaultLocalizationForEntityType(
          updatedQuestionDefinitionBuilder,
          (EnumeratorQuestionDefinition) currentQuestionDefinition,
          ((EnumeratorQuestionForm) questionForm).getEntityType());
    }

    if (questionForm instanceof MultiOptionQuestionForm) {
      final ImmutableList<QuestionOption> updatedQuestionOptions;
      try {
        updatedQuestionOptions =
            ((MultiOptionQuestionDefinition) updatedQuestionDefinitionBuilder.build()).getOptions();
      } catch (UnsupportedQuestionTypeException e) {
        // Impossible - we checked the type above.
        throw new RuntimeException(e);
      }
      updateDefaultLocalizationForOptions(
          updatedQuestionDefinitionBuilder,
          (MultiOptionQuestionDefinition) currentQuestionDefinition,
          updatedQuestionOptions);
    }
  }

  /** Update the default locale text for an enumerator question's entity type name. */
  private void updateDefaultLocalizationForEntityType(
      QuestionDefinitionBuilder updated,
      EnumeratorQuestionDefinition existing,
      String updatedEntityType) {
    updated.setEntityType(
        existing
            .getEntityType()
            .updateTranslation(LocalizedStrings.DEFAULT_LOCALE, updatedEntityType));
  }

  /** Update the default locale text only for a multi-option question's option text. */
  private void updateDefaultLocalizationForOptions(
      QuestionDefinitionBuilder updatedQuestionDefinitionBuilder,
      MultiOptionQuestionDefinition currentQuestionDefinition,
      ImmutableList<QuestionOption> updatedQuestionOptions) {

    var existingOptions = currentQuestionDefinition.getOptions();
    ImmutableList.Builder<QuestionOption> newOptionsListBuilder = ImmutableList.builder();

    for (QuestionOption updatedQuestionOption : updatedQuestionOptions) {
      var updatedQuestionOptionText = updatedQuestionOption.optionText();

      var maybeExistingOptionWithSameId =
          existingOptions.stream()
              .filter(existingOption -> existingOption.id() == updatedQuestionOption.id())
              .findFirst();
      if (maybeExistingOptionWithSameId.isPresent()
          && maybeExistingOptionWithSameId
              .get()
              .optionText()
              .getDefault()
              .equals(updatedQuestionOptionText.getDefault())) {
        // If there's an existing option with the same ID and same default locale text, then use it
        // and only update the displayOrder, preserving the adminName and translations.
        newOptionsListBuilder.add(
            maybeExistingOptionWithSameId.get().toBuilder()
                .setDisplayOrder(updatedQuestionOption.displayOrder())
                .build());
      } else if (maybeExistingOptionWithSameId.isPresent()) {
        // If there's an existing option with the same ID but different text, then use it
        // and update the displayOrder and default locale text, preserving the adminName but
        // clearing any existing translations.
        newOptionsListBuilder.add(
            maybeExistingOptionWithSameId.get().toBuilder()
                .setDisplayOrder(updatedQuestionOption.displayOrder())
                .setOptionText(updatedQuestionOptionText)
                .build());
      } else {
        // If there wasn't an option with the same ID, treat it as a new
        // option with a new adminName, displayOrder, and text.
        newOptionsListBuilder.add(updatedQuestionOption);
      }
    }

    updatedQuestionDefinitionBuilder.setQuestionOptions(newOptionsListBuilder.build());
  }

  private String invalidQuestionTypeMessage(String questionType) {
    return String.format(
        "unrecognized question type: '%s', accepted values include: %s",
        questionType.toUpperCase(Locale.ROOT), Arrays.toString(QuestionType.values()));
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
