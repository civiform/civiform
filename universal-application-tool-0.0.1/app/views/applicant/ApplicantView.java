package views.applicant;


public class ApplicantView extends BaseHtmlView {

  /** 
   * Anything that renders a tag should be in ApplicantView. 
   */
  protected ContainerTag renderNavBar(Optional<UatProfile> profile, Messages messages) {
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
  protected ContainerTag maybeRenderTiButton(Optional<UatProfile> profile) {
      // TODO: i18n cleanup.
      String tiDashboardLinkText = "Trusted Intermediary Dashboard";
    if (profile.isPresent() && profile.get().getRoles().contains(Roles.ROLE_TI.toString())) {
      String tiDashboardLink = routes.TrustedIntermediaryController.dashboard().url();
      return a(tiDashboardLinkText)
          .withHref(tiDashboardLink)
          .withClasses(
              Styles.PX_3, Styles.TEXT_SM, Styles.OPACITY_75, StyleUtils.hover(Styles.OPACITY_100));
    }
    return div();
  }

  /** Renders the CiviForm title.  */
  protected ContainerTag branding() {
    return div()
        .withId("brand-id")
        .withClasses(Styles.W_1_2, ApplicantStyles.LOGO_STYLE)
        .with(span("Civi"))
        .with(span("Form").withClasses(Styles.FONT_THIN));
  }

  /**  Anything that renders a tag should be in ApplicantView.  */
  protected ContainerTag status(Messages messages) {
      String linkViewText = messages.at(MessageKey.LINK_VIEW_APPLICATIONS.getKeyName());
    return div()
        .withId("application-status")
        .withClasses(Styles.W_1_4, Styles.TEXT_RIGHT, Styles.TEXT_SM, Styles.UNDERLINE)
        .with(span(linkViewText));
  }

  /** 
   * Anything that renders a tag should be in ApplicantView. 
   */
  protected ContainerTag logoutButton(Messages messages) {
      String logoutText = messages.at(MessageKey.BUTTON_LOGOUT.getKeyName());
    String logoutLink = org.pac4j.play.routes.LogoutController.logout().url();
    return a(logoutText)
        .withHref(logoutLink)
        .withClasses(
            Styles.PX_3, Styles.TEXT_SM, Styles.OPACITY_75, StyleUtils.hover(Styles.OPACITY_100));
  }  
}