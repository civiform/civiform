package services;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A collection of localized strings intended to represent the same message, translated into
 * different locales.
 *
 * <p>There is a bit of nuance around {@link #isRequired()}.
 */
@AutoValue
public abstract class LocalizedStrings {

  /** The default locale for CiviForm is US English. */
  public static final Locale DEFAULT_LOCALE = Locale.US;

  /** A mapping from {@link Locale} to the translation. */
  @JsonProperty("translations")
  public abstract ImmutableMap<Locale, String> translations();

  /**
   * The only time it matters whether localized strings are required are when there are no
   * translations. If there are no translations and the localized strings are NOT required, then
   * {@link #get(Locale)} returns an empty string. If there are no translations and the localized
   * strings are required, then {@link #get(Locale)} will throw {@link TranslationNotFoundException}
   * for every locale.
   *
   * <p>The only way to create non-required localized strings is with {@link #create(ImmutableMap,
   * boolean)}, where the translations are empty and they can be empty.
   */
  // isRequired inside of questionText
  // already has a JsonProperty on it which is interesting...
  @JsonProperty("isRequired")
  public abstract boolean isRequired();

  /** This factory is intended to be used by Jackson to construct localized strings from storage. */
  @JsonCreator
  public static LocalizedStrings jsonCreator(
      @JsonProperty("translations") ImmutableMap<Locale, String> translations,
      @JsonProperty("isRequired") boolean isRequired) {
    return create(translations, !isRequired);
  }

  /** Creates required localized strings. */
  public static LocalizedStrings create(ImmutableMap<Locale, String> translations) {
    return builder().setTranslations(translations).build();
  }

  /**
   * Creates localized strings, where if the translations are empty and they can be empty, then the
   * resulting localized strings are NOT required (see {@link #isRequired()}).
   */
  public static LocalizedStrings create(
      ImmutableMap<Locale, String> translations, boolean canBeEmpty) {
    boolean isRequired = !canBeEmpty || !translations.isEmpty();
    return builder().setTranslations(translations).setIsRequired(isRequired).build();
  }

  /** Creates localized strings with a translation for the default locale. */
  public static LocalizedStrings withDefaultValue(String defaultValue) {
    return create(ImmutableMap.of(DEFAULT_LOCALE, defaultValue));
  }

  /** Creates localized strings with an empty string for the default locale. */
  public static LocalizedStrings withEmptyDefault() {
    return LocalizedStrings.create(ImmutableMap.of(DEFAULT_LOCALE, ""));
  }

  /** Creates localized strings with no translations, that are not required. */
  public static LocalizedStrings empty() {
    return create(ImmutableMap.of(), true);
  }

  /** Creates localized strings with no translations, that are required. */
  public static LocalizedStrings of() {
    return create(ImmutableMap.of());
  }

  /** Create localized strings with one translation. */
  public static LocalizedStrings of(Locale k1, String v1) {
    return LocalizedStrings.create(ImmutableMap.of(k1, v1));
  }

  /**
   * Create localized strings with two translations.
   *
   * @throws IllegalArgumentException for duplicate keys.
   */
  public static LocalizedStrings of(Locale k1, String v1, Locale k2, String v2) {
    return LocalizedStrings.create(ImmutableMap.of(k1, v1, k2, v2));
  }

  /**
   * Create localized strings with three translations.
   *
   * @throws IllegalArgumentException for duplicate keys.
   */
  public static LocalizedStrings of(
      Locale k1, String v1, Locale k2, String v2, Locale k3, String v3) {
    return LocalizedStrings.create(ImmutableMap.of(k1, v1, k2, v2, k3, v3));
  }

  /**
   * Create localized strings with four translations.
   *
   * @throws IllegalArgumentException for duplicate keys.
   */
  public static LocalizedStrings of(
      Locale k1, String v1, Locale k2, String v2, Locale k3, String v3, Locale k4, String v4) {
    return LocalizedStrings.create(ImmutableMap.of(k1, v1, k2, v2, k3, v3, k4, v4));
  }

  /** Create localized strings with translation data from the provided map. */
  public static LocalizedStrings of(ImmutableMap<Locale, String> translations) {
    return LocalizedStrings.create(translations);
  }

  /** Returns true if these localized strings have a translation for the locale. */
  public boolean hasTranslationFor(Locale locale) {
    return translations().containsKey(locale);
  }

  /** Returns the set of locales these localized strings support. */
  public ImmutableSet<Locale> locales() {
    return translations().keySet();
  }

  /** Returns true if these localized strings have no translations. */
  @JsonIgnore
  public boolean isEmpty() {
    return translations().isEmpty();
  }

  /** Returns true if any of the translations are blank strings. */
  public boolean hasEmptyTranslation() {
    return translations().values().stream().anyMatch(String::isBlank);
  }

  /**
   * Attempts to get the translation for the given locale, and falls back on the translation for the
   * default locale if it is missing.
   *
   * @throws RuntimeException if it tries to use the translation for the default locale and it is
   *     missing.
   */
  public String getOrDefault(Locale locale) {
    try {
      return get(locale);
    } catch (TranslationNotFoundException e) {
      return getDefault();
    }
  }

  /**
   * Gets the translation for CiviForm's default locale.
   *
   * @throws RuntimeException if the translation for the default locale is missing.
   */
  @JsonIgnore
  public String getDefault() {
    try {
      return get(DEFAULT_LOCALE, true);
    } catch (TranslationNotFoundException e) {
      // This should never happen - US English should always be supported.
      throw new RuntimeException(e);
    }
  }

  /**
   * Get an {@link Optional} containing the translation for the locale, or {@link Optional#empty()}
   * if the locale is not supported. This method does not care about {@link #isRequired()}.
   */
  public Optional<String> maybeGet(Locale locale) {
    try {
      return Optional.of(get(locale, true));
    } catch (TranslationNotFoundException e) {
      return Optional.empty();
    }
  }

  /**
   * Get the translation for the given locale.
   *
   * <p>If the localized strings are not required (see {@link #isRequired()}, and there are no
   * translations, this will return the empty string.
   *
   * @throws TranslationNotFoundException if the locale is not supported.
   */
  public String get(Locale locale) throws TranslationNotFoundException {
    return get(locale, isRequired());
  }

  /**
   * Get the translation for the given locale.
   *
   * <p>If these localized strings are not required (see {@link #isRequired()}), and there are no
   * translations, then return the empty string.
   *
   * <p>This method exists because {@link #maybeGet(Locale)} needs a {@link #get(Locale, boolean)}
   * where {@code isRequired} is true, whether or not {@link #isRequired()} is.
   *
   * @param isRequired if true, allows the empty string to be thrown if there are no locales
   *     supported. If there is at least one locale supported, then it still throws {@link
   *     TranslationNotFoundException} if the locale is not supported.
   * @throws TranslationNotFoundException if translations are required, but the locale is not
   *     supported.
   */
  private String get(Locale locale, boolean isRequired) throws TranslationNotFoundException {
    if (!isRequired && isEmpty()) {
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

  /** Returns a new set of localized strings with a new translation in the default locale. */
  public LocalizedStrings updateDefaultTranslation(String string) {
    return updateTranslation(DEFAULT_LOCALE, string);
  }

  /**
   * Returns a new set of localized strings with a new translation.
   *
   * <p>Since this supports at least one locale, it will be required.
   *
   * <p>TODO(#3144): Consider removing this and only use the Optional version.
   */
  public LocalizedStrings updateTranslation(Locale locale, String value) {
    return translationsForAllLocalesExcept(locale).put(locale, value).build();
  }

  /**
   * Returns a new set of localized strings with an updated translation for the locale. If value is
   * provided, the translation will be added/edited for the given locale. If the value is not
   * provided, the associated translation will be cleared for the given locale.
   */
  public LocalizedStrings updateTranslation(Locale locale, Optional<String> value) {
    if (value.isPresent()) {
      return updateTranslation(locale, value.get());
    }
    return translationsForAllLocalesExcept(locale).build();
  }

  private LocalizedStrings.Builder translationsForAllLocalesExcept(Locale locale) {
    LocalizedStrings.Builder builder = builder();
    for (Map.Entry<Locale, String> entry : translations().entrySet()) {
      if (!entry.getKey().equals(locale)) {
        builder.put(entry.getKey(), entry.getValue());
      }
    }
    return builder;
  }

  public abstract Builder toBuilder();

  public static Builder builder() {
    Builder builder = new AutoValue_LocalizedStrings.Builder();
    return builder.setIsRequired(true);
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract LocalizedStrings build();

    public abstract Builder setTranslations(ImmutableMap<Locale, String> translations);

    public abstract Builder setIsRequired(boolean isRequired);

    public abstract ImmutableMap.Builder<Locale, String> translationsBuilder();

    public Builder put(Locale locale, String string) {
      translationsBuilder().put(locale, string);
      return this;
    }
  }
}
