package views;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static j2html.TagCreator.div;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.option;
import static j2html.TagCreator.select;

import com.google.common.collect.ImmutableList;
import controllers.LanguageUtils;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.LabelTag;
import j2html.tags.specialized.OptionTag;
import j2html.tags.specialized.SelectTag;
import java.util.Locale;
import javax.inject.Inject;
import play.i18n.Lang;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/**
 * Contains functions for rendering language-related components. These are used to allow an
 * applicant to select their preferred language.
 */
public final class LanguageSelector {

  public final ImmutableList<Locale> supportedLanguages;

  @Inject
  public LanguageSelector(LanguageUtils languageUtils) {
    this.supportedLanguages =
        languageUtils.getApplicantEnabledLanguages().stream()
            .map(Lang::toLocale)
            .collect(toImmutableList());
  }

  public SelectTag renderDropdown(String preferredLanguage) {
    SelectTag dropdownTag =
        select()
            .withId("select-language")
            .withName("locale")
            .withClasses(
                "justify-center",
                "outline-none",
                "px-3",
                "mx-3",
                "py-1",
                "text-xs",
                "rounded-full",
                "border",
                // On hover/focus, invert the colors to make the focus state visually distinctive.
                // See https://github.com/civiform/civiform/issues/3558.
                "border-gray-500",
                StyleUtils.focus("border-black"),
                StyleUtils.hover("border-black"),
                "bg-white",
                StyleUtils.focus("bg-black"),
                StyleUtils.hover("bg-black"),
                "text-black",
                StyleUtils.focus("text-white"),
                StyleUtils.hover("text-white"));

    // An option consists of the language (localized to that language - for example,
    // this would display 'Español' for es-US), and the value is the ISO code.
    this.supportedLanguages.stream()
        .forEach(
            locale -> {
              String value = locale.toLanguageTag();
              String label = formatLabel(locale);
              OptionTag optionTag = option(label).withLang(value).withValue(value);
              if (value.equals(preferredLanguage)) {
                optionTag.isSelected();
              }
              dropdownTag.with(optionTag);
            });
    return dropdownTag;
  }

  public DivTag renderRadios(String preferredLanguage) {
    DivTag options = div();
    this.supportedLanguages.stream()
        .forEach(
            locale ->
                options.with(
                    renderRadioOption(
                        formatLabel(locale),
                        locale,
                        locale.toLanguageTag().equals(preferredLanguage))));
    return options;
  }

  private DivTag renderRadioOption(String text, Locale locale, boolean checked) {
    LabelTag labelTag =
        label()
            .withLang(locale.toLanguageTag())
            .withClasses(
                ReferenceClasses.RADIO_OPTION,
                checked ? BaseStyles.RADIO_LABEL_SELECTED : BaseStyles.RADIO_LABEL)
            .with(
                input()
                    .withType("radio")
                    .withName("locale")
                    .withValue(locale.toLanguageTag())
                    .withCondChecked(checked)
                    .withClasses(
                        StyleUtils.joinStyles(ReferenceClasses.RADIO_INPUT, BaseStyles.RADIO)))
            .withText(text);

    return div().withClasses("my-2", "relative").with(labelTag);
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
