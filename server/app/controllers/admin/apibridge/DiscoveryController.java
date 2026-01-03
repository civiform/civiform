package controllers.admin.apibridge;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.ApiBridgeConfigurationModel;
import org.pac4j.play.java.Secure;
import play.Environment;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.ValidationError;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import repository.ApiBridgeConfigurationRepository;
import services.AlertType;
import services.apibridge.ApiBridgeService;
import services.apibridge.ApiBridgeServiceDto;
import services.settings.SettingsManifest;
import views.admin.apibridge.MessagePartialView;
import views.admin.apibridge.MessagePartialViewModel;
import views.admin.apibridge.discovery.DiscoveryAddCommand;
import views.admin.apibridge.discovery.DiscoveryDetailsPartialView;
import views.admin.apibridge.discovery.DiscoveryDetailsPartialViewModel;
import views.admin.apibridge.discovery.DiscoveryPageView;
import views.admin.apibridge.discovery.DiscoveryPageViewModel;
import views.admin.apibridge.discovery.DiscoverySearchCommand;

/** This controller deals with the discovery and adding of api bridge endpoints. */
@Slf4j
public class DiscoveryController extends Controller {
  private final ClassLoaderExecutionContext classLoaderExecutionContext;
  private final SettingsManifest settingsManifest;
  private final Environment environment;
  private final ApiBridgeConfigurationRepository apiBridgeConfigurationRepository;
  private final ApiBridgeService apiBridgeService;
  private final FormFactory formFactory;
  private final DiscoveryPageView discoveryPageView;
  private final DiscoveryDetailsPartialView discoveryDetailsPartialView;
  private final MessagePartialView messagePartialView;

  @Inject
  public DiscoveryController(
      ClassLoaderExecutionContext classLoaderExecutionContext,
      SettingsManifest settingsManifest,
      Environment environment,
      ApiBridgeConfigurationRepository apiBridgeConfigurationRepository,
      ApiBridgeService apiBridgeService,
      FormFactory formFactory,
      DiscoveryPageView discoveryPageView,
      DiscoveryDetailsPartialView discoveryDetailsPartialView,
      MessagePartialView messagePartialView) {

    this.classLoaderExecutionContext = checkNotNull(classLoaderExecutionContext);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.environment = checkNotNull(environment);
    this.apiBridgeConfigurationRepository = checkNotNull(apiBridgeConfigurationRepository);
    this.apiBridgeService = checkNotNull(apiBridgeService);
    this.formFactory = checkNotNull(formFactory);
    this.discoveryPageView = checkNotNull(discoveryPageView);
    this.discoveryDetailsPartialView = checkNotNull(discoveryDetailsPartialView);
    this.messagePartialView = checkNotNull(messagePartialView);
  }

  /** Show the initial discovery page */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result discovery(Http.Request request) {
    if (!settingsManifest.getApiBridgeEnabled()) {
      return notFound();
    }

    var viewModel = new DiscoveryPageViewModel();
    return ok(discoveryPageView.render(request, viewModel)).as(Http.MimeTypes.HTML);
  }

  /** Displays the results of api bridge endpoints found by the discovery process. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> hxDiscoveryPopulate(Http.Request request) {
    if (!settingsManifest.getApiBridgeEnabled()) {
      return CompletableFuture.completedFuture(notFound());
    }

    Form<DiscoverySearchCommand> form =
        formFactory.form(DiscoverySearchCommand.class).bindFromRequest(request);

    if (form.hasErrors()) {
      Result result =
          createMessageView(
              request,
              AlertType.ERROR,
              form.errors().stream().map(ValidationError::message).toArray(String[]::new));
      return CompletableFuture.completedFuture(result);
    }

    DiscoverySearchCommand command = form.get();

    // Enforce https endpoints when in production mode
    if (environment.isProd() && !command.getHostUrl().startsWith("https://")) {
      return CompletableFuture.completedFuture(
          createMessageView(request, AlertType.ERROR, "URL must start with HTTPS"));
    }

    return getNonAddedDiscoveryEndpoints(command.getHostUrl())
        .thenApplyAsync(
            endpoints ->
                ok(discoveryDetailsPartialView.render(
                        request,
                        DiscoveryDetailsPartialViewModel.builder()
                            .hostUrl(command.getHostUrl())
                            .endpoints(endpoints)
                            .build()))
                    .as(Http.MimeTypes.HTML),
            classLoaderExecutionContext.current())
        .exceptionallyAsync(
            ex -> {
              log.error("hxDiscoveryPopulate", ex);
              return createMessageView(
                  request,
                  AlertType.ERROR,
                  "An error occurred. Verify the URL is correct and try again.");
            },
            classLoaderExecutionContext.current());
  }

  /** Filters out endpoints from the discovery list that are already added. */
  private CompletableFuture<ImmutableMap<String, ApiBridgeServiceDto.Endpoint>>
      getNonAddedDiscoveryEndpoints(String hostUrl) {
    var bridgeConfigurationsFuture =
        apiBridgeConfigurationRepository.findByHostUrl(hostUrl).toCompletableFuture();
    var discoverResponseFuture = apiBridgeService.discovery(hostUrl).toCompletableFuture();

    return CompletableFuture.allOf(bridgeConfigurationsFuture, discoverResponseFuture)
        .thenApplyAsync(
            v -> {
              var bridgeConfigurations = bridgeConfigurationsFuture.join();
              var errorAndList = discoverResponseFuture.join();

              if (errorAndList.isError()) {
                String errMsg =
                    errorAndList.getErrors().stream()
                        .map(
                            x ->
                                String.format(
                                    "title='%s' detail='%s' type='%s' status=%d",
                                    x.title(), x.detail(), x.type(), x.status()))
                        .collect(Collectors.joining(". "));

                log.error(
                    "Unable to load bridge discovery endpoint for: {}. Details: {}",
                    hostUrl,
                    errMsg);
                return ImmutableMap.of();
              }

              ImmutableSet<String> endpointsToRemove =
                  bridgeConfigurations.stream()
                      .map(ApiBridgeConfigurationModel::urlPath)
                      .collect(ImmutableSet.toImmutableSet());

              ImmutableMap<String, ApiBridgeServiceDto.Endpoint> endpointsThatCanBeAdded =
                  errorAndList.getResult().endpoints().entrySet().stream()
                      .filter(entry -> !endpointsToRemove.contains(entry.getKey()))
                      .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

              return endpointsThatCanBeAdded;
            });
  }

  /** Save the selected endpoint */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> hxAdd(Http.Request request) {
    if (!settingsManifest.getApiBridgeEnabled()) {
      return CompletableFuture.completedFuture(notFound());
    }

    Form<DiscoveryAddCommand> form =
        formFactory.form(DiscoveryAddCommand.class).bindFromRequest(request);

    if (form.hasErrors()) {
      Result result =
          createMessageView(
              request,
              AlertType.ERROR,
              form.errors().stream().map(ValidationError::message).toArray(String[]::new));
      return CompletableFuture.completedFuture(result);
    }

    DiscoveryAddCommand command = form.get();

    // Enforce https endpoints when in production mode
    if (environment.isProd() && !command.getHostUrl().startsWith("https://")) {
      return CompletableFuture.completedFuture(
          createMessageView(request, AlertType.ERROR, "URL must start with HTTPS"));
    }

    return apiBridgeService
        .discovery(command.getHostUrl())
        .thenComposeAsync(
            errorAnd -> {
              if (errorAnd.isError()) {
                throw new IllegalArgumentException();
              }

              ApiBridgeServiceDto.Endpoint endpoint =
                  errorAnd.getResult().endpoints().get(command.getUrlPath());

              if (endpoint == null) {
                throw new IllegalStateException();
              }

              return apiBridgeConfigurationRepository.insert(
                  new ApiBridgeConfigurationModel()
                      .setHostUrl(command.getHostUrl())
                      .setUrlPath(command.getUrlPath())
                      .setCompatibilityLevel(endpoint.compatibilityLevel())
                      .setAdminName(command.buildAdminName())
                      .setDescription(endpoint.description())
                      .setRequestSchema(endpoint.requestSchema().toString())
                      .setRequestSchemaChecksum(endpoint.requestSchemaChecksum())
                      .setResponseSchema(endpoint.responseSchema().toString())
                      .setResponseSchemaChecksum(endpoint.responseSchemaChecksum())
                      .setEnabled(true)
                      .setGlobalBridgeDefinition(
                          new ApiBridgeConfigurationModel.ApiBridgeDefinition(
                              ImmutableList.of(), ImmutableList.of())));
            },
            classLoaderExecutionContext.current())
        .thenApplyAsync(
            v -> createMessageView(request, AlertType.SUCCESS, "Saved successfully"),
            classLoaderExecutionContext.current())
        .exceptionallyAsync(
            ex -> {
              log.error("Failed to add endpoint", ex);
              return createMessageView(
                  request, AlertType.ERROR, "An error occurred trying to add endpoint");
            },
            classLoaderExecutionContext.current());
  }

  private Result createMessageView(Http.Request request, AlertType alertType, String... messages) {
    var viewModel = new MessagePartialViewModel(alertType, ImmutableList.copyOf(messages));
    return ok(messagePartialView.render(request, viewModel)).as(Http.MimeTypes.HTML);
  }
}
