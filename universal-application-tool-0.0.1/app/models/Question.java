package models;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import io.ebean.annotation.DbJsonB;
import play.data.validation.Constraints;
import services.question.QuestionDefinition;

@Entity
@Table(name = "questions")
public class Question extends BaseModel {

  private QuestionDefinition questionDefinition;

  private @Constraints.Required long version;

  private @Constraints.Required String path;

  private @Constraints.Required String name;

  private @Constraints.Required String description;

  private @Constraints.Required @DbJsonB ImmutableMap<Locale, String> questionText;

  private ImmutableMap<Locale, String> questionHelpText;

  public Question(QuestionDefinition questionDefinition) {
    this.questionDefinition = questionDefinition;
    this.version = questionDefinition.getVersion();
    this.path = questionDefinition.getPath();
    this.name = questionDefinition.getName();
    this.description = questionDefinition.getDescription();
    this.questionText = questionDefinition.getQuestionText();
    this.questionHelpText = questionDefinition.getQuestionHelpText().orElse(null);
  }

  /** Populates column values from {@link QuestionDefinition}. */
  @PreUpdate
  public void persistChangesToQuestionDefinition() {
    this.id = questionDefinition.getId();
    this.version = questionDefinition.getVersion();
    this.path = questionDefinition.getPath();
    this.name = questionDefinition.getName();
    this.description = questionDefinition.getDescription();
    this.questionText = questionDefinition.getQuestionText();
    this.questionHelpText = questionDefinition.getQuestionHelpText().orElse(null);
  }

  /** Populates {@link QuestionDefinition} from column values. */
  @PostLoad
  @PostPersist
  @PostUpdate
  public void loadQuestionDefinition() {
    this.questionDefinition = new QuestionDefinition(this.id, this.version, this.name, this.path, this.description, this.questionText, Optional.ofNullable(this.questionHelpText));
  }

  public QuestionDefinition getQuestionDefinition() {
    return checkNotNull(this.questionDefinition);
  }
}
