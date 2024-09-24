package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import controllers.FlashKey;
import forms.admin.BlockEligibilityMessageForm;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.DynamicForm;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.VersionRepository;
import services.LocalizedStrings;
import services.program.BlockDefinition;
import services.program.EligibilityDefinition;
import services.program.EligibilityNotValidForProgramTypeException;
import services.program.IllegalPredicateOrderingException;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramQuestionDefinitionNotFoundException;
import services.program.ProgramService;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateGenerator;
import services.question.QuestionService;
import services.question.ReadOnlyQuestionService;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.QuestionDefinition;
import views.admin.programs.ProgramPredicateConfigureView;
import views.admin.programs.ProgramPredicatesEditView;

/**
 * Controller for admins editing and viewing program predicates for eligibility and visibility
 * logic.
 */
public class AdminProgramBlockPredicatesController extends CiviFormController {
  private final PredicateGenerator predicateGenerator;
  private final ProgramService programService;
  private final QuestionService questionService;
  private final ProgramPredicatesEditView predicatesEditViewV2;
  private final ProgramPredicateConfigureView predicatesConfigureView;
  private final FormFactory formFactory;
  private final RequestChecker requestChecker;

  @Inject
  public AdminProgramBlockPredicatesController(
      PredicateGenerator predicateGenerator,
      ProgramService programService,
      QuestionService questionService,
      ProgramPredicatesEditView predicatesEditViewV2,
      ProgramPredicateConfigureView predicatesConfigureView,
      FormFactory formFactory,
      RequestChecker requestChecker,
      ProfileUtils profileUtils,
      VersionRepository versionRepository) {
    super(profileUtils, versionRepository);
    this.predicateGenerator = checkNotNull(predicateGenerator);
    this.programService = checkNotNull(programService);
    this.questionService = checkNotNull(questionService);
    this.predicatesEditViewV2 = checkNotNull(predicatesEditViewV2);
    this.predicatesConfigureView = checkNotNull(predicatesConfigureView);
    this.formFactory = checkNotNull(formFactory);
    this.requestChecker = checkNotNull(requestChecker);
  }

  /**
   * Return an HTML page containing current show-hide configurations and forms to edit the
   * configurations.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result editVisibility(Request request, long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    try {
      ProgramDefinition programDefinition = programService.getFullProgramDefinition(programId);
      BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockDefinitionId);

      return ok(
          predicatesEditViewV2.render(
              request,
              programDefinition,
              blockDefinition,
              programDefinition.getAvailableVisibilityPredicateQuestionDefinitions(
                  blockDefinitionId),
              ProgramPredicatesEditView.ViewType.VISIBILITY));
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    }
  }

  /**
   * Return an HTML page containing current eligibility configurations and forms to edit the
   * configurations.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result editEligibility(Request request, long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    try {
      ProgramDefinition programDefinition = programService.getFullProgramDefinition(programId);
      BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockDefinitionId);

      return ok(
          predicatesEditViewV2.render(
              request,
              programDefinition,
              blockDefinition,
              programDefinition.getAvailableEligibilityPredicateQuestionDefinitions(
                  blockDefinitionId),
              ProgramPredicatesEditView.ViewType.ELIGIBILITY));
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    }
  }

  /** POST endpoint for updating show-hide configurations. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result updateVisibility(Request request, long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    ReadOnlyQuestionService roQuestionService =
        questionService.getReadOnlyQuestionService().toCompletableFuture().join();

    try {
      PredicateDefinition predicateDefinition =
          predicateGenerator.generatePredicateDefinition(
              programService.getFullProgramDefinition(programId),
              formFactory.form().bindFromRequest(request),
              roQuestionService);

      programService.setBlockVisibilityPredicate(
          programId, blockDefinitionId, Optional.of(predicateDefinition));
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    } catch (IllegalPredicateOrderingException
        | QuestionNotFoundException
        | ProgramQuestionDefinitionNotFoundException e) {
      return redirect(
              routes.AdminProgramBlockPredicatesController.editVisibility(
                  programId, blockDefinitionId))
          .flashing(FlashKey.ERROR, e.getLocalizedMessage());
    }

    return redirect(
            routes.AdminProgramBlockPredicatesController.editVisibility(
                programId, blockDefinitionId))
        .flashing(FlashKey.SUCCESS, "Saved visibility condition");
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result configureNewVisibilityPredicate(
      Request request, long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    DynamicForm form = formFactory.form().bindFromRequest(request);

    try {
      ProgramDefinition programDefinition = programService.getFullProgramDefinition(programId);
      BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockDefinitionId);

      return ok(
          predicatesConfigureView.renderNewVisibility(
              request,
              programDefinition,
              blockDefinition,
              getQuestionDefinitionsForForm(programDefinition, blockDefinition, form)));
    } catch (ProgramNotFoundException | ProgramBlockDefinitionNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result configureExistingVisibilityPredicate(
      Request request, long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    try {
      ProgramDefinition programDefinition = programService.getFullProgramDefinition(programId);
      BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockDefinitionId);

      ImmutableList<Long> visibilityQuestionIds =
          blockDefinition
              .visibilityPredicate()
              .orElseThrow(
                  () ->
                      new RuntimeException(
                          String.format(
                              "Block %d has no visibility predicate", blockDefinition.id())))
              .getQuestions();

      ImmutableList<QuestionDefinition> visibilityQuestionDefinitions =
          getQuestionDefinitions(
              programDefinition,
              blockDefinition,
              /* selectionPredicate= */ (QuestionDefinition questionDefinition) ->
                  visibilityQuestionIds.contains(questionDefinition.getId()));

      return ok(
          predicatesConfigureView.renderExistingVisibility(
              request, programDefinition, blockDefinition, visibilityQuestionDefinitions));
    } catch (ProgramNotFoundException | ProgramBlockDefinitionNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result configureNewEligibilityPredicate(
      Request request, long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    DynamicForm form = formFactory.form().bindFromRequest(request);

    try {
      ProgramDefinition programDefinition = programService.getFullProgramDefinition(programId);
      BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockDefinitionId);

      return ok(
          predicatesConfigureView.renderNewEligibility(
              request,
              programDefinition,
              blockDefinition,
              getQuestionDefinitionsForForm(programDefinition, blockDefinition, form)));
    } catch (ProgramNotFoundException | ProgramBlockDefinitionNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result configureExistingEligibilityPredicate(
      Request request, long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    try {
      ProgramDefinition programDefinition = programService.getFullProgramDefinition(programId);
      BlockDefinition blockDefinition = programDefinition.getBlockDefinition(blockDefinitionId);

      ImmutableList<Long> eligibilityQuestionIds =
          blockDefinition
              .eligibilityDefinition()
              .orElseThrow(
                  () ->
                      new RuntimeException(
                          String.format(
                              "Block %d has no eligibility definition", blockDefinition.id())))
              .predicate()
              .getQuestions();

      ImmutableList<QuestionDefinition> eligibilityQuestionDefinitions =
          getQuestionDefinitions(
              programDefinition,
              blockDefinition,
              /* selectionPredicate= */ (QuestionDefinition questionDefinition) ->
                  eligibilityQuestionIds.contains(questionDefinition.getId()));

      return ok(
          predicatesConfigureView.renderExistingEligibility(
              request, programDefinition, blockDefinition, eligibilityQuestionDefinitions));
    } catch (ProgramNotFoundException | ProgramBlockDefinitionNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private ImmutableList<QuestionDefinition> getQuestionDefinitionsForForm(
      ProgramDefinition programDefinition, BlockDefinition blockDefinition, DynamicForm form) {
    return getQuestionDefinitions(
        programDefinition,
        blockDefinition,
        (QuestionDefinition questionDefinition) ->
            form.rawData().containsKey(String.format("question-%d", questionDefinition.getId())));
  }

  private ImmutableList<QuestionDefinition> getQuestionDefinitions(
      ProgramDefinition programDefinition,
      BlockDefinition blockDefinition,
      Predicate<QuestionDefinition> selectionPredicate) {

    try {
      return programDefinition
          .getAvailablePredicateQuestionDefinitions(blockDefinition.id())
          .stream()
          .filter(selectionPredicate)
          .collect(ImmutableList.toImmutableList());
    } catch (ProgramBlockDefinitionNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /** POST endpoint for updating eligibility configurations. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result updateEligibility(Request request, long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    ReadOnlyQuestionService roQuestionService =
        questionService.getReadOnlyQuestionService().toCompletableFuture().join();

    try {
      EligibilityDefinition eligibility =
          EligibilityDefinition.builder()
              .setPredicate(
                  predicateGenerator.generatePredicateDefinition(
                      programService.getFullProgramDefinition(programId),
                      formFactory.form().bindFromRequest(request),
                      roQuestionService))
              .build();

      programService.setBlockEligibilityDefinition(
          programId, blockDefinitionId, Optional.of(eligibility));
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    } catch (IllegalPredicateOrderingException
        | QuestionNotFoundException
        | ProgramQuestionDefinitionNotFoundException
        | EligibilityNotValidForProgramTypeException e) {
      return redirect(
              routes.AdminProgramBlockPredicatesController.editEligibility(
                  programId, blockDefinitionId))
          .flashing(FlashKey.ERROR, e.getLocalizedMessage());
    }

    return redirect(
            routes.AdminProgramBlockPredicatesController.editEligibility(
                programId, blockDefinitionId))
        .flashing(FlashKey.SUCCESS, "Saved eligibility condition");
  }

  /** POST endpoint for deleting show-hide configurations. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result destroyVisibility(long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    try {
      programService.removeBlockPredicate(programId, blockDefinitionId);
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    }

    return redirect(
            routes.AdminProgramBlockPredicatesController.editVisibility(
                programId, blockDefinitionId))
        .flashing(FlashKey.SUCCESS, "Removed the visibility condition for this screen.");
  }

  /** POST endpoint for deleting eligibility configurations. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result destroyEligibility(long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    try {
      programService.removeBlockEligibilityPredicate(programId, blockDefinitionId);
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", programId));
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(
          String.format("Block ID %d not found for Program %d", blockDefinitionId, programId));
    }

    return redirect(
            routes.AdminProgramBlockPredicatesController.editEligibility(
                programId, blockDefinitionId))
        .flashing(FlashKey.SUCCESS, "Removed the eligibility condition for this screen.");
  }

  /** POST endpoint for updating eligibility message. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result updateEligibilityMessage(
      Http.Request request, long programId, long blockDefinitionId) {
    requestChecker.throwIfProgramNotDraft(programId);

    Form<BlockEligibilityMessageForm> form =
        formFactory
            .form(BlockEligibilityMessageForm.class)
            .bindFromRequest(
                request, BlockEligibilityMessageForm.FIELD_NAMES.toArray(new String[0]));

    String newMessage = form.get().getEligibilityMessage();

    String toastType;
    String toastMessage;
    try {
      ProgramDefinition programDefinition = programService.getFullProgramDefinition(programId);
      BlockDefinition blockDefinition =
          programDefinition.getBlockDefinition(blockDefinitionId).toBuilder()
              .setLocalizedMessage(Optional.of(LocalizedStrings.of(Locale.US, newMessage)))
              .build();

      programService.setBlockEligibilityMessage(
          programId, blockDefinitionId, Optional.of(LocalizedStrings.of(Locale.US, newMessage)));

      toastType = "success";
      if (newMessage.isBlank()) {
        toastMessage = "Eligibility message removed";
      } else {
        toastMessage = "Eligibility message set to " + newMessage;
      }
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
    } catch (ProgramBlockDefinitionNotFoundException e) {
      return notFound(e.toString());
    } catch (IllegalPredicateOrderingException e) {
      return notFound(e.toString());
    }

    final String indexUrl =
        routes.AdminProgramBlockPredicatesController.editEligibility(programId, blockDefinitionId)
            .url();

    return redirect(indexUrl).flashing(toastType, toastMessage);
  }
}
