package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableSet;
import controllers.AssetsFinder;
import controllers.CiviFormController;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import modules.ThymeleafModule;
import org.pac4j.play.java.Secure;
import org.thymeleaf.TemplateEngine;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import repository.VersionRepository;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService;
import services.settings.SettingsManifest;

/**
 * Controller for handling methods for an applicant applying to programs. CAUTION: you must
 * explicitly check the current profile so that an unauthorized user cannot access another
 * applicant's data!
 */
public final class NorthStarApplicantProgramsController extends CiviFormController {

  private final ApplicantService applicantService;
  private final SettingsManifest settingsManifest;
  private final TemplateEngine templateEngine;
  private final ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory;
  private final AssetsFinder assetsFinder;

  @Inject
  public NorthStarApplicantProgramsController(
      ApplicantService applicantService,
      AssetsFinder assetsFinder,
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      SettingsManifest settingsManifest,
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory) {
    super(profileUtils, versionRepository);
    this.applicantService = checkNotNull(applicantService);
    this.assetsFinder = checkNotNull(assetsFinder);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.templateEngine = checkNotNull(templateEngine);
    this.playThymeleafContextFactory = checkNotNull(playThymeleafContextFactory);
  }

  @Secure
  public CompletionStage<Result> index(Request request) {
    if (!settingsManifest.getNewApplicantUrlSchemaEnabled()) {
      // This route is only operative for the new URL schema, so send the user home.
      return CompletableFuture.completedFuture(redirectToHome());
    }

    Optional<Long> applicantId = getApplicantId(request);
    if (applicantId.isEmpty()) {
      // This route should not have been computed for the user in this case, but they may have
      // gotten the URL from another source.
      return CompletableFuture.completedFuture(redirectToHome());
    }
    CompletionStage<ApplicantPersonalInfo> applicantStage =
        this.applicantService.getPersonalInfo(applicantId.get());

    return applicantStage.thenApplyAsync(
        v -> {
          String content =
              templateEngine.process("applicant/ProgramIndexView", buildContext(request));
          return ok(content).as(Http.MimeTypes.HTML);
        });
  }

  @Secure
  public Result button(Request request) {
    String content =
        templateEngine.process(
            "applicant/ButtonView", ImmutableSet.of("my-button"), buildContext(request));
    return ok(content).as(Http.MimeTypes.HTML);
  }

  private ThymeleafModule.PlayThymeleafContext buildContext(Request request) {
    ThymeleafModule.PlayThymeleafContext context = playThymeleafContextFactory.create(request);
    context.setVariable("tailwindStylesheet", assetsFinder.path("stylesheets/tailwind.css"));
    context.setVariable("uswdsStylesheet", assetsFinder.path("dist/uswds.min.css"));
    context.setVariable("uswdsJsBundle", assetsFinder.path("javascripts/uswds/uswds-init.min.js"));
    context.setVariable("adminJsBundle", assetsFinder.path("dist/admin.bundle.js"));
    context.setVariable("ApiDocsController", controllers.api.routes.ApiDocsController);
    return context;
  }
}
