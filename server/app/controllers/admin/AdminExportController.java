package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.CiviFormController;
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
import services.settings.SettingsManifest;
import views.admin.migration.AdminExportView;
import views.admin.migration.AdminProgramExportForm;

/**
 * A controller for the export part of program migration (allowing admins to easily migrate programs
 * between different environments). This controller is responsible for exporting a program into a
 * JSON format. The JSON can then be imported to a different environment to re-create the program
 * there. {@link AdminImportController} is responsible for reading the program JSON and turning it
 * into a program definition with blocks and questions.
 *
 * <p>Typically, admins will export from one environment (e.g. staging) and import to another
 * environment (e.g. production).
 */
public class AdminExportController extends CiviFormController {
  private final AdminExportView adminExportView;
  private final FormFactory formFactory;
  private final ObjectMapper objectMapper;
  private final ProgramService programService;
  private final QuestionService questionService;
  private final SettingsManifest settingsManifest;

  @Inject
  public AdminExportController(
      AdminExportView adminExportView,
      FormFactory formFactory,
      ObjectMapper objectMapper,
      ProfileUtils profileUtils,
      ProgramService programService,
      QuestionService questionService,
      SettingsManifest settingsManifest,
      VersionRepository versionRepository) {
    super(profileUtils, versionRepository);
    this.adminExportView = checkNotNull(adminExportView);
    this.formFactory = checkNotNull(formFactory);
    // These extra modules let ObjectMapper serialize Guava types like ImmutableList.
    this.objectMapper =
        checkNotNull(objectMapper)
            .registerModule(new GuavaModule())
            .registerModule(new Jdk8Module());
    this.programService = checkNotNull(programService);
    this.questionService = checkNotNull(questionService);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request) {
    if (!settingsManifest.getProgramMigrationEnabled(request)) {
      return notFound("Program export is not enabled");
    }
    return ok(
        adminExportView.render(
            request,
            // TODO(#7087): Should we allow admins to export only active programs, only
            // draft programs, or both?
            programService.getActiveAndDraftPrograms().getActivePrograms()));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result exportProgram(Http.Request request) {
    if (!settingsManifest.getProgramMigrationEnabled(request)) {
      return notFound("Program export is not enabled");
    }
    Form<AdminProgramExportForm> form =
        formFactory
            .form(AdminProgramExportForm.class)
            .bindFromRequest(request, AdminProgramExportForm.FIELD_NAMES.toArray(new String[0]));

    Long programId = form.get().getProgramId();
    if (programId == null) {
      // If they didn't select anything, just re-render the main export page.
      return redirect(routes.AdminExportController.index().url());
    }

    ProgramDefinition program;
    try {
      program = programService.getFullProgramDefinition(programId);
    } catch (ProgramNotFoundException e) {
      return badRequest(String.format("Program with ID %s could not be found", programId));
    }

    ImmutableList<QuestionDefinition> questionsInProgram =
        program.getQuestionIdsInProgram().stream()
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

    String programJson;
    try {
      programJson =
          objectMapper
              .writerWithDefaultPrettyPrinter()
              .writeValueAsString(new ProgramMigration(program, questionsInProgram));
    } catch (JsonProcessingException e) {
      return badRequest(String.format("Program could not be serialized: %s", e));
    }

    String filename = program.adminName() + "-exported.json";
    return ok(programJson)
        .as(Http.MimeTypes.JSON)
        .withHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
  }
}
