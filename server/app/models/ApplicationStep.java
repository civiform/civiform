package models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import services.LocalizedStrings;
import services.TranslationNotFoundException;

public final class ApplicationStep {

  private LocalizedStrings title;
  private LocalizedStrings description;

  public ApplicationStep(String title, String description) {
    this.setDefaultTitle(title);
    this.setDefaultDescription(description);
  }

  @JsonCreator
  public ApplicationStep(
      @JsonProperty("title") LocalizedStrings title,
      @JsonProperty("description") LocalizedStrings description) {
    this.title = title;
    this.description = description;
  }

  /** This factory is intended to be used by Jackson to construct application steps from storage. */
  // I don't think I need this explicit method... could i just put it on the constructor?
  // Yeah I think we only need this method if there's a way we want to manipulate the data before
  // creating the json
  @JsonCreator
  //   public static ApplicationStep jsonCreator(
  //       @JsonProperty("title") LocalizedStrings title,
  //       @JsonProperty("description") LocalizedStrings description) {
  //     return new ApplicationStep(title, description);
  //   }

  @JsonProperty("title")
  public LocalizedStrings getTitle() {
    return this.title;
  }

  @JsonProperty("description")
  public LocalizedStrings getDescription() {
    return this.description;
  }

  public String getTitleForLocale(Locale locale) {
    try {
      return this.title.get(locale);
    } catch (TranslationNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public String getDescriptionForLocale(Locale locale) {
    try {
      return this.description.get(locale);
    } catch (TranslationNotFoundException e) {
      throw new RuntimeException(e);
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
    this.title.updateTranslation(locale, title);
    return this;
  }

  public ApplicationStep setNewDescriptionTranslation(Locale locale, String description) {
    this.description.updateTranslation(locale, description);
    return this;
  }
}
