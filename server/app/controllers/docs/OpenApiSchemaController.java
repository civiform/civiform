package controllers.docs;

import static com.google.common.base.Preconditions.checkNotNull;
import static play.mvc.Results.badRequest;
import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;

import auth.Authorizers;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import mapping.admin.docs.SchemaPageMapper;
import models.LifecycleStage;
import org.pac4j.play.java.Secure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.mvc.Result;
import services.DeploymentType;
import services.docs.ApiDocsService;
import services.openapi.OpenApiSchemaGenerator;
import services.openapi.OpenApiSchemaGeneratorFactory;
import services.openapi.OpenApiSchemaSettings;
import services.openapi.OpenApiVersion;
import services.program.ProgramDefinition;
import services.settings.SettingsManifest;
import views.admin.docs.SchemaPageView;
import views.docs.SchemaView;

/** This handles endpoints related to serving openapi schema data */
public final class OpenApiSchemaController {
  private final ApiDocsService apiDocsService;
  private final SettingsManifest settingsManifest;
  private final DeploymentType deploymentType;
  private final SchemaView schemaView;
  private final SchemaPageView schemaPageView;
  private final MessagesApi messagesApi;
  private static final Logger logger = LoggerFactory.getLogger(OpenApiSchemaController.class);

  @Inject
  public OpenApiSchemaController(
      ApiDocsService apiDocsService,
      SettingsManifest settingsManifest,
      DeploymentType deploymentType,
      SchemaView schemaView,
      SchemaPageView schemaPageView,
      MessagesApi messagesApi) {
    this.apiDocsService = checkNotNull(apiDocsService);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.deploymentType = checkNotNull(deploymentType);
    this.schemaView = checkNotNull(schemaView);
    this.schemaPageView = checkNotNull(schemaPageView);
    this.messagesApi = checkNotNull(messagesApi);
  }

  /** Endpoint to return the generated openapi schema */
  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
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
        apiDocsService.getProgramDefinition(programSlug, lifecycleStage);

    if (optionalProgramDefinition.isEmpty()) {
      return notFound(messagesApi.preferred(request).at("adminApiDocs.notFound"));
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
          messagesApi
              .preferred(request)
              .at(
                  "adminSchemaViewer.yamlError",
                  openApiVersion.orElse(""),
                  programSlug,
                  lifecycleStage.getValue());

      logger.error(errorMsg, ex);
      return badRequest(errorMsg);
    }
  }

  /** Get either the IT email address or the support email address */
  private String getEmailAddress(Http.Request request) {
    Optional<String> contactEmailAddress =
        settingsManifest.getItEmailAddress().isPresent()
                && !settingsManifest.getItEmailAddress().get().isBlank()
            ? settingsManifest.getItEmailAddress()
            : settingsManifest.getSupportEmailAddress(request);
    return contactEmailAddress.orElse("");
  }

  /** Render the swagger ui to view the select swagger/openapi */
  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result getSchemaUI(
      Http.Request request,
      String programSlug,
      Optional<String> stage,
      Optional<String> openApiVersion) {

    ImmutableSet<String> allNonExternalProgramSlugs =
        apiDocsService.getAllNonExternalProgramSlugs();

    if (programSlug.isEmpty() || !allNonExternalProgramSlugs.contains(programSlug)) {
      programSlug = allNonExternalProgramSlugs.stream().findFirst().orElse("");
    }

    // This will only happen if there are no programs at all in the system
    if (programSlug.isEmpty()) {
      return ok(messagesApi.preferred(request).at("adminApiDocs.notFound"));
    }

    String url =
        routes.OpenApiSchemaController.getSchemaByProgramSlug(
                programSlug,
                Optional.of(stage.orElse(LifecycleStage.ACTIVE.getValue())),
                Optional.of(openApiVersion.orElse(OpenApiVersion.OPENAPI_V3_0.toString())))
            .url();

    if (settingsManifest.getAdminUiMigrationScEnabled(request)) {
      return ok(schemaPageView.render(
              request,
              SchemaPageMapper.map(
                  messagesApi.preferred(request),
                  programSlug,
                  stage,
                  openApiVersion,
                  url,
                  allNonExternalProgramSlugs)))
          .as(Http.MimeTypes.HTML);
    }

    SchemaView.Form form = new SchemaView.Form(programSlug, stage, openApiVersion);

    return ok(schemaView.render(request, form, url, allNonExternalProgramSlugs));
  }

  /** Redirect to the swagger ui to view the select swagger/openapi */
  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result getSchemaUIRedirect(
      Http.Request request,
      Optional<String> programSlug,
      Optional<String> stage,
      Optional<String> openApiVersion) {

    return getSchemaUI(request, programSlug.orElse(""), stage, openApiVersion);
  }
}
