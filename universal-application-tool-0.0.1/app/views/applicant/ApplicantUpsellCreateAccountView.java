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
import views.style.Styles;

public final class ApplicantUpsellCreateAccountView extends BaseHtmlView {

  private final ApplicantLayout layout;

  @Inject
  public ApplicantUpsellCreateAccountView(ApplicantLayout layout) {
    this.layout = checkNotNull(layout);
  }

  /** Renders a sign-up page with a baked-in redirect. */
  public Content render(
      Http.Request request, String redirectTo, Messages messages, String userName) {
    String title = "Account Creation Interstitial";
    HtmlBundle bundle = layout.getBundle().setTitle(title);

    ContainerTag content = div().withClasses(Styles.MX_16);
    content.with(h2(messages.at(MessageKey.CONTENT_PLEASE_SIGN_IN.getKeyName())));
    content.with(
        new LinkElement()
            .setHref(routes.LoginController.idcsLoginWithRedirect(Optional.of(redirectTo)).url())
            .setText(messages.at(MessageKey.LINK_DO_SIGN_IN.getKeyName()))
            .asButton());
    content.with(
        new LinkElement()
            .setHref(redirectTo)
            .setText(messages.at(MessageKey.LINK_DONT_SIGN_IN.getKeyName()))
            .asButton());

    bundle.addMainContent(
        layout.renderProgramApplicationTitleAndProgressIndicator(title),
        h1(title).withClasses(Styles.PX_16, Styles.PY_4),
        content);

    return layout.renderWithNav(request, userName, messages, bundle);
  }
}
