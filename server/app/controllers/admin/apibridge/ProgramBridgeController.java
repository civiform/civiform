package controllers.admin.apibridge;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.ApiBridgeConfigurationModel;
import models.ApiBridgeConfigurationModel.ApiBridgeDefinition;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.ValidationError;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import repository.ApiBridgeConfigurationRepository;
import repository.VersionRepository;
import services.AlertType;
import services.CiviFormError;
import services.JsonUtils;
import services.apibridge.ProgramBridgeService;
import services.applicant.question.Scalar;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.question.types.QuestionDefinition;
import services.settings.SettingsManifest;
import views.admin.apibridge.MessagePartialView;
import views.admin.apibridge.MessagePartialViewModel;
import views.admin.apibridge.programbridge.EditPageView;
import views.admin.apibridge.programbridge.EditPageViewModel;
import views.admin.apibridge.programbridge.ProgramBridgeEditCommand;
import views.admin.apibridge.programbridge.ProgramBridgeSaveCommand;
import views.admin.apibridge.programbridge.fragments.ProgramBridgeEditPartialView;
import views.admin.apibridge.programbridge.fragments.ProgramBridgeEditPartialViewModel;

/**
 * This controller handles working with configuring the api bridge settings for a specified program.
 */
@Slf4j
public class ProgramBridgeController extends Controller {
  private final ClassLoaderExecutionContext classLoaderExecutionContext;
  private final SettingsManifest settingsManifest;
  private final ProgramService programService;
  private final ProgramBridgeService programBridgeService;
  private final FormFactory formFactory;
  private final EditPageView editView;
  private final ProgramBridgeEditPartialView programBridgeEditPartialView;
  private final VersionRepository versionRepository;
  private final ApiBridgeConfigurationRepository apiBridgeConfigurationRepository;
  private final MessagePartialView messagePartialView;
  private final ObjectMapper mapper;

  @Inject
  public ProgramBridgeController(
      ClassLoaderExecutionContext classLoaderExecutionContext,
      SettingsManifest settingsManifest,
      ProgramService programService,
      ProgramBridgeService programBridgeService,
      FormFactory formFactory,
      EditPageView editView,
      ProgramBridgeEditPartialView programBridgeEditPartialView,
      VersionRepository versionRepository,
      ApiBridgeConfigurationRepository apiBridgeConfigurationRepository,
      MessagePartialView messagePartialView,
      ObjectMapper mapper) {
    this.classLoaderExecutionContext = checkNotNull(classLoaderExecutionContext);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.programService = checkNotNull(programService);
    this.programBridgeService = checkNotNull(programBridgeService);
    this.formFactory = checkNotNull(formFactory);
    this.editView = checkNotNull(editView);
    this.programBridgeEditPartialView = checkNotNull(programBridgeEditPartialView);
    this.versionRepository = checkNotNull(versionRepository);
    this.apiBridgeConfigurationRepository = checkNotNull(apiBridgeConfigurationRepository);
    this.messagePartialView = checkNotNull(messagePartialView);
    this.mapper = checkNotNull(mapper);
  }

  /** Primary edit page for the program api bridge configuration listing available api bridges */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> edit(Http.Request request, Long programId) {
    if (!settingsManifest.getApiBridgeEnabled()) {
      return CompletableFuture.completedFuture(notFound());
    }

    ProgramDefinition program;
    try {
      program = programService.getFullProgramDefinition(programId);
    } catch (ProgramNotFoundException ex) {
      return CompletableFuture.completedFuture(notFound());
    }

    Boolean isDraftProgram = versionRepository.isDraftProgram(programId);

    if (!isDraftProgram) {
      return CompletableFuture.completedFuture(
          badRequest(
              "Editing must be done with a draft program (programId=%s)".formatted(programId)));
    }

    return apiBridgeConfigurationRepository
        .findAll()
        .thenApplyAsync(
            allBridgeConfigurations -> {
              ImmutableList<QuestionDefinition> allQuestions =
                  programBridgeService.getAllowedQuestions(program);

              String questionScalarsJson =
                  JsonUtils.writeValueAsString(
                      mapper, programBridgeService.getQuestionScalarMap(allQuestions));

              EditPageViewModel viewModel =
                  EditPageViewModel.builder()
                      .programDefinition(program)
                      .isDraftProgram(isDraftProgram)
                      .enabledApiBridgeConfigurations(
                          allBridgeConfigurations.stream()
                              .filter(ApiBridgeConfigurationModel::enabled)
                              .collect(ImmutableList.toImmutableList()))
                      .questionScalarsJson(questionScalarsJson)
                      .build();

              return ok(editView.render(request, viewModel)).as(Http.MimeTypes.HTML);
            },
            classLoaderExecutionContext.current())
        .exceptionallyAsync(
            ex -> {
              log.error("Error loading edit (programId=%s)".formatted(programId), ex);
              return internalServerError();
            },
            classLoaderExecutionContext.current());
  }

  /** HTMX partial that returns the edit form for the specified api bridge */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> hxBridgeConfiguration(Http.Request request, Long programId) {
    if (!settingsManifest.getApiBridgeEnabled()) {
      return CompletableFuture.completedFuture(notFound());
    }

    Form<ProgramBridgeEditCommand> form =
        formFactory.form(ProgramBridgeEditCommand.class).bindFromRequest(request);

    if (form.hasErrors()) {
      // Not really an error, but if we get here without a bridgeAdminName
      // value from the select we just want to reset the child element to nothing
      return CompletableFuture.completedFuture(ok("").as(Http.MimeTypes.HTML));
    }

    ProgramBridgeEditCommand command = form.get();

    ProgramDefinition program;
    try {
      program = programService.getFullProgramDefinition(programId);
    } catch (ProgramNotFoundException e) {
      return CompletableFuture.completedFuture(notFound());
    }

    Boolean isDraftProgram = versionRepository.isDraftProgram(programId);

    if (!isDraftProgram) {
      return CompletableFuture.completedFuture(
          badRequest(
              "Editing must be done with a draft program (programId=%s)".formatted(programId)));
    }

    ImmutableList<QuestionDefinition> allQuestions =
        programBridgeService.getAllowedQuestions(program);

    return apiBridgeConfigurationRepository
        .findByAdminName(command.getBridgeAdminName())
        .thenApplyAsync(
            optionalModel -> {
              if (optionalModel.isEmpty()) {
                return notFound();
              }

              ApiBridgeConfigurationModel model = optionalModel.get();
              Optional<ApiBridgeDefinition> optionalBridgeDefinition =
                  Optional.ofNullable(
                      program.bridgeDefinitions().get(command.getBridgeAdminName()));

              var viewmodel =
                  ProgramBridgeEditPartialViewModel.builder()
                      .programId(programId)
                      .bridgeConfiguration(model)
                      .requestSchema(JsonUtils.readTree(mapper, model.requestSchema()))
                      .responseSchema(JsonUtils.readTree(mapper, model.responseSchema()))
                      .inputFields(
                          optionalBridgeDefinition
                              .map(x -> x.inputFields())
                              .orElse(ImmutableList.of()))
                      .outputFields(
                          optionalBridgeDefinition
                              .map(x -> x.outputFields())
                              .orElse(ImmutableList.of()))
                      .questions(allQuestions)
                      .build();

              return ok(programBridgeEditPartialView.render(request, viewmodel))
                  .as(Http.MimeTypes.HTML);
            },
            classLoaderExecutionContext.current())
        .exceptionallyAsync(
            ex -> {
              log.error(
                  "Error loading hxBridgeConfiguration (programId=%s)".formatted(programId), ex);
              return internalServerError();
            },
            classLoaderExecutionContext.current());
  }

  /** Form post endpoint to save configuration settings of the specified api bridge */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result save(Http.Request request, Long programId) {
    if (!settingsManifest.getApiBridgeEnabled()) {
      return notFound();
    }

    var form = formFactory.form(ProgramBridgeSaveCommand.class).bindFromRequest(request);

    if (form.hasErrors()) {
      return createErrorResponse(
          request, form.errors().stream().map(ValidationError::message).toArray(String[]::new));
    }

    var viewModel = form.get();

    try {
      ProgramDefinition programDefinition = programService.getFullProgramDefinition(programId);

      if (!versionRepository.isDraftProgram(programId)) {
        return badRequest(
            "Editing must be done with a draft program (programId=%s)".formatted(programId));
      }

      var errors =
          programService.updateBridgeConfiguration(
              programDefinition.id(),
              mapNewBridgeDefinitions(programDefinition.bridgeDefinitions(), viewModel));

      if (errors.isError()) {
        log.error(errors.toString());
        return createErrorResponse(
            request,
            errors.getErrors().stream().map(CiviFormError::message).toArray(String[]::new));
      }

      Long firstBlockId =
          programDefinition.blockDefinitions().stream()
              .findFirst()
              .map(BlockDefinition::id)
              .orElse(1L);

      return redirect(
          controllers.admin.routes.AdminProgramBlocksController.edit(programId, firstBlockId)
              .url());
    } catch (ProgramNotFoundException e) {
      return notFound();
    }
  }

  /** Maps the form post view model data to the model format that will be used when saving. */
  private ImmutableMap<String, ApiBridgeDefinition> mapNewBridgeDefinitions(
      ImmutableMap<String, ApiBridgeDefinition> oldBridgeDefinitions,
      ProgramBridgeSaveCommand viewModel) {
    return ImmutableMap.<String, ApiBridgeDefinition>builder()
        .putAll(oldBridgeDefinitions)
        .put(
            viewModel.getBridgeAdminName(),
            new ApiBridgeDefinition(
                viewModel.getInputFields().stream()
                    .map(
                        x ->
                            new ApiBridgeConfigurationModel.ApiBridgeDefinitionItem(
                                x.getQuestionName(),
                                Scalar.valueOf(x.getQuestionScalar()),
                                x.getExternalName()))
                    .collect(ImmutableList.toImmutableList()),
                viewModel.getOutputFields().stream()
                    .map(
                        x ->
                            new ApiBridgeConfigurationModel.ApiBridgeDefinitionItem(
                                x.getQuestionName(),
                                Scalar.valueOf(x.getQuestionScalar()),
                                x.getExternalName()))
                    .collect(ImmutableList.toImmutableList())))
        .buildKeepingLast();
  }

  /** Formats the output response for an error */
  private Result createErrorResponse(Http.Request request, String... messages) {
    var viewModel = new MessagePartialViewModel(AlertType.ERROR, ImmutableList.copyOf(messages));
    return ok(messagePartialView.render(request, viewModel)).as(Http.MimeTypes.HTML);
  }
}
