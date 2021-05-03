package models;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "accounts")
public class Account extends BaseModel {
  private static final long serialVersionUID = 1L;

  @OneToMany(mappedBy = "account")
  private List<Applicant> applicants;

  @ManyToOne private TrustedIntermediaryGroup memberOfGroup;
  @ManyToOne private TrustedIntermediaryGroup managedByGroup;

  private String emailAddress;

  public ImmutableList<Long> ownedApplicantIds() {
    return getApplicants().stream().map(applicant -> applicant.id).collect(toImmutableList());
  }

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

  public void setMemberOfGroup(TrustedIntermediaryGroup group) {
    this.memberOfGroup = group;
  }

  public void setManagedByGroup(TrustedIntermediaryGroup group) {
    this.managedByGroup = group;
  }

  public Optional<TrustedIntermediaryGroup> getMemberOfGroup() {
    return Optional.fromNullable(this.memberOfGroup);
  }

  public Optional<TrustedIntermediaryGroup> getManagedByGroup() {
    return Optional.fromNullable(this.managedByGroup);
  }

  /**
   * Returns the name, as a string, of the most-recently created Applicant associated with this
   * Account. There is no particular reason for an Account to have more than one Applicant - this
   * was a capability we built but did not use - so the ordering is somewhat arbitrary /
   * unnecessary.
   */
  public String getApplicantName() {
    return this.getApplicants().stream()
        .max(Comparator.comparing(Applicant::getWhenCreated))
        .map(u -> u.getApplicantData().getApplicantName())
        .orElse("<Unnamed User>");
  }
}
