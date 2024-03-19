package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import auth.Authorizers;
import auth.ProfileUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import controllers.CiviFormController;
import java.util.List;
import java.util.concurrent.CompletionStage;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.question.QuestionService;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.QuestionDefinition;
import views.admin.importexport.AdminImportExportView;
import views.admin.importexport.AdminProgramExportForm;

/** TODO */
public final class AdminImportExportController extends CiviFormController {
  private final AdminImportExportView adminImportExportView;
  private final FormFactory formFactory;
  private final ObjectMapper objectMapper;
  private final ProgramService programService;
  private final QuestionService questionService;

  @Inject
  public AdminImportExportController(
      AdminImportExportView adminImportExportView,
      FormFactory formFactory,
      ObjectMapper objectMapper,
      ProfileUtils profileUtils,
      ProgramService programService,
      QuestionService questionService,
      VersionRepository versionRepository) {
    super(profileUtils, versionRepository);
    this.adminImportExportView = adminImportExportView;
    this.formFactory = checkNotNull(formFactory);
    this.objectMapper = checkNotNull(objectMapper);
    this.programService = checkNotNull(programService);
    this.questionService = checkNotNull(questionService);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> index(Http.Request request) {
    // TODO: active or draft?
    return questionService
        .getReadOnlyQuestionService()
        .thenApplyAsync(
            readOnlyQuestionService ->
                ok(
                    adminImportExportView.render(
                        request,
                        programService.getActiveAndDraftPrograms().getActivePrograms(),
                        readOnlyQuestionService
                            .getActiveAndDraftQuestions()
                            .getActiveQuestions())));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> exportPrograms(Http.Request request) {
    Form<AdminProgramExportForm> form =
        formFactory.form(AdminProgramExportForm.class).bindFromRequest(request);

    List<Long> exportingIds = form.get().getProgramIds();
    System.out.println("exported IDs=" + exportingIds);

    // TODO: Need to fetch all the question definitions for the programs

    ImmutableList<ProgramDefinition> programs =
        exportingIds.stream()
            .map(
                programId -> {
                  try {
                    return programService.getFullProgramDefinition(programId);
                  } catch (ProgramNotFoundException e) {
                    throw new RuntimeException(e);
                  }
                })
            .collect(ImmutableList.toImmutableList());

    ImmutableSet.Builder<Long> questionsUsedByPrograms = ImmutableSet.builder();
    programs.forEach(
        programDefinition ->
            questionsUsedByPrograms.addAll(programDefinition.getQuestionIdsInProgram()));

    System.out.println("questions used= " + questionsUsedByPrograms.build());

    ImmutableList<QuestionDefinition> questions =
        questionsUsedByPrograms.build().stream()
            .map(
                questionId -> {
                  try {
                    return questionService
                        .getReadOnlyQuestionServiceSync()
                        .getQuestionDefinition(questionId);
                  } catch (QuestionNotFoundException e) {
                    throw new RuntimeException(e);
                  }
                })
            .collect(ImmutableList.toImmutableList());

    String programJson = getPrettyJson(programs);
    String questionJson = getPrettyJson(questions);

    return supplyAsync(
        () -> redirect(controllers.admin.routes.AdminImportExportController.index()));
    /*
    return questionService.getReadOnlyQuestionService()
            .thenApplyAsync(
                    readOnlyQuestionService ->
                            ok(adminImportExportView.render(request,
                                    programService.getActiveAndDraftPrograms().getActivePrograms(),
                                    readOnlyQuestionService.getActiveAndDraftQuestions().getActiveQuestions()))

            );

     */
  }

  // From DatabaseSeedView
  private <T> String getPrettyJson(ImmutableList<T> list) {
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(list);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
