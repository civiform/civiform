package models;

import io.ebean.annotation.DbJson;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import play.data.validation.Constraints;

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

  public Application(Applicant applicant, Program program) {
    this.applicant = applicant;
    this.object = applicant.getApplicantData().asJsonString();
    this.program = program;
  }

  public Applicant getApplicant() {
    return this.applicant;
  }

  public Program getProgram() {
    return this.program;
  }
}
