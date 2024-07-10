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
      QuestionRepository questionRepository) {
    super(profileUtils, versionRepository);
    this.adminImportView = checkNotNull(adminImportView);
    this.adminImportViewPartial = checkNotNull(adminImportViewPartial);
    this.formFactory = checkNotNull(formFactory);
    this.programMigrationService = checkNotNull(programMigrationService);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.programRepository = checkNotNull(programRepository);
    this.questionRepository = checkNotNull(questionRepository);
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
              .renderError(deserializeResult.getErrors().stream().findFirst().orElseThrow())
              .render());
    }

    ProgramMigrationWrapper programMigrationWrapper = deserializeResult.getResult();
    if (programMigrationWrapper.getProgram() == null) {
      return ok(
          adminImportViewPartial
              .renderError("JSON did not have a top-level \"program\" field")
              .render());
    }
    return ok(
        adminImportViewPartial
            .renderProgramData(request, programMigrationWrapper, jsonString)
            .render());
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result saveProgram(Http.Request request) {
    if (!settingsManifest.getProgramMigrationEnabled(request)) {
      return notFound("Program import is not enabled");
    }

    ErrorAnd<ProgramMigrationWrapper, String> deserializeResult = getDeserializeResult(request);
    if (deserializeResult.isError()) {
      return ok(
          adminImportViewPartial
              .renderError(deserializeResult.getErrors().stream().findFirst().orElseThrow())
              .render());
    }

    ProgramMigrationWrapper programMigrationWrapper = deserializeResult.getResult();
    ImmutableList<QuestionDefinition> questionsOnJson = programMigrationWrapper.getQuestions();
    ProgramDefinition programOnJson = programMigrationWrapper.getProgram();

    ProgramDefinition updatedProgram = programOnJson;

    if (questionsOnJson != null) {
      ImmutableMap<Long, QuestionDefinition> questionsOnJsonById =
          questionsOnJson.stream()
              .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getId, qd -> qd));

      ImmutableMap<String, QuestionDefinition> updatedQuestionsMap =
          updateEnumeratorQuestionsAndSave(questionsOnJson, questionsOnJsonById);
      ImmutableList<BlockDefinition> updatedBlockDefinitions =
          updateBlockDefinitions(programOnJson, questionsOnJsonById, updatedQuestionsMap);
      updatedProgram =
          programOnJson.toBuilder().setBlockDefinitions(updatedBlockDefinitions).build();
    }

    programRepository.insertProgramSync(
        new ProgramModel(updatedProgram, versionRepository.getDraftVersionOrCreate()));

    return ok(adminImportView.render(request));
  }

  private ErrorAnd<ProgramMigrationWrapper, String> getDeserializeResult(Http.Request request) {
    Form<AdminProgramImportForm> form =
        formFactory
            .form(AdminProgramImportForm.class)
            .bindFromRequest(request, AdminProgramImportForm.FIELD_NAMES.toArray(new String[0]));

    String jsonString = form.get().getProgramJson();

    return programMigrationService.deserialize(jsonString);
  }

  private ImmutableMap<String, QuestionDefinition> updateEnumeratorQuestionsAndSave(
      ImmutableList<QuestionDefinition> questionsOnJson,
      ImmutableMap<Long, QuestionDefinition> questionsOnJsonById) {
    // We have to update questions that do not have an enumerator id first so that we can update
    // questions
    // that reference an enumerator id with the id of the newly saved enumerator question
    ImmutableList<QuestionDefinition> nonEnumeratorQuestions =
        questionsOnJson.stream()
            .filter(qd -> qd.getEnumeratorId().isEmpty())
            .collect(ImmutableList.toImmutableList());
    ImmutableMap<String, QuestionDefinition> updatedNonEnumeratorQuestionsMap =
        questionRepository.bulkCreateQuestions(nonEnumeratorQuestions).stream()
            .map(question -> question.getQuestionDefinition())
            .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getName, qd -> qd));

    ImmutableList<QuestionDefinition> enumeratorQuestions =
        questionsOnJson.stream()
            .filter(qd -> qd.getEnumeratorId().isPresent())
            .map(
                qd -> {
                  Long oldEnumeratorId = qd.getEnumeratorId().get();
                  QuestionDefinition oldQuestion = questionsOnJsonById.get(oldEnumeratorId);
                  String oldQuestionAdminName = oldQuestion.getName();
                  QuestionDefinition newQuestion =
                      updatedNonEnumeratorQuestionsMap.get(oldQuestionAdminName);
                  Long newEnumeratorId = newQuestion.getId();
                  return questionRepository.updateEnumeratorId(qd, newEnumeratorId);
                })
            .collect(ImmutableList.toImmutableList());

    ImmutableMap<String, QuestionDefinition> updatedEnumeratorQuestionsMap =
        questionRepository.bulkCreateQuestions(enumeratorQuestions).stream()
            .map(question -> question.getQuestionDefinition())
            .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getName, qd -> qd));

    return ImmutableMap.<String, QuestionDefinition>builder()
        .putAll(updatedNonEnumeratorQuestionsMap)
        .putAll(updatedEnumeratorQuestionsMap)
        .build();
  }

  private ProgramQuestionDefinition updateProgramQuestionDefinition(
      ProgramQuestionDefinition programQuestionDefinitionFromJson,
      ImmutableMap<Long, QuestionDefinition> questionsOnJsonById,
      ImmutableMap<String, QuestionDefinition> updatedQuestionsMap) {
    Long id = programQuestionDefinitionFromJson.id();
    // This is how we turn the updated question into a ProgramQuestionDefinition
    // so it can be inserted into the BlockDefinition
    // The BlockDefinition contains the id of the question from the old environment
    // so we need to use the unique question name (admin name) to associate
    // the question from the old environment with the version of that question
    // that we just saved in the new environment.
    QuestionDefinition questionOnJson = questionsOnJsonById.get(id);
    QuestionDefinition updatedQuestion = updatedQuestionsMap.get(questionOnJson.getName());
    return ProgramQuestionDefinition.create(
        updatedQuestion,
        Optional.empty(),
        programQuestionDefinitionFromJson.optional(),
        programQuestionDefinitionFromJson.addressCorrectionEnabled());
  }

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
}
