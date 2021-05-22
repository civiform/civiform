package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.nav;

import auth.ProfileUtils;
import auth.Roles;
import auth.UatProfile;
import com.typesafe.config.Config;
import controllers.routes;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Optional;
import j2html.TagCreator;
import javax.inject.Inject;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import views.BaseHtmlLayout;
import views.html.helper.CSRF;
import views.HtmlBundle;
import views.LanguageUtils;
import views.ViewUtils;
import views.style.ApplicantStyles;
import views.style.BaseStyles;
import views.style.StyleUtils;
import views.style.Styles;

public class ApplicantLayout extends BaseHtmlLayout {
  private static final String CIVIFORM_TITLE = "CiviForm";

  private final ProfileUtils profileUtils;
  public final LanguageUtils languageUtils;

  @Inject
  public ApplicantLayout(
      ViewUtils viewUtils,
      Config configuration,
      ProfileUtils profileUtils,
      LanguageUtils languageUtils) {
    super(viewUtils, configuration);
    this.profileUtils = checkNotNull(profileUtils);
    this.languageUtils = checkNotNull(languageUtils);
  }

  @Override
  public Content render(HtmlBundle bundle) {
    bundle.addBodyStyles(ApplicantStyles.BODY);
    String currentTitle = bundle.getTitle();
    if (currentTitle != null && !currentTitle.isEmpty()) {
      bundle.setTitle(String.format("%s — %s", currentTitle, CIVIFORM_TITLE));
    } else {
      bundle.setTitle(CIVIFORM_TITLE);
    }
    return super.render(bundle);
  }

  public Content renderWithNav(
      Http.Request request, String userName, Messages messages, HtmlBundle bundle) {
    // TODO: This will set the html lang attribute to the requested language when we actually want
    // the rendered language.
    Optional<String> language = languageUtils.getPreferredLangage(request);
    if (language.isPresent()) {
      bundle.setLanguage(language.get());
    }
    bundle.addHeaderContent(renderNavBar(request, userName, messages));
    return render(bundle);
  }

  private ContainerTag renderNavBar(Http.Request request, String userName, Messages messages) {
    Optional<UatProfile> profile = profileUtils.currentUserProfile(request);

    String csrfToken =  CSRF.getToken(request.asScala()).value();
    Tag csrfInput = input().isHidden().withValue(csrfToken).withName("csrfToken");
  
    ContainerTag languageForm = div();
    if (profile.isPresent()) { // Show language switcher.
      long userId = Long.parseLong(profile.get().getId());
      String updateLanguageAction = controllers.applicant.routes.ApplicantInformationController
            .updateWithRedirect(userId, request.uri())
            .url();    
      String preferredLanguage = languageUtils.getPreferredLangage(request).orElse("en");
      ContainerTag languageDropdown = languageUtils.renderDropdown(preferredLanguage)
          .attr("onchange", "this.form.submit()");
      languageForm = form()
        .withAction(updateLanguageAction)
        .withMethod(Http.HttpVerbs.POST)
        .with(csrfInput)
        .with(languageDropdown)
        .with(TagCreator.button().withId("cf-update-lang").withType("submit").isHidden());
    }

    return nav()
        .withClasses(
            Styles.BG_WHITE,
            Styles.BORDER_B,
            Styles.ALIGN_MIDDLE,
            Styles.P_4,
            Styles.GRID,
            Styles.GRID_COLS_3)
        .with(branding())
        .with(maybeRenderTiButton(profile, userName))
        .with(
            div(languageForm, logoutButton(messages))
                .withClasses(Styles.JUSTIFY_SELF_END, Styles.FLEX, Styles.FLEX_ROW));
  }

  private ContainerTag branding() {
    return a().withHref(routes.HomeController.index().url())
        .with(
            div()
                .withId("brand-id")
                .withClasses(ApplicantStyles.CIVIFORM_LOGO)
                .withText("CiviForm"));
  }

  private ContainerTag maybeRenderTiButton(Optional<UatProfile> profile, String userName) {
    if (profile.isPresent() && profile.get().getRoles().contains(Roles.ROLE_TI.toString())) {
      String tiDashboardText = "Trusted intermediary dashboard";
      String tiDashboardLink =
          controllers.ti.routes.TrustedIntermediaryController.dashboard().url();
      return div(
          a(tiDashboardText)
              .withHref(tiDashboardLink)
              .withClasses(
                  Styles.PX_3,
                  Styles.TEXT_SM,
                  Styles.OPACITY_75,
                  StyleUtils.hover(Styles.OPACITY_100)),
          div("(applying as: " + userName + ")")
              .withClasses(Styles.TEXT_SM, Styles.PX_3, Styles.OPACITY_75));
    }
    return div();
  }

  private ContainerTag logoutButton(Messages messages) {
    String logoutLink = org.pac4j.play.routes.LogoutController.logout().url();
    return a(messages.at(MessageKey.BUTTON_LOGOUT.getKeyName()))
        .withHref(logoutLink)
        .withClasses(ApplicantStyles.LINK_LOGOUT);
  }

  /**
   * Use this one after the application has been submitted, to show a complete progress indicator.
   */
  protected ContainerTag renderProgramApplicationTitleAndProgressIndicator(String programTitle) {
    return renderProgramApplicationTitleAndProgressIndicator(programTitle, 0, 0, true);
  }

  /**
   * The progress indicator is a bit different while an application is being filled out vs for the
   * summary view.
   *
   * <p>While in progress, the current incomplete block is counted towards progress, but will not
   * show full progress while filling out the last block of the program.
   *
   * <p>For the summary view, there is no "current" block, and full progress can be shown.
   */
  protected ContainerTag renderProgramApplicationTitleAndProgressIndicator(
      String programTitle, int blockIndex, int totalBlockCount, boolean forSummary) {
    int percentComplete = getPercentComplete(blockIndex, totalBlockCount, forSummary);

    ContainerTag progressInner =
        div()
            .withClasses(
                BaseStyles.BG_SEATTLE_BLUE,
                Styles.TRANSITION_ALL,
                Styles.DURATION_300,
                Styles.H_FULL,
                Styles.BLOCK,
                Styles.ABSOLUTE,
                Styles.LEFT_0,
                Styles.TOP_0,
                Styles.W_1,
                Styles.ROUNDED_FULL)
            .withStyle("width:" + percentComplete + "%");
    ContainerTag progressIndicator =
        div(progressInner)
            .withId("progress-indicator")
            .withClasses(
                Styles.BORDER,
                BaseStyles.BORDER_SEATTLE_BLUE,
                Styles.ROUNDED_FULL,
                Styles.FONT_SEMIBOLD,
                Styles.BG_WHITE,
                Styles.RELATIVE,
                Styles.H_4,
                Styles.MT_4);

    // While applicant is filling out the application, include the block they are on as part of
    // their progress.
    if (!forSummary) {
      blockIndex++;
    }

    ContainerTag blockNumberTag = div();
    if (!forSummary) {
      blockNumberTag
          .withText(String.format("%d of %d", blockIndex, totalBlockCount))
          .withClasses(Styles.TEXT_GRAY_500, Styles.TEXT_RIGHT);
    }

    Tag programTitleDiv =
        div()
            .with(h2(programTitle).withClasses(ApplicantStyles.H2_PROGRAM_TITLE))
            .with(blockNumberTag)
            .withClasses(Styles.GRID, Styles.GRID_COLS_2);

    return div().with(programTitleDiv).with(progressIndicator);
  }

  /**
   * Returns whole number out of 100 representing the completion percent of this program.
   *
   * <p>See {@link #renderProgramApplicationTitleAndProgressIndicator(String, int, int, boolean)}
   * about why there's a difference between the percent complete for summary views, and for
   * non-summary views.
   */
  private int getPercentComplete(int blockIndex, int totalBlockCount, boolean forSummary) {
    if (totalBlockCount == 0) return 100;
    if (blockIndex == -1) return 0;

    // While in progress, add one to blockIndex for 1-based indexing, so that when applicant is on
    // first block, we show
    // some amount of progress; and add one to totalBlockCount so that when applicant is on the last
    // block, we show that they're
    // still in progress.
    // For summary views, we don't need to do any of the tricks, so we just use the actual total
    // block count and block index.
    double numerator = forSummary ? blockIndex : blockIndex + 1;
    double denominator = forSummary ? totalBlockCount : totalBlockCount + 1;

    return (int) (numerator / denominator * 100.0);
  }
}
