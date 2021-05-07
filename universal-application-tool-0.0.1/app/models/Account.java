package models;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import io.ebean.annotation.DbArray;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import services.program.ProgramDefinition;

@Entity
@Table(name = "accounts")
public class Account extends BaseModel {
  private static final long serialVersionUID = 1L;

  @OneToMany(mappedBy = "account")
  private List<Applicant> applicants;

  @ManyToOne private TrustedIntermediaryGroup memberOfGroup;
  @ManyToOne private TrustedIntermediaryGroup managedByGroup;

  // This must be a mutable collection so we can add to the list later.
  @DbArray private List<String> adminOf = new ArrayList<>();

  private String emailAddress;

  public ImmutableList<Long> ownedApplicantIds() {
    return getApplicants().stream().map(applicant -> applicant.id).collect(toImmutableList());
  }

  public List<Applicant> getApplicants() {
    return applicants;
  }

  public Optional<Applicant> newestApplicant() {
    return applicants.stream().max(Comparator.comparing(Applicant::getWhenCreated));
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
    return Optional.ofNullable(this.memberOfGroup);
  }

  public Optional<TrustedIntermediaryGroup> getManagedByGroup() {
    return Optional.ofNullable(this.managedByGroup);
  }

  public ImmutableList<String> getAdministeredProgramNames() {
    return ImmutableList.copyOf(this.adminOf);
  }

  public void addAdministeredProgram(ProgramDefinition program) {
    this.adminOf.add(program.adminName());
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
