package forms;

import java.util.ArrayList;
import java.util.List;

/** Form for updating name and description of a program. */
public final class ProgramForm {
  private String adminName;
  private String adminDescription;
  private String localizedDisplayName;
  private String localizedDisplayDescription;
  private String localizedShortDescription;
  private String localizedConfirmationMessage;
  private String externalLink;
  private String displayMode;
  private List<String> notificationPreferences;
  private Boolean isCommonIntakeForm;

  // Represents whether or not the user has confirmed that they want to change which program is
  // marked as the common intake form.
  private Boolean confirmedChangeCommonIntakeForm;
  private Boolean eligibilityIsGating;
  private List<Long> tiGroups;
  private List<Long> categories;

  public ProgramForm() {
    adminName = "";
    adminDescription = "";
    localizedDisplayName = "";
    localizedDisplayDescription = "";
    localizedShortDescription = "";
    localizedConfirmationMessage = "";
    externalLink = "";
    displayMode = "";
    notificationPreferences = new ArrayList<>();
    isCommonIntakeForm = false;
    confirmedChangeCommonIntakeForm = false;
    eligibilityIsGating = true;
    tiGroups = new ArrayList<>();
    categories = new ArrayList<>();
  }

  public void setTiGroups(List<Long> tiGroups) {
    this.tiGroups = tiGroups;
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

  public List<String> getNotificationPreferences() {
    return notificationPreferences;
  }

  public void setNotificationPreferences(List<String> notificationPreferences) {
    this.notificationPreferences = notificationPreferences;
  }

  public String getLocalizedDisplayName() {
    return localizedDisplayName;
  }

  public String getLocalizedDisplayDescription() {
    return localizedDisplayDescription;
  }

  public String getLocalizedShortDescription() {
    return localizedShortDescription;
  }

  public String getLocalizedConfirmationMessage() {
    return localizedConfirmationMessage;
  }

  public void setLocalizedDisplayName(String localizedDisplayName) {
    this.localizedDisplayName = localizedDisplayName;
  }

  public void setLocalizedDisplayDescription(String localizedDisplayDescription) {
    this.localizedDisplayDescription = localizedDisplayDescription;
  }

  public void setLocalizedShortDescription(String localizedShortDescription) {
    this.localizedShortDescription = localizedShortDescription;
  }

  public Boolean getIsCommonIntakeForm() {
    return isCommonIntakeForm;
  }

  public void setIsCommonIntakeForm(Boolean isCommonIntakeForm) {
    this.isCommonIntakeForm = isCommonIntakeForm;
  }

  public Boolean getConfirmedChangeCommonIntakeForm() {
    return confirmedChangeCommonIntakeForm;
  }

  public void setConfirmedChangeCommonIntakeForm(Boolean confirmedChangeCommonIntakeForm) {
    this.confirmedChangeCommonIntakeForm = confirmedChangeCommonIntakeForm;
  }

  public boolean getEligibilityIsGating() {
    return eligibilityIsGating;
  }

  public void setEligibilityIsGating(boolean eligibilityIsGating) {
    this.eligibilityIsGating = eligibilityIsGating;
  }

  public void setLocalizedConfirmationMessage(String localizedConfirmationMessage) {
    this.localizedConfirmationMessage = localizedConfirmationMessage;
  }

  public List<Long> getTiGroups() {
    return this.tiGroups;
  }

  public List<Long> getCategories() {
    return this.categories;
  }
}
