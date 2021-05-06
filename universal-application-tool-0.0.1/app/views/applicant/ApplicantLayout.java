package views.applicant;

import static j2html.TagCreator.a;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.head;
import static j2html.TagCreator.nav;
import static j2html.TagCreator.span;
import static j2html.TagCreator.title;

import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import javax.inject.Inject;
import play.i18n.Messages;
import play.twirl.api.Content;
import services.MessageKey;
import views.BaseHtmlLayout;
import views.ViewUtils;
import views.style.ApplicantStyles;
import views.style.BaseStyles;
import views.style.StyleUtils;
import views.style.Styles;

public class ApplicantLayout extends BaseHtmlLayout {

  @Inject
  public ApplicantLayout(ViewUtils viewUtils) {
    super(viewUtils);
  }

  /** Renders mainDomContents within the main tag, in the context of the applicant layout. */
  protected Content render(Messages messages, DomContent... mainDomContents) {
    return htmlContent(
        head().with(title("Applicant layout title")).with(tailwindStyles()),
        body()
            .with(renderNavBar(messages))
            .with(mainDomContents)
            .with(viewUtils.makeLocalJsTag("main"))
            .withClasses(BaseStyles.APPLICANT_BG_COLOR));
  }

  private ContainerTag renderNavBar(Messages messages) {
    return nav()
        .withClasses(
            Styles.PT_8,
            Styles.PB_4,
            Styles.MB_12,
            Styles.FLEX,
            Styles.ALIGN_MIDDLE,
            Styles.BORDER_B_4,
            Styles.BORDER_WHITE)
        .with(branding(), status(messages), logoutButton(messages));
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
        .withClasses(Styles.W_1_2, Styles.TEXT_RIGHT, Styles.TEXT_SM, Styles.UNDERLINE)
        .with(span(messages.at(MessageKey.LINK_VIEW_APPLICATIONS.getKeyName())));
  }

  private ContainerTag logoutButton(Messages messages) {
    String logoutLink = org.pac4j.play.routes.LogoutController.logout().url();
    return a(messages.at(MessageKey.BUTTON_LOGOUT.getKeyName()))
        .withHref(logoutLink)
        .withClasses(
            Styles.PX_3, Styles.TEXT_SM, Styles.OPACITY_75, StyleUtils.hover(Styles.OPACITY_100));
  }
}
