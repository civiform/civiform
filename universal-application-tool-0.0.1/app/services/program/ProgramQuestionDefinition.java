package services.program;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Inject;
import models.Question;
import services.question.QuestionDefinition;
import services.question.QuestionService;

public class ProgramQuestionDefinition {
  private long id;
  private QuestionDefinition questionDefinition;
  private String config;
    private QuestionService service;

    public ProgramQuestionDefinition(QuestionDefinition questionDefinition, String config) {
      this.id = questionDefinition.getId();
      this.questionDefinition = questionDefinition;
      this.config = config;
  }

  @Inject
  public ProgramQuestionDefinition(long id, String config, QuestionService service) {
      this.id = id;
      this.questionDefinition = service.getReadOnlyQuestionService()
      this.config = config;
      this.service = service;
  }

  @JsonProperty("id")
  public long id() {
    return this.id;
  }

  public void setId(long id) {
    this.id = id;
  }

  @JsonProperty("config")
  public String config() {
    return this.config;
  }
  ;

  public void setConfig(String config) {
    this.config = config;
  }

  public QuestionDefinition questionDefinition() {
    return this.questionDefinition;
  }
  ;

  public void setQuestionDefinition(QuestionDefinition questionDefinition) {
    this.questionDefinition = questionDefinition;
  }
}
