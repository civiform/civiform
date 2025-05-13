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
import play.mvc.Results;
import repository.ApiBridgeRepository;
import views.admin.apibridge.bridgeconfiguration.EditView;
import views.admin.apibridge.bridgeconfiguration.EditViewModel;
import views.admin.apibridge.bridgeconfiguration.ListView;
import views.admin.apibridge.bridgeconfiguration.ListViewModel;

public class BridgeConfigurationController extends Controller {
  private final ClassLoaderExecutionContext classLoaderExecutionContext;
  private final ApiBridgeRepository apiBridgeRepository;
  private final FormFactory formFactory;
  private final ListView listView;
  private final EditView editView;

  @Inject
  public BridgeConfigurationController(
      ClassLoaderExecutionContext classLoaderExecutionContext,
      ApiBridgeRepository apiBridgeRepository,
      FormFactory formFactory,
      ListView listView,
      EditView editView) {
    this.classLoaderExecutionContext = checkNotNull(classLoaderExecutionContext);
    this.apiBridgeRepository = checkNotNull(apiBridgeRepository);
    this.formFactory = checkNotNull(formFactory);
    this.listView = checkNotNull(listView);
    this.editView = checkNotNull(editView);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> hxDelete(Http.Request request, Long id) {
    return apiBridgeRepository
        .delete(id)
        .thenApplyAsync(
            x -> x ? ok("deleted") : badRequest("failed"), classLoaderExecutionContext.current());
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> list(Http.Request request) {
    return apiBridgeRepository
        .getAll()
        .thenApplyAsync(
            list -> ok(listView.render(request, new ListViewModel(list))).as(Http.MimeTypes.HTML),
            classLoaderExecutionContext.current());
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> edit(Http.Request request, Long id) {
    return apiBridgeRepository
        .getById(id)
        .thenApplyAsync(
            optionalItem ->
                optionalItem
                    .map(
                        item ->
                            ok(editView.render(request, EditViewModel.create(item)))
                                .as(Http.MimeTypes.HTML))
                    .orElseGet(Results::notFound),
            classLoaderExecutionContext.current());
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> save(Http.Request request, Long id) {
    DynamicForm form = formFactory.form().bindFromRequest(request);
    EditViewModel viewModel = EditViewModel.create(form);

    if (form.hasErrors()) {
      return CompletableFuture.completedFuture(badRequest(editView.render(request, viewModel)));
    }

    ApiBridgeConfigurationModel model =
        new ApiBridgeConfigurationModel()
            .setId(id)
            .setHostUri(viewModel.hostUri())
            .setUriPath(viewModel.uriPath())
            .setCompatibilityLevel(viewModel.compatibilityLevel())
            .setDescription(viewModel.description())
            .setRequestSchema(viewModel.requestSchema())
            .setRequestSchemaChecksum(viewModel.requestSchemaChecksum())
            .setResponseSchema(viewModel.responseSchema())
            .setResponseSchemaChecksum(viewModel.responseSchemaChecksum())
            .setGlobalBridgeDefinition(viewModel.globalBridgeDefinition())
            .setEnabled(viewModel.isEnabled());

    return apiBridgeRepository
        .update(model)
        .thenApplyAsync(x -> redirect(routes.BridgeConfigurationController.edit(id).url()));
  }
}
