package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import controllers.CiviFormController;
import java.util.Optional;
import models.ProgramModel;
import models.QuestionModel;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import repository.ApplicationStatusesRepository;
import repository.ProgramRepository;
import repository.QuestionRepository;
import repository.VersionRepository;
import services.ErrorAnd;
import services.migration.ProgramMigrationService;
import services.program.BlockDefinition;
import services.program.EligibilityDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.predicate.AndNode;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.OrNode;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.question.types.QuestionDefinition;
import services.settings.SettingsManifest;
import services.statuses.StatusDefinitions;
import views.admin.migration.AdminImportView;
import views.admin.migration.AdminImportViewPartial;
import views.admin.migration.AdminProgramImportForm;

/**
 * A controller for the import part of program migration (allowing admins to easily migrate programs
 * between different environments). This controller is responsible for reading a JSON file that
 * represents a program and turning it into a full-fledged {@link
 * services.program.ProgramDefinition}, including all blocks and questions. {@link
 * AdminExportController} is responsible for exporting an existing program into the JSON format that
 * will be read by this controller.
 *
 * <p>Typically, admins will export from one environment (e.g. staging) and import to another
 * environment (e.g. production).
 */
public class AdminImportController extends CiviFormController {

  private final AdminImportView adminImportView;
  private final AdminImportViewPartial adminImportViewPartial;
  private final FormFactory formFactory;
  private final ProgramMigrationService programMigrationService;
  private final SettingsManifest settingsManifest;
  private final ProgramRepository programRepository;
  private final QuestionRepository questionRepository;
  private final ApplicationStatusesRepository applicationStatusesRepository;

  @Inject
  public AdminImportController(
      AdminImportView adminImportView,
      AdminImportViewPartial adminImportViewPartial,
      FormFactory formFactory,
      ProfileUtils profileUtils,
      ProgramMigrationService programMigrationService,
      SettingsManifest settingsManifest,
      VersionRepository versionRepository,
      ProgramRepository programRepository,
      QuestionRepository questionRepository,
      ApplicationStatusesRepository applicationStatusesRepository) {
    super(profileUtils, versionRepository);
    this.adminImportView = checkNotNull(adminImportView);
    this.adminImportViewPartial = checkNotNull(adminImportViewPartial);
    this.formFactory = checkNotNull(formFactory);
    this.programMigrationService = checkNotNull(programMigrationService);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.programRepository = checkNotNull(programRepository);
    this.questionRepository = checkNotNull(questionRepository);
    this.applicationStatusesRepository = checkNotNull(applicationStatusesRepository);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request) {
    if (!settingsManifest.getProgramMigrationEnabled(request)) {
      return notFound("Program import is not enabled");
    }
    return ok(adminImportView.render(request));
  }

  /** HTMX Partial that parses and renders the program data included in the request. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result hxImportProgram(Http.Request request) {
    if (!settingsManifest.getProgramMigrationEnabled(request)) {
      return notFound("Program import is not enabled");
    }

    Form<AdminProgramImportForm> form =
        formFactory
            .form(AdminProgramImportForm.class)
            .bindFromRequest(request, AdminProgramImportForm.FIELD_NAMES.toArray(new String[0]));
    String jsonString = form.get().getProgramJson();
    if (jsonString == null) {
      // If they didn't upload anything, just re-render the main import page.
      return redirect(routes.AdminImportController.index().url());
    }

    ErrorAnd<ProgramMigrationWrapper, String> deserializeResult =
        programMigrationService.deserialize(jsonString);

    if (deserializeResult.isError()) {
      return ok(
          adminImportViewPartial
              .renderError(
                  "Error processing JSON",
                  deserializeResult.getErrors().stream().findFirst().orElseThrow())
              .render());
    }

    ProgramMigrationWrapper programMigrationWrapper = deserializeResult.getResult();
    if (programMigrationWrapper.getProgram() == null) {
      return ok(
          adminImportViewPartial
              .renderError(
                  "Error processing JSON", "JSON did not have a top-level \"program\" field")
              .render());
    }

    ProgramDefinition program = programMigrationWrapper.getProgram();
    ImmutableList<QuestionDefinition> questions = programMigrationWrapper.getQuestions();

    // Prevent admin from importing a program that already exists in the import environment
    String adminName = program.adminName();
    boolean programExists = programRepository.checkProgramAdminNameExists(adminName);
    if (programExists) {
      return ok(
          adminImportViewPartial
              .renderError(
                  "This program already exists in our system.",
                  "Please check your file and and try again.")
              .render());
    }

    boolean withDuplicates = !settingsManifest.getNoDuplicateQuestionsForMigrationEnabled(request);

    // When we are importing without duplicate questions, we expect all drafts to be published
    // before the import process begins.
    if (!withDuplicates
        && versionRepository.getDraftVersion().isPresent()
        && !versionRepository.getDraftVersion().get().getPrograms().isEmpty()) {
      return ok(
          adminImportViewPartial
              .renderError(
                  "There are draft programs and questions in our system.",
                  "Please publish all drafts and try again.")
              .render());
    }

    if (questions == null) {
      return ok(
          adminImportViewPartial
              .renderProgramData(request, program, questions, ImmutableMap.of(), jsonString, true)
              .render());
    }

    // Overwrite the admin names for any questions that already exist in the import environment so
    // we can create new versions of the questions.
    // This creates a map of the old question name -> updated question data
    // We want to do this even when we don't want to save the updated admin names
    // so that we can use the count of existing questions in the alert
    ImmutableMap<String, QuestionDefinition> updatedQuestionsMap =
        programMigrationService.maybeOverwriteQuestionName(questions);

    if (withDuplicates) {
      questions = ImmutableList.copyOf(updatedQuestionsMap.values());
    }

    ErrorAnd<String, String> serializeResult =
        programMigrationService.serialize(program, questions);
    if (serializeResult.isError()) {
      return badRequest(serializeResult.getErrors().stream().findFirst().orElseThrow());
    }

    return ok(
        adminImportViewPartial
            .renderProgramData(
                request,
                program,
                questions,
                updatedQuestionsMap,
                serializeResult.getResult(),
                withDuplicates)
            .render());
  }

  /**
   * Saves the imported program to the db. If there are questions on the program, perform the
   * neccessary question updates before saving the program
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result hxSaveProgram(Http.Request request) {
    if (!settingsManifest.getProgramMigrationEnabled(request)) {
      return notFound("Program import is not enabled");
    }

    ErrorAnd<ProgramMigrationWrapper, String> deserializeResult = getDeserializeResult(request);
    if (deserializeResult.isError()) {
      return ok(
          adminImportViewPartial
              .renderError(
                  "Error processing JSON",
                  deserializeResult.getErrors().stream().findFirst().orElseThrow())
              .render());
    }

    try {
      ProgramMigrationWrapper programMigrationWrapper = deserializeResult.getResult();
      ImmutableList<QuestionDefinition> questionsOnJson = programMigrationWrapper.getQuestions();
      ProgramDefinition programOnJson = programMigrationWrapper.getProgram();

      ProgramDefinition updatedProgram = programOnJson;

      if (questionsOnJson != null) {
        ImmutableMap<Long, QuestionDefinition> questionsOnJsonById =
            questionsOnJson.stream()
                .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getId, qd -> qd));

        ImmutableMap<String, QuestionDefinition> updatedQuestionsMap =
            updateEnumeratorIdsAndSaveAllQuestions(questionsOnJson, questionsOnJsonById);

        ImmutableList<BlockDefinition> updatedBlockDefinitions =
            updateBlockDefinitions(programOnJson, questionsOnJsonById, updatedQuestionsMap);

        updatedProgram =
            programOnJson.toBuilder().setBlockDefinitions(updatedBlockDefinitions).build();
      }

      ProgramModel savedProgram =
          programRepository.insertProgramSync(
              new ProgramModel(updatedProgram, versionRepository.getDraftVersionOrCreate()));

      // If we are re-using existing questions, we will put all programs in draft mode to ensure no
      // errors. We could also go through each question being updated and see what program it
      // applies to, but this is more straightforward.
      if (settingsManifest.getNoDuplicateQuestionsForMigrationEnabled(request)) {
        ImmutableList<Long> programsInDraft =
            versionRepository.getProgramsForVersion(versionRepository.getDraftVersion()).stream()
                .map(p -> p.id)
                .collect(ImmutableList.toImmutableList());
        versionRepository
            .getProgramsForVersion(versionRepository.getActiveVersion())
            .forEach(
                program -> {
                  if (!programsInDraft.contains(program.id)) {
                    programRepository.createOrUpdateDraft(program);
                  }
                });
      }

      // TODO(#7087) migrate application statuses for the program
      applicationStatusesRepository.createOrUpdateStatusDefinitions(
          updatedProgram.adminName(), new StatusDefinitions());

      return ok(
          adminImportViewPartial
              .renderProgramSaved(request, updatedProgram.adminName(), savedProgram.id)
              .render());
    } catch (RuntimeException error) {
      return ok(
          adminImportViewPartial
              .renderError(
                  "Unable to save program. Please try again or contact your IT team for support.",
                  "Error: " + error.toString())
              .render());
    }
  }

  private ErrorAnd<ProgramMigrationWrapper, String> getDeserializeResult(Http.Request request) {
    Form<AdminProgramImportForm> form =
        formFactory
            .form(AdminProgramImportForm.class)
            .bindFromRequest(request, AdminProgramImportForm.FIELD_NAMES.toArray(new String[0]));

    String jsonString = form.get().getProgramJson();

    return programMigrationService.deserialize(jsonString);
  }

  /**
   * Save all questions and then update enumerator child questions with the correct ids of their
   * newly saved parent questions.
   */
  private ImmutableMap<String, QuestionDefinition> updateEnumeratorIdsAndSaveAllQuestions(
      ImmutableList<QuestionDefinition> questionsOnJson,
      ImmutableMap<Long, QuestionDefinition> questionsOnJsonById) {

    // Save all the questions
    ImmutableList<QuestionModel> newlySavedQuestions =
        questionRepository.bulkCreateQuestions(questionsOnJson);

    ImmutableMap<String, QuestionDefinition> newlySaveQuestionsByAdminName =
        newlySavedQuestions.stream()
            .map(question -> question.getQuestionDefinition())
            .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getName, qd -> qd));

    ImmutableMap<String, QuestionDefinition> fullyUpdatedQuestions =
        newlySavedQuestions.stream()
            .map(
                question -> {
                  QuestionDefinition qd = question.getQuestionDefinition();
                  if (qd.getEnumeratorId().isPresent()) {
                    // The child question was saved with the incorrect enumerator id so we need to
                    // update it
                    Long oldEnumeratorId = qd.getEnumeratorId().get();
                    // Use the old enumerator id to get the admin name of the parent question off
                    // the old question map
                    String parentQuestionAdminName =
                        questionsOnJsonById.get(oldEnumeratorId).getName();
                    // Use the admin name to get the updated id for the parent question off the new
                    // question map
                    Long newlySavedParentQuestionId =
                        newlySaveQuestionsByAdminName.get(parentQuestionAdminName).getId();
                    // Update the child question with the correct id and save the question
                    qd = questionRepository.updateEnumeratorId(qd, newlySavedParentQuestionId);
                    qd = questionRepository.createOrUpdateDraft(qd).getQuestionDefinition();
                  }
                  return qd;
                })
            .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getName, qd -> qd));

    return fullyUpdatedQuestions;
  }

  /**
   * Update the block definitions on the program with the newly saved questions before saving the
   * program.
   */
  private ImmutableList<BlockDefinition> updateBlockDefinitions(
      ProgramDefinition programOnJson,
      ImmutableMap<Long, QuestionDefinition> questionsOnJsonById,
      ImmutableMap<String, QuestionDefinition> updatedQuestionsMap) {
    return programOnJson.blockDefinitions().stream()
        .map(
            blockDefinition -> {
              ImmutableList<ProgramQuestionDefinition> updatedProgramQuestionDefinitions =
                  blockDefinition.programQuestionDefinitions().stream()
                      .map(
                          pqd ->
                              updateProgramQuestionDefinition(
                                  pqd, questionsOnJsonById, updatedQuestionsMap))
                      .collect(ImmutableList.toImmutableList());

              BlockDefinition.Builder blockDefinitionBuilder =
                  maybeUpdatePredicates(blockDefinition, questionsOnJsonById, updatedQuestionsMap);
              blockDefinitionBuilder.setProgramQuestionDefinitions(
                  updatedProgramQuestionDefinitions);

              return blockDefinitionBuilder.build();
            })
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Map through each imported question and create a new ProgramQuestionDefinition to save on the
   * program. We use the old question id from the json to fetch the question admin name and match it
   * to the newly saved question so we can create a ProgramQuestionDefinition with the updated
   * question.
   */
  private ProgramQuestionDefinition updateProgramQuestionDefinition(
      ProgramQuestionDefinition programQuestionDefinitionFromJson,
      ImmutableMap<Long, QuestionDefinition> questionsOnJsonById,
      ImmutableMap<String, QuestionDefinition> updatedQuestionsMap) {
    Long id = programQuestionDefinitionFromJson.id();
    String adminName = questionsOnJsonById.get(id).getName();
    QuestionDefinition updatedQuestion = updatedQuestionsMap.get(adminName);
    return ProgramQuestionDefinition.create(
        updatedQuestion,
        Optional.empty(),
        programQuestionDefinitionFromJson.optional(),
        programQuestionDefinitionFromJson.addressCorrectionEnabled());
  }

  /**
   * If there are eligibility and/or visibility predicates on the questions, update those with the
   * id of the newly saved question.
   */
  private BlockDefinition.Builder maybeUpdatePredicates(
      BlockDefinition blockDefinition,
      ImmutableMap<Long, QuestionDefinition> questionsOnJsonById,
      ImmutableMap<String, QuestionDefinition> updatedQuestionsMap) {
    BlockDefinition.Builder blockDefinitionBuilder = blockDefinition.toBuilder();

    if (blockDefinition.visibilityPredicate().isPresent()) {
      PredicateDefinition visibilityPredicateDefinition =
          blockDefinition.visibilityPredicate().get();
      PredicateDefinition newPredicateDefinition =
          updatePredicateDefinition(
              visibilityPredicateDefinition, questionsOnJsonById, updatedQuestionsMap);
      blockDefinitionBuilder.setVisibilityPredicate(newPredicateDefinition);
    }
    if (blockDefinition.eligibilityDefinition().isPresent()) {
      PredicateDefinition eligibilityPredicateDefinition =
          blockDefinition.eligibilityDefinition().get().predicate();
      PredicateDefinition newPredicateDefinition =
          updatePredicateDefinition(
              eligibilityPredicateDefinition, questionsOnJsonById, updatedQuestionsMap);
      EligibilityDefinition newEligibilityDefinition =
          EligibilityDefinition.builder().setPredicate(newPredicateDefinition).build();
      blockDefinitionBuilder.setEligibilityDefinition(newEligibilityDefinition);
    }
    return blockDefinitionBuilder;
  }

  /**
   * Update the predicate definition by updating the predicate expression, starting with the root
   * node.
   */
  private PredicateDefinition updatePredicateDefinition(
      PredicateDefinition predicateDefinition,
      ImmutableMap<Long, QuestionDefinition> questionsOnJsonById,
      ImmutableMap<String, QuestionDefinition> updatedQuestionsMap) {
    return PredicateDefinition.create(
        updatePredicateExpression(
            predicateDefinition.rootNode(), questionsOnJsonById, updatedQuestionsMap),
        predicateDefinition.action());
  }

  /**
   * Update the eligibility or visibility predicate with the id from the newly saved question. We
   * use the old question id from the json to fetch the question admin name and match it to the
   * newly saved question so we can set the new question id on the predicate definition. We
   * recursively call this function on predicates with children.
   */
  private PredicateExpressionNode updatePredicateExpression(
      PredicateExpressionNode predicateExpressionNode,
      ImmutableMap<Long, QuestionDefinition> questionsOnJsonById,
      ImmutableMap<String, QuestionDefinition> updatedQuestionsMap) {

    switch (predicateExpressionNode.getType()) {
      case OR:
        OrNode orNode = predicateExpressionNode.getOrNode();
        ImmutableList<PredicateExpressionNode> orNodeChildren =
            orNode.children().stream()
                .map(
                    child ->
                        updatePredicateExpression(child, questionsOnJsonById, updatedQuestionsMap))
                .collect(ImmutableList.toImmutableList());
        return PredicateExpressionNode.create(OrNode.create(orNodeChildren));
      case AND:
        AndNode andNode = predicateExpressionNode.getAndNode();
        ImmutableList<PredicateExpressionNode> andNodeChildren =
            andNode.children().stream()
                .map(
                    child ->
                        updatePredicateExpression(child, questionsOnJsonById, updatedQuestionsMap))
                .collect(ImmutableList.toImmutableList());
        return PredicateExpressionNode.create(AndNode.create(andNodeChildren));
      case LEAF_ADDRESS_SERVICE_AREA:
        // TODO(#8450): Ensure we support service area validation.
      case LEAF_OPERATION:
        LeafOperationExpressionNode leafNode = predicateExpressionNode.getLeafOperationNode();
        Long oldQuestionId = leafNode.questionId();
        String questionAdminName = questionsOnJsonById.get(oldQuestionId).getName();
        Long newQuestionId = updatedQuestionsMap.get(questionAdminName).getId();
        return PredicateExpressionNode.create(
            leafNode.toBuilder().setQuestionId(newQuestionId).build());
      default:
        throw new IllegalStateException(
            String.format(
                "Unsupported predicate expression type for import: %s",
                predicateExpressionNode.getType()));
    }
  }
}
