package models;

import com.google.common.annotations.VisibleForTesting;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.WhenCreated;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import play.data.validation.Constraints;
import services.applicant.ApplicantData;

/**
 * An EBean mapped class that records the submission of a single applicant to a single program.
 *
 * <p>A draft {@code Application} is created when an {@code Applicant} submits at least one block
 * for a {@code Program}. The application transitions to active when submitted from the review page,
 * and obsolete when a second application is submitted for the same program by the same applicant.
 * The application's {@code ApplicantData} is immutable when it is not in draft {@code
 * LifecycleStage}. This ensures that the data seen by the program administrator is consistent with
 * what was actually submitted and not subject to change if the resident or trusted intermediary
 * changes answers to shared questions after submitting.
 */
@Entity
@Table(name = "applications")
public class ApplicationModel extends BaseModel {

  @ManyToOne private ApplicantModel applicant;

  @ManyToOne private ProgramModel program;

  // Note: there is not an index on createTime currently as we don't filter on
  // it and expect the number of results to be small.
  @OneToMany(mappedBy = "application")
  @OrderBy("createTime desc")
  private List<ApplicationEventModel> applicationEvents;

  @Constraints.Required private LifecycleStage lifecycleStage;

  @WhenCreated private Instant createTime;

  @Constraints.Required @DbJson private String object;

  private Instant submitTime;
  private String preferredLocale;
  private String submitterEmail;
  private String latestStatus;
  private boolean isAdmin;
  private String latestNote;

  public ApplicationModel(
      ApplicantModel applicant, ProgramModel program, LifecycleStage lifecycleStage) {
    this.applicant = applicant;
    this.program = program;
    this.object = "{}";
    this.lifecycleStage = lifecycleStage;
    this.isAdmin =
        applicant.getAccount().getGlobalAdmin()
            || !applicant.getAccount().getAdministeredProgramNames().isEmpty();
  }

  public static ApplicationModel create(
      ApplicantModel applicant, ProgramModel program, LifecycleStage lifecycleStage) {
    ApplicationModel application = new ApplicationModel(applicant, program, lifecycleStage);
    application.save();
    return application;
  }

  public Optional<String> getSubmitterEmail() {
    return Optional.ofNullable(this.submitterEmail);
  }

  /**
   * Set the email address of the TI that submitted the application. TODO(#5325): Rename this field
   * to reduce confusion.
   *
   * @param submitterEmail The email address of the TI that submitted the application.
   * @return this Application
   */
  public ApplicationModel setSubmitterEmail(String submitterEmail) {
    this.submitterEmail = submitterEmail;
    return this;
  }

  public ApplicantModel getApplicant() {
    return this.applicant;
  }

  public ProgramModel getProgram() {
    return this.program;
  }

  public ApplicantData getApplicantData() {
    if (this.preferredLocale == null || this.preferredLocale.isEmpty()) {
      // Default to English.
      return new ApplicantData(this.object);
    }

    return new ApplicantData(Optional.of(Locale.forLanguageTag(preferredLocale)), this.object);
  }

  public ApplicationModel setApplicantData(ApplicantData data) {
    this.preferredLocale =
        data.hasPreferredLocale() ? data.preferredLocale().toLanguageTag() : null;
    this.object = data.asJsonString();
    return this;
  }

  public List<ApplicationEventModel> getApplicationEvents() {
    return applicationEvents;
  }

  public LifecycleStage getLifecycleStage() {
    return this.lifecycleStage;
  }

  public Instant getSubmitTime() {
    return this.submitTime;
  }

  public Instant getCreateTime() {
    return this.createTime;
  }

  public boolean getIsAdmin() {
    return this.isAdmin;
  }

  public ApplicationModel setLifecycleStage(LifecycleStage stage) {
    this.lifecycleStage = stage;
    return this;
  }

  public ApplicationModel setSubmitTimeToNow() {
    this.submitTime = Instant.now();
    return this;
  }

  @VisibleForTesting
  public ApplicationModel setSubmitTimeForTest(Instant v) {
    this.submitTime = v;
    return this;
  }

  @VisibleForTesting
  public ApplicationModel setCreateTimeForTest(Instant v) {
    this.createTime = v;
    return this;
  }

  /**
   * Returns the latest application status text value associated with the application.
   *
   * <p>This value is updated by ApplicationEventRepository which set the status to the latest
   * ApplicationEventDetails event for the application with a type of "status_change". Attempts
   * should not be made to update the status manually outside the repository class.
   *
   * <p>For more information, please check the @insertStatusEvent() instead.
   */
  public Optional<String> getLatestStatus() {
    return Optional.ofNullable(latestStatus);
  }

  /**
   * Returns the latest application note value associated with the application.
   *
   * <p>This value is updated by Program Admins who want to add more details to the application.
   */
  public Optional<String> getLatestNote() {
    return Optional.ofNullable(latestNote);
  }

  /**
   * This is visible only for tests to manipulate the latest status directly in order to ensure that
   * updates to it are overridden by the application code.
   */
  @VisibleForTesting
  void setLatestStatusForTest(String latestStatus) {
    this.latestStatus = latestStatus;
  }

  /**
   * Point the application to a new {@link ProgramModel}.
   *
   * <p>This typically doesn't need to be done aside from when we want to migrate an application to
   * a newer version of a program.
   *
   * @param program {@link ProgramModel}
   */
  public void setProgram(ProgramModel program) {
    this.program = program;
  }
}
