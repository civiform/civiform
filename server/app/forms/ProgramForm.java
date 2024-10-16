package forms;

import com.google.common.collect.ImmutableList;
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

  private String applyStep1Title;
  private String applyStep1Description;
  private String applyStep2Title;
  private String applyStep2Description;
  private String applyStep3Title;
  private String applyStep3Description;
  private String applyStep4Title;
  private String applyStep4Description;
  private String applyStep5Title;
  private String applyStep5Description;

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
    applyStep1Title = "";
    applyStep1Description = "";
    applyStep2Title = "";
    applyStep2Description = "";
    applyStep3Title = "";
    applyStep3Description = "";
    applyStep4Title = "";
    applyStep4Description = "";
    applyStep5Title = "";
    applyStep5Description = "";
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

  public ImmutableList<String> getAllApplicationStepTitles() {
    return ImmutableList.of(
        getApplyStep1Title(),
        getApplyStep2Title(),
        getApplyStep3Title(),
        getApplyStep4Title(),
        getApplyStep5Title());
  }

  public ImmutableList<String> getAllApplicationStepDescriptions() {
    return ImmutableList.of(
        getApplyStep1Description(),
        getApplyStep2Description(),
        getApplyStep3Description(),
        getApplyStep4Description(),
        getApplyStep5Description());
  }

  public String getApplyStep1Title() {
    return applyStep1Title;
  }

  public String getApplyStep1Description() {
    return applyStep1Description;
  }

  public String getApplyStep2Title() {
    return applyStep2Title;
  }

  public String getApplyStep2Description() {
    return applyStep2Description;
  }

  public String getApplyStep3Title() {
    return applyStep3Title;
  }

  public String getApplyStep3Description() {
    return applyStep3Description;
  }

  public String getApplyStep4Title() {
    return applyStep4Title;
  }

  public String getApplyStep4Description() {
    return applyStep4Description;
  }

  public String getApplyStep5Title() {
    return applyStep5Title;
  }

  public String getApplyStep5Description() {
    return applyStep5Description;
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

  public void setApplyStep1Title(String applyStep1Title) {
    this.applyStep1Title = applyStep1Title;
  }

  public void setApplyStep1Description(String applyStep1Description) {
    this.applyStep1Description = applyStep1Description;
  }

  public void setApplyStep2Title(String applyStep2Title) {
    this.applyStep2Title = applyStep2Title;
  }

  public void setApplyStep2Description(String applyStep2Description) {
    this.applyStep2Description = applyStep2Description;
  }

  public void setApplyStep3Title(String applyStep3Title) {
    this.applyStep3Title = applyStep3Title;
  }

  public void setApplyStep3Description(String applyStep3Description) {
    this.applyStep3Description = applyStep3Description;
  }

  public void setApplyStep4Title(String applyStep4Title) {
    this.applyStep4Title = applyStep4Title;
  }

  public void setApplyStep4Description(String applyStep4Description) {
    this.applyStep4Description = applyStep4Description;
  }

  public void setApplyStep5Title(String applyStep5Title) {
    this.applyStep5Title = applyStep5Title;
  }

  public void setApplyStep5Description(String applyStep5Description) {
    this.applyStep5Description = applyStep5Description;
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
