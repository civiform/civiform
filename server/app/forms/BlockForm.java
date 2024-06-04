package forms;

/** Form for updating the name and description of a screen (block) of a program. */
public final class BlockForm {
  private String name;
  private String description;
  private String localizedName;
  private String localizedDescription;

  public BlockForm(
      String name, String description, String localizedName, String localizedDescription) {
    this.name = name;
    this.description = description;
    this.localizedName = localizedName;
    this.localizedDescription = localizedDescription;
  }

  public BlockForm(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public BlockForm() {
    name = "";
    description = "";
    localizedName = "";
    localizedDescription = "";
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

  public String getLocalizedName() {
    return localizedName;
  }

  public void setLocalizedName(String localizedName) {
    this.localizedName = localizedName;
  }

  public String getLocalizedDescription() {
    return localizedDescription;
  }

  public void setLocalizedDescription(String localizedDescription) {
    this.localizedDescription = localizedDescription;
  }
}
