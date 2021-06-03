package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;

import com.google.inject.Inject;
import controllers.routes;
import j2html.tags.ContainerTag;
import java.util.Optional;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.LinkElement;
import views.components.ToastMessage;
import views.style.ApplicantStyles;
import views.style.Styles;

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
      String userName,
      String programTitle,
      Long applicationId,
      Messages messages,
      Optional<String> banner) {
    // TODO(natsid): i18n this title
    String title = "Application confirmation";

    HtmlBundle bundle = layout.getBundle().setTitle(title);

    // TODO(natsid): Either i18n this title, or move the content of this page to the application
    //  confirmation page.
    String createAccountText = "Create an account or sign in";

    ContainerTag createAccountBox =
        div()
            .withClasses(
                Styles.BORDER,
                Styles.BORDER_GRAY_200,
                Styles.SHADOW_MD,
                Styles.BG_WHITE,
                Styles.P_10,
                Styles.MY_6)
            .with(h2(createAccountText).withClasses(Styles.MB_4))
            .with(
                div(messages.at(MessageKey.CONTENT_PLEASE_CREATE_ACCOUNT.getKeyName()))
                    .withClasses(Styles.MB_4))
            .with(
                div()
                    .withClasses(Styles.FLEX, Styles.FLEX_ROW, Styles.GAP_4)
                    // Empty div to push buttons to the right.
                    .with(div().withClasses(Styles.FLEX_GROW))
                    .with(
                        new LinkElement()
                            .setHref(redirectTo)
                            .setText(messages.at(MessageKey.LINK_DONT_SIGN_IN.getKeyName()))
                            .asButton()
                            .withClasses(ApplicantStyles.BUTTON_NOT_RIGHT_NOW))
                    .with(
                        new LinkElement()
                            .setHref(
                                routes.LoginController.idcsLoginWithRedirect(
                                        Optional.of(redirectTo))
                                    .url())
                            .setText(messages.at(MessageKey.LINK_DO_SIGN_IN.getKeyName()))
                            .asButton()
                            .withClasses(ApplicantStyles.BUTTON_CREATE_ACCOUNT)));

    ContainerTag content =
        div()
            .with(
                div(messages.at(
                        MessageKey.CONTENT_CONFIRMED.getKeyName(), programTitle, applicationId))
                    .withClasses(Styles.TEXT_LG))
            .with(createAccountBox);

    if (banner.isPresent()) {
      bundle.addToastMessages(ToastMessage.error(banner.get()));
    }

    bundle
        .addMainStyles(ApplicantStyles.MAIN_PROGRAM_APPLICATION)
        .addMainContent(h1(title).withClasses(ApplicantStyles.H1_PROGRAM_APPLICATION), content);

    return layout.renderWithNav(request, userName, messages, bundle);
  }
}
