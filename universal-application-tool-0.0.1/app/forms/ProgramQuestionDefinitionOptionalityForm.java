package forms;

/** Form for updating whether a question is optional in a program. */
public class ProgramQuestionDefinitionOptionalityForm {
  private Boolean optional;

  public ProgramQuestionDefinitionOptionalityForm() {
    optional = false;
  }

  public Boolean getOptional() {
    return optional;
  }

  public void setOptional(Boolean optional) {
    this.optional = optional;
  }
}
