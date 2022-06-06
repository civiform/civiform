package views.admin;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;

import com.google.common.collect.ImmutableList;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import java.util.Locale;
import play.i18n.Lang;
import play.i18n.Langs;
import play.mvc.Http;
import services.LocalizedStrings;
import views.BaseHtmlView;
import views.components.FieldWithLabel;
import views.components.LinkElement;
import views.style.AdminStyles;
import views.style.Styles;

/**
 * Contains helper methods for rendering a form allow an admin to translate a given entity, such as
 * a question or program.
 */
public abstract class TranslationFormView extends BaseHtmlView {

  private final ImmutableList<Locale> supportedLocales;

  public TranslationFormView(Langs langs) {
    this.supportedLocales =
        langs.availables().stream().map(Lang::toLocale).collect(toImmutableList());
  }

  /** Render a list of languages, with the currently selected language underlined. */
  public DivTag renderLanguageLinks(long entityId, Locale currentlySelected) {
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

  /**
   * Given the ID of the entity to translate and a locale for translation, returns a link
   * destination URL for the edit form to translate the entity in the given locale.
   */
  protected abstract String languageLinkDestination(long entityId, Locale locale);

  /**
   * Renders a single locale as the English version of the language (ex: es-US would read
   * "Spanish"). The text links to a form to translate the entity into that language.
   */
  private DivTag renderLanguageLink(
      String linkDestination, Locale locale, boolean isCurrentlySelected) {
    LinkElement link =
        new LinkElement()
            .setHref(linkDestination)
            .setText(locale.getDisplayLanguage(LocalizedStrings.DEFAULT_LOCALE));

    if (isCurrentlySelected) {
      link.setStyles(AdminStyles.LANGUAGE_LINK_SELECTED);
    } else {
      link.setStyles(AdminStyles.LANGUAGE_LINK_NOT_SELECTED);
    }

    return link.asAnchorText();
  }

  /**
   * Renders a form that allows an admin to enter localized text for an entity's applicant-visible
   * fields.
   */
  protected FormTag renderTranslationForm(
      Http.Request request,
      Locale locale,
      String formAction,
      ImmutableList<FieldWithLabel> formFields) {
    FormTag form =
        form()
            .withMethod("POST")
            .with(makeCsrfTokenInputTag(request))
            .withAction(formAction)
            .with(each(formFields, FieldWithLabel::getContainer))
            .with(
                submitButton(
                        String.format(
                            "Save %s updates",
                            locale.getDisplayLanguage(LocalizedStrings.DEFAULT_LOCALE)))
                    .withId("update-localizations-button"));
    return form;
  }
}
