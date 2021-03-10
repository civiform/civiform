package models;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "accounts")
public class Account extends BaseModel {
  private static final long serialVersionUID = 1L;

  @OneToMany(mappedBy = "account")
  private List<Applicant> applicants;

  private String emailAddress;

  public List<Applicant> getApplicants() {
    return applicants;
  }

  public void setApplicants(List<Applicant> applicants) {
    this.applicants = applicants;
  }

  public void setEmailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
  }

  public String getEmailAddress() {
    return this.emailAddress;
  }
}
