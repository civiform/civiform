package forms;

/** Form for updating a screen (block) of a program. */
public class BlockForm {
  private String name;
  private String description;

  public BlockForm(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public BlockForm() {
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
