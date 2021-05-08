package services;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@AutoValue
public abstract class LocalizedStrings {

  /** The default locale for CiviForm is US English. */
  public static final Locale DEFAULT_LOCALE = Locale.US;

  public abstract ImmutableMap<Locale, String> translations();

  public abstract boolean isRequired();

  public static LocalizedStrings create(ImmutableMap<Locale, String> translations) {
    return builder().setTranslations(translations).build();
  }

  public static LocalizedStrings create(ImmutableMap<Locale, String> translations, boolean isRequired) {
    return builder().setTranslations(translations).setIsRequired(isRequired).build();
  }

  public static LocalizedStrings withDefaultValue(String defaultValue) {
    return create(ImmutableMap.of(DEFAULT_LOCALE, defaultValue));
  }

  /** Creating an empty LocalizedStrings means it is not required. */
  public static LocalizedStrings of() {
    return create(ImmutableMap.of(), false);
  }

  public static LocalizedStrings of(Locale k1, String v1) {
    return LocalizedStrings.create(ImmutableMap.of(k1, v1));
  }

  public static LocalizedStrings of(Locale k1, String v1, Locale k2, String v2) {
    return LocalizedStrings.create(ImmutableMap.of(k1, v1, k2, v2));
  }

  public static LocalizedStrings of(
      Locale k1, String v1, Locale k2, String v2, Locale k3, String v3) {
    return LocalizedStrings.create(ImmutableMap.of(k1, v1, k2, v2, k3, v3));
  }

  public static LocalizedStrings of(
      Locale k1, String v1, Locale k2, String v2, Locale k3, String v3, Locale k4, String v4) {
    return LocalizedStrings.create(ImmutableMap.of(k1, v1, k2, v2, k3, v3, k4, v4));
  }

  public boolean hasTranslationFor(Locale locale) {
    return translations().containsKey(locale);
  }

  public ImmutableSet<Locale> locales() {
    return translations().keySet();
  }

  public boolean isEmpty() {
    return translations().isEmpty();
  }

  public boolean hasEmptyTranslation() {
    return translations().values().stream().anyMatch(String::isBlank);
  }



  /**
   * Attempts to get question text for the given locale. If there is no text for the given locale,
   * it will return the text in the default locale.
   */
  public String getOrDefault(Locale locale) {
    try {
      return get(locale);
    } catch (TranslationNotFoundException e) {
      return getDefault();
    }
  }

  /** Gets the question text for CiviForm's default locale. */
  public String getDefault() {
    try {
      return get(DEFAULT_LOCALE);
    } catch (TranslationNotFoundException e) {
      // This should never happen - US English should always be supported.
      throw new RuntimeException(e);
    }
  }

  /**
   * Return an {@link Optional} containing the question text for this locale, or of if this locale
   * is not supported.
   */
  public Optional<String> maybeGet(Locale locale) {
    try {
      return Optional.of(get(locale));
    } catch (TranslationNotFoundException e) {
      return Optional.empty();
    }
  }

  /** Get the question text for the given locale. */
  public String get(Locale locale) throws TranslationNotFoundException {
    if (!isRequired() && isEmpty()) {
      return "";
    }

    if (translations().containsKey(locale)) {
      return translations().get(locale);
    }

    // If we don't have the user's preferred locale, check if we have one which
    // contains their preferred language.  e.g. return "en_US" for "en_CA", or
    // "es_US" for "es_MX".  This is needed since some of our locale sources
    // provide only the language (e.g. "en").
    Optional<Locale> maybeLocale =
        translations().keySet().stream()
            .filter(l -> l.getLanguage().equals(locale.getLanguage()))
            .findFirst();
    return maybeLocale
        .map(foundLocale -> translations().get(foundLocale))
        .orElseThrow(() -> new TranslationNotFoundException(locale));
  }

  public LocalizedStrings updateTranslation(Locale locale, String string) {
    LocalizedStrings.Builder builder = toEmptyBuilder();
    for (Map.Entry<Locale, String> entry : translations().entrySet()) {
      if (!entry.getKey().equals(locale)) {
        builder.put(entry.getKey(), entry.getValue());
      }
    }
    builder.put(locale, string);
    return builder.build();
  }

  public abstract Builder toBuilder();

  public static Builder builder() {
    Builder builder = new AutoValue_LocalizedStrings.Builder();
    return builder.setIsRequired(true);
  }

  private Builder toEmptyBuilder() {
    Builder builder = new AutoValue_LocalizedStrings.Builder();
    return builder.setIsRequired(isRequired());
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setTranslations(ImmutableMap<Locale, String> translations);

    public abstract Builder setIsRequired(boolean isRequired);

    public abstract ImmutableMap.Builder<Locale, String> translationsBuilder();

    public abstract LocalizedStrings build();

    public Builder put(Locale locale, String string) {
      translationsBuilder().put(locale, string);
      return this;
    }

    public Builder clearTranslations() {
      return setTranslations(ImmutableMap.of());
    }
  }
}
