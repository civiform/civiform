package models;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import io.ebean.annotation.DbJsonB;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import javax.persistence.Entity;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import play.data.validation.Constraints;
import services.Path;
import services.question.InvalidQuestionTypeException;
import services.question.MultiOptionQuestionDefinition;
import services.question.QuestionDefinition;
import services.question.QuestionDefinitionBuilder;
import services.question.QuestionType;
import services.question.UnsupportedQuestionTypeException;

@Entity
@Table(name = "questions")
public class Question extends BaseModel {

  private QuestionDefinition questionDefinition;

  private @Constraints.Required long version;

  private @Constraints.Required String path;

  private @Constraints.Required String name;

  private Long repeaterId;

  private @Constraints.Required String description;

  private @Constraints.Required @DbJsonB ImmutableMap<Locale, String> questionText;

  private @Constraints.Required @DbJsonB ImmutableMap<Locale, String> questionHelpText;

  private @Constraints.Required String questionType;

  private @Constraints.Required @DbJsonB String validationPredicates;

  @Constraints.Required private LifecycleStage lifecycleStage;

  private @DbJsonB ImmutableListMultimap<Locale, String> questionOptions;

  public String getPath() {
    return path;
  }

  public Question(QuestionDefinition questionDefinition) {
    this.questionDefinition = checkNotNull(questionDefinition);
    setFieldsFromQuestionDefinition(questionDefinition);
  }

  public Question(QuestionDefinition questionDefinition, LifecycleStage lifecycleStage) {
    this(questionDefinition);
    this.lifecycleStage = lifecycleStage;
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
            .setVersion(version)
            .setName(name)
            .setPath(Path.create(path))
            .setRepeaterId(
                Optional.ofNullable(repeaterId)
                    .map(OptionalLong::of)
                    .orElseGet(OptionalLong::empty))
            .setDescription(description)
            .setLifecycleStage(lifecycleStage)
            .setQuestionText(questionText)
            .setQuestionHelpText(questionHelpText)
            .setQuestionType(QuestionType.valueOf(questionType))
            .setValidationPredicatesString(validationPredicates);

    if (QuestionType.of(questionType).isMultiOptionType()) {
      builder.setQuestionOptions(questionOptions);
    }

    this.questionDefinition = builder.build();
  }

  public QuestionDefinition getQuestionDefinition() {
    return checkNotNull(questionDefinition);
  }

  public LifecycleStage getLifecycleStage() {
    return this.getQuestionDefinition().getLifecycleStage();
  }

  public void setLifecycleStage(LifecycleStage lifecycleStage) {
    // A Question object is entirely determined by a QuestionDefinition, so Question objects are
    // usually
    // immutable since QuestionDefinitions are immutable.  This is not a true setter - it creates an
    // entirely
    // new QuestionDefinition from the existing one - but it's present here as a convenience method,
    // to save
    // on verbosity due to us doing this many times throughout the application.
    try {
      this.questionDefinition =
          new QuestionDefinitionBuilder(this.questionDefinition)
              .setLifecycleStage(lifecycleStage)
              .build();
    } catch (UnsupportedQuestionTypeException e) {
      // Throw as runtime exception because this should never happen - we are using an existing
      // question definition type.
      throw new RuntimeException(e);
    }
  }

  private void setFieldsFromQuestionDefinition(QuestionDefinition questionDefinition) {
    if (questionDefinition.isPersisted()) {
      id = questionDefinition.getId();
    }
    version = questionDefinition.getVersion();
    path = questionDefinition.getPath().path();
    repeaterId =
        Optional.of(questionDefinition.getRepeaterId())
            .filter(OptionalLong::isPresent)
            .map(OptionalLong::getAsLong)
            .orElse(null);
    name = questionDefinition.getName();
    description = questionDefinition.getDescription();
    questionText = questionDefinition.getQuestionText();
    questionHelpText = questionDefinition.getQuestionHelpText();
    questionType = questionDefinition.getQuestionType().toString();
    validationPredicates = questionDefinition.getValidationPredicatesAsString();
    lifecycleStage = questionDefinition.getLifecycleStage();

    if (questionDefinition.getQuestionType().isMultiOptionType()) {
      MultiOptionQuestionDefinition multiOption =
          (MultiOptionQuestionDefinition) questionDefinition;
      questionOptions = multiOption.getOptions();
    }
  }
}
