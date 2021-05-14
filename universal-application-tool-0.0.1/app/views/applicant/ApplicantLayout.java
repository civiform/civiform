package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.header;
import static j2html.TagCreator.nav;
import static j2html.TagCreator.span;

import auth.ProfileUtils;
import auth.Roles;
import auth.UatProfile;
import com.typesafe.config.Config;
import controllers.ti.routes;
import j2html.tags.ContainerTag;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import views.BaseHtmlLayout;
import views.HtmlBundle;
import views.ViewUtils;
import views.style.ApplicantStyles;
import views.style.StyleUtils;
import views.style.Styles;

public class ApplicantLayout extends BaseHtmlLayout {

  private final ProfileUtils profileUtils;

  @Inject
  public ApplicantLayout(ViewUtils viewUtils, Config configuration, ProfileUtils profileUtils) {
    super(viewUtils, configuration);
    this.profileUtils = checkNotNull(profileUtils);
  }

  @Override
  public Content render(HtmlBundle bundle) {
    bundle.addBodyStyles(ApplicantStyles.BODY_BG_COLOR);
    String currentTitle = bundle.getTitle();
    if (currentTitle != null && !currentTitle.isEmpty()) {
      bundle.setTitle(currentTitle + " - CiviForm");
    }
    return super.render(bundle);
  }

  public Content renderWithNav(Http.Request request, Messages messages, HtmlBundle bundle) {
    bundle.addHeaderContent(renderNavBar(request, messages));
    return render(bundle);
  }

  private ContainerTag renderNavBar(Http.Request request, Messages messages) {
    Optional<UatProfile> profile = profileUtils.currentUserProfile(request);
    return renderNavBar(profile, messages);
  }

  private ContainerTag renderNavBar(Optional<UatProfile> profile, Messages messages) {
    return nav()
        .withClasses(Styles.PT_8, Styles.PB_4, Styles.MB_12, Styles.FLEX, Styles.ALIGN_MIDDLE)
        .with(branding(), status(messages), maybeRenderTiButton(profile), logoutButton(messages));
  }

  private ContainerTag maybeRenderTiButton(Optional<UatProfile> profile) {
    if (profile.isPresent() && profile.get().getRoles().contains(Roles.ROLE_TI.toString())) {
      String tiDashboardText = "Trusted intermediary dashboard";
      String tiDashboardLink = routes.TrustedIntermediaryController.dashboard().url();
      return a(tiDashboardText)
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
        .withText("CiviForm");
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
