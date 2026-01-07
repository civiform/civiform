package views.questiontypes;

import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import java.util.Optional;
import modules.ThymeleafModule;
import services.BundledAssetsFinder;
import services.DeploymentType;
import services.settings.SettingsManifest;
import views.ApplicantBaseView;
import views.ApplicationBaseViewParams;
import views.fileupload.FileUploadViewStrategy;
import org.thymeleaf.TemplateEngine;
import play.mvc.Http.Request;
import com.google.inject.Inject;
import com.google.common.collect.ImmutableList;

public class SpikeFileUploadQuestionPartialView extends ApplicantBaseView {
  private final FileUploadViewStrategy fileUploadViewStrategy;

  @Inject
  SpikeFileUploadQuestionPartialView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      BundledAssetsFinder bundledAssetsFinder,
      ApplicantRoutes applicantRoutes,
      FileUploadViewStrategy fileUploadViewStrategy,
      SettingsManifest settingsManifest,
      LanguageUtils languageUtils,
      DeploymentType deploymentType) {
    super(
        templateEngine,
        playThymeleafContextFactory,
        bundledAssetsFinder,
        applicantRoutes,
        settingsManifest,
        languageUtils,
        deploymentType);
    this.fileUploadViewStrategy = fileUploadViewStrategy;
  }

  public String render(
      Request request, ApplicationBaseViewParams applicationParams, String programSlug) {
    ThymeleafModule.PlayThymeleafContext context =
        createThymeleafContext(
            request,
            Optional.of(applicationParams.applicantId()),
            Optional.of(applicationParams.profile()),
            applicationParams.applicantPersonalInfo(),
            applicationParams.messages());

    // TODO(#6910): Why am I unable to access static vars directly from Thymeleaf
    context.setVariable("fileUploadViewStrategy", fileUploadViewStrategy);
    context.setVariable("question", applicationParams.block().getQuestions().get(0));
    context.setVariable("programId", applicationParams.programId());
    context.setVariable("blockId", applicationParams.block().getId());
    context.setVariable("applicationParams", applicationParams);

    return templateEngine.process("questiontypes/SpikeFileUploadQuestionFragment", context);
  }
}
