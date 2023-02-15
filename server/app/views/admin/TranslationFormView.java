package views.admin;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.fieldset;
import static j2html.TagCreator.form;
import static j2html.TagCreator.p;
import static j2html.TagCreator.pre;

import com.google.common.collect.ImmutableList;
import j2html.tags.DomContent;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FieldsetTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.LegendTag;
import java.util.Locale;
import play.mvc.Http;
import services.LocalizedStrings;
import services.TranslationLocales;
import views.BaseHtmlView;
import views.components.LinkElement;
import views.style.AdminStyles;

/**
 * Contains helper methods for rendering a form allow an admin to translate a given entity, such as
 * a question or program.
 */
public abstract class TranslationFormView extends BaseHtmlView {

  private final TranslationLocales translationLocales;

  public TranslationFormView(TranslationLocales translationLocales) {
    this.translationLocales = checkNotNull(translationLocales);
  }

  /** Render a list of languages, with the currently selected language underlined. */
  protected final DivTag renderLanguageLinks(long entityId, Locale currentlySelected) {
    return div()
        .withClasses("m-2")
        .with(
            each(
                translationLocales.translatableLocales(),
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
  private ATag renderLanguageLink(
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
  protected final FormTag renderTranslationForm(
      Http.Request request,
      Locale locale,
      String formAction,
      ImmutableList<DomContent> formFieldContent) {
    FormTag form =
        form()
            .withMethod("POST")
            .with(makeCsrfTokenInputTag(request))
            .withAction(formAction)
            .with(formFieldContent)
            .with(
                submitButton(
                        String.format(
                            "Save %s updates",
                            locale.getDisplayLanguage(LocalizedStrings.DEFAULT_LOCALE)))
                    .withId("update-localizations-button")
                    .withClasses(AdminStyles.PRIMARY_BUTTON_STYLES));
    return form;
  }

  /**
   * Returns a div containing the default text to be translated. This allows for admins to more
   * easily identify which text to translate.
   */
  protected final DivTag fieldWithDefaultLocaleTextHint(
      DomContent field, LocalizedStrings localizedStrings) {
    return div()
        .withClasses("grid", "gap-6", "grid-cols-2")
        .with(
            field,
            div()
                .withClasses("px-2", "py-1", "text-sm", "bg-gray-100")
                .with(
                    p("English text:").withClass("font-medium"),
                    pre(localizedStrings.getDefault()).withClasses("font-sans")));
  }

  /** Creates a fieldset wrapping several form fields to be rendered. */
  protected final FieldsetTag fieldSetForFields(
      LegendTag legendContent, ImmutableList<DomContent> fields) {
    return fieldset()
        .withClasses("my-4", "pt-1", "pb-2", "px-2", "border")
        .with(legendContent, div().withClasses("flex-row", "space-y-4").with(fields));
  }
}
