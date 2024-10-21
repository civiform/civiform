package models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import services.LocalizedStrings;
import services.TranslationNotFoundException;

public final class ApplicationStep {

  private LocalizedStrings title;
  private LocalizedStrings description;

  public ApplicationStep(String title, String description) {
    this.setDefaultTitle(title);
    this.setDefaultDescription(description);
  }

  public ApplicationStep(LocalizedStrings title, LocalizedStrings description) {
    this.title = title;
    this.description = description;
  }

  /** This factory is intended to be used by Jackson to construct application steps from storage. */
  @JsonCreator
  public static ApplicationStep jsonCreator(
      @JsonProperty("title") LocalizedStrings title,
      @JsonProperty("description") LocalizedStrings description) {
    return new ApplicationStep(title, description);
  }

  @JsonProperty("title")
  public LocalizedStrings getTitle() {
    return this.title;
  }

  @JsonProperty("description")
  public LocalizedStrings getDescription() {
    return this.description;
  }

  public Optional<String> getTitleForLocale(Locale locale) {
    try {
      return Optional.of(this.title.get(locale));
    } catch (TranslationNotFoundException e) {
      return Optional.empty();
    }
  }

  public Optional<String> getDescriptionForLocale(Locale locale) {
    try {
      return Optional.of(this.description.get(locale));
    } catch (TranslationNotFoundException e) {
      return Optional.empty();
    }
  }

  public ApplicationStep setDefaultTitle(String title) {
    this.title = LocalizedStrings.create(ImmutableMap.of(LocalizedStrings.DEFAULT_LOCALE, title));
    return this;
  }

  public ApplicationStep setDefaultDescription(String description) {
    this.description =
        LocalizedStrings.create(ImmutableMap.of(LocalizedStrings.DEFAULT_LOCALE, description));
    return this;
  }

  public ApplicationStep setNewTitleTranslation(Locale locale, String title) {
    this.title = this.title.updateTranslation(locale, title);
    return this;
  }

  public ApplicationStep setNewDescriptionTranslation(Locale locale, String description) {
    this.description = this.description.updateTranslation(locale, description);
    return this;
  }
}
