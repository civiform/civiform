package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import controllers.routes;

import j2html.tags.specialized.DivTag;

import java.util.Optional;
import models.Account;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.LinkElement;
import views.components.ToastMessage;
import views.style.ApplicantStyles;
import views.style.StyleUtils;
import views.style.Styles;

/** Renders a confirmation page after application submission. */
public final class ApplicantUpsellCreateAccountView extends BaseHtmlView {

  private final ApplicantLayout layout;

  @Inject
  public ApplicantUpsellCreateAccountView(ApplicantLayout layout) {
    this.layout = checkNotNull(layout);
  }

  /** Renders a sign-up page with a baked-in redirect. */
  public Content render(
      Http.Request request,
      String redirectTo,
      Account account,
      String programTitle,
      Optional<String> applicantName,
      Long applicationId,
      Messages messages,
      Optional<String> banner) {
    String title = messages.at(MessageKey.TITLE_APPLICATION_CONFIRMATION.getKeyName());

    HtmlBundle bundle = layout.getBundle().setTitle(title);

    DivTag createAccountBox =
        div()
            .withClasses(
                Styles.BORDER,
                Styles.BORDER_GRAY_200,
                Styles.ROUNDED_2XL,
                Styles.SHADOW_MD,
                Styles.BG_WHITE,
                Styles.P_4,
                StyleUtils.responsiveSmall(Styles.P_6),
                Styles.MY_6)
            .with(
                h2(messages.at(MessageKey.TITLE_CREATE_AN_ACCOUNT.getKeyName()))
                    .withClasses(Styles.MB_4))
            .with(
                div(messages.at(MessageKey.CONTENT_PLEASE_CREATE_ACCOUNT.getKeyName()))
                    .withClasses(Styles.MB_4))
            .with(
                div()
                    .withClasses(
                        Styles.FLEX,
                        Styles.FLEX_COL,
                        Styles.GAP_4,
                        StyleUtils.responsiveSmall(Styles.FLEX_ROW))
                    // Empty div to push buttons to the right on desktop.
                    .with(div().withClasses(Styles.FLEX_GROW))
                    .with(
                        new LinkElement()
                            .setHref(redirectTo)
                            .setText(
                                messages.at(MessageKey.LINK_APPLY_TO_ANOTHER_PROGRAM.getKeyName()))
                            .asButton()
                            .withClasses(ApplicantStyles.BUTTON_NOT_RIGHT_NOW))
                    .with(
                        new LinkElement()
                            .setHref(org.pac4j.play.routes.LogoutController.logout().url())
                            .setText(messages.at(MessageKey.LINK_ALL_DONE.getKeyName()))
                            .asButton()
                            .withClasses(ApplicantStyles.BUTTON_NOT_RIGHT_NOW))
                    .with(
                        new LinkElement()
                            .setHref(
                                routes.LoginController.applicantLogin(Optional.of(redirectTo))
                                    .url())
                            .setText(
                                messages.at(MessageKey.LINK_CREATE_ACCOUNT_OR_SIGN_IN.getKeyName()))
                            .asButton()
                            .withClasses(ApplicantStyles.BUTTON_CREATE_ACCOUNT)));

    DivTag content =
        div()
            .with(
                div(messages.at(
                        MessageKey.CONTENT_CONFIRMED.getKeyName(), programTitle, applicationId))
                    .withClasses(Styles.TEXT_LG));

    // Don't show "create an account" upsell box to TIs, or anyone with an email address already.
    if (Strings.isNullOrEmpty(account.getEmailAddress()) && account.getMemberOfGroup().isEmpty()) {
      content.with(createAccountBox);
    } else {
      content.with(
          new LinkElement()
              .setHref(redirectTo)
              .setText(messages.at(MessageKey.LINK_APPLY_TO_ANOTHER_PROGRAM.getKeyName()))
              .asAnchorText());
    }

    if (banner.isPresent()) {
      bundle.addToastMessages(ToastMessage.error(banner.get()));
    }

    bundle
        .addMainStyles(ApplicantStyles.MAIN_PROGRAM_APPLICATION)
        .addMainContent(div(h1(title).withClasses(ApplicantStyles.H1_PROGRAM_APPLICATION)), content);

    return layout.renderWithNav(request, applicantName, messages, bundle);
  }
}
