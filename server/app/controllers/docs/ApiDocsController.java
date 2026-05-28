package controllers.docs;

import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;
import static play.mvc.Results.redirect;

import auth.Authorizers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import javax.inject.Inject;
import mapping.admin.docs.ApiDocsPageMapper;
import models.LifecycleStage;
import org.pac4j.play.java.Secure;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.mvc.Result;
import services.docs.ApiDocsService;
import services.program.ProgramDefinition;
import services.settings.SettingsManifest;
import views.admin.docs.ApiDocsPageView;
import views.docs.ApiDocsView;

public final class ApiDocsController {

  private final ApiDocsView docsView;
  private final SettingsManifest settingsManifest;
  private final ApiDocsPageView apiDocsPageView;
  private final ApiDocsService apiDocsService;
  private final MessagesApi messagesApi;

  @Inject
  public ApiDocsController(
      ApiDocsView docsView,
      SettingsManifest settingsManifest,
      ApiDocsPageView apiDocsPageView,
      ApiDocsService apiDocsService,
      MessagesApi messagesApi) {
    this.docsView = docsView;
    this.settingsManifest = settingsManifest;
    this.apiDocsPageView = apiDocsPageView;
    this.apiDocsService = apiDocsService;
    this.messagesApi = messagesApi;
  }

  /**
   * Like {@link #docsForSlug}, but defaults to an arbitrary program when one is not set in the URL.
   */
  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result index(Http.Request request) {
    Optional<String> firstProgramSlug =
        apiDocsService.getAllNonExternalProgramSlugs().stream().findFirst();
    return firstProgramSlug
        .map(slug -> redirect(routes.ApiDocsController.activeDocsForSlug(slug)))
        .orElse(notFound(messagesApi.preferred(request).at("adminApiDocs.notFound")));
  }

  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result activeDocsForSlug(Http.Request request, String selectedProgramSlug) {
    return docsForSlug(request, selectedProgramSlug, LifecycleStage.ACTIVE);
  }

  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result draftDocsForSlug(Http.Request request, String selectedProgramSlug) {
    return docsForSlug(request, selectedProgramSlug, LifecycleStage.DRAFT);
  }

  private Result docsForSlug(
      Http.Request request, String selectedProgramSlug, LifecycleStage lifecycleStage) {
    ImmutableSet<String> allNonExternalProgramSlugs =
        apiDocsService.getAllNonExternalProgramSlugs();
    Optional<ProgramDefinition> programDefinition =
        apiDocsService.getProgramDefinition(selectedProgramSlug, lifecycleStage);

    if (settingsManifest.getAdminUiMigrationScEnabled(request)) {
      String jsonPreview = programDefinition.map(apiDocsService::getSampleJsonPreview).orElse("");
      ImmutableMap<String, ImmutableList<String>> historicOptionsByQuestionNameKey =
          programDefinition
              .map(apiDocsService::getHistoricOptionsByQuestionNameKey)
              .orElse(ImmutableMap.of());

      return ok(apiDocsPageView.render(
              request,
              new ApiDocsPageMapper()
                  .map(
                      request,
                      selectedProgramSlug,
                      lifecycleStage,
                      allNonExternalProgramSlugs,
                      programDefinition,
                      jsonPreview,
                      historicOptionsByQuestionNameKey)))
          .as(Http.MimeTypes.HTML);
    }
    return ok(
        docsView.render(
            request, selectedProgramSlug, programDefinition, allNonExternalProgramSlugs));
  }

  /** Redirect to the api docs view for the slug/stage combo */
  @Secure(authorizers = Authorizers.Labels.ANY_ADMIN)
  public Result getApiDocsRedirect(
      Http.Request request, Optional<String> programSlug, Optional<String> stage) {

    String programNotFoundMsg = messagesApi.preferred(request).at("adminApiDocs.notFound");

    String programSlugN = programSlug.orElse("");
    if (!apiDocsService.getAllNonExternalProgramSlugs().contains(programSlugN)) {
      return notFound(programNotFoundMsg);
    }

    return switch (stage.orElse("")) {
      case "draft" -> redirect(routes.ApiDocsController.draftDocsForSlug(programSlugN));
      case "active" -> redirect(routes.ApiDocsController.activeDocsForSlug(programSlugN));
      default -> notFound(programNotFoundMsg);
    };
  }
}
