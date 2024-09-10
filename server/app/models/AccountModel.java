package models;

import static com.google.common.collect.ImmutableList.toImmutableList;

import auth.oidc.SerializedIdTokens;
import autovalue.shaded.com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import io.ebean.annotation.DbArray;
import io.ebean.annotation.DbJsonB;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import services.program.ProgramDefinition;

/**
 * An EBean mapped class that represents a single user account in CiviForm. Users of all roles have
 * an {@code Account}.
 *
 * <p>When a user logs in for the first time either using SSO or as a guest, CiviForm creates an
 * {@code Account} record for them.
 *
 * <p>authorityId serves as the unchanging unique identifier for accounts.
 *
 * <p>Note that residents have a single {@code Account} and a single {@code Applicant} record,
 * despite the one to many relationship. This is technical debt that stems from earlier reasoning
 * about the approach wherein we expected we'd need to create multiple versions of the resident's
 * {@code ApplicantData} for each version they interact with. That isn't the case and their {@code
 * ApplicantData} migrates seamlessly with each additional version but the database schema remains.
 *
 * <p>Accounts are not versioned.
 */
@Entity
@Table(name = "accounts")
public class AccountModel extends BaseModel {

  private static final long serialVersionUID = 1L;

  @OneToMany(mappedBy = "account")
  private List<ApplicantModel> applicants;

  @ManyToOne private TrustedIntermediaryGroupModel memberOfGroup;
  @ManyToOne private TrustedIntermediaryGroupModel managedByGroup;
  private boolean globalAdmin;

  // This must be a mutable collection so we can add to the list later.
  @DbArray private List<String> adminOf = new ArrayList<>();

  private String authorityId;
  private String emailAddress;

  @DbJsonB(name = "id_tokens")
  private SerializedIdTokens serializedIdTokens;

  private String tiNote;

  public AccountModel setTiNote(String tiNote) {
    this.tiNote = tiNote;
    return this;
  }

  public String getTiNote() {
    if (Strings.isNullOrEmpty(this.tiNote)) return "";
    return this.tiNote;
  }

  public ImmutableList<Long> ownedApplicantIds() {
    return getApplicants().stream().map(applicant -> applicant.id).collect(toImmutableList());
  }

  public List<ApplicantModel> getApplicants() {
    return applicants;
  }

  public Optional<ApplicantModel> newestApplicant() {
    return applicants.stream().max(Comparator.comparing(ApplicantModel::getWhenCreated));
  }

  public AccountModel setApplicants(List<ApplicantModel> applicants) {
    this.applicants = applicants;
    return this;
  }

  public AccountModel setAuthorityId(String authorityId) {
    this.authorityId = authorityId;
    return this;
  }

  public String getAuthorityId() {
    return this.authorityId;
  }

  public AccountModel setEmailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
    return this;
  }

  public String getEmailAddress() {
    return this.emailAddress;
  }

  public AccountModel setMemberOfGroup(TrustedIntermediaryGroupModel group) {
    this.memberOfGroup = group;
    return this;
  }

  public AccountModel setManagedByGroup(TrustedIntermediaryGroupModel group) {
    this.managedByGroup = group;
    return this;
  }

  public Optional<TrustedIntermediaryGroupModel> getMemberOfGroup() {
    return Optional.ofNullable(this.memberOfGroup);
  }

  public Optional<TrustedIntermediaryGroupModel> getManagedByGroup() {
    return Optional.ofNullable(this.managedByGroup);
  }

  public SerializedIdTokens getSerializedIdTokens() {
    return serializedIdTokens;
  }

  public AccountModel setSerializedIdTokens(SerializedIdTokens serializedIdTokens) {
    this.serializedIdTokens = serializedIdTokens;
    return this;
  }

  public ImmutableList<String> getAdministeredProgramNames() {
    if (this.adminOf == null) {
      return ImmutableList.of();
    }
    return ImmutableList.copyOf(this.adminOf);
  }

  /**
   * Set whether or not the user is a global admin. If they are a global admin, they are cleared of
   * any program-admin role.
   */
  public AccountModel setGlobalAdmin(boolean isGlobalAdmin) {
    this.globalAdmin = isGlobalAdmin;
    if (this.globalAdmin) {
      this.adminOf.clear();
    }
    return this;
  }

  public boolean getGlobalAdmin() {
    return globalAdmin;
  }

  /**
   * If this account does not already administer this program, add it to the list of administered
   * programs.
   */
  public AccountModel addAdministeredProgram(ProgramDefinition program) {
    if (this.adminOf == null) {
      this.adminOf = new ArrayList<>();
    }
    if (!this.adminOf.contains(program.adminName())) {
      this.adminOf.add(program.adminName());
    }
    return this;
  }

  /**
   * If this account administers the provided program, remove it from the list of administered
   * programs.
   */
  public void removeAdministeredProgram(ProgramDefinition program) {
    this.adminOf.remove(program.adminName());
  }

  /**
   * Returns the name, as a string, of the most-recently created Applicant associated with this
   * Account. Or the email if no name is associated with the applicant. There is no particular
   * reason for an Account to have more than one Applicant - this was a capability we built but did
   * not use - so the ordering is somewhat arbitrary / unnecessary.
   */
  public String getApplicantDisplayName() {
    return this.getApplicants().stream()
        .max(Comparator.comparing(ApplicantModel::getWhenCreated))
        .map(u -> u.getApplicantData().getApplicantDisplayName().orElse("<Unnamed User>"))
        .orElse("<Unnamed User>");
  }
}
