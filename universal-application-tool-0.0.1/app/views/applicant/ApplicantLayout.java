package views.applicant;

import static j2html.TagCreator.a;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.header;
import static j2html.TagCreator.nav;
import static j2html.TagCreator.span;

import auth.ProfileUtils;
import auth.Roles;
import auth.UatProfile;
import com.google.common.base.Preconditions;
import controllers.ti.routes;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import views.BaseHtmlLayout;
import views.ViewUtils;
import views.style.ApplicantStyles;
import views.style.StyleUtils;
import views.style.Styles;

public class ApplicantLayout extends BaseHtmlLayout {

  private final ProfileUtils profileUtils;

  @Inject
  public ApplicantLayout(ViewUtils viewUtils, ProfileUtils profileUtils) {
    super(viewUtils);
    this.profileUtils = Preconditions.checkNotNull(profileUtils);
  }

  protected Content render(Http.Request request, Messages messages, DomContent... mainDomContents) {
    return render(profileUtils.currentUserProfile(request), messages, mainDomContents);
  }
  /** Renders mainDomContents within the main tag, in the context of the applicant layout. */
  protected Content render(
      Optional<UatProfile> profile, Messages messages, DomContent... mainDomContents) {
    return htmlContent(
        headContent("Applicant layout title"),
        body()
            .with(renderNavBar(profile, messages))
            .with(mainDomContents)
            .with(viewUtils.makeLocalJsTag("main"))
            .withClasses(ApplicantStyles.BODY_BG_COLOR));
  }

  private ContainerTag renderNavBar(Optional<UatProfile> profile, Messages messages) {
    return nav()
        .withClasses(
            Styles.PT_8,
            Styles.PB_4,
            Styles.MB_12,
            Styles.FLEX,
            Styles.ALIGN_MIDDLE,
            Styles.BORDER_B_4,
            Styles.BORDER_WHITE)
        .with(branding(), status(messages), maybeRenderTiButton(profile), logoutButton(messages));
  }

  private ContainerTag maybeRenderTiButton(Optional<UatProfile> profile) {
    if (profile.isPresent() && profile.get().getRoles().contains(Roles.ROLE_TI.toString())) {
      String tiDashboardLink = routes.TrustedIntermediaryController.dashboard().url();
      return a("Trusted Intermediary Dashboard")
          .withHref(tiDashboardLink)
          .withClasses(
              Styles.PX_3, Styles.TEXT_SM, Styles.OPACITY_75, StyleUtils.hover(Styles.OPACITY_100));
    }
    return div();
  }

  private ContainerTag branding() {
    return div()
        .withId("brand-id")
        .withClasses(Styles.W_1_2, ApplicantStyles.LOGO_STYLE)
        .with(span("Civi"))
        .with(span("Form").withClasses(Styles.FONT_THIN));
  }

  private ContainerTag status(Messages messages) {
    return div()
        .withId("application-status")
        .withClasses(Styles.W_1_4, Styles.TEXT_RIGHT, Styles.TEXT_SM, Styles.UNDERLINE)
        .with(span(messages.at(MessageKey.LINK_VIEW_APPLICATIONS.getKeyName())));
  }

  private ContainerTag logoutButton(Messages messages) {
    String logoutLink = org.pac4j.play.routes.LogoutController.logout().url();
    return a(messages.at(MessageKey.BUTTON_LOGOUT.getKeyName()))
        .withHref(logoutLink)
        .withClasses(
            Styles.PX_3, Styles.TEXT_SM, Styles.OPACITY_75, StyleUtils.hover(Styles.OPACITY_100));
  }

  protected ContainerTag renderHeader(int percentComplete) {
    ContainerTag headerTag = header().withClasses(Styles.FLEX, Styles.FLEX_COL, Styles._MT_12);
    ContainerTag progressInner =
        div()
            .withClasses(
                Styles.BG_YELLOW_400,
                Styles.TRANSITION_ALL,
                Styles.DURATION_300,
                Styles.H_FULL,
                Styles.BLOCK,
                Styles.ABSOLUTE,
                Styles.LEFT_0,
                Styles.TOP_0,
                Styles.W_1,
                Styles.ROUNDED_R_FULL)
            .withStyle("width:" + percentComplete + "%");
    ContainerTag progressIndicator =
        div(progressInner)
            .withId("progress-indicator")
            .withClasses(
                Styles.BORDER,
                Styles.FONT_SEMIBOLD,
                Styles.BG_GRAY_200,
                Styles.RELATIVE,
                Styles.H_2);

    headerTag.with(progressIndicator);
    return headerTag;
  }
}
