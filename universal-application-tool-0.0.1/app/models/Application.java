package models;

import io.ebean.annotation.DbJson;
import io.ebean.annotation.WhenCreated;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
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

  @Constraints.Required private LifecycleStage lifecycleStage;

  @WhenCreated private Instant createTime;

  @Constraints.Required @DbJson private String object;

  private Instant submitTime;
  private String preferredLocale;
  private String submitterEmail;

  public Application(Applicant applicant, Program program, LifecycleStage lifecycleStage) {
    this.applicant = applicant;
    setApplicantData(applicant.getApplicantData());
    this.program = program;
    this.lifecycleStage = lifecycleStage;
  }

  public static Application createSync(
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

  public void setApplicantData(ApplicantData data) {
    this.preferredLocale =
        data.hasPreferredLocale() ? data.preferredLocale().toLanguageTag() : null;
    this.object = data.asJsonString();
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

  public void setLifecycleStage(LifecycleStage stage) {
    this.lifecycleStage = stage;
  }

  public void setSubmitTimeToNow() {
    this.submitTime = Instant.now();
  }
}
