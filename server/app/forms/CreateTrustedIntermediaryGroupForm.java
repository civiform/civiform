package forms;

/** Form for adding a new trusted intermediary group. */
public class CreateTrustedIntermediaryGroupForm {
  private String name;
  private String description;

  public CreateTrustedIntermediaryGroupForm(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public CreateTrustedIntermediaryGroupForm() {
    name = "";
    description = "";
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
