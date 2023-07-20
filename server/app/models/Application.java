package models;

import com.google.common.annotations.VisibleForTesting;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.WhenCreated;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
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
public class Application extends BaseModel {

  @ManyToOne private Applicant applicant;

  @ManyToOne private Program program;

  // Note: there is not an index on createTime currently as we don't filter on
  // it and expect the number of results to be small.
  @OneToMany(mappedBy = "application")
  @OrderBy("createTime desc")
  private List<ApplicationEvent> applicationEvents;

  @Constraints.Required private LifecycleStage lifecycleStage;

  @WhenCreated private Instant createTime;

  @Constraints.Required @DbJson private String object;

  private Instant submitTime;
  private String preferredLocale;
  private String submitterEmail;
  private String latestStatus;
  private boolean isAdmin;

  public Application(Applicant applicant, Program program, LifecycleStage lifecycleStage) {
    this.applicant = applicant;
    this.program = program;
    this.object = "{}";
    this.lifecycleStage = lifecycleStage;
    this.isAdmin =
        applicant.getAccount().getGlobalAdmin()
            || !applicant.getAccount().getAdministeredProgramNames().isEmpty();
  }

  public static Application create(
      Applicant applicant, Program program, LifecycleStage lifecycleStage) {
    Application application = new Application(applicant, program, lifecycleStage);
    application.save();
    return application;
  }

  public Optional<String> getSubmitterEmail() {
    return Optional.ofNullable(this.submitterEmail);
  }

  public Application setSubmitterEmail(String submitterEmail) {
    this.submitterEmail = submitterEmail;
    return this;
  }

  public Applicant getApplicant() {
    return this.applicant;
  }

  public Program getProgram() {
    return this.program;
  }

  public ApplicantData getApplicantData() {
    if (this.preferredLocale == null || this.preferredLocale.isEmpty()) {
      // Default to English.
      return new ApplicantData(this.object);
    }

    return new ApplicantData(Optional.of(Locale.forLanguageTag(preferredLocale)), this.object);
  }

  public Application setApplicantData(ApplicantData data) {
    this.preferredLocale =
        data.hasPreferredLocale() ? data.preferredLocale().toLanguageTag() : null;
    this.object = data.asJsonString();
    return this;
  }

  public List<ApplicationEvent> getApplicationEvents() {
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

  public Application setLifecycleStage(LifecycleStage stage) {
    this.lifecycleStage = stage;
    return this;
  }

  public Application setSubmitTimeToNow() {
    this.submitTime = Instant.now();
    return this;
  }

  @VisibleForTesting
  public Application setSubmitTimeForTest(Instant v) {
    this.submitTime = v;
    return this;
  }

  @VisibleForTesting
  public Application setCreateTimeForTest(Instant v) {
    this.createTime = v;
    return this;
  }

  /**
   * Returns the latest application status text value associated with the application.
   *
   * <p>This value is updated by DB triggers defined in conf/evolutions/default/44.sql which set the
   * status to the latest ApplicationEventDetails event for the application with a type of
   * "status_change". Attempts to update the status manually will be overridden by the trigger (and
   * has associated tests confirming this).
   *
   * <p>If information about the actual event that set this status is desired, make use of
   * getApplicationEvents instead.
   */
  public Optional<String> getLatestStatus() {
    return Optional.ofNullable(latestStatus);
  }

  /**
   * This is visible only for tests to manipulate the latest status directly in order to ensure that
   * updates to it are overridden by the configured database trigger.
   */
  @VisibleForTesting
  void setLatestStatusForTest(String latestStatus) {
    this.latestStatus = latestStatus;
  }
}
