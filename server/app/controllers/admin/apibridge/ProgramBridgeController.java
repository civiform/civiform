package controllers.admin.apibridge;

import static autovalue.shaded.com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.BridgeDefinition;
import models.ProgramNotificationPreference;
import org.pac4j.play.java.Secure;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.LocalizedStrings;
import services.TranslationNotFoundException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import views.admin.apibridge.programbridge.EditView;
import views.admin.apibridge.programbridge.EditViewModel;
import views.admin.apibridge.programbridge.ProgramBridgeEditViewModel;

public class ProgramBridgeController extends Controller {
  private final ClassLoaderExecutionContext classLoaderExecutionContext;
  private final ProgramService programService;
  private final FormFactory formFactory;
  private final EditView editView;

  @Inject
  public ProgramBridgeController(
      ClassLoaderExecutionContext classLoaderExecutionContext,
      ProgramService programService,
      FormFactory formFactory,
      EditView editView) {
    this.classLoaderExecutionContext = checkNotNull(classLoaderExecutionContext);
    this.programService = checkNotNull(programService);
    this.formFactory = checkNotNull(formFactory);
    this.editView = checkNotNull(editView);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> edit(Http.Request request, Long id) {
    try {
      ProgramDefinition program = programService.getFullProgramDefinition(id);

      return CompletableFuture.completedFuture(
          ok(editView.render(request, new EditViewModel(program))).as(Http.MimeTypes.HTML));
    } catch (ProgramNotFoundException e) {
      return CompletableFuture.completedFuture(notFound(e.toString()));
    }
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public CompletionStage<Result> save(Http.Request request, Long id) {
    DynamicForm form = formFactory.form().bindFromRequest(request);
    ProgramBridgeEditViewModel viewModel = ProgramBridgeEditViewModel.create(form);

    try {
      ProgramDefinition programDefinition = programService.getFullProgramDefinition(id);

      ObjectMapper mapper = new ObjectMapper().registerModule(new GuavaModule());
      ImmutableList<BridgeDefinition> bridgeDefinitions =
          mapper.readValue(
              viewModel.bridgeDefinitions(),
              mapper
                  .getTypeFactory()
                  .constructCollectionType(ImmutableList.class, BridgeDefinition.class));

      programService.updateProgramDefinition(
          programDefinition.id(),
          LocalizedStrings.DEFAULT_LOCALE,
          programDefinition.adminDescription(),
          programDefinition.localizedName().get(Locale.US),
          programDefinition.localizedDescription().get(Locale.US),
          programDefinition.localizedShortDescription().get(Locale.US),
          programDefinition.localizedConfirmationMessage().get(Locale.US),
          programDefinition.externalLink(),
          programDefinition.displayMode().name(),
          programDefinition.notificationPreferences().stream()
              .map(ProgramNotificationPreference::getValue)
              .toList(),
          programDefinition.eligibilityIsGating(),
          programDefinition.programType(),
          // TODO TI Groups
          ImmutableList.of(),
          ImmutableList.copyOf(
              programDefinition.categories().stream()
                  .map(x -> x.id)
                  .collect(ImmutableList.toImmutableList())),
          ImmutableList.copyOf(programDefinition.applicationSteps()),
          ImmutableList.copyOf(bridgeDefinitions));

      //      return CompletableFuture.completedFuture(ok());
      return CompletableFuture.completedFuture(
          redirect(controllers.admin.routes.AdminProgramBlocksController.edit(id, 1).url()));
    } catch (ProgramNotFoundException e) {
      return CompletableFuture.completedFuture(notFound(e.toString()));
    } catch (TranslationNotFoundException e) {
      throw new RuntimeException(e);
    } catch (JsonMappingException e) {
      throw new RuntimeException(e);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
