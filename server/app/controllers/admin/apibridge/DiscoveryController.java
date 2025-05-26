package controllers.admin.apibridge;

import static autovalue.shaded.com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.ApiBridgeConfigurationModel;
import org.pac4j.play.java.Secure;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import repository.ApiBridgeRepository;
import services.apibridge.ApiBridgeService;
import services.apibridge.ApiBridgeServiceDto;
import views.admin.apibridge.discovery.DiscoveryDetailsView;
import views.admin.apibridge.discovery.DiscoveryDetailsViewModel;
import views.admin.apibridge.discovery.DiscoveryView;
import views.admin.apibridge.discovery.DiscoveryViewModel;

public class DiscoveryController extends Controller {
  private final ClassLoaderExecutionContext classLoaderExecutionContext;
  private final ApiBridgeRepository apiBridgeRepository;
  private final ApiBridgeService apiBridgeService;
  private final FormFactory formFactory;
  private final DiscoveryView discoveryView;
  private final DiscoveryDetailsView discoveryDetailsView;

  @Inject
  public DiscoveryController(
      ClassLoaderExecutionContext classLoaderExecutionContext,
      ApiBridgeRepository apiBridgeRepository,
      ApiBridgeService apiBridgeService,
      FormFactory formFactory,
      DiscoveryView discoveryView,
      DiscoveryDetailsView discoveryDetailsView) {
    this.classLoaderExecutionContext = checkNotNull(classLoaderExecutionContext);
    this.apiBridgeRepository = checkNotNull(apiBridgeRepository);
    this.apiBridgeService = checkNotNull(apiBridgeService);
    this.formFactory = checkNotNull(formFactory);
    this.discoveryView = checkNotNull(discoveryView);
    this.discoveryDetailsView = checkNotNull(discoveryDetailsView);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> discovery(Http.Request request) {
    return CompletableFuture.completedFuture(
        ok(discoveryView.render(request, new DiscoveryViewModel())).as(Http.MimeTypes.HTML));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> hxDiscoveryPopulate(Http.Request request) {

    DiscoveryViewModel model =
        formFactory.form(DiscoveryViewModel.class).bindFromRequest(request).get();

    return apiBridgeService
        .discovery(model.hostUri)
        .thenApplyAsync(
            list ->
                ok(discoveryDetailsView.render(
                        request, new DiscoveryDetailsViewModel(model.hostUri(), list)))
                    .as(Http.MimeTypes.HTML),
            classLoaderExecutionContext.current());
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> hxAdd(Http.Request request) {
    DynamicForm form = formFactory.form().bindFromRequest(request);

    String hostUri = form.get("hostUri");
    String uriPath = form.get("uriPath");

    return apiBridgeService
        .discovery(hostUri)
        .thenComposeAsync(
            list -> {
              ApiBridgeServiceDto.Endpoint endpoint = list.endpoints().get(uriPath);

              if (endpoint == null) {
                return CompletableFuture.completedFuture(null);
              }

              return apiBridgeRepository.insert(
                  new ApiBridgeConfigurationModel()
                      .setHostUri(hostUri)
                      .setUriPath(uriPath)
                      .setCompatibilityLevel(endpoint.compatibilityLevel().name())
                      .setDescription(endpoint.description())
                      .setRequestSchema(endpoint.requestSchema().toString())
                      .setRequestSchemaChecksum(endpoint.requestSchemaChecksum())
                      .setResponseSchema(endpoint.responseSchema().toString())
                      .setResponseSchemaChecksum(endpoint.responseSchemaChecksum())
                      .setEnabled(true)
                      .setGlobalBridgeDefinition("{}"));
            },
            classLoaderExecutionContext.current())
        .thenApplyAsync(
            apiBridgeConfigurationModel -> {
              if (apiBridgeConfigurationModel == null || apiBridgeConfigurationModel.getId() == 0) {
                return internalServerError("bad").as(Http.MimeTypes.HTML);
              }

              return ok("ok").as(Http.MimeTypes.HTML);
            },
            classLoaderExecutionContext.current())
        .exceptionally(x -> internalServerError("error"));
  }
}
