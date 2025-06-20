package controllers.docs;

import static com.google.common.base.Preconditions.checkNotNull;
import static play.mvc.Results.badRequest;
import static play.mvc.Results.internalServerError;
import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;

import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import models.LifecycleStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;
import play.mvc.Result;
import services.DeploymentType;
import services.openapi.OpenApiSchemaGenerator;
import services.openapi.OpenApiSchemaGeneratorFactory;
import services.openapi.OpenApiSchemaSettings;
import services.openapi.OpenApiVersion;
import services.program.ProgramDefinition;
import services.program.ProgramDraftNotFoundException;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import views.docs.SchemaView;

/** This handles endpoints related to serving openapi schema data */
public final class OpenApiSchemaController {
  private final ProgramService programService;
  private final SettingsManifest settingsManifest;
  private final DeploymentType deploymentType;
  private final SchemaView schemaView;
  private static final Logger logger = LoggerFactory.getLogger(OpenApiSchemaController.class);

  @Inject
  public OpenApiSchemaController(
      ProgramService programService,
      SettingsManifest settingsManifest,
      DeploymentType deploymentType,
      SchemaView schemaView) {
    this.programService = checkNotNull(programService);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.deploymentType = checkNotNull(deploymentType);
    this.schemaView = checkNotNull(schemaView);
  }

  /** Endpoint to return the generated openapi schema */
  public Result getSchemaByProgramSlug(
      Http.Request request,
      String programSlug,
      Optional<String> stage,
      Optional<String> openApiVersion) {
    if (!settingsManifest.getApiGeneratedDocsEnabled(request)) {
      return notFound("API Docs are not enabled.");
    }

    LifecycleStage lifecycleStage =
        stage
            .map(x -> LifecycleStage.valueOf(x.toUpperCase(Locale.ROOT)))
            .orElse(LifecycleStage.ACTIVE);

    Optional<ProgramDefinition> optionalProgramDefinition =
        programService.getAllNonExternalProgramSlugs().contains(programSlug)
            ? getProgramDefinition(programSlug, lifecycleStage)
            : Optional.empty();

    if (optionalProgramDefinition.isEmpty()) {
      return notFound("No program found");
    }

    try {
      OpenApiVersion openApiVersionType = OpenApiVersion.fromString(openApiVersion);

      OpenApiSchemaSettings openApiSchemaSettings =
          new OpenApiSchemaSettings(
              settingsManifest.getBaseUrl().orElse(""),
              getEmailAddress(request),
              deploymentType.isDev());

      OpenApiSchemaGenerator openApiSchemaGenerator =
          OpenApiSchemaGeneratorFactory.createGenerator(openApiVersionType, openApiSchemaSettings);

      String response = openApiSchemaGenerator.createSchema(optionalProgramDefinition.get());
      return ok(response).as("text/yaml");
    } catch (RuntimeException ex) {
      String errorMsg =
          String.format(
              "Unable to generate OpenApi version '%s' for program '%s' at stage '%s'.",
              openApiVersion.orElse(""), programSlug, lifecycleStage.getValue());
      logger.error(errorMsg, ex);
      return badRequest(errorMsg);
    }
  }

  /** Get program definition for the specific slug and version */
  private Optional<ProgramDefinition> getProgramDefinition(
      String programSlug, LifecycleStage lifecycleStage) {
    try {
      switch (lifecycleStage) {
        case ACTIVE -> {
          ProgramDefinition activeProgramDefinition =
              programService
                  .getActiveFullProgramDefinitionAsync(programSlug)
                  .toCompletableFuture()
                  .join();
          return Optional.of(activeProgramDefinition);
        }
        case DRAFT -> {
          ProgramDefinition draftProgramDefinition =
              programService.getDraftFullProgramDefinition(programSlug);
          return Optional.of(draftProgramDefinition);
        }
        default -> {
          return Optional.empty();
        }
      }
    } catch (RuntimeException | ProgramDraftNotFoundException e) {
      return Optional.empty();
    }
  }

  /** Get either the IT email address or the support email address */
  private String getEmailAddress(Http.Request request) {
    Optional<String> contactEmailAddress =
        settingsManifest.getItEmailAddress(request).isPresent()
                && !settingsManifest.getItEmailAddress(request).get().isBlank()
            ? settingsManifest.getItEmailAddress(request)
            : settingsManifest.getSupportEmailAddress(request);
    return contactEmailAddress.orElse("");
  }

  /** Render the swagger ui to view the select swagger/openapi */
  public Result getSchemaUI(
      Http.Request request,
      String programSlug,
      Optional<String> stage,
      Optional<String> openApiVersion) {
    if (!settingsManifest.getApiGeneratedDocsEnabled(request)) {
      return notFound("API Docs are not enabled.");
    }

    ImmutableSet<String> allNonExternalProgramSlugs =
        programService.getAllNonExternalProgramSlugs();

    if (programSlug.isEmpty() || !allNonExternalProgramSlugs.contains(programSlug)) {
      programSlug = allNonExternalProgramSlugs.stream().findFirst().orElse("");
    }

    // This will only happen if there are no programs at all in the system
    if (programSlug.isEmpty()) {
      return ok("Please add a program.");
    }

    try {
      String url =
          routes.OpenApiSchemaController.getSchemaByProgramSlug(
                  programSlug,
                  Optional.of(stage.orElse(LifecycleStage.ACTIVE.getValue())),
                  Optional.of(openApiVersion.orElse(OpenApiVersion.OPENAPI_V3_0.toString())))
              .url();

      SchemaView.Form form = new SchemaView.Form(programSlug, stage, openApiVersion);

      return ok(schemaView.render(request, form, url, allNonExternalProgramSlugs));
    } catch (RuntimeException ex) {
      return internalServerError();
    }
  }

  /** Redirect to the swagger ui to view the select swagger/openapi */
  public Result getSchemaUIRedirect(
      Http.Request request,
      Optional<String> programSlug,
      Optional<String> stage,
      Optional<String> openApiVersion) {

    return getSchemaUI(request, programSlug.orElse(""), stage, openApiVersion);
  }
}
