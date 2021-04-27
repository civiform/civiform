package views;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;

import com.google.common.collect.ImmutableList;
import j2html.tags.ContainerTag;
import java.util.Locale;
import play.i18n.Lang;
import play.i18n.Langs;
import services.LocalizationUtils;
import views.components.LinkElement;
import views.style.AdminStyles;
import views.style.Styles;

public abstract class TranslationFormView extends BaseHtmlView {

  private final ImmutableList<Locale> supportedLocales;

  public TranslationFormView(Langs langs) {
    this.supportedLocales =
        langs.availables().stream().map(Lang::toLocale).collect(toImmutableList());
  }

  /** Render a list of languages, with the currently selected language underlined. */
  public ContainerTag renderLanguageLinks(long entityId, Locale currentlySelected) {
    return div()
        .withClasses(Styles.M_2)
        .with(
            each(
                supportedLocales,
                locale -> {
                  String linkDestination = languageLinkDestination(entityId, locale);
                  return renderLanguageLink(
                      linkDestination, locale, locale.equals(currentlySelected));
                }));
  }

  protected abstract String languageLinkDestination(long entityId, Locale locale);

  private ContainerTag renderLanguageLink(
      String linkDestination, Locale locale, boolean isCurrentlySelected) {
    LinkElement link =
        new LinkElement()
            .setStyles("language-link", Styles.M_2)
            .setHref(linkDestination)
            .setText(locale.getDisplayLanguage(LocalizationUtils.DEFAULT_LOCALE));

    if (isCurrentlySelected) {
      link.setStyles(AdminStyles.LANGUAGE_LINK_SELECTED);
    } else {
      link.setStyles(AdminStyles.LANGUAGE_LINK_NOT_SELECTED);
    }

    return link.asAnchorText();
  }
}
