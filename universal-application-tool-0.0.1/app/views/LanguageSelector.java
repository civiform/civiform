package views;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static j2html.TagCreator.div;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.option;
import static j2html.TagCreator.select;

import com.google.common.collect.ImmutableList;
import j2html.attributes.Attr;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Locale;
import javax.inject.Inject;
import play.i18n.Lang;
import play.i18n.Langs;
import play.i18n.MessagesApi;
import play.mvc.Http;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/**
 * Contains functions for rendering language-related components. These are used to allow an
 * applicant to select their preferred language.
 */
public class LanguageSelector {

  public final ImmutableList<Locale> supportedLanguages;
  private final MessagesApi messagesApi;

  @Inject
  public LanguageSelector(Langs langs, MessagesApi messagesApi) {
    this.messagesApi = messagesApi;
    this.supportedLanguages =
        langs.availables().stream().map(Lang::toLocale).collect(toImmutableList());
  }

  public Lang getPreferredLangage(Http.RequestHeader request) {
    return messagesApi.preferred(request).lang();
  }

  public ContainerTag renderDropdown(String preferredLanguage) {
    ContainerTag dropdownTag =
        select()
            .withId("select-language")
            .withName("locale")
            .withValue(preferredLanguage)
            .withClasses(
                Styles.BLOCK,
                Styles.OUTLINE_NONE,
                Styles.PX_3,
                Styles.MX_3,
                Styles.PY_1,
                Styles.BORDER,
                Styles.BORDER_GRAY_500,
                Styles.ROUNDED_FULL,
                Styles.BG_WHITE,
                Styles.TEXT_XS,
                StyleUtils.focus(BaseStyles.BORDER_SEATTLE_BLUE));

    // An option consists of the language (localized to that language - for example,
    // this would display 'Español' for es-US), and the value is the ISO code.
    this.supportedLanguages.stream()
        .forEach(
            locale -> {
              String value = locale.toLanguageTag();
              String label = formatLabel(locale);
              Tag optionTag = option(label).withValue(value);
              if (value.equals(preferredLanguage)) {
                optionTag.attr(Attr.SELECTED);
              }
              dropdownTag.with(optionTag);
            });
    return dropdownTag;
  }

  public ContainerTag renderRadios(String preferredLanguage) {
    ContainerTag options = div();
    this.supportedLanguages.stream()
        .forEach(
            locale ->
                options.with(
                    renderRadioOption(
                        formatLabel(locale),
                        locale.toLanguageTag(),
                        locale.toLanguageTag().equals(preferredLanguage))));
    return options;
  }

  private Tag renderRadioOption(String text, String value, boolean checked) {
    ContainerTag labelTag =
        label()
            .withClasses(
                ReferenceClasses.RADIO_OPTION,
                BaseStyles.RADIO_LABEL,
                checked ? BaseStyles.BORDER_SEATTLE_BLUE : "")
            .with(
                input()
                    .withType("radio")
                    .withName("locale")
                    .withValue(value)
                    .condAttr(checked, Attr.CHECKED, "")
                    .withClasses(
                        StyleUtils.joinStyles(ReferenceClasses.RADIO_INPUT, BaseStyles.RADIO)))
            .withText(text);

    return div().withClasses(Styles.MY_2, Styles.RELATIVE).with(labelTag);
  }

  /**
   * The dropdown option label should be the language name localized to that language - for example,
   * "español" for "es-US". We capitalize the first letter, since some locales do not capitalize
   * languages.
   */
  private String formatLabel(Locale locale) {
    // The default for Java is 中文, but the City of Seattle prefers 繁體中文
    if (locale.equals(Locale.TRADITIONAL_CHINESE)) {
      return "繁體中文";
    }
    String language = locale.getDisplayLanguage(locale);
    return language.substring(0, 1).toUpperCase(locale) + language.substring(1);
  }
}
