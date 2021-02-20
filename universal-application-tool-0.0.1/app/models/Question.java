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
    version = questionDefinition.getVersion();
    path = questionDefinition.getPath();
    name = questionDefinition.getName();
    description = questionDefinition.getDescription();
    questionText = questionDefinition.getQuestionText();
    questionHelpText = questionDefinition.getQuestionHelpText().orElse(null);
    questionType = questionDefinition.getQuestionType().toString();
  }

  /** Populates column values from {@link QuestionDefinition}. */
  @PreUpdate
  @PrePersist
  public void persistChangesToQuestionDefinition() {
    id = questionDefinition.getId();
    version = questionDefinition.getVersion();
    path = questionDefinition.getPath();
    name = questionDefinition.getName();
    description = questionDefinition.getDescription();
    questionText = questionDefinition.getQuestionText();
    questionHelpText = questionDefinition.getQuestionHelpText().orElse(null);
    questionType = questionDefinition.getQuestionType().toString();
  }

  /** Populates {@link QuestionDefinition} from column values. */
  @PostLoad
  @PostPersist
  @PostUpdate
  public void loadQuestionDefinition() {
    this.questionDefinition =
        new QuestionDefinitionBuilder()
            .setId(id)
            .setVersion(version)
            .setName(name)
            .setPath(path)
            .setDescription(description)
            .setQuestionText(questionText)
            .setQuestionHelpText(Optional.ofNullable(questionHelpText))
            .setQuestionType(QuestionType.valueOf(questionType))
            .build();
  }

  public QuestionDefinition getQuestionDefinition() {
    return checkNotNull(questionDefinition);
  }
}
