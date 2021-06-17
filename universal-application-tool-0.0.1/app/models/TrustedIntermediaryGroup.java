package models;

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
public class TrustedIntermediaryGroup extends BaseModel {

  @OneToMany(mappedBy = "memberOfGroup")
  private List<Account> tiAccounts;

  @OneToMany(mappedBy = "managedByGroup")
  private List<Account> managedAccounts;

  private String name;
  private String description;

  public TrustedIntermediaryGroup(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public ImmutableList<Account> getTrustedIntermediaries() {
    return ImmutableList.copyOf(tiAccounts);
  }

  /** Get all the accounts, sorted by applicant name. */
  public ImmutableList<Account> getManagedAccounts() {
    return managedAccounts.stream()
        .sorted(Comparator.comparing(Account::getApplicantName))
        .collect(ImmutableList.toImmutableList());
  }

  public String getName() {
    return this.name;
  }

  public String getDescription() {
    return this.description;
  }

  public ImmutableList<Account> getManagedAccounts(Optional<String> search) {
    ImmutableList<Account> allAccounts = getManagedAccounts();
    if (search.isPresent()) {
      allAccounts =
          allAccounts.stream()
              .filter(
                  account ->
                      account
                          .getApplicantName()
                          .toLowerCase(Locale.ROOT)
                          .contains(search.get().toLowerCase(Locale.ROOT)))
              .collect(ImmutableList.toImmutableList());
    }
    return allAccounts;
  }
}
