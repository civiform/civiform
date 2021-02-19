package models;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import io.ebean.annotation.DbJsonB;
import java.util.Locale;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import play.data.validation.Constraints;
import services.question.QuestionDefinition;
import services.question.QuestionDefinitionBuilder;
import services.question.QuestionType;

@Entity
@Table(name = "questions")
public class Question extends BaseModel {

  private QuestionDefinition questionDefinition;

  private @Constraints.Required long version;

  private @Constraints.Required String path;

  private @Constraints.Required String name;

  private @Constraints.Required String description;

  private @Constraints.Required @DbJsonB ImmutableMap<Locale, String> questionText;

  private @DbJsonB ImmutableMap<Locale, String> questionHelpText;

  private @Constraints.Required String questionType;

  public Question(QuestionDefinition questionDefinition) {
    this.questionDefinition = checkNotNull(questionDefinition);
    this.version = questionDefinition.getVersion();
    this.path = questionDefinition.getPath();
    this.name = questionDefinition.getName();
    this.description = questionDefinition.getDescription();
    this.questionText = questionDefinition.getQuestionText();
    this.questionHelpText = questionDefinition.getQuestionHelpText().orElse(null);
    this.questionType = questionDefinition.getQuestionType().toString();
  }

  /** Populates column values from {@link QuestionDefinition}. */
  @PreUpdate
  @PrePersist
  public void persistChangesToQuestionDefinition() {
    this.id = questionDefinition.getId();
    this.version = questionDefinition.getVersion();
    this.path = questionDefinition.getPath();
    this.name = questionDefinition.getName();
    this.description = questionDefinition.getDescription();
    this.questionText = questionDefinition.getQuestionText();
    this.questionHelpText = questionDefinition.getQuestionHelpText().orElse(null);
    this.questionType = questionDefinition.getQuestionType().toString();
  }

  /** Populates {@link QuestionDefinition} from column values. */
  @PostLoad
  @PostPersist
  @PostUpdate
  public void loadQuestionDefinition() {
    this.questionDefinition =
        new QuestionDefinitionBuilder()
            .setId(this.id)
            .setVersion(this.version)
            .setName(this.name)
            .setPath(this.path)
            .setDescription(this.description)
            .setQuestionText(this.questionText)
            .setQuestionHelpText(Optional.ofNullable(this.questionHelpText))
            .setQuestionType(QuestionType.valueOf(questionType))
            .build();
  }

  public QuestionDefinition getQuestionDefinition() {
    return checkNotNull(this.questionDefinition);
  }
}
