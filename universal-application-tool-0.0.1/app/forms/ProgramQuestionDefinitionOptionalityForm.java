package forms;

public class ProgramQuestionDefinitionOptionalityForm {
  private Long questionDefinitionId;
  private Boolean optional;

  public ProgramQuestionDefinitionOptionalityForm() {
    questionDefinitionId = 0L;
    optional = true;
  }

  public Long getQuestionDefinitionId() {
    return questionDefinitionId;
  }

  public void setQuestionDefinitionId(Long questionDefinitionId) {
    this.questionDefinitionId = questionDefinitionId;
  }

  public Boolean getOptional() {
    return optional;
  }

  public void setOptional(Boolean optional) {
    this.optional = optional;
  }
}
