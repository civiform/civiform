package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import views.HtmlBundle;
import org.thymeleaf.TemplateEngine;
import modules.ThymeleafModule;
import play.mvc.Http.Request;
import controllers.AssetsFinder;
import com.google.inject.Inject;

/** Renders a page for answering questions in a program screen (block). */
public final class NorthStarApplicantProgramBlockEditView {
  private final TemplateEngine templateEngine;
  private final ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory;
  private final AssetsFinder assetsFinder;

  @Inject
  NorthStarApplicantProgramBlockEditView(
    TemplateEngine templateEngine,
    ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder) {
    this.templateEngine = checkNotNull(templateEngine);
    this.playThymeleafContextFactory = checkNotNull(playThymeleafContextFactory);
    this.assetsFinder = checkNotNull(assetsFinder);
  }

  public String render(Request request) {
    ThymeleafModule.PlayThymeleafContext context = playThymeleafContextFactory.create(request);
    context.setVariable("tailwindStylesheet", assetsFinder.path("stylesheets/tailwind.css"));
    context.setVariable("uswdsStylesheet", assetsFinder.path("dist/uswds.min.css"));
    context.setVariable("uswdsJsBundle", assetsFinder.path("javascripts/uswds/uswds-init.min.js"));
    context.setVariable("adminJsBundle", assetsFinder.path("dist/admin.bundle.js"));
    context.setVariable("ApiDocsController", controllers.api.routes.ApiDocsController);
    return templateEngine.process("applicant/ApplicantProgramBlockEditView", context);
  }
}
