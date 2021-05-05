package models;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import io.ebean.annotation.DbJsonB;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import play.data.validation.Constraints;
import services.Path;
import services.question.QuestionOption;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

@Entity
@Table(name = "questions")
public class Question extends BaseModel {

  private QuestionDefinition questionDefinition;

  private @Constraints.Required String path;

  /** Different versions of the same question are linked by their immutable name. */
  private @Constraints.Required String name;

  private Long enumeratorId;

  private @Constraints.Required String description;

  private @Constraints.Required @DbJsonB ImmutableMap<Locale, String> questionText;

  private @Constraints.Required @DbJsonB ImmutableMap<Locale, String> questionHelpText;

  private @Constraints.Required String questionType;

  private @Constraints.Required @DbJsonB String validationPredicates;

  // questionOptions is the legacy storage column for multi-option questions.
  // A few questions created early on in April 2021 may use this, but all
  // other multi-option questions should not. In practice one can assume only
  // a single locale is present for questions that have values stored in this
  // column.
  private @DbJsonB ImmutableListMultimap<Locale, String> questionOptions;

  // questionOptionsWithLocales is the current storage column for multi-option questions.
  private @DbJsonB ImmutableList<QuestionOption> questionOptionsWithLocales;

  @ManyToMany
  @JoinTable(name = "versions_questions")
  private List<Version> versions;

  public ImmutableList<Version> getVersions() {
    return ImmutableList.copyOf(versions);
  }

  public void addVersion(Version version) {
    this.versions.add(version);
  }

  public Question(QuestionDefinition questionDefinition) {
    this.questionDefinition = checkNotNull(questionDefinition);
    setFieldsFromQuestionDefinition(questionDefinition);
  }

  /** Populates column values from {@link QuestionDefinition}. */
  @PreUpdate
  @PrePersist
  public void persistChangesToQuestionDefinition() {
    setFieldsFromQuestionDefinition(questionDefinition);
  }

  /** Populates {@link QuestionDefinition} from column values. */
  @PostLoad
  @PostPersist
  @PostUpdate
  public void loadQuestionDefinition()
      throws UnsupportedQuestionTypeException, InvalidQuestionTypeException {
    QuestionDefinitionBuilder builder =
        new QuestionDefinitionBuilder()
            .setId(id)
            .setName(name)
            .setPath(Path.create(path))
            .setEnumeratorId(Optional.ofNullable(enumeratorId))
            .setDescription(description)
            .setQuestionText(questionText)
            .setQuestionHelpText(questionHelpText)
            .setQuestionType(QuestionType.valueOf(questionType))
            .setValidationPredicatesString(validationPredicates);

    setQuestionOptions(builder);

    this.questionDefinition = builder.build();
  }

  private void setQuestionOptions(QuestionDefinitionBuilder builder)
      throws InvalidQuestionTypeException {
    if (!QuestionType.of(questionType).isMultiOptionType()) {
      return;
    }

    // The majority of multi option questions should have `questionOptionsWithLocales` and not
    // `questionOptions`.
    // `questionOptions` is a legacy implementation that only supported a single locale.
    if (questionOptionsWithLocales != null) {
      builder.setQuestionOptions(questionOptionsWithLocales);
      return;
    }

    // If the multi option question does have questionOptions, we can assume there is only one
    // locale and convert the strings to QuestionOption instances each with a single locale.
    Locale firstKey = questionOptions.keySet().stream().iterator().next();

    ImmutableList<QuestionOption> options =
        Streams.mapWithIndex(
                questionOptions.get(firstKey).stream(),
                (optionText, i) ->
                    QuestionOption.create(Long.valueOf(i), ImmutableMap.of(firstKey, optionText)))
            .collect(toImmutableList());

    builder.setQuestionOptions(options);
  }

  public QuestionDefinition getQuestionDefinition() {
    return checkNotNull(questionDefinition);
  }

  private void setFieldsFromQuestionDefinition(QuestionDefinition questionDefinition) {
    if (questionDefinition.isPersisted()) {
      id = questionDefinition.getId();
    }

    // TODO(https://github.com/seattle-uat/civiform/issues/673): delete this when questions don't
    //  need paths
    path = questionDefinition.getPath().path();

    enumeratorId = questionDefinition.getEnumeratorId().orElse(null);
    name = questionDefinition.getName();
    description = questionDefinition.getDescription();
    questionText = questionDefinition.getQuestionText();
    questionHelpText = questionDefinition.getQuestionHelpText();
    questionType = questionDefinition.getQuestionType().toString();
    validationPredicates = questionDefinition.getValidationPredicatesAsString();

    if (questionDefinition.getQuestionType().isMultiOptionType()) {
      MultiOptionQuestionDefinition multiOption =
          (MultiOptionQuestionDefinition) questionDefinition;
      questionOptionsWithLocales = multiOption.getOptions();
    }
  }
}
