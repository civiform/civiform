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
import services.program.predicate.LeafOperationExpressionNode;
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
                  "Program already exists",
                  "A program with the admin name "
                      + adminName
                      + " already exists. Please try again.")
              .render());
    }

    // Get the admin names of any incoming questions that already exist in the import environment so
    // we can warn the user that new versions of these questions will be created
    ImmutableList<String> matchingQuestionAdminNames =
        questionRepository.getMatchingAdminNames(questions);

    // Overwrite the admin names for any questions that already exist in the import environment so
    // we can create new versions of the questions
    questions = programMigrationService.maybeOverwriteQuestionName(questions);

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
                matchingQuestionAdminNames,
                serializeResult.getResult())
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

      // TODO(#7087) migrate application statuses for the program
      applicationStatusesRepository.createOrUpdateStatusDefinitions(
          updatedProgram.adminName(), new StatusDefinitions());

      return ok(
          adminImportViewPartial
              .renderProgramSaved(updatedProgram.adminName(), savedProgram.id)
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
   * Update enumerator type questions (and other questions that do not have the `enumeratorId` field
   * set) first so that we can update questions that reference the id of the enumerator question
   * with the id of the newly saved enumerator question.
   */
  private ImmutableMap<String, QuestionDefinition> updateEnumeratorIdsAndSaveAllQuestions(
      ImmutableList<QuestionDefinition> questionsOnJson,
      ImmutableMap<Long, QuestionDefinition> questionsOnJsonById) {

    ImmutableList<QuestionDefinition> questionsWithoutEnumeratorId =
        questionsOnJson.stream()
            .filter(qd -> qd.getEnumeratorId().isEmpty())
            .collect(ImmutableList.toImmutableList());

    ImmutableMap<String, QuestionDefinition> updatedQuestionsWithoutEnumeratorId =
        questionRepository.bulkCreateQuestions(questionsWithoutEnumeratorId).stream()
            .map(question -> question.getQuestionDefinition())
            .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getName, qd -> qd));

    ImmutableList<QuestionDefinition> questionsWithEnumeratorId =
        questionsOnJson.stream()
            .filter(qd -> qd.getEnumeratorId().isPresent())
            .map(
                qd -> {
                  Long oldEnumeratorId = qd.getEnumeratorId().get();
                  QuestionDefinition oldQuestion = questionsOnJsonById.get(oldEnumeratorId);
                  String oldQuestionAdminName = oldQuestion.getName();
                  QuestionDefinition newQuestion =
                      updatedQuestionsWithoutEnumeratorId.get(oldQuestionAdminName);
                  // update the enumeratorId on the child question before saving
                  Long newEnumeratorId = newQuestion.getId();
                  return questionRepository.updateEnumeratorId(qd, newEnumeratorId);
                })
            .collect(ImmutableList.toImmutableList());

    ImmutableMap<String, QuestionDefinition> updatedQuestionsWithEnumeratorId =
        questionRepository.bulkCreateQuestions(questionsWithEnumeratorId).stream()
            .map(question -> question.getQuestionDefinition())
            .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getName, qd -> qd));

    return ImmutableMap.<String, QuestionDefinition>builder()
        .putAll(updatedQuestionsWithoutEnumeratorId)
        .putAll(updatedQuestionsWithEnumeratorId)
        .build();
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
   * Update the eligibility or visibility predicate with the id from the newly saved question. We
   * use the old question id from the json to fetch the question admin name and match it to the
   * newly saved question so we can set the new question id on the predicate definition.
   */
  private PredicateDefinition updatePredicateDefinition(
      PredicateDefinition predicateDefinition,
      ImmutableMap<Long, QuestionDefinition> questionsOnJsonById,
      ImmutableMap<String, QuestionDefinition> updatedQuestionsMap) {

    LeafOperationExpressionNode leafNode = predicateDefinition.rootNode().getLeafOperationNode();

    Long oldQuestionId = leafNode.questionId();
    String questionAdminName = questionsOnJsonById.get(oldQuestionId).getName();
    Long newQuestionId = updatedQuestionsMap.get(questionAdminName).getId();

    PredicateExpressionNode newPredicateExpressionNode =
        PredicateExpressionNode.create(leafNode.toBuilder().setQuestionId(newQuestionId).build());

    return PredicateDefinition.create(newPredicateExpressionNode, predicateDefinition.action());
  }
}
