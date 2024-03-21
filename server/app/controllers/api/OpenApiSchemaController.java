package controllers.api;

import static com.google.common.base.Preconditions.checkNotNull;
import static play.mvc.Results.badRequest;
import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;

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

public final class OpenApiSchemaController {

  private final ProgramService programService;
  private final SettingsManifest settingsManifest;
  private final DeploymentType deploymentType;
  private static final Logger logger = LoggerFactory.getLogger(OpenApiSchemaController.class);

  @Inject
  public OpenApiSchemaController(
      ProgramService programService,
      SettingsManifest settingsManifest,
      DeploymentType deploymentType) {
    this.programService = checkNotNull(programService);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.deploymentType = checkNotNull(deploymentType);
  }

  public Result getSchemaByProgramSlug(
      Http.Request request,
      String programSlug,
      Optional<String> stage,
      Optional<String> openApiVersion) {
    LifecycleStage lifecycleStage =
        stage
            .map(x -> LifecycleStage.valueOf(x.toUpperCase(Locale.ROOT)))
            .orElse(LifecycleStage.ACTIVE);

    Optional<ProgramDefinition> optionalProgramDefinition =
        getProgramDefinition(programSlug, lifecycleStage);

    if (optionalProgramDefinition.isEmpty()) {
      return notFound("No program found");
    }

    OpenApiVersion openApiVersionType = OpenApiVersion.fromString(openApiVersion);

    OpenApiSchemaSettings openApiSchemaSettings =
        OpenApiSchemaSettings.builder()
            .setBaseUrl(settingsManifest.getBaseUrl().orElse(""))
            .setItEmailAddress(getEmailAddress(request))
            .setAllowHttpScheme(deploymentType.isDev())
            .build();

    OpenApiSchemaGenerator openApiSchemaGenerator =
        OpenApiSchemaGeneratorFactory.createGenerator(openApiVersionType, openApiSchemaSettings);

    try {
      String response = openApiSchemaGenerator.createSchema(optionalProgramDefinition.get());

      // Setting result mime type to text/yaml or application/x-yaml forces file to download
      return ok(response);
    } catch (RuntimeException ex) {
      String errorMsg =
          String.format(
              "Unable to generate OpenApi version \"%s\" for program \"%s\" at stage \"%s\".",
              openApiVersionType, programSlug, lifecycleStage.getValue());
      logger.error(errorMsg, ex);
      return badRequest(errorMsg);
    }
  }

  /** Get program definition for the specific slug and version */
  private Optional<ProgramDefinition> getProgramDefinition(
      String programSlug, LifecycleStage lifecycleStage) {
    try {
      switch (lifecycleStage) {
        case ACTIVE:
          ProgramDefinition activeProgramDefinition =
              programService
                  .getActiveFullProgramDefinitionAsync(programSlug)
                  .toCompletableFuture()
                  .join();
          return Optional.of(activeProgramDefinition);
        case DRAFT:
          ProgramDefinition draftProgramDefinition =
              programService.getDraftFullProgramDefinition(programSlug);
          return Optional.of(draftProgramDefinition);
        default:
          return Optional.empty();
      }
    } catch (RuntimeException | ProgramDraftNotFoundException e) {
      return Optional.empty();
    }
  }

  /** Get either IT email address or the support email address */
  private String getEmailAddress(Http.Request request) {
    Optional<String> contactEmailAddress =
        settingsManifest.getItEmailAddress(request).isPresent()
                && !settingsManifest.getItEmailAddress(request).get().isBlank()
            ? settingsManifest.getItEmailAddress(request)
            : settingsManifest.getSupportEmailAddress(request);
    return contactEmailAddress.orElse("");
  }
}
