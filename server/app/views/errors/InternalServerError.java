package views.errors;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.rawHtml;

import com.google.inject.Inject;
import controllers.LanguageUtils;
import j2html.tags.UnescapedText;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import services.settings.SettingsManifest;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.applicant.ApplicantLayout;
import views.components.LinkElement;
import views.components.TextFormatter;
import views.style.ApplicantStyles;

/**
 * Renders a page to handle internal server errors that will be shown to users instead of the
 * unthemed default Play page.
 */
public final class InternalServerError extends BaseHtmlView {

  private final ApplicantLayout layout;
  private final LanguageUtils languageUtils;
  private final SettingsManifest settingsManifest;

  @Inject
  public InternalServerError(
      ApplicantLayout layout, LanguageUtils languageUtils, SettingsManifest settingsManifest) {
    this.layout = checkNotNull(layout);
    this.languageUtils = checkNotNull(languageUtils);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  public Content render(Http.RequestHeader request, Messages messages, String exceptionId) {
    HtmlBundle bundle = layout.getBundle(request);
    String language = languageUtils.getPreferredLanguage(request).code();
    bundle.setLanguage(language);
    bundle.addMainContent(mainContent(request, messages, exceptionId));
    return layout.render(bundle);
  }

  /** Page returned on 500 error */
  private DivTag mainContent(
      Http.RequestHeader requestHeader, Messages messages, String exceptionId) {

    // TODO: update strings once translations come back
    String title = messages.at(MessageKey.ERROR_INTERNAL_SERVER_TITLE.getKeyName());
    Optional<UnescapedText> additionalInfo =
        Optional.of(buildAdditionalInfo(requestHeader, messages, exceptionId));
    String buttonText = messages.at(MessageKey.BUTTON_HOME_PAGE.getKeyName());

    return ErrorComponent.renderErrorComponent(
        title, Optional.empty(), additionalInfo, buttonText, messages, Optional.empty());
  }

  private UnescapedText buildAdditionalInfo(
      Http.RequestHeader requestHeader, Messages messages, String exceptionId) {
    String supportEmail = settingsManifest.getSupportEmailAddress(requestHeader).get();
    String emailLinkHref =
        String.format("mailto:%s?body=[CiviForm Error ID: %s]", supportEmail, exceptionId);
    ATag emailAction =
        new LinkElement()
            .setText(supportEmail)
            .setHref(emailLinkHref)
            .asAnchorText()
            .withClasses(ApplicantStyles.LINK);
    String descriptionText =
        messages.at(MessageKey.ERROR_INTERNAL_SERVER_DESCRIPTION.getKeyName(), exceptionId);
    // Since the exceptionId comes through in a query param, we want to sanitize it before allowing
    // the raw html to be rendered to remove any script tags or other potentially harmful injections
    String sanitizedDescription =
        TextFormatter.sanitizeHtml(String.format(descriptionText, emailAction.render()));
    return rawHtml(sanitizedDescription);
  }
}
