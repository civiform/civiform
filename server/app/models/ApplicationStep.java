package models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import services.LocalizedStrings;

/**
 * Defines a single application step to save on a program. Each application step contains a
 * localized title and a localized description. Programs contain between 1-5 application steps.
 */
public final class ApplicationStep {

  private LocalizedStrings title;
  private LocalizedStrings description;

  @JsonCreator
  public ApplicationStep(
      @JsonProperty("title") LocalizedStrings title,
      @JsonProperty("description") LocalizedStrings description) {
    this.title = title;
    this.description = description;
  }

  /** Constructs an {@code ApplicationStep} with values for our default locale (en-US) */
  public ApplicationStep(String title, String description) {
    this.title = LocalizedStrings.create(ImmutableMap.of(LocalizedStrings.DEFAULT_LOCALE, title));
    this.description =
        LocalizedStrings.create(ImmutableMap.of(LocalizedStrings.DEFAULT_LOCALE, description));
  }

  public LocalizedStrings getTitle() {
    return this.title;
  }

  public LocalizedStrings getDescription() {
    return this.description;
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
