package forms;

/** Form for updating the name and description of a screen (block) of a program. */
public final class BlockForm {
  private String name;
  private String description;
  private boolean isRepeated;

  public BlockForm(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public BlockForm(String name, String description, boolean isRepeated) {
    this.name = name;
    this.description = description;
    this.isRepeated = isRepeated;
  }

  public BlockForm() {
    name = "";
    description = "";
    isRepeated = false;
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

  public boolean isRepeated() {
    return this.isRepeated;
  }

  public void setIsRepeated(boolean isRepeated) {
    this.isRepeated = isRepeated;
  }
}
