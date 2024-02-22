package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import org.thymeleaf.TemplateEngine;
import modules.ThymeleafModule;
import play.mvc.Http.Request;
import controllers.AssetsFinder;
import com.google.inject.Inject;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import services.applicant.Block;

/** Renders a list of sections in the form with their status. */
public final class NorthStarApplicantProgramSummaryView {
  private final TemplateEngine templateEngine;
  private final ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory;
  private final AssetsFinder assetsFinder;

  @Inject
  NorthStarApplicantProgramSummaryView(
    TemplateEngine templateEngine,
    ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder) {
    this.templateEngine = checkNotNull(templateEngine);
    this.playThymeleafContextFactory = checkNotNull(playThymeleafContextFactory);
    this.assetsFinder = checkNotNull(assetsFinder);
  }

  public String render(Request request, Params params) {
    ThymeleafModule.PlayThymeleafContext context = playThymeleafContextFactory.create(request);
    context.setVariable("tailwindStylesheet", assetsFinder.path("stylesheets/tailwind.css"));
    context.setVariable("uswdsStylesheet", assetsFinder.path("dist/uswds.min.css"));
    context.setVariable("uswdsJsBundle", assetsFinder.path("javascripts/uswds/uswds-init.min.js"));
    context.setVariable("adminJsBundle", assetsFinder.path("dist/admin.bundle.js"));
    context.setVariable("ApiDocsController", controllers.api.routes.ApiDocsController);
    // context.setVariable("formAction", getFormAction(applicationParams));
    // context.setVariable("previousUrl", getPreviousUrl(applicationParams));
    // context.setVariable("reviewUrl", getReviewUrl(applicationParams));
    // context.setVariable("csrfToken", CSRF.getToken(request.asScala()).value());
    // context.setVariable("applicationParams", applicationParams);
    context.setVariable("blocks", params.blocks());
    return templateEngine.process("applicant/ApplicantProgramSummaryView", context);
  }

  @AutoValue
  public abstract static class Params {

    public static Builder builder() {
      return new AutoValue_NorthStarApplicantProgramSummaryView_Params.Builder();
    }

    abstract ImmutableList<Block> blocks();

    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder setBlocks(ImmutableList<Block> blocks);

      public abstract Params build();
    }
  }
}
