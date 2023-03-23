package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.section;

import com.google.common.base.Strings;
import controllers.routes;
import j2html.tags.DomContent;
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

/**
 * Renders a confirmation page after application submission.
 *
 * <p>This is the base view, and accepts title and body content params.
 */
public abstract class UpsellCreateAccountBaseView extends BaseHtmlView {

  private final ApplicantLayout layout;

  public UpsellCreateAccountBaseView(ApplicantLayout layout) {
    this.layout = checkNotNull(layout);
  }

  /** Renders a sign-up page with a baked-in redirect. */
  public Content renderContainer(
      Http.Request request,
      String redirectTo,
      Account account,
      Optional<String> applicantName,
      Messages messages,
      String title,
      DomContent confirmationBodyContent,
      Optional<ToastMessage> bannerMessage) {

    HtmlBundle bundle = layout.getBundle().setTitle(title);

    var content =
        div()
            .withClasses(ApplicantStyles.PROGRAM_INFORMATION_BOX)
            .with(h1(title).withClasses("text-3xl", "text-black", "font-bold", "mb-4"))
            .with(confirmationBodyContent);

    var createAccountSection =
        section()
            .with(
                h2(messages.at(MessageKey.TITLE_CREATE_AN_ACCOUNT.getKeyName()))
                    .withClasses("mb-4", "font-bold"))
            .with(
                div(messages.at(MessageKey.CONTENT_PLEASE_CREATE_ACCOUNT.getKeyName()))
                    .withClasses("mb-4"))
            .with(
                div()
                    .withClasses(
                        "flex", "flex-col", "gap-4", StyleUtils.responsiveMedium("flex-row"))
                    // Empty div to push buttons to the right on desktop.
                    .with(div().withClasses("flex-grow"))
                    .with(
                        redirectButton(
                                "another-program",
                                messages.at(MessageKey.LINK_APPLY_TO_ANOTHER_PROGRAM.getKeyName()),
                                redirectTo)
                            .withClasses(ApplicantStyles.BUTTON_UPSELL_SECONDARY_ACTION))
                    .with(
                        redirectButton(
                                "all-done",
                                messages.at(MessageKey.LINK_ALL_DONE.getKeyName()),
                                org.pac4j.play.routes.LogoutController.logout().url())
                            .withClasses(ApplicantStyles.BUTTON_UPSELL_SECONDARY_ACTION))
                    .with(
                        redirectButton(
                                "sign-in",
                                messages.at(MessageKey.LINK_CREATE_ACCOUNT_OR_SIGN_IN.getKeyName()),
                                routes.LoginController.applicantLogin(Optional.of(redirectTo))
                                    .url())
                            .withClasses(ApplicantStyles.BUTTON_UPSELL_PRIMARY_ACTION)));

    // Don't show "create an account" upsell box to TIs, or anyone with an email address already.
    if (Strings.isNullOrEmpty(account.getEmailAddress()) && account.getMemberOfGroup().isEmpty()) {
      content.with(createAccountSection);
    } else {
      content.with(
          new LinkElement()
              .setHref(redirectTo)
              .setText(messages.at(MessageKey.LINK_APPLY_TO_ANOTHER_PROGRAM.getKeyName()))
              .asAnchorText());
    }

    bannerMessage.ifPresent(bundle::addToastMessages);

    bundle.addMainStyles(ApplicantStyles.MAIN_PROGRAM_APPLICATION).addMainContent(content);

    return layout.renderWithNav(request, applicantName, messages, bundle);
  }
}
