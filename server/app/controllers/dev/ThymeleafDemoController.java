package controllers.dev;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

public final class ThymeleafDemoController extends Controller {
  private final TemplateEngine templateEngine;
  private final ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory;
  private final AssetsFinder assetsFinder;

  @Inject
  ThymeleafDemoController(
      AssetsFinder assetsFinder,
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory) {
    this.templateEngine = checkNotNull(templateEngine);
    this.playThymeleafContextFactory = checkNotNull(playThymeleafContextFactory);
    this.assetsFinder = checkNotNull(assetsFinder);
  }

  public Result index(Http.Request request) {
    var context = playThymeleafContextFactory.create(request);

    context.setVariable("tailwindStylesheet", assetsFinder.path("stylesheets/tailwind.css"));
    context.setVariable("uswdsStylesheet", assetsFinder.path("dist/uswds.min.css"));
    context.setVariable("uswdsJsBundle", assetsFinder.path("javascripts/uswds/uswds-init.min.js"));
    context.setVariable("adminJsBundle", assetsFinder.path("dist/admin.bundle.js"));
    context.setVariable("ApiDocsController", controllers.api.routes.ApiDocsController);

    String content = templateEngine.process("dev/ThymeleafDemoView", context);
    return ok(content).as(Http.MimeTypes.HTML);
  }

  public Result componentToggle(Http.Request request, String state) {
    String toggleOnOrOff = "toggle-" + state;

    String content =
        templateEngine.process(
            "dev/ThymeleafComponentsDemo",
            ImmutableSet.of(toggleOnOrOff),
            playThymeleafContextFactory.create(request));
    return ok(content).as(Http.MimeTypes.HTML);
  }
}