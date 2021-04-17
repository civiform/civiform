package forms;

public class ProgramForm {
  private String adminName;
  private String adminDescription;

  // TODO: Support multiple locales
  private String localizedName;
  private String localizedDescription;

  public ProgramForm() {
    adminName = "";
    adminDescription = "";
    localizedName = "";
    localizedDescription = "";
  }

  public String getAdminName() {
    return adminName;
  }

  public void setAdminName(String adminName) {
    this.adminName = adminName;
  }

  public String getAdminDescription() {
    return adminDescription;
  }

  public void setAdminDescription(String adminDescription) {
    this.adminDescription = adminDescription;
  }

  public String getLocalizedName() {
    return localizedName;
  }

  public String getLocalizedDescription() {
    return localizedDescription;
  }

  public void setLocalizedName(String localizedName) {
    this.localizedName = localizedName;
  }

  public void setLocalizedDescription(String localizedDescription) {
    this.localizedDescription = localizedDescription;
  }
}
