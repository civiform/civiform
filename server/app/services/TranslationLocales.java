package services;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import play.i18n.Lang;
import play.i18n.Langs;

/** Helpers to support locales in which translations are supported. */
public class TranslationLocales {

  private final ImmutableList<Locale> localesForTranslation;
  private final ImmutableMap<String, Locale> languageTagToLocale;

  @Inject
  public TranslationLocales(Langs langs) {
    this.localesForTranslation =
        langs.availables().stream()
            .map(Lang::toLocale)
            .filter(locale -> !LocalizedStrings.DEFAULT_LOCALE.equals(locale))
            .collect(ImmutableList.toImmutableList());
    this.languageTagToLocale =
        this.localesForTranslation.stream()
            .collect(ImmutableMap.toImmutableMap(Locale::toLanguageTag, Function.identity()));
  }

  /** Returns whether the provided locale is supported for translations. */
  public Optional<Locale> getSupportedLocale(String languageTag) {
    return Optional.ofNullable(languageTagToLocale.get(languageTag));
  }

  /**
   * Holds a list of {@link java.util.Locale} objects representing the non-English locales used for
   * translations.
   *
   * <p>Note: If the set of supported locales is ONLY English, the resulting list will be empty.
   */
  public ImmutableList<Locale> localesForTranslation() {
    return localesForTranslation;
  }
}
