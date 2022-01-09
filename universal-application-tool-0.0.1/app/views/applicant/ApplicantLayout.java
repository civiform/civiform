package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.input;
import static j2html.TagCreator.nav;
import static j2html.TagCreator.text;

import auth.CiviFormProfile;
import auth.GuestClient;
import auth.ProfileUtils;
import auth.Roles;
import com.typesafe.config.Config;
import controllers.routes;
import io.jsonwebtoken.lang.Strings;
import j2html.TagCreator;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Optional;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import views.BaseHtmlLayout;
import views.HtmlBundle;
import views.LanguageSelector;
import views.ViewUtils;
import views.html.helper.CSRF;
import views.style.ApplicantStyles;
import views.style.BaseStyles;
import views.style.StyleUtils;
import views.style.Styles;

/** Contains methods rendering common compoments used across applicant pages. */
public class ApplicantLayout extends BaseHtmlLayout {

  private static final String CIVIFORM_TITLE = "CiviForm";
  private static final Logger LOG = LoggerFactory.getLogger(ApplicantLayout.class);

  private final ProfileUtils profileUtils;
  public final LanguageSelector languageSelector;
  public final String supportEmail;

  @Inject
  public ApplicantLayout(
      ViewUtils viewUtils,
      Config configuration,
      ProfileUtils profileUtils,
      LanguageSelector languageSelector) {
    super(viewUtils, configuration);
    this.profileUtils = checkNotNull(profileUtils);
    this.languageSelector = checkNotNull(languageSelector);
    this.supportEmail = checkNotNull(configuration).getString("support_email_address");
  }

  public ContainerTag getSupportLink(Messages messages) {
    ContainerTag supportLink =
        div()
            .with(
                text(messages.at(MessageKey.FOOTER_SUPPORT_LINK_DESCRIPTION.getKeyName())),
                text(" "),
                a(supportEmail)
                    .withHref("mailto:" + supportEmail)
                    .withTarget("_blank")
                    .withClasses(Styles.TEXT_BLUE_800))
            .withClasses(Styles.MX_AUTO, Styles.MAX_W_SCREEN_SM, Styles.W_5_6);

    return supportLink;
  }

  private Content renderWithSupportFooter(HtmlBundle bundle, Messages messages) {
    ContainerTag supportLink = getSupportLink(messages);
    bundle.addFooterContent(supportLink);

    return render(bundle);
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

    bundle.addFooterStyles(Styles.MT_24);

    Content rendered = super.render(bundle);
    if (!rendered.body().contains("<h1")) {
      LOG.error("Page does not contain an <h1>, which is important for screen readers.");
    }
    if (Strings.countOccurrencesOf(rendered.body(), "<h1") > 1) {
      LOG.error("Page contains more than one <h1>, which is detrimental to screen readers.");
    }
    return rendered;
  }

  private ContainerTag renderBaseNavBar() {
    return nav()
        .withClasses(
            Styles.BG_WHITE,
            Styles.BORDER_B,
            Styles.ALIGN_MIDDLE,
            Styles.P_4,
            Styles.GRID,
            Styles.GRID_COLS_3)
        .with(branding());
  }

  /** Nav bar when logged in */
  public Content renderWithNav(
      Http.RequestHeader request, String userName, Messages messages, HtmlBundle bundle) {
    String language = languageSelector.getPreferredLangage(request).code();
    bundle.setLanguage(language);
    bundle.addHeaderContent(renderNavBarLoggedIn(request, userName, messages));
    return renderWithSupportFooter(bundle, messages);
  }

  /** Nav bar when logged out */
  public Content renderWithNav(Http.RequestHeader request, Messages messages, HtmlBundle bundle) {
    String language = languageSelector.getPreferredLangage(request).code();
    bundle.setLanguage(language);
    bundle.addHeaderContent(renderNavBarLoggedOut(request, messages));
    return renderWithSupportFooter(bundle, messages);
  }

  public ContainerTag renderNavBarLoggedIn(
      Http.RequestHeader request, String userName, Messages messages) {
    Optional<CiviFormProfile> profile = profileUtils.currentUserProfile(request);

    return renderBaseNavBar()
        .with(
            maybeRenderTiButton(profile, userName),
            div(getLanguageForm(request, profile, messages), logoutButton(userName, messages))
                .withClasses(Styles.JUSTIFY_SELF_END, Styles.FLEX, Styles.FLEX_ROW));
  }

  public ContainerTag renderNavBarLoggedOut(Http.RequestHeader request, Messages messages) {
    return renderBaseNavBar().with(div(), loginButton(messages));
  }

  private ContainerTag getLanguageForm(
      Http.RequestHeader request, Optional<CiviFormProfile> profile, Messages messages) {
    ContainerTag languageForm = div();
    if (profile.isPresent()) { // Show language switcher.
      long userId = profile.get().getApplicant().join().id;

      String applicantInfoUrl =
          controllers.applicant.routes.ApplicantInformationController.edit(userId).url();
      String updateLanguageAction =
          controllers.applicant.routes.ApplicantInformationController.update(userId).url();

      // Show language switcher if we're not on the applicant info page.
      boolean showLanguageSwitcher = !request.uri().equals(applicantInfoUrl);
      if (showLanguageSwitcher) {
        String csrfToken = CSRF.getToken(request.asScala()).value();
        Tag csrfInput = input().isHidden().withValue(csrfToken).withName("csrfToken");
        Tag redirectInput = input().isHidden().withValue(request.uri()).withName("redirectLink");
        String preferredLanguage = languageSelector.getPreferredLangage(request).code();
        ContainerTag languageDropdown =
            languageSelector
                .renderDropdown(preferredLanguage)
                .attr("onchange", "this.form.submit()")
                .attr("aria-label", messages.at(MessageKey.LANGUAGE_LABEL_SR.getKeyName()));
        languageForm =
            form()
                .withAction(updateLanguageAction)
                .withMethod(Http.HttpVerbs.POST)
                .with(csrfInput)
                .with(redirectInput)
                .with(languageDropdown)
                .with(TagCreator.button().withId("cf-update-lang").withType("submit").isHidden());
      }
    }
    return languageForm;
  }

  public ContainerTag branding() {
    return a().withHref(routes.HomeController.index().url())
        .with(
            div()
                .withId("brand-id")
                .withClasses(ApplicantStyles.CIVIFORM_LOGO)
                .withText("CiviForm"));
  }

  private ContainerTag maybeRenderTiButton(Optional<CiviFormProfile> profile, String userName) {
    if (profile.isPresent() && profile.get().getRoles().contains(Roles.ROLE_TI.toString())) {
      String tiDashboardText = "Trusted intermediary dashboard";
      String tiDashboardLink =
          controllers.ti.routes.TrustedIntermediaryController.dashboard(
                  Optional.empty(), Optional.empty())
              .url();
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

  private ContainerTag logoutButton(String userName, Messages messages) {
    String logoutLink = org.pac4j.play.routes.LogoutController.logout().url();
    return div(
        div(messages.at(MessageKey.USER_NAME.getKeyName(), userName)).withClasses(Styles.TEXT_SM),
        a(messages.at(MessageKey.BUTTON_LOGOUT.getKeyName()))
            .withHref(logoutLink)
            .withClasses(ApplicantStyles.LINK_LOGOUT));
  }

  private ContainerTag loginButton(Messages messages) {
    String loginLink = routes.CallbackController.callback(GuestClient.CLIENT_NAME).url();

    return div(
            this.viewUtils
                .makeLocalSvgTag("login_icon")
                .withClasses(Styles.INLINE_BLOCK, Styles.MR_2, Styles.MB_1),
            a(messages.at(MessageKey.BUTTON_LOGIN.getKeyName()))
                .withId("guestLogin")
                .withHref(loginLink)
                .withClasses(ApplicantStyles.LINK_LOGOUT, Styles.INLINE_BLOCK, Styles.MT_1))
        .withClasses(Styles.JUSTIFY_SELF_END, Styles.WHITESPACE_NOWRAP);
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
    if (totalBlockCount == 0) {
      return 100;
    }
    if (blockIndex == -1) {
      return 0;
    }

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
