package services.question;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Locale;

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

  /** Required - numerical ID for this question. */
  public String id() {
    return this.id;
  }

  /** Required - the version this question is pinned to. */
  public String version() {
    return this.version;
  }

  /** Required - the name of this question. */
  public String name() {
    return this.name;
  }

  /** Required - the path of this question's parent, in JSON notation. */
  public String path() {
    return this.path;
  }

  /** Required - a human-readable description for the data this question collects. */
  public String description() {
    return this.description;
  }

  /** Localized question text. Keys are ISO language codes, and values are the translated text. */
  public ImmutableMap<Locale, String> questionText() {
    return this.questionText;
  }

  /** Localized question help text. */
  public ImmutableMap<Locale, String> questionHelpText() {
    return this.questionHelpText;
  }

  /** Required - the type of widget for this question. */
  public QuestionType questionType() {
    return QuestionType.TEXT;
  }

  /** Set of admin-visible tags for sorting and searching (ex: "General", "Financial", etc.) */
  public ImmutableSet<String> tags() {
    return this.tags;
  }

  /** The list of scalars stored by this question definition. */
  public ImmutableSet<String> getScalars() {
    return ImmutableSet.of("text");
  }
}
