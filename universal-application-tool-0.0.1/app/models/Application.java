package models;

import io.ebean.annotation.DbJson;
import java.time.Instant;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import play.data.validation.Constraints;
import services.WellKnownPaths;
import services.applicant.ApplicantData;

@Entity
@Table(name = "applications")
public class Application extends BaseModel {
  @ManyToOne private Applicant applicant;

  @ManyToOne private Program program;

  // used by generated code
  @SuppressWarnings("UnusedVariable")
  @Constraints.Required
  @DbJson
  private String object;

  public Application(Applicant applicant, Program program, Instant submitTime) {
    this.applicant = applicant;
    ApplicantData data = applicant.getApplicantData();
    data.putString(WellKnownPaths.APPLICATION_SUBMITTED_TIME, submitTime.toString());
    this.object = data.asJsonString();
    this.program = program;
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
}
