package services.question;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Locale;

/** Defines a single question. */
public class QuestionDefinition {
  private final String id;
  private final String version;
  private final String name;
  private final String path;
  private final String description;
  private final ImmutableMap<Locale, String> questionText;
  private final ImmutableMap<Locale, String> questionHelpText;
  private final ImmutableSet<String> tags;

  public QuestionDefinition(
      String id,
      String version,
      String name,
      String path,
      String description,
      ImmutableMap<Locale, String> questionText,
      ImmutableMap<Locale, String> questionHelpText,
      ImmutableSet<String> tags) {
    this.id = id;
    this.version = version;
    this.name = name;
    this.path = path;
    this.description = description;
    this.questionText = questionText;
    this.questionHelpText = questionHelpText;
    this.tags = tags;
  }

  /** Get the unique identifier for this question. */
  public String getId() {
    return this.id;
  }

  /** Get the system version this question is pinned to. */
  public String getVersion() {
    return this.version;
  }

  /** Get the name of this question. */
  public String getName() {
    return this.name;
  }

  /** Get the path of this question's parent, in JSON notation. */
  public String getPath() {
    return this.path;
  }

  /** Get a human-readable description for the data this question collects. */
  public String getDescription() {
    return this.description;
  }

  /** Localized question text. Keys are ISO language codes, and values are the translated text. */
  public ImmutableMap<Locale, String> getQuestionText() {
    return this.questionText;
  }

  /**
   * Localized question help text. Keys are ISO language codes, and values are the translated text.
   */
  public ImmutableMap<Locale, String> getQuestionHelpText() {
    return this.questionHelpText;
  }

  /**
   * Get a set of admin-visible tags for sorting and searching (ex: "General", "Financial", etc.)
   */
  public ImmutableSet<String> getTags() {
    return this.tags;
  }

  /** Get the type of this question. */
  public QuestionType getQuestionType() {
    return QuestionType.TEXT;
  }

  /** Get a set of scalars stored by this question definition. */
  public ImmutableSet<String> getScalars() {
    return ImmutableSet.of("text");
  }
}
