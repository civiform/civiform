package views.errors;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.p;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.span;

import com.google.inject.Inject;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.H1Tag;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.LanguageSelector;
import views.applicant.ApplicantLayout;
import views.components.LinkElement;
import views.style.ErrorStyles;

/**
 * Renders a page to handle 404 not found errors that will be shown to users instead of the unthemed
 * default Play page.
 */
public final class NotFound extends BaseHtmlView {

  private final ApplicantLayout layout;
  private final LanguageSelector languageSelector;

  @Inject
  public NotFound(ApplicantLayout layout, LanguageSelector languageSelector) {
    this.layout = checkNotNull(layout);
    this.languageSelector = checkNotNull(languageSelector);
  }

  public Content render(Http.RequestHeader request, Messages messages) {
    HtmlBundle bundle = addBodyFooter(request, messages);
    return layout.render(bundle);
  }

  private H1Tag h1Content(Messages messages) {
    return h1(span(messages.at(MessageKey.ERROR_NOT_FOUND_TITLE.getKeyName())))
        .withClasses(ErrorStyles.H1_NOT_FOUND);
  }

  private DivTag descriptionContent(Messages messages) {
    ATag homepageLink =
        new LinkElement()
            .setStyles("underline")
            .setText(messages.at(MessageKey.ERROR_NOT_FOUND_DESCRIPTION_LINK.getKeyName()))
            .setHref("/")
            .opensInNewTab()
            .asAnchorText()
            .attr(
                "aria-label",
                messages.at(MessageKey.ERROR_NOT_FOUND_DESCRIPTION_LINK.getKeyName()));

    return div(p(span(
                rawHtml(
                    messages.at(
                        MessageKey.ERROR_NOT_FOUND_DESCRIPTION.getKeyName(), homepageLink))))
            .withClasses(ErrorStyles.P_MOBILE_INLINE))
        .withClasses(ErrorStyles.P_DESCRIPTION);
  }

  /** Page returned on 404 error */
  private DivTag mainContent(Messages messages) {
    return div(h1Content(messages), descriptionContent(messages))
        .withClasses("text-center", "max-w-screen-sm", "w-5/6", "mx-auto");
  }

  private HtmlBundle addBodyFooter(Http.RequestHeader request, Messages messages) {
    HtmlBundle bundle = layout.getBundle(request);
    String language = languageSelector.getPreferredLangage(request).code();
    bundle.setLanguage(language);
    bundle.addMainContent(mainContent(messages));

    return bundle;
  }
}
