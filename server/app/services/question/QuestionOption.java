package services.question;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import services.LocalizedStrings;
import services.TranslationNotFoundException;

/**
 * Represents a single option in a {@link services.question.types.MultiOptionQuestionDefinition}.
 */
@JsonDeserialize(builder = AutoValue_QuestionOption.Builder.class)
@AutoValue
public abstract class QuestionOption {

  /** The id for this option. */
  @JsonProperty("id")
  public abstract long id();

  /** The immutable identifier for this option, used to reference it in the API and predicates. */
  @JsonProperty("adminName")
  public abstract String adminName();

  /** The text strings to display to the user, keyed by locale. */
  @JsonProperty("localizedOptionText")
  public abstract LocalizedStrings optionText();

  /** The display ordering of this option - if empty, do not display. */
  @JsonProperty("displayOrder")
  public abstract OptionalLong displayOrder();

  /** Whether to display the answer option to the applicant. */
  @JsonProperty("displayInAnswerOptions")
  public abstract Optional<Boolean> displayInAnswerOptions();

  /**
   * The score this option contributes to a submitted application, for programs that use scoring.
   * Never shown to applicants. Absent means unscored, which is distinct from a score of 0.
   */
  @JsonProperty("score")
  public abstract Optional<Integer> score();

  /**
   * Create a QuestionOption, used for JSON mapping to account for the legacy `optionText`.
   *
   * <p>Legacy QuestionOptions from before early May 2021 will not have `localizedOptionText`.
   */
  @JsonCreator
  public static QuestionOption jsonCreator(
      @JsonProperty("id") long id,
      @JsonProperty(value = "displayOrder", defaultValue = "-1L") long displayOrder,
      @JsonProperty("adminName") String adminName,
      @JsonProperty("localizedOptionText") LocalizedStrings localizedOptionText,
      @JsonProperty("optionText") ImmutableMap<Locale, String> legacyOptionText,
      @JsonProperty("displayInAnswerOptions") Optional<Boolean> displayInAnswerOptions,
      @JsonProperty("score") Optional<Integer> score) {
    if (displayOrder == -1) {
      displayOrder = id;
    }
    if (localizedOptionText != null) {
      return QuestionOption.create(
          id, displayOrder, adminName, localizedOptionText, displayInAnswerOptions, score);
    }
    return QuestionOption.create(
            id, displayOrder, adminName, LocalizedStrings.create(legacyOptionText))
        .toBuilder()
        .setScore(score)
        .build();
  }

  /**
   * Create a {@link QuestionOption}.
   *
   * @param id the option id
   * @param displayOrder the option display
   * @param adminName the option's immutable admin name, exposed via the API
   * @param optionText the option's user-facing text
   * @return the {@link QuestionOption}
   */
  public static QuestionOption create(
      long id, long displayOrder, String adminName, LocalizedStrings optionText) {
    return QuestionOption.builder()
        .setId(id)
        .setAdminName(adminName)
        .setOptionText(optionText)
        .setDisplayOrder(OptionalLong.of(displayOrder))
        .build();
  }

  /**
   * Create a {@link QuestionOption}.
   *
   * @param id the option id
   * @param adminName the option's immutable admin name, exposed via the API
   * @param optionText the option's user-facing text
   * @return the {@link QuestionOption}
   */
  public static QuestionOption create(long id, String adminName, LocalizedStrings optionText) {
    return QuestionOption.builder()
        .setId(id)
        .setAdminName(adminName)
        .setOptionText(optionText)
        .setDisplayOrder(OptionalLong.empty())
        .build();
  }

  /**
   * Create a {@link QuestionOption}.
   *
   * @param id the option id
   * @param displayOrder the option display
   * @param adminName the option's immutable admin name, exposed via the API
   * @param optionText the option's user-facing text
   * @param displayInAnswerOptions whether to show the option to the applicant
   * @return the {@link QuestionOption}
   */
  public static QuestionOption create(
      long id,
      long displayOrder,
      String adminName,
      LocalizedStrings optionText,
      Optional<Boolean> displayInAnswerOptions) {
    return QuestionOption.builder()
        .setId(id)
        .setAdminName(adminName)
        .setOptionText(optionText)
        .setDisplayOrder(OptionalLong.of(displayOrder))
        .setDisplayInAnswerOptions(displayInAnswerOptions)
        .build();
  }

  /**
   * Create a {@link QuestionOption}.
   *
   * @param id the option id
   * @param displayOrder the option display
   * @param adminName the option's immutable admin name, exposed via the API
   * @param optionText the option's user-facing text
   * @param displayInAnswerOptions whether to show the option to the applicant
   * @param score the score the option contributes to a scored application, absent if unscored
   * @return the {@link QuestionOption}
   */
  public static QuestionOption create(
      long id,
      long displayOrder,
      String adminName,
      LocalizedStrings optionText,
      Optional<Boolean> displayInAnswerOptions,
      Optional<Integer> score) {
    return QuestionOption.builder()
        .setId(id)
        .setAdminName(adminName)
        .setOptionText(optionText)
        .setDisplayOrder(OptionalLong.of(displayOrder))
        .setDisplayInAnswerOptions(displayInAnswerOptions)
        .setScore(score)
        .build();
  }

  public LocalizedQuestionOption localize(Locale locale) {
    if (!optionText().hasTranslationFor(locale)) {
      throw new RuntimeException(
          String.format("Locale %s not supported for question option %s", locale, this));
    }

    return localizeOrDefault(locale);
  }

  /**
   * Localize this question option for the given locale. If we cannot localize, use the default
   * locale.
   */
  public LocalizedQuestionOption localizeOrDefault(Locale locale) {
    try {
      String localizedText = optionText().get(locale);
      return LocalizedQuestionOption.create(
          id(),
          displayOrder().orElse(id()),
          adminName(),
          localizedText,
          displayInAnswerOptions(),
          locale);
    } catch (TranslationNotFoundException e) {
      return LocalizedQuestionOption.create(
          id(),
          displayOrder().orElse(id()),
          adminName(),
          optionText().getDefault(),
          displayInAnswerOptions(),
          LocalizedStrings.DEFAULT_LOCALE);
    }
  }

  public abstract Builder toBuilder();

  public static QuestionOption.Builder builder() {
    return new AutoValue_QuestionOption.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    @JsonProperty("id")
    public abstract Builder setId(long id);

    @JsonProperty("adminName")
    public abstract Builder setAdminName(String adminName);

    @JsonProperty("localizedOptionText")
    public abstract Builder setOptionText(LocalizedStrings optionText);

    @JsonProperty("displayOrder")
    public abstract Builder setDisplayOrder(OptionalLong displayOrder);

    @JsonProperty("displayInAnswerOptions")
    public abstract Builder setDisplayInAnswerOptions(Optional<Boolean> displayInAnswerOptions);

    @JsonProperty("score")
    public abstract Builder setScore(Optional<Integer> score);

    public Builder setScore(int score) {
      return setScore(Optional.of(score));
    }

    public abstract QuestionOption build();
  }
}
