package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static featureflags.FeatureFlag.SHOW_CIVIFORM_IMAGE_TAG_ON_LANDING_PAGE;
import static j2html.TagCreator.br;
import static j2html.TagCreator.div;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.legend;
import static j2html.TagCreator.p;

import auth.FakeAdminClient;
import com.typesafe.config.Config;
import controllers.applicant.routes;
import featureflags.FeatureFlags;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FieldsetTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.InputTag;
import j2html.tags.specialized.LegendTag;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.DeploymentType;
import services.MessageKey;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.ButtonStyles;
import views.style.ApplicantStyles;
import views.style.ReferenceClasses;

/**
 * Provides a form for selecting an applicant's preferred language. Note that we cannot use Play's
 * {@link play.i18n.Messages}, since the applicant has no language set yet. Instead, we use English
 * since this is the CiviForm default language.
 */
public class ApplicantInformationView extends BaseHtmlView {

  private final ApplicantLayout layout;
  private final boolean isDevOrStaging;
  private final boolean disableDemoModeLogins;
  private final FeatureFlags featureFlags;
  private final String civiformVersion;
  private final String civiformImageTag;

  @Inject
  public ApplicantInformationView(
      ApplicantLayout layout,
      DeploymentType deploymentType,
      Config config,
      FeatureFlags featureFlags) {
    this.layout = checkNotNull(layout);
    this.isDevOrStaging = deploymentType.isDevOrStaging();
    this.disableDemoModeLogins =
        this.isDevOrStaging && config.getBoolean("staging_disable_demo_mode_logins");
    this.featureFlags = featureFlags;
    this.civiformVersion = config.getString("civiform_version");
    this.civiformImageTag = config.getString("civiform_image_tag");
  }

  public Content render(
      Http.Request request,
      Optional<String> userName,
      Messages messages,
      long applicantId,
      Optional<String> redirectTo,
      boolean isTrustedIntermediary) {
    String formAction = routes.ApplicantInformationController.update(applicantId).url();
    String redirectLink = null;
    if (isTrustedIntermediary) {
      redirectLink =
          redirectTo.orElse(
              controllers.ti.routes.TrustedIntermediaryController.dashboard(
                      /* nameQuery= */ Optional.empty(),
                      /* dateQuery= */ Optional.empty(),
                      /* page= */ Optional.of(1))
                  .url());
    } else {
      redirectLink = redirectTo.orElse(routes.ApplicantProgramsController.index(applicantId).url());
    }
    InputTag redirectInput = input().isHidden().withValue(redirectLink).withName("redirectLink");

    String questionText = messages.at(MessageKey.CONTENT_SELECT_LANGUAGE.getKeyName());
    LegendTag questionTextLegend =
        legend(questionText)
            .withClasses(ReferenceClasses.APPLICANT_QUESTION_TEXT, ApplicantStyles.QUESTION_TEXT);
    String preferredLanguage = layout.languageSelector.getPreferredLangage(request).code();
    FieldsetTag languageSelectorFieldset =
        fieldset()
            // legend must be a direct child of fieldset for screenreaders to work properly
            .with(questionTextLegend)
            .with(layout.languageSelector.renderRadios(preferredLanguage));
    FormTag formContent =
        form()
            .withAction(formAction)
            .withMethod(Http.HttpVerbs.POST)
            .with(makeCsrfTokenInputTag(request))
            .with(redirectInput)
            .with(languageSelectorFieldset);

    String submitText = messages.at(MessageKey.BUTTON_UNTRANSLATED_SUBMIT.getKeyName());
    ButtonTag formSubmit = submitButton(submitText).withClasses(ButtonStyles.SOLID_BLUE, "mx-auto");
    formContent.with(formSubmit);

    // No translation needed since this appears before applicants select their preferred language,
    // so we always use the default.
    String title = "Select language";
    HtmlBundle bundle =
        layout
            .getBundle()
            .setTitle(title)
            .addMainStyles(ApplicantStyles.MAIN_APPLICANT_INFO)
            .addMainContent(h1(title).withClasses("sr-only"), formContent);

    if (isDevOrStaging && !disableDemoModeLogins) {
      bundle.addMainContent(br(), debugContent());
      bundle.addMainStyles("flex", "flex-col");
    }

    if (featureFlags.getFlagEnabled(request, SHOW_CIVIFORM_IMAGE_TAG_ON_LANDING_PAGE)) {
      // civiformVersion is the version the deployer requests, like "latest" or
      // "v1.18.0". civiformImageTag is set by bin/build-prod and is a string
      // like "SNAPSHOT-3af8997-1678895722".
      String version = civiformVersion;
      if (civiformVersion.equals("") || civiformVersion.equals("latest")) {
        version = civiformImageTag;
      }
      bundle.addFooterContent(
          div()
              .with(p("CiviForm version: " + version).withClasses("text-gray-600", "mx-auto"))
              .withClasses("flex", "flex-row"));
    }

    // We probably don't want the nav bar here (or we need it somewhat different - no dropdown.)
    return layout.renderWithNav(request, userName, messages, bundle);
  }

  private DivTag debugContent() {
    return div()
        .withClasses("flex", "flex-col")
        .with(
            p("DEVELOPMENT MODE TOOLS:").withClasses("text-2xl"),
            redirectButton(
                "admin",
                "CiviForm Admin",
                controllers.routes.CallbackController.fakeAdmin(
                        FakeAdminClient.CLIENT_NAME, FakeAdminClient.GLOBAL_ADMIN)
                    .url()),
            redirectButton(
                "program-admin",
                "Program Admin",
                controllers.routes.CallbackController.fakeAdmin(
                        FakeAdminClient.CLIENT_NAME, FakeAdminClient.PROGRAM_ADMIN)
                    .url()),
            redirectButton(
                "dual-admin",
                "Program and Civiform Admin",
                controllers.routes.CallbackController.fakeAdmin(
                        FakeAdminClient.CLIENT_NAME, FakeAdminClient.DUAL_ADMIN)
                    .url()),
            redirectButton(
                "trusted-intermediary",
                "Trusted Intermediary",
                controllers.routes.CallbackController.fakeAdmin(
                        FakeAdminClient.CLIENT_NAME, FakeAdminClient.TRUSTED_INTERMEDIARY)
                    .url()),
            redirectButton(
                "feature-flags",
                "Feature Flags",
                controllers.dev.routes.FeatureFlagOverrideController.index().url()),
            redirectButton(
                "database-seed",
                "Seed Database",
                controllers.dev.routes.DatabaseSeedController.index().url()));
  }
}
