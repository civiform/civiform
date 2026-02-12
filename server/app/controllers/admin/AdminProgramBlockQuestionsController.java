package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;
import static views.ViewUtils.ProgramDisplayType.DRAFT;

import auth.Authorizers.Labels;
import com.google.common.collect.ImmutableList;
import controllers.FlashKey;
import forms.EnumeratorQuestionForm;
import forms.ProgramQuestionDefinitionOptionalityForm;
import forms.QuestionFormBuilder;
import java.util.Map.Entry;
import java.util.Optional;
import javax.inject.Inject;
import models.QuestionModel;
import org.pac4j.play.java.Secure;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.VersionRepository;
import services.CiviFormError;
import services.ErrorAnd;
import services.program.BlockDefinition;
import services.program.CantAddQuestionToBlockException;
import services.program.IllegalApiBridgeStateException;
import services.program.IllegalPredicateOrderingException;
import services.program.InvalidQuestionPositionException;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramQuestionDefinitionInvalidException;
import services.program.ProgramQuestionDefinitionNotFoundException;
import services.program.ProgramService;
import services.question.QuestionService;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.QuestionNotFoundException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import services.settings.SettingsManifest;
import views.admin.programs.ProgramBlocksView;
import views.components.ProgramQuestionBank;

/** Controller for admins editing questions on a screen (block) of a program. */
public class AdminProgramBlockQuestionsController extends Controller {

  private final ProgramService programService;
  private final QuestionService questionService;
  private final VersionRepository versionRepository;
  private final FormFactory formFactory;
  private final RequestChecker requestChecker;
  private final SettingsManifest settingsManifest;
  private final MessagesApi messagesApi;
  private final ProgramBlocksView blockEditView;

  @Inject
  public AdminProgramBlockQuestionsController(
      ProgramService programService,
      QuestionService questionService,
      VersionRepository versionRepository,
      FormFactory formFactory,
      RequestChecker requestChecker,
      SettingsManifest settingsManifest,
      MessagesApi messagesApi,
      ProgramBlocksView.Factory programBlockViewFactory) {
    this.programService = checkNotNull(programService);
    this.questionService = checkNotNull(questionService);
    this.versionRepository = checkNotNull(versionRepository);
    this.formFactory = checkNotNull(formFactory);
    this.requestChecker = checkNotNull(requestChecker);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.messagesApi = checkNotNull(messagesApi);
    this.blockEditView = checkNotNull(programBlockViewFactory.create(DRAFT));
  }

  /** POST endpoint for adding one or more questions to a screen. */
  @Secure(authorizers = Labels.CIVIFORM_ADMIN)
  public Result create(Request request, long programId, long blockId) {
    requestChecker.throwIfProgramNotDraft(programId);

    DynamicForm requestData = formFactory.form().bindFromRequest(request);
    ImmutableList<Long> questionIds =
        requestData.rawData().entrySet().stream()
            .filter(formField -> formField.getKey().startsWith("question-"))
            .map(Entry::getValue)
            .map(Long::valueOf)
            .collect(ImmutableList.toImmutableList());

    // The users' browser may be out of date. Find the last revision of each question.
    ImmutableList.Builder<Long> idBuilder = new ImmutableList.Builder<Long>();
    for (Long qId : questionIds) {
      Optional<QuestionModel> latestQuestion = versionRepository.getLatestVersionOfQuestion(qId);
      if (latestQuestion.isEmpty()) {
        return notFound(String.format("Question ID %s not found", qId));
      }
      idBuilder.add(latestQuestion.get().id);
    }
    ImmutableList<Long> latestQuestionIds = idBuilder.build();

    try {
      // we pass down the boolean here instead of the entire request because it simplifies
      // DevDatabaseSeedTask
      programService.addQuestionsToBlock(
          programId,
          blockId,
          latestQuestionIds,
          settingsManifest.getEnumeratorImprovementsEnabled(request));
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(String.format("Block ID %d not found for Program %d", blockId, programId));
    } catch (QuestionNotFoundException e) {
      return notFound(String.format("Question IDs %s not found", latestQuestionIds));
    } catch (CantAddQuestionToBlockException e) {
      return notFound(e.externalMessage());
    }

    return redirect(
        ProgramQuestionBank.addShowQuestionBankParam(
            controllers.admin.routes.AdminProgramBlocksController.edit(programId, blockId).url()));
  }

  /** HTMX POST endpoint for creating a new enumerator question and adding it to a screen. */
  @Secure(authorizers = Labels.CIVIFORM_ADMIN)
  public Result hxCreateEnumerator(Request request, long programId, long blockId) {
    requestChecker.throwIfProgramNotDraft(programId);

    // Create the new enumerator question in the same way as in AdminQuestionController#create.
    EnumeratorQuestionForm questionForm;
    try {
      questionForm =
          (EnumeratorQuestionForm)
              QuestionFormBuilder.createFromRequest(request, formFactory, QuestionType.ENUMERATOR);
    } catch (InvalidQuestionTypeException e) {
      return badRequest(e.getMessage());
    }

    QuestionDefinition questionDefinition;
    try {
      questionDefinition = questionForm.getBuilder().build();
    } catch (UnsupportedQuestionTypeException e) {
      // Valid question type that is not yet fully supported.
      return badRequest(e.getMessage());
    }

    ErrorAnd<QuestionDefinition, CiviFormError> result = questionService.create(questionDefinition);
    if (result.isError()) {
      return ok(
          blockEditView
              .renderEnumeratorSetupSection(
                  request,
                  messagesApi.preferred(request),
                  programId,
                  blockId,
                  Optional.of(questionForm),
                  result.getErrors())
              .render());
    }

    QuestionDefinition createdQuestionDefinition;

    try {
      createdQuestionDefinition = result.getResult();
    } catch (RuntimeException e) {
      return internalServerError("Problem getting the newly-created question definition.");
    }

    ImmutableList<Long> latestQuestionIds = ImmutableList.of(createdQuestionDefinition.getId());

    ProgramDefinition programDefinition;
    BlockDefinition blockDefinition;
    ProgramQuestionDefinition programQuestionDefinition;

    try {
      programDefinition =
          programService.addQuestionsToBlock(
              programId,
              blockId,
              latestQuestionIds,
              settingsManifest.getEnumeratorImprovementsEnabled(request));
      blockDefinition = programDefinition.getBlockDefinition(blockId);
      programQuestionDefinition =
          blockDefinition.programQuestionDefinitions().stream()
              .filter(pqd -> pqd.id() == createdQuestionDefinition.getId())
              .findFirst()
              .orElseThrow(
                  () ->
                      new ProgramQuestionDefinitionNotFoundException(
                          programId, blockId, createdQuestionDefinition.getId()));
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(String.format("Block ID %d not found for Program %d", blockId, programId));
    } catch (QuestionNotFoundException e) {
      return notFound(String.format("Question IDs %s not found", latestQuestionIds));
    } catch (CantAddQuestionToBlockException e) {
      return notFound(e.externalMessage());
    } catch (ProgramQuestionDefinitionNotFoundException e) {
      return notFound("ProgramQuestionDefinition not found.");
    }

    return ok(
        blockEditView
            .renderEnumeratorQuestionCardSection(
                messagesApi.preferred(request),
                /* optionalQuestionCard= */ Optional.of(
                    blockEditView.renderQuestion(
                        /* optionalCsrfTag= */ Optional.empty(),
                        programDefinition,
                        blockDefinition,
                        createdQuestionDefinition,
                        programQuestionDefinition,
                        /* questionIndex= */ 0, // Enumerator blocks have only one question
                        blockDefinition.getQuestionCount(),
                        request)))
            .render());
  }

  /** POST endpoint for removing a question from a screen. */
  @Secure(authorizers = Labels.CIVIFORM_ADMIN)
  public Result delete(
      Request request, long programId, long blockDefinitionId, long questionDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    try {
      programService.removeQuestionsFromBlock(
          programId,
          blockDefinitionId,
          ImmutableList.of(questionDefinitionId),
          settingsManifest,
          request);
    } catch (IllegalPredicateOrderingException | IllegalApiBridgeStateException e) {
      return redirect(
              controllers.admin.routes.AdminProgramBlocksController.edit(
                  programId, blockDefinitionId))
          .flashing(FlashKey.ERROR, e.getLocalizedMessage());
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    } catch (QuestionNotFoundException e) {
      return notFound(String.format("Question ID %s not found", questionDefinitionId));
    }

    return redirect(
        controllers.admin.routes.AdminProgramBlocksController.edit(programId, blockDefinitionId));
  }

  /** POST endpoint for editing whether or not a question is optional on a screen. */
  @Secure(authorizers = Labels.CIVIFORM_ADMIN)
  public Result setOptional(
      Request request, long programId, long blockDefinitionId, long questionDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    ProgramQuestionDefinitionOptionalityForm programQuestionDefinitionOptionalityForm =
        formFactory
            .form(ProgramQuestionDefinitionOptionalityForm.class)
            .bindFromRequest(request)
            .get();

    try {
      programService.setProgramQuestionDefinitionOptionality(
          programId,
          blockDefinitionId,
          questionDefinitionId,
          programQuestionDefinitionOptionalityForm.getOptional());
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    } catch (ProgramQuestionDefinitionNotFoundException e) {
      return notFound(
          String.format(
              "Question ID %d not found in Block %d for program %d",
              questionDefinitionId, blockDefinitionId, programId));
    }

    return redirect(
        controllers.admin.routes.AdminProgramBlocksController.edit(programId, blockDefinitionId));
  }

  /** POST endpoint for editing whether or not a question has address correction enabled. */
  @Secure(authorizers = Labels.CIVIFORM_ADMIN)
  public Result toggleAddressCorrectionEnabledState(
      Request request, long programId, long blockDefinitionId, long questionDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    try {
      ProgramDefinition programDefinition = programService.getFullProgramDefinition(programId);
      BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockDefinitionId);

      // In these cases, we warn admins that changing address correction is not allowed in the
      // tooltip, so we can silently ignore the request.
      if (!settingsManifest.getEsriAddressCorrectionEnabled(request)
          || programDefinition.isQuestionUsedInPredicate(questionDefinitionId)
          || programDefinition
              .getBlockDefinition(blockDefinitionId)
              .hasAddressCorrectionEnabledOnDifferentQuestion(questionDefinitionId)) {
        return redirect(
            controllers.admin.routes.AdminProgramBlocksController.edit(
                programId, blockDefinitionId));
      }

      Optional<ProgramQuestionDefinition> programQuestionDefinition =
          blockDefinition.programQuestionDefinitions().stream()
              .filter(pqd -> pqd.id() == questionDefinitionId)
              .findFirst();

      if (programQuestionDefinition.isEmpty()) {
        throw new ProgramQuestionDefinitionNotFoundException(
            programId, blockDefinitionId, questionDefinitionId);
      }

      programService.setProgramQuestionDefinitionAddressCorrectionEnabled(
          programId,
          blockDefinitionId,
          questionDefinitionId,
          // Flop the bit to the next (desired state) from the current setting.
          !programQuestionDefinition.get().addressCorrectionEnabled());
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    } catch (ProgramQuestionDefinitionNotFoundException e) {
      return notFound(
          String.format(
              "Question ID %d not found in Block %d for program %d",
              questionDefinitionId, blockDefinitionId, programId));
    } catch (ProgramQuestionDefinitionInvalidException e) {
      return notAcceptable(
          String.format(
              "Tried enabling address correction in a block that already contains an a question"
                  + " with address correction enabled. Program ID %d, Block ID %d, Question ID %d",
              programId, blockDefinitionId, questionDefinitionId));
    }

    return redirect(
        controllers.admin.routes.AdminProgramBlocksController.edit(programId, blockDefinitionId));
  }

  /** POST endpoint for changing position of a question in its block. */
  @Secure(authorizers = Labels.CIVIFORM_ADMIN)
  public Result move(
      Request request, long programId, long blockDefinitionId, long questionDefinitionId)
      throws InvalidQuestionPositionException, ProgramNotFoundException {
    requestChecker.throwIfProgramNotDraft(programId);

    DynamicForm requestData = formFactory.form().bindFromRequest(request);
    final int newPosition;
    try {
      newPosition =
          Integer.parseInt(requestData.get(ProgramBlocksView.MOVE_QUESTION_POSITION_FIELD));
    } catch (NumberFormatException e) {
      throw InvalidQuestionPositionException.missingPositionArgument();
    }

    try {
      programService.setProgramQuestionDefinitionPosition(
          programId, blockDefinitionId, questionDefinitionId, newPosition);
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    } catch (ProgramQuestionDefinitionNotFoundException e) {
      return notFound(
          String.format(
              "Question ID %d not found in Block %d for program %d",
              questionDefinitionId, blockDefinitionId, programId));
    }

    return redirect(
        controllers.admin.routes.AdminProgramBlocksController.edit(programId, blockDefinitionId));
  }
}
