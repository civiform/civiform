package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.CiviFormProfile;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.applicant.ApplicantRoutes;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.mvc.Http.Request;
import services.applicant.Block;

/** Renders a list of sections in the form with their status. */
public final class NorthStarApplicantProgramSummaryView extends NorthStarApplicantBaseView {

  @Inject
  NorthStarApplicantProgramSummaryView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder,
      ApplicantRoutes applicantRoutes) {
        super(templateEngine, playThymeleafContextFactory, assetsFinder, applicantRoutes);
  }

  public String render(Request request, Params params) {
    ThymeleafModule.PlayThymeleafContext context = createThymeleafContext(request);
    context.setVariable("blocks", params.blocks());
    context.setVariable("blockEditUrlMap", blockEditUrlMap(params));
    return templateEngine.process("applicant/ApplicantProgramSummaryView", context);
  }

  // Returns a map of block ids to edit urls.
  private Map<String, String> blockEditUrlMap(Params params) {
    return params.blocks().stream()
        .collect(Collectors.toMap(value -> value.getId(), value -> getBlockEditUrl(params, value)));
  }

  private String getBlockEditUrl(Params params, Block block) {
    if (block.isAnsweredWithoutErrors()) {
      return applicantRoutes
          .blockReview(
              params.profile(),
              params.applicantId(),
              params.programId(),
              block.getId(),
              Optional.empty())
          .url();
    } else {
      return applicantRoutes
          .blockEdit(
              params.profile(),
              params.applicantId(),
              params.programId(),
              block.getId(),
              Optional.empty())
          .url();
    }
  }

  @AutoValue
  public abstract static class Params {

    public static Builder builder() {
      return new AutoValue_NorthStarApplicantProgramSummaryView_Params.Builder();
    }

    abstract long applicantId();

    abstract ImmutableList<Block> blocks();

    abstract CiviFormProfile profile();

    abstract long programId();

    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder setApplicantId(long applicantId);

      public abstract Builder setBlocks(ImmutableList<Block> blocks);

      public abstract Builder setProfile(CiviFormProfile profile);

      public abstract Builder setProgramId(long programId);

      public abstract Params build();
    }
  }
}
