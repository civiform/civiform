package services;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import play.i18n.Lang;
import play.i18n.Langs;

/** Helpers to support locales in which translations are supported. */
public class TranslationLocales {

  private final ImmutableList<Locale> translatableLocales;
  private final ImmutableMap<String, Locale> languageTagToLocale;

  @Inject
  public TranslationLocales(Langs langs) {
    this.translatableLocales =
        langs.availables().stream()
            .map(Lang::toLocale)
            .filter(Predicate.not(LocalizedStrings.DEFAULT_LOCALE::equals))
            .collect(ImmutableList.toImmutableList());
    this.languageTagToLocale =
        this.translatableLocales.stream()
            .collect(ImmutableMap.toImmutableMap(Locale::toLanguageTag, Function.identity()));
  }

  /**
   * Returns a {@link Locale} for the provided language tag if it is a locale that has support for
   * translations.
   */
  public Optional<Locale> fromLanguageTag(String languageTag) {
    return Optional.ofNullable(languageTagToLocale.get(languageTag));
  }

  /**
   * The set of supported non-English {@link java.util.Locale}s used for translations.
   *
   * <p>Note: If the set of supported locales is ONLY English, the resulting list will be empty.
   */
  public ImmutableList<Locale> translatableLocales() {
    return translatableLocales;
  }
}
