package views.applicant;

import static j2html.TagCreator.a;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.head;
import static j2html.TagCreator.nav;
import static j2html.TagCreator.span;
import static j2html.TagCreator.title;

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

  // Build bundle...
  bundle = new HtmlBundle()
    .setTitle("Page Title")
    .addHeaderContent(ApplicantView.renderNavBar(profile, messages))
    .addMainContent(mainContents);
    
  protected Content render(HtmlBundle bundle) {
    return htmlContent(bundle);
  }

  protected Content render(Http.Request request, Messages messages, DomContent... mainDomContents) {
    return render(profileUtils.currentUserProfile(request), messages, mainDomContents);
  }

  /** Renders mainDomContents within the main tag, in the context of the applicant layout. */
  protected Content render(
      Optional<UatProfile> profile, Messages messages, DomContent... mainDomContents) {
    return htmlContent(
        head(
          title("Applicant layout title"), 
          tailwindStyles()
        ),      
        body(
          renderNavBar(profile, messages),
          mainDomContents,
          viewUtils.makeLocalJsTag("main")
        )
    );
  }

  /** 
   * Anything that renders a tag should be in ApplicantView. 
   */
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

  /** 
   * Anything that renders a tag should be in ApplicantView. 
   */
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

  /** 
   * Anything that renders a tag should be in ApplicantView. 
   */
  private ContainerTag branding() {
    return div()
        .withId("brand-id")
        .withClasses(Styles.W_1_2, ApplicantStyles.LOGO_STYLE)
        .with(span("Civi"))
        .with(span("Form").withClasses(Styles.FONT_THIN));
  }

  /** 
   * Anything that renders a tag should be in ApplicantView. 
   */
  private ContainerTag status(Messages messages) {
    return div()
        .withId("application-status")
        .withClasses(Styles.W_1_4, Styles.TEXT_RIGHT, Styles.TEXT_SM, Styles.UNDERLINE)
        .with(span(messages.at(MessageKey.LINK_VIEW_APPLICATIONS.getKeyName())));
  }

  /** 
   * Anything that renders a tag should be in ApplicantView. 
   */
  private ContainerTag logoutButton(Messages messages) {
    String logoutLink = org.pac4j.play.routes.LogoutController.logout().url();
    return a(messages.at(MessageKey.BUTTON_LOGOUT.getKeyName()))
        .withHref(logoutLink)
        .withClasses(
            Styles.PX_3, Styles.TEXT_SM, Styles.OPACITY_75, StyleUtils.hover(Styles.OPACITY_100));
  }  
}
