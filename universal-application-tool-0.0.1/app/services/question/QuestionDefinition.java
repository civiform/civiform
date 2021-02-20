package services.question;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;

/** Defines a single question. */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "serialization_type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = AddressQuestionDefinition.class, name = "address"),
  @JsonSubTypes.Type(value = NameQuestionDefinition.class, name = "name"),
  @JsonSubTypes.Type(value = TextQuestionDefinition.class, name = "text")
})
public abstract class QuestionDefinition {
  private final long id;
  private final long version;
  private final String name;
  private final String path;
  private final String description;
  private final ImmutableMap<Locale, String> questionText;
  private final Optional<ImmutableMap<Locale, String>> questionHelpText;

  @JsonCreator
  public QuestionDefinition(
      @JsonProperty("id") long id,
      @JsonProperty("version") long version,
      @JsonProperty("name") String name,
      @JsonProperty("path") String path,
      @JsonProperty("description") String description,
      @JsonProperty("questionText") ImmutableMap<Locale, String> questionText,
      @JsonProperty("questionHelpText") Optional<ImmutableMap<Locale, String>> questionHelpText) {
    this.id = checkNotNull(id);
    this.version = checkNotNull(version);
    this.name = checkNotNull(name);
    this.path = checkNotNull(path);
    this.description = checkNotNull(description);
    this.questionText = checkNotNull(questionText);
    this.questionHelpText = checkNotNull(questionHelpText);
  }

  /** Get the unique identifier for this question. */
  public long getId() {
    return this.id;
  }

  /** Get the system version this question is pinned to. */
  public long getVersion() {
    return this.version;
  }

  /**
   * Get the name of this question.
   *
   * <p>NOTE: This field will not be localized as it is for admin use only.
   */
  public String getName() {
    return this.name;
  }

  /** Get the full path of this question, in JSON notation. */
  public String getPath() {
    return this.path;
  }

  /**
   * Get a human-readable description for the data this question collects.
   *
   * <p>NOTE: This field will not be localized as it is for admin use only.
   */
  public String getDescription() {
    return this.description;
  }

  /** Get the question text for the given locale. */
  public String getQuestionText(Locale locale) throws TranslationNotFoundException {
    if (this.questionText.containsKey(locale)) {
      return this.questionText.get(locale);
    }
    throw new TranslationNotFoundException(this.getPath(), locale);
  }

  /** Get the question tests for all locales. This is used for serialization. */
  public ImmutableMap<Locale, String> getQuestionText() {
    return questionText;
  }

  /** Get the question help text for the given locale. */
  public String getQuestionHelpText(Locale locale) throws TranslationNotFoundException {
    if (!this.questionHelpText.isPresent()) {
      return "";
    }

    if (this.questionHelpText.get().containsKey(locale)) {
      return this.questionHelpText.get().get(locale);
    }

    throw new TranslationNotFoundException(this.getPath(), locale);
  }

  /** Get the question help text for all locales. This is used for serialization. */
  public Optional<ImmutableMap<Locale, String>> getQuestionHelpText() {
    return this.questionHelpText;
  }

  /** Get the type of this question. */
  @JsonIgnore
  public QuestionType getQuestionType() {
    return QuestionType.TEXT;
  }

  /** Get a map of scalars stored by this question definition. */
  @JsonIgnore
  public ImmutableMap<String, ScalarType> getScalars() {
    return ImmutableMap.of("text", ScalarType.STRING);
  }

  /** Get a map of scalars stored by this question definition. */
  @JsonIgnore
  public ImmutableMap<String, ScalarType> getFullyQualifiedScalars() {
    ImmutableMap<String, ScalarType> scalars = this.getScalars();
    ImmutableMap.Builder<String, ScalarType> ret = new ImmutableMap.Builder<String, ScalarType>();
    scalars.entrySet().stream()
        .forEach(e -> ret.put(this.getPath() + "." + e.getKey(), e.getValue()));
    return ret.build();
  }

  @JsonIgnore
  public Optional<ScalarType> getScalarType(String key) {
    return Optional.ofNullable(this.getScalars().get(key));
  }
}
