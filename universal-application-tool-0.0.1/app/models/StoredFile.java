package models;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import play.data.validation.Constraints;

/** The ebean mapped class for a file stored in AWS S3 */
@Entity
@Table(name = "files")
public class StoredFile extends BaseModel {
  private static final long serialVersionUID = 1L;

  @ManyToOne private Applicant applicant;

  @Constraints.Required String name;

  public StoredFile(Applicant applicant) {
    this.applicant = applicant;
  }

  public Applicant getApplicant() {
    return applicant;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
