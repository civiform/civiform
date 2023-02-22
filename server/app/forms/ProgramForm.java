package forms;

/** Form for updating name and description of a program. */
public final class ProgramForm {
  private String adminName;
  private String adminDescription;
  private String localizedDisplayName;
  private String localizedDisplayDescription;
  private String externalLink;
  private String displayMode;
  private Boolean isCommonIntakeForm;

  public ProgramForm() {
    adminName = "";
    adminDescription = "";
    localizedDisplayName = "";
    localizedDisplayDescription = "";
    externalLink = "";
    displayMode = "";
    isCommonIntakeForm = false;
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

  public String getExternalLink() {
    return externalLink;
  }

  public void setExternalLink(String externalLink) {
    this.externalLink = externalLink;
  }

  public String getDisplayMode() {
    return displayMode;
  }

  public void setDisplayMode(String displayMode) {
    this.displayMode = displayMode;
  }

  public String getLocalizedDisplayName() {
    return localizedDisplayName;
  }

  public String getLocalizedDisplayDescription() {
    return localizedDisplayDescription;
  }

  public void setLocalizedDisplayName(String localizedDisplayName) {
    this.localizedDisplayName = localizedDisplayName;
  }

  public void setLocalizedDisplayDescription(String localizedDisplayDescription) {
    this.localizedDisplayDescription = localizedDisplayDescription;
  }

  public Boolean getIsCommonIntakeForm() {
    return isCommonIntakeForm;
  }

  public void setIsCommonIntakeForm(Boolean isCommonIntakeForm) {
    this.isCommonIntakeForm = isCommonIntakeForm;
  }
}
