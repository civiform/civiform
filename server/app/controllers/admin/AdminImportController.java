package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import controllers.CiviFormController;
import java.util.Map.Entry;
import mapping.admin.migration.AdminImportErrorListPartialMapper;
import mapping.admin.migration.AdminImportErrorPartialMapper;
import mapping.admin.migration.AdminImportPageMapper;
import mapping.admin.migration.AdminImportProgramDataPartialMapper;
import mapping.admin.migration.AdminImportProgramSavedPartialMapper;
import models.DisplayMode;
import models.ProgramModel;
import org.pac4j.play.java.Secure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import parsers.LargeFormUrlEncodedBodyParser;
import play.data.DynamicForm;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.BodyParser;
import play.mvc.Http;
import play.mvc.Result;
import repository.ProgramRepository;
import repository.VersionRepository;
import services.CiviFormError;
import services.ErrorAnd;
import services.migration.ProgramMigrationService;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramService;
import services.question.types.QuestionDefinition;
import services.settings.SettingsManifest;
import views.PartialView;
import views.admin.migration.AdminImportErrorListPartialViewModel;
import views.admin.migration.AdminImportErrorPartialViewModel;
import views.admin.migration.AdminImportPageView;
import views.admin.migration.AdminImportProgramDataPartialViewModel;
import views.admin.migration.AdminImportProgramSavedPartialViewModel;
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
  private final Logger logger = LoggerFactory.getLogger(AdminImportController.class);
  private final AdminImportView adminImportView;
  private final AdminImportViewPartial adminImportViewPartial;
  private final AdminImportPageView adminImportPageView;
  private final PartialView<AdminImportErrorPartialViewModel> adminImportErrorPartialView;
  private final PartialView<AdminImportErrorListPartialViewModel> adminImportErrorListPartialView;
  private final PartialView<AdminImportProgramDataPartialViewModel>
      adminImportProgramDataPartialView;
  private final PartialView<AdminImportProgramSavedPartialViewModel>
      adminImportProgramSavedPartialView;
  private final FormFactory formFactory;
  private final ProgramMigrationService programMigrationService;
  private final ProgramRepository programRepository;
  private final ProgramService programService;
  private final SettingsManifest settingsManifest;

  @Inject
  public AdminImportController(
      AdminImportView adminImportView,
      AdminImportViewPartial adminImportViewPartial,
      AdminImportPageView adminImportPageView,
      PartialView<AdminImportErrorPartialViewModel> adminImportErrorPartialView,
      PartialView<AdminImportErrorListPartialViewModel> adminImportErrorListPartialView,
      PartialView<AdminImportProgramDataPartialViewModel> adminImportProgramDataPartialView,
      PartialView<AdminImportProgramSavedPartialViewModel> adminImportProgramSavedPartialView,
      FormFactory formFactory,
      ProfileUtils profileUtils,
      ProgramMigrationService programMigrationService,
      VersionRepository versionRepository,
      ProgramRepository programRepository,
      ProgramService programService,
      SettingsManifest settingsManifest) {
    super(profileUtils, versionRepository);
    this.adminImportView = checkNotNull(adminImportView);
    this.adminImportViewPartial = checkNotNull(adminImportViewPartial);
    this.adminImportPageView = checkNotNull(adminImportPageView);
    this.adminImportErrorPartialView = checkNotNull(adminImportErrorPartialView);
    this.adminImportErrorListPartialView = checkNotNull(adminImportErrorListPartialView);
    this.adminImportProgramDataPartialView = checkNotNull(adminImportProgramDataPartialView);
    this.adminImportProgramSavedPartialView = checkNotNull(adminImportProgramSavedPartialView);
    this.formFactory = checkNotNull(formFactory);
    this.programMigrationService = checkNotNull(programMigrationService);
    this.programRepository = checkNotNull(programRepository);
    this.programService = checkNotNull(programService);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request) {
    if (settingsManifest.getAdminUiMigrationJ2htmlToThymeleafScEnabled(request)) {
      return ok(adminImportPageView.render(request, new AdminImportPageMapper().map()))
          .as(Http.MimeTypes.HTML);
    }

    return ok(adminImportView.render(request));
  }

  /**
   * HTMX Partial that parses and renders the program data included in the request. This uses {@link
   * parsers.LargeFormUrlEncodedBodyParser} to increase the request limit to 1 MiB. Program json is
   * often quite large so we need to increase the buffer limit above the Play default.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  @BodyParser.Of(LargeFormUrlEncodedBodyParser.class)
  public Result hxImportProgram(Http.Request request) {
    boolean thymeleafEnabled =
        settingsManifest.getAdminUiMigrationJ2htmlToThymeleafScEnabled(request);

    Form<AdminProgramImportForm> form =
        formFactory
            .form(AdminProgramImportForm.class)
            .bindFromRequest(request, AdminProgramImportForm.FIELD_NAMES.toArray(new String[0]));
    String jsonString = form.get().getProgramJson();
    if (jsonString == null) {
      // If they didn't upload anything, just re-render the main import page.
      return redirect(routes.AdminImportController.index().url());
    }

    try {
      ErrorAnd<ProgramMigrationWrapper, String> deserializeResult =
          programMigrationService.deserialize(jsonString);

      if (deserializeResult.isError()) {
        if (thymeleafEnabled) {
          return renderThymeleafImportError(
              request,
              "Error processing JSON",
              deserializeResult.getErrors().stream().findFirst().orElseThrow());
        }
        return ok(
            adminImportViewPartial
                .renderError(
                    "Error processing JSON",
                    deserializeResult.getErrors().stream().findFirst().orElseThrow())
                .render());
      }

      ProgramMigrationWrapper programMigrationWrapper = deserializeResult.getResult();

      if (programMigrationWrapper.getProgram() == null) {
        if (thymeleafEnabled) {
          return renderThymeleafImportError(
              request, "Error processing JSON", "JSON did not have a top-level \"program\" field");
        }
        return ok(
            adminImportViewPartial
                .renderError(
                    "Error processing JSON", "JSON did not have a top-level \"program\" field")
                .render());
      }

      ProgramDefinition program =
          programMigrationService.prepForImport(programMigrationWrapper.getProgram());
      ImmutableList<QuestionDefinition> questions = programMigrationWrapper.getQuestions();

      // Prevent admin from importing a program that already exists in the import environment
      String adminName = program.adminName();
      boolean programExists = programRepository.checkProgramAdminNameExists(adminName);
      if (programExists) {
        if (thymeleafEnabled) {
          return renderThymeleafImportError(
              request,
              "This program already exists in our system.",
              "Please check your file and and try again.");
        }
        return ok(
            adminImportViewPartial
                .renderError(
                    "This program already exists in our system.",
                    "Please check your file and and try again.")
                .render());
      }

      // Prevent admin from importing a program with visiblity set to "Visible to selected trusted
      // intermediaries only" since we don't migrate TI groups
      if (program.displayMode() == DisplayMode.SELECT_TI) {
        if (thymeleafEnabled) {
          return renderThymeleafImportError(
              request,
              "Display mode 'SELECT_TI' is not allowed.",
              "Please select another program display mode and try again");
        }
        return ok(
            adminImportViewPartial
                .renderError(
                    "Display mode 'SELECT_TI' is not allowed.",
                    "Please select another program display mode and try again")
                .render());
      }

      // Check that all block definition ids are positive numbers
      for (BlockDefinition blockDefintion : program.blockDefinitions()) {
        long blockId = blockDefintion.id();
        if (blockId < 1) {
          if (thymeleafEnabled) {
            return renderThymeleafImportError(
                request,
                "Block definition ids must be greater than 0.",
                "Please check your block definition ids and try again.");
          }
          return ok(
              adminImportViewPartial
                  .renderError(
                      "Block definition ids must be greater than 0.",
                      "Please check your block definition ids and try again.")
                  .render());
        }
      }

      // Check for other validation errors like invalid program admin names
      ImmutableList<String> notificationPreferences =
          program.notificationPreferences().stream()
              .map(preference -> preference.getValue())
              .collect(ImmutableList.toImmutableList());

      ImmutableSet<CiviFormError> programErrors =
          programService.validateProgramDataForCreate(
              program.adminName(),
              program.localizedName().getDefault(),
              program.localizedShortDescription().getDefault(),
              program.externalLink(),
              program.displayMode().getValue(),
              notificationPreferences,
              ImmutableList.of(), // categories are not migrated
              ImmutableList.of(), // associated TI groups are not migrated
              program.applicationSteps(),
              program.bridgeDefinitions(),
              program.programType());
      if (!programErrors.isEmpty()) {
        // We want to reference "admin name" instead of "URL" in errors, because there is no URL
        // field in program migration. The existing error strings were created for the program
        // create/edit UI which has a URL field that is generated from the admin name.
        String errorString = joinErrors(programErrors).replace("URL", "admin name");
        if (thymeleafEnabled) {
          return renderThymeleafImportError(
              request, "One or more program errors occured:", errorString);
        }
        return ok(
            adminImportViewPartial
                .renderError("One or more program errors occured:", errorString)
                .render());
      }

      if (questions == null) {
        if (thymeleafEnabled) {
          return renderThymeleafProgramData(
              request,
              program,
              questions,
              /* duplicateQuestionNames= */ ImmutableList.of(),
              jsonString);
        }
        return ok(
            adminImportViewPartial
                .renderProgramData(
                    request,
                    program,
                    questions,
                    /* duplicateQuestionNames= */ ImmutableList.of(),
                    jsonString)
                .render());
      }

      programMigrationService.validateQuestionKeyUniqueness(questions);
      ImmutableList<String> existingAdminNames =
          programMigrationService.getExistingAdminNames(questions);
      ImmutableSet<CiviFormError> questionErrors =
          programMigrationService.validateQuestions(program, questions, existingAdminNames);
      if (!questionErrors.isEmpty()) {
        if (thymeleafEnabled) {
          return ok(adminImportErrorListPartialView.render(
                  request,
                  new AdminImportErrorListPartialMapper()
                      .map("One or more question errors occured:", joinErrors(questionErrors))))
              .as(Http.MimeTypes.HTML);
        }
        return ok(
            adminImportViewPartial
                .renderErrorWithLineBreaks(
                    "One or more question errors occured:", joinErrors(questionErrors))
                .render());
      }

      ErrorAnd<String, String> serializeResult =
          programMigrationService.serialize(program, questions);
      if (serializeResult.isError()) {
        return badRequest(serializeResult.getErrors().stream().findFirst().orElseThrow());
      }

      if (thymeleafEnabled) {
        return renderThymeleafProgramData(
            request, program, questions, existingAdminNames, serializeResult.getResult());
      }
      return ok(
          adminImportViewPartial
              .renderProgramData(
                  request, program, questions, existingAdminNames, serializeResult.getResult())
              .render());
    } catch (RuntimeException e) {
      if (thymeleafEnabled) {
        return renderThymeleafImportError(
            request,
            "There was an error rendering your program.",
            "Please check your data and try again. Error: " + e.getMessage());
      }
      return ok(
          adminImportViewPartial
              .renderError(
                  "There was an error rendering your program.",
                  "Please check your data and try again. Error: " + e.getMessage())
              .render());
    }
  }

  /**
   * Saves the imported program to the db. If there are questions on the program, perform the
   * neccessary question updates before saving the program
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  @BodyParser.Of(LargeFormUrlEncodedBodyParser.class)
  public Result hxSaveProgram(Http.Request request) {
    boolean thymeleafEnabled =
        settingsManifest.getAdminUiMigrationJ2htmlToThymeleafScEnabled(request);

    ErrorAnd<ProgramMigrationWrapper, String> deserializeResult = getDeserializeResult(request);
    if (deserializeResult.isError()) {
      if (thymeleafEnabled) {
        return renderThymeleafImportError(
            request,
            "Error processing JSON",
            deserializeResult.getErrors().stream().findFirst().orElseThrow());
      }
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
      ImmutableMap<String, ProgramMigrationWrapper.DuplicateQuestionHandlingOption>
          duplicateHandlingOptions = programMigrationWrapper.getDuplicateQuestionHandlingOptions();

      ErrorAnd<ProgramModel, String> savedProgram =
          programMigrationService.saveImportedProgram(
              programOnJson, questionsOnJson, duplicateHandlingOptions);
      if (savedProgram.isError()) {
        if (thymeleafEnabled) {
          return renderThymeleafImportError(
              request,
              "Error saving program",
              savedProgram.getErrors().stream().findFirst().orElseThrow());
        }
        return ok(
            adminImportViewPartial
                .renderError(
                    "Error saving program",
                    savedProgram.getErrors().stream().findFirst().orElseThrow())
                .render());
      }
      ProgramDefinition savedProgramDefinition =
          programRepository.getShallowProgramDefinition(savedProgram.getResult());

      if (thymeleafEnabled) {
        return ok(adminImportProgramSavedPartialView.render(
                request,
                new AdminImportProgramSavedPartialMapper()
                    .map(savedProgramDefinition.adminName(), savedProgramDefinition.id())))
            .as(Http.MimeTypes.HTML);
      }
      return ok(
          adminImportViewPartial
              .renderProgramSaved(savedProgramDefinition.adminName(), savedProgramDefinition.id())
              .render());
    } catch (RuntimeException error) {
      logger.error("Error saving program", error);
      if (thymeleafEnabled) {
        return renderThymeleafImportError(
            request,
            "Unable to save program. Please try again or contact your IT team for support.",
            "Error: " + error.toString());
      }
      return ok(
          adminImportViewPartial
              .renderError(
                  "Unable to save program. Please try again or contact your IT team for support.",
                  "Error: " + error.toString())
              .render());
    }
  }

  /** Renders the Thymeleaf import-error partial. */
  private Result renderThymeleafImportError(
      Http.Request request, String title, String errorMessage) {
    return ok(adminImportErrorPartialView.render(
            request, new AdminImportErrorPartialMapper().map(title, errorMessage)))
        .as(Http.MimeTypes.HTML);
  }

  /** Renders the Thymeleaf program-preview partial. */
  private Result renderThymeleafProgramData(
      Http.Request request,
      ProgramDefinition program,
      ImmutableList<QuestionDefinition> questions,
      ImmutableList<String> duplicateQuestionNames,
      String json) {
    return ok(adminImportProgramDataPartialView.render(
            request,
            new AdminImportProgramDataPartialMapper()
                .map(program, questions, duplicateQuestionNames, json)))
        .as(Http.MimeTypes.HTML);
  }

  @VisibleForTesting
  ErrorAnd<ProgramMigrationWrapper, String> getDeserializeResult(Http.Request request) {
    DynamicForm form = formFactory.form().bindFromRequest(request);

    String programJsonString = form.get(AdminProgramImportForm.PROGRAM_JSON_FIELD);
    // Form data for handling duplicates all share a prefix. So we are looking for fields with
    // that prefix, and then removing the prefix to get the admin name. The form data in question
    // looks like:
    // `{DUPLICATE_QUESTION_HANDLING_FIELD_PREFIX}{adminName}->{duplicateHandlingOption}`
    ImmutableMap<String, String> adminNameToDuplicateHandling =
        form.rawData().entrySet().stream()
            .filter(
                field ->
                    field
                        .getKey()
                        .startsWith(
                            AdminProgramImportForm.DUPLICATE_QUESTION_HANDLING_FIELD_PREFIX))
            .map(
                field ->
                    Maps.immutableEntry(
                        field
                            .getKey()
                            .replace(
                                AdminProgramImportForm.DUPLICATE_QUESTION_HANDLING_FIELD_PREFIX,
                                ""),
                        field.getValue()))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

    return programMigrationService.deserialize(programJsonString, adminNameToDuplicateHandling);
  }
}
