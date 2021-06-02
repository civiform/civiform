package models;

import io.ebean.annotation.DbJson;
import io.ebean.annotation.UpdatedTimestamp;
import java.time.Instant;
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

  @UpdatedTimestamp private Instant submitTime;

  // used by generated code
  @SuppressWarnings("UnusedVariable")
  @Constraints.Required
  @DbJson
  private String object;

  private String submitterEmail;

  public Application(Applicant applicant, Program program, LifecycleStage lifecycleStage) {
    this.applicant = applicant;
    ApplicantData data = applicant.getApplicantData();
    this.object = data.asJsonString();
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
    return new ApplicantData(this.object);
  }

  public LifecycleStage getLifecycleStage() {
    return this.lifecycleStage;
  }

  public Instant getSubmitTime() {
    return this.submitTime;
  }

  public void setLifecycleStage(LifecycleStage stage) {
    this.lifecycleStage = stage;
  }
}
