package models;

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * An EBean mapped class that represents a group of trusted intermediaries, usually corresponding to
 * a single community-based organization.
 *
 * <p>Permissions for trusted intermediary {@code Account}s are managed via their membership in a
 * {@code TrustedIntermediaryGroup}.
 */
@Entity
@Table(name = "ti_organizations")
public class TrustedIntermediaryGroupModel extends BaseModel {

  @OneToMany(mappedBy = "memberOfGroup")
  private List<AccountModel> tiAccounts;

  @OneToMany(mappedBy = "managedByGroup")
  private List<AccountModel> managedAccounts;

  private String name;
  private String description;

  public TrustedIntermediaryGroupModel(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public ImmutableList<AccountModel> getTrustedIntermediaries() {
    return ImmutableList.copyOf(tiAccounts);
  }

  /** Gets the count of TrustedIntermediaries */
  public int getMembersCount() {
    return tiAccounts.size();
  }

  /** Get all the accounts, sorted by applicant name. */
  public ImmutableList<AccountModel> getManagedAccounts() {
    return managedAccounts.stream()
        .sorted(Comparator.comparing(AccountModel::getApplicantDisplayName))
        .collect(ImmutableList.toImmutableList());
  }

  /** Gets the count of Managed Accounts */
  public int getManagedAccountsCount() {
    return managedAccounts.size();
  }

  public String getName() {
    return this.name;
  }

  public String getDescription() {
    return this.description;
  }
}
