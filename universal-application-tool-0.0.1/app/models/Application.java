package models;

import io.ebean.annotation.CreatedTimestamp;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.UpdatedTimestamp;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import play.data.validation.Constraints;
import services.applicant.ApplicantData;

@Entity
@Table(name = "applications")
public class Application extends BaseModel {
  @ManyToOne private Applicant applicant;

  @ManyToOne private Program program;

  @Constraints.Required private LifecycleStage lifecycleStage;

  @CreatedTimestamp private Instant createTime;
  @UpdatedTimestamp private Instant submitTime;

  @Constraints.Required @DbJson private String object;

  private String preferredLocale;
  private String submitterEmail;

  public Application(Applicant applicant, Program program, LifecycleStage lifecycleStage) {
    this.applicant = applicant;
    setApplicantData(applicant.getApplicantData());
    this.program = program;
    this.lifecycleStage = lifecycleStage;
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
}
