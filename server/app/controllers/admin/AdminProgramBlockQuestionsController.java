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
import services.program.ActiveAndDraftPrograms;
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
import services.question.ReadOnlyQuestionService;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.InvalidUpdateException;
import services.question.exceptions.QuestionNotFoundException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.NullQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
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
          settingsManifest.getEnumeratorImprovementsEnabled(request),
          settingsManifest.getFileUploadQuestionImprovementsEnabled(request));
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

    QuestionDefinition enumeratorQuestionDefinition;
    try {
      enumeratorQuestionDefinition = questionForm.getBuilder().build();
    } catch (UnsupportedQuestionTypeException e) {
      // Valid question type that is not yet fully supported.
      return badRequest(e.getMessage());
    }

    DynamicForm requestData = formFactory.form().bindFromRequest(request);
    Optional<Long> initialQuestionIdOpt =
        Optional.ofNullable(requestData.get("initialQuestionId"))
            .filter(s -> !s.isBlank())
            .map(Long::valueOf);
    boolean initialQuestionWasNewlyCreated = "true".equals(requestData.get("initialQuestionWasNewlyCreated"));

    // Resolve the initial question (if any) BEFORE creating the enumerator. If it has been
    // tombstoned or otherwise can't be resolved, abort here — we don't want to leave an orphan
    // enumerator question in the draft when the admin's intent can't be fulfilled.
    Optional<QuestionDefinition> resolvedInitialOriginalOpt = Optional.empty();
    if (initialQuestionIdOpt.isPresent()) {
      try {
        resolvedInitialOriginalOpt =
            Optional.of(resolveInitialQuestionOrFail(initialQuestionIdOpt.get()));
      } catch (InitialQuestionAttachmentException e) {
        return e.toResult();
      }
    }

    // Not wrapped in an outer Transaction: CiviForm's question/program services use supplyAsync
    // for DB lookups (e.g. QuestionRepository.lookupQuestion), and Ebean transactions are
    // thread-local — async lookups don't see pending writes from a wrapping transaction. Each
    // underlying service call commits independently, so a downstream failure may leave partial
    // state. Errors are surfaced via the response so the admin can investigate.
    ErrorAnd<QuestionDefinition, CiviFormError> result =
        questionService.create(enumeratorQuestionDefinition);
    if (result.isError()) {
      return ok(
          blockEditView
              .renderEnumeratorSetupSection(
                  request,
                  messagesApi.preferred(request),
                  programId,
                  blockId,
                  Optional.of(questionForm),
                  result.getErrors(),
                  /* optionalInitialQuestionCard= */ Optional.empty(),
                  /* optionalInitialQuestion= */ Optional.empty())
              .render());
    }

    QuestionDefinition createdEnumeratorDefinition;
    try {
      createdEnumeratorDefinition = result.getResult();
    } catch (RuntimeException e) {
      return internalServerError("Problem getting the newly-created question definition.");
    }

    // If an initial question was selected, prepare it now (resolve + copy/update + link on
    // enumerator) BEFORE adding either question to the block. The first addQuestionsToBlock
    // call below loads version.getQuestions() into Ebean's relationship cache; that cache is
    // reused for the second addQuestionsToBlock call. Both questions must already exist in the
    // draft when the cache is loaded, otherwise the second add fails validation with
    // QUESTION_NOT_IN_ACTIVE_OR_DRAFT_STATE.
    Optional<QuestionDefinition> createdInitialDefinitionOpt = Optional.empty();
    if (resolvedInitialOriginalOpt.isPresent()) {
      try {
        createdInitialDefinitionOpt =
            Optional.of(
                prepareInitialQuestion(
                    createdEnumeratorDefinition, resolvedInitialOriginalOpt.get(), initialQuestionWasNewlyCreated));
      } catch (InitialQuestionAttachmentException e) {
        return e.toResult();
      }
    }

    // Add enumerator first so the block retains isEnumerator=true; adding a non-enumerator
    // question first would clear the flag and cause ENUMERATOR_ON_NON_ENUMERATOR_BLOCK.
    ProgramDefinition programDefinition;
    BlockDefinition blockDefinition;
    ProgramQuestionDefinition programQuestionDefinition;
    try {
      programDefinition =
          programService.addQuestionsToBlock(
              programId,
              blockId,
              ImmutableList.of(createdEnumeratorDefinition.getId()),
              settingsManifest.getEnumeratorImprovementsEnabled(request),
              settingsManifest.getFileUploadQuestionImprovementsEnabled(request));
      blockDefinition = programDefinition.getBlockDefinition(blockId);
      programQuestionDefinition =
          blockDefinition.programQuestionDefinitions().stream()
              .filter(pqd -> pqd.id() == createdEnumeratorDefinition.getId())
              .findFirst()
              .orElseThrow(
                  () ->
                      new ProgramQuestionDefinitionNotFoundException(
                          programId, blockId, createdEnumeratorDefinition.getId()));

      if (createdInitialDefinitionOpt.isPresent()) {
        programDefinition =
            programService.addQuestionsToBlock(
                programId,
                blockId,
                ImmutableList.of(createdInitialDefinitionOpt.get().getId()),
                settingsManifest.getEnumeratorImprovementsEnabled(request),
                settingsManifest.getFileUploadQuestionImprovementsEnabled(request));
        blockDefinition = programDefinition.getBlockDefinition(blockId);
      }
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(String.format("Block ID %d not found for Program %d", blockId, programId));
    } catch (QuestionNotFoundException e) {
      return notFound(
          String.format("Question ID %d not found", createdEnumeratorDefinition.getId()));
    } catch (CantAddQuestionToBlockException e) {
      return notFound(e.externalMessage());
    } catch (ProgramQuestionDefinitionNotFoundException e) {
      return notFound("ProgramQuestionDefinition not found.");
    }

    if (createdInitialDefinitionOpt.isPresent()) {
      try {
        propagateInitialQuestionToOtherBlocks(
            request,
            programId,
            blockId,
            createdEnumeratorDefinition,
            createdInitialDefinitionOpt.get());
      } catch (InitialQuestionAttachmentException e) {
        return e.toResult();
      }
    }

    return ok(
        blockEditView
            .renderEnumeratorSectionWithSelectedQuestion(
                messagesApi.preferred(request),
                /* optionalQuestionCard= */ Optional.of(
                    blockEditView.renderQuestion(
                        /* optionalCsrfTag= */ Optional.empty(),
                        programDefinition,
                        blockDefinition,
                        createdEnumeratorDefinition,
                        programQuestionDefinition,
                        /* questionIndex= */ 0, // Enumerator blocks have only one question
                        blockDefinition.getQuestionCount(),
                        request)),
                /* blockHasEnumeratorQuestion= */ true,
                blockDefinition)
            .render());
  }

  /**
   * HTMX POST endpoint for selecting an initial question for an enumerator block setup. Returns the
   * initial question card HTML that replaces the {@code #initial-question-slot} div, preserving the
   * rest of the enumerator creation form.
   */
  @Secure(authorizers = Labels.CIVIFORM_ADMIN)
  public Result hxSelectInitialQuestion(Request request, long programId, long blockId) {
    requestChecker.throwIfProgramNotDraft(programId);

    DynamicForm requestData = formFactory.form().bindFromRequest(request);
    Optional<Long> questionId =
        requestData.rawData().entrySet().stream()
            .filter(formField -> formField.getKey().startsWith("question-"))
            .map(Entry::getValue)
            .map(Long::valueOf)
            .findFirst();

    if (questionId.isEmpty()) {
      return badRequest("No question selected");
    }

    InitialQuestionResolution resolution = resolveInitialQuestion(questionId.get());
    QuestionDefinition selected;
    switch (resolution) {
      case InitialQuestionResolution.Found f -> selected = f.question();
      case InitialQuestionResolution.Superseded s -> selected = s.latest();
      case InitialQuestionResolution.Tombstoned t -> {
        return badRequest(
            "The selected question has been deleted. Please reload the question bank.");
      }
      case InitialQuestionResolution.NotFound nf -> {
        return notFound(String.format("Question ID %d not found", nf.id()));
      }
    }

    try {
      ProgramDefinition programDefinition = programService.getFullProgramDefinition(programId);
      BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockId);
      return ok(blockEditView
              .hxRenderInitialQuestionSlot(selected, programDefinition, blockDefinition, request)
              .render())
          .withHeader("HX-Trigger", "closeQuestionBank");
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(String.format("Block ID %d not found for Program %d", blockId, programId));
    }
  }

  /**
   * HTMX GET endpoint that returns the empty {@code #initial-question-slot} fragment ("Add
   * question" button). Used by the Delete button on the initial-question card during the
   * enumerator-creation flow to reverse the swap performed by {@link #hxSelectInitialQuestion}. The
   * initial question is not yet attached to the block at this point, so no DB writes occur.
   */
  @Secure(authorizers = Labels.CIVIFORM_ADMIN)
  public Result hxClearInitialQuestionSlot(Request request, long programId, long blockId) {
    requestChecker.throwIfProgramNotDraft(programId);
    return ok(
        blockEditView.renderEmptyInitialQuestionSlot(messagesApi.preferred(request)).render());
  }

  /**
   * HTMX POST endpoint for backfilling an initial question onto an enumerator block whose
   * enumerator question already exists but has no initial question linked. The bank's "Select
   * existing" form posts here. The "Create new question" path goes through {@link
   * AdminProgramBlocksController#edit} instead, which calls into {@link
   * #backfillInitialQuestionOnBlock} via its newly-created-question branch.
   *
   * <p>Returns the re-rendered enumerator section to swap into {@code #enumerator-block-content},
   * with an {@code HX-Trigger: closeQuestionBank} header so the bank closes without a page reload.
   */
  @Secure(authorizers = Labels.CIVIFORM_ADMIN)
  public Result hxBackfillInitialQuestion(Request request, long programId, long blockId) {
    requestChecker.throwIfProgramNotDraft(programId);

    DynamicForm requestData = formFactory.form().bindFromRequest(request);
    Optional<Long> selectedQuestionId =
        requestData.rawData().entrySet().stream()
            .filter(formField -> formField.getKey().startsWith("question-"))
            .map(Entry::getValue)
            .map(Long::valueOf)
            .findFirst();
    if (selectedQuestionId.isEmpty()) {
      return badRequest("No question selected");
    }

    try {
      backfillInitialQuestionOnBlock(
          request, programId, blockId, selectedQuestionId.get(), /* initialQuestionWasNewlyCreated= */ false);
    } catch (InitialQuestionAttachmentException e) {
      return e.toResult();
    }

    try {
      ProgramDefinition refreshedProgram = programService.getFullProgramDefinition(programId);
      BlockDefinition refreshedBlock = refreshedProgram.getBlockDefinition(blockId);
      ProgramQuestionDefinition refreshedEnumeratorPqd =
          refreshedBlock.programQuestionDefinitions().stream()
              .filter(pqd -> pqd.getQuestionDefinition() instanceof EnumeratorQuestionDefinition)
              .findFirst()
              .orElseThrow(() -> new IllegalStateException("Enumerator question missing on block"));
      return ok(blockEditView
              .renderEnumeratorSectionWithSelectedQuestion(
                  messagesApi.preferred(request),
                  Optional.of(
                      blockEditView.renderQuestion(
                          /* optionalCsrfTag= */ Optional.empty(),
                          refreshedProgram,
                          refreshedBlock,
                          refreshedEnumeratorPqd.getQuestionDefinition(),
                          refreshedEnumeratorPqd,
                          /* questionIndex= */ 0,
                          /* questionsCount= */ 1,
                          request)),
                  /* blockHasEnumeratorQuestion= */ true,
                  refreshedBlock,
                  /* showBackfillInitialQuestionButton= */ false)
              .render())
          .withHeader("HX-Trigger", "closeQuestionBank");
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(String.format("Block ID %d not found for Program %d", blockId, programId));
    }
  }

  /**
   * Backfill the selected question as the initial question on a block whose enumerator question
   * already exists. Runs the prepare step, the block addition, and the cross-program propagation
   * sequentially; throws {@link InitialQuestionAttachmentException} on failure.
   *
   * <p>Not wrapped in a transaction — see the note in {@link #hxCreateEnumerator}. Each underlying
   * service call commits independently, so a downstream failure may leave partial state.
   *
   * <p>Called by both {@link #hxBackfillInitialQuestion} (HTMX submit) and {@link
   * AdminProgramBlocksController#edit} (newly-created-question redirect).
   */
  void backfillInitialQuestionOnBlock(
      Request request,
      long programId,
      long blockId,
      long selectedQuestionId,
      boolean initialQuestionWasNewlyCreated) {
    ProgramDefinition programDefinition;
    BlockDefinition blockDefinition;
    QuestionDefinition enumeratorDef;
    try {
      programDefinition = programService.getFullProgramDefinition(programId);
      blockDefinition = programDefinition.getBlockDefinition(blockId);
      enumeratorDef =
          blockDefinition.programQuestionDefinitions().stream()
              .map(ProgramQuestionDefinition::getQuestionDefinition)
              .filter(qd -> qd instanceof EnumeratorQuestionDefinition)
              .findFirst()
              .orElseThrow(() -> new IllegalStateException("Block has no enumerator question"));
    } catch (ProgramNotFoundException e) {
      throw new InitialQuestionAttachmentException(
          notFound(String.format("Program ID %d not found.", programId)));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      throw new InitialQuestionAttachmentException(
          notFound(String.format("Block ID %d not found for Program %d", blockId, programId)));
    }

    QuestionDefinition originalDefinition = resolveInitialQuestionOrFail(selectedQuestionId);
    QuestionDefinition initialDefinition =
        prepareInitialQuestion(enumeratorDef, originalDefinition, initialQuestionWasNewlyCreated);

    // Add the initial question to the block. The enumerator is already on the block, so this is
    // the first addQuestionsToBlock call in this request — the version-questions cache load
    // will include the just-prepared initial question.
    try {
      programService.addQuestionsToBlock(
          programId,
          blockId,
          ImmutableList.of(initialDefinition.getId()),
          settingsManifest.getEnumeratorImprovementsEnabled(request),
          settingsManifest.getFileUploadQuestionImprovementsEnabled(request));
    } catch (ProgramNotFoundException e) {
      throw new InitialQuestionAttachmentException(
          notFound(String.format("Program ID %d not found.", programId)));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      throw new InitialQuestionAttachmentException(
          notFound(String.format("Block ID %d not found for Program %d", blockId, programId)));
    } catch (QuestionNotFoundException e) {
      throw new InitialQuestionAttachmentException(
          notFound(String.format("Question ID %d not found", initialDefinition.getId())));
    } catch (CantAddQuestionToBlockException e) {
      throw new InitialQuestionAttachmentException(notFound(e.externalMessage()));
    }

    propagateInitialQuestionToOtherBlocks(
        request, programId, blockId, enumeratorDef, initialDefinition);
  }

  /**
   * Resolves the selected question against the current draft + active versions and returns it.
   * Throws {@link InitialQuestionAttachmentException} if the question has been tombstoned or cannot
   * be found. Use this before doing any other writes (e.g. creating the enumerator) so a stale
   * selection can't leave orphan state behind.
   */
  private QuestionDefinition resolveInitialQuestionOrFail(long selectedQuestionId) {
    InitialQuestionResolution resolution = resolveInitialQuestion(selectedQuestionId);
    return switch (resolution) {
      case InitialQuestionResolution.Found f -> f.question();
      case InitialQuestionResolution.Superseded s -> s.latest();
      case InitialQuestionResolution.Tombstoned t ->
          throw new InitialQuestionAttachmentException(
              badRequest(
                  "The selected initial question has been deleted. Please reload the page and"
                      + " choose a different question."));
      case InitialQuestionResolution.NotFound nf ->
          throw new InitialQuestionAttachmentException(
              notFound(String.format("Question ID %d not found", nf.id())));
    };
  }

  /**
   * Prepares the resolved initial question for use on the enumerator: copies it (or updates it with
   * the enumeratorId, when the question was just created), then links it on the enumerator via
   * {@code initialQuestionId}. Returns the prepared initial question.
   *
   * <p>This step is intentionally <em>narrow</em>: it does not add either question to a block, and
   * it does not propagate. Callers must perform those steps in a specific order — both questions
   * must exist in the draft before the first {@code addQuestionsToBlock} call, because Ebean caches
   * the version's question relationship list on first access for the rest of the request.
   *
   * @param originalDefinition the already-resolved question to use as the initial question. Should
   *     come from {@link #resolveInitialQuestionOrFail} so consistency has been checked.
   * @param initialQuestionWasNewlyCreated true when the question was just created via the bank's "Create new
   *     question" path (in which case no copy is made — the question is updated to be a repeated
   *     question by setting its enumeratorId).
   */
  private QuestionDefinition prepareInitialQuestion(
      QuestionDefinition enumeratorDef,
      QuestionDefinition originalDefinition,
      boolean initialQuestionWasNewlyCreated) {
    QuestionDefinition initialDefinition;
    try {
      if (initialQuestionWasNewlyCreated) {
        initialDefinition =
            new QuestionDefinitionBuilder(originalDefinition)
                .setEnumeratorId(Optional.of(enumeratorDef.getId()))
                .build();
        ErrorAnd<QuestionDefinition, CiviFormError> updateResult =
            questionService.update(Optional.of(originalDefinition), initialDefinition);
        if (!updateResult.isError()) {
          initialDefinition = updateResult.getResult();
        }
      } else {
        ErrorAnd<QuestionDefinition, CiviFormError> copyResult =
            questionService.createInitialQuestionCopy(originalDefinition, enumeratorDef.getId());
        if (copyResult.isError()) {
          throw new InitialQuestionAttachmentException(
              badRequest("Could not create initial question copy."));
        }
        initialDefinition = copyResult.getResult();
      }
    } catch (UnsupportedQuestionTypeException e) {
      throw new InitialQuestionAttachmentException(
          badRequest("Unsupported initial question type: " + e.getMessage()));
    } catch (InvalidUpdateException e) {
      throw new InitialQuestionAttachmentException(
          internalServerError("Could not set enumeratorId on initial question: " + e.getMessage()));
    }

    try {
      QuestionDefinition updatedEnumeratorDef =
          new QuestionDefinitionBuilder(enumeratorDef)
              .setInitialQuestionId(Optional.of(initialDefinition.getId()))
              .unsafeBuild();
      questionService.update(Optional.of(enumeratorDef), updatedEnumeratorDef);
    } catch (InvalidUpdateException e) {
      throw new InitialQuestionAttachmentException(
          internalServerError("Could not link initial question to enumerator: " + e.getMessage()));
    }

    return initialDefinition;
  }

  /**
   * Adds the initial question to every other block (in any program) that references the same
   * enumerator question. Relies on the fact that the preceding {@link QuestionService#update} call
   * on the enumerator already created drafts of any active programs that referenced it (via {@code
   * VersionRepository.updateProgramsThatReferenceQuestion}), so iterating draft programs here
   * covers both draft and active scopes.
   *
   * <p>The initial question is shared (not re-copied) — every matching block ends up with the same
   * initial-question definition.
   *
   * <p>Atomic with the rest of the operation: per-block failures throw {@link
   * InitialQuestionAttachmentException} so the surrounding transaction rolls back all writes,
   * including the initial-question copy and the enumerator's {@code initialQuestionId} link.
   */
  private void propagateInitialQuestionToOtherBlocks(
      Request request,
      long currentProgramId,
      long currentBlockId,
      QuestionDefinition enumeratorDef,
      QuestionDefinition initialDefinition) {
    String enumeratorName = enumeratorDef.getName();
    ActiveAndDraftPrograms activeAndDraft = programService.getActiveAndDraftPrograms();

    for (ProgramDefinition draftProgram : activeAndDraft.getDraftPrograms()) {
      for (BlockDefinition block : draftProgram.blockDefinitions()) {
        if (draftProgram.id() == currentProgramId && block.id() == currentBlockId) {
          continue;
        }
        boolean blockReferencesEnumerator =
            block.programQuestionDefinitions().stream()
                .anyMatch(pqd -> pqd.getQuestionDefinition().getName().equals(enumeratorName));
        if (!blockReferencesEnumerator) {
          continue;
        }
        boolean alreadyHasInitial =
            block.programQuestionDefinitions().stream()
                .anyMatch(pqd -> pqd.id() == initialDefinition.getId());
        if (alreadyHasInitial) {
          continue;
        }
        try {
          programService.addQuestionsToBlock(
              draftProgram.id(),
              block.id(),
              ImmutableList.of(initialDefinition.getId()),
              settingsManifest.getEnumeratorImprovementsEnabled(request),
              settingsManifest.getFileUploadQuestionImprovementsEnabled(request));
        } catch (ProgramNotFoundException
            | ProgramBlockDefinitionNotFoundException
            | QuestionNotFoundException
            | CantAddQuestionToBlockException e) {
          throw new InitialQuestionAttachmentException(
              internalServerError(
                  String.format(
                      "Could not propagate initial question to program %d block %d: %s",
                      draftProgram.id(), block.id(), e.getMessage())));
        }
      }
    }
  }

  /**
   * Carries a {@link Result} (typically an error response) so the prepare/backfill helpers can
   * short-circuit and let their callers forward the result. Used because the helpers have multiple
   * failure modes that map to different HTTP responses.
   */
  static final class InitialQuestionAttachmentException extends RuntimeException {
    private final Result result;

    InitialQuestionAttachmentException(Result result) {
      this.result = result;
    }

    Result toResult() {
      return result;
    }
  }

  /**
   * Outcome of resolving an initial-question ID submitted from a (possibly stale) admin page. The
   * page may have been rendered before another admin saved a new draft of the same question, or
   * before the question was tombstoned for deletion. Callers switch on the result to choose how to
   * react.
   */
  private sealed interface InitialQuestionResolution
      permits InitialQuestionResolution.Found,
          InitialQuestionResolution.Superseded,
          InitialQuestionResolution.Tombstoned,
          InitialQuestionResolution.NotFound {

    /** The submitted ID matches an up-to-date question; use it directly. */
    record Found(QuestionDefinition question) implements InitialQuestionResolution {}

    /**
     * The submitted ID points at a question that has been superseded by a newer draft with the same
     * name. {@code latest} is the canonical current version.
     */
    record Superseded(QuestionDefinition stale, QuestionDefinition latest)
        implements InitialQuestionResolution {}

    /** The submitted ID points at a question whose name is tombstoned in the current draft. */
    record Tombstoned(QuestionDefinition stale) implements InitialQuestionResolution {}

    /** No question exists for the submitted ID. */
    record NotFound(long id) implements InitialQuestionResolution {}
  }

  /**
   * Resolves an initial-question ID against the current draft + active versions. See {@link
   * InitialQuestionResolution} for the possible outcomes.
   */
  private InitialQuestionResolution resolveInitialQuestion(long id) {
    ReadOnlyQuestionService roqs =
        questionService.getReadOnlyQuestionService().toCompletableFuture().join();

    QuestionDefinition snapshot;
    try {
      snapshot = roqs.getQuestionDefinition(id);
    } catch (QuestionNotFoundException e) {
      return new InitialQuestionResolution.NotFound(id);
    }
    if (snapshot instanceof NullQuestionDefinition) {
      return new InitialQuestionResolution.NotFound(id);
    }

    ImmutableList<QuestionDefinition> upToDate = roqs.getUpToDateQuestions();

    Optional<QuestionDefinition> byId = upToDate.stream().filter(q -> q.getId() == id).findFirst();
    if (byId.isPresent()) {
      return new InitialQuestionResolution.Found(byId.get());
    }

    Optional<QuestionDefinition> byName =
        upToDate.stream().filter(q -> q.getName().equals(snapshot.getName())).findFirst();
    if (byName.isPresent()) {
      return new InitialQuestionResolution.Superseded(snapshot, byName.get());
    }

    return new InitialQuestionResolution.Tombstoned(snapshot);
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
