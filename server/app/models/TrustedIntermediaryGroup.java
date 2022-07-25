package models;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.SearchParameters;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
  private static final Logger LOGGER = LoggerFactory.getLogger(TrustedIntermediaryGroup.class);

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

  public ImmutableList<Account> getManagedAccounts(SearchParameters searchParameters) {
    ImmutableList<Account> allAccounts = getManagedAccounts();
    try {
      allAccounts = allAccounts.stream()
        .filter(account -> {
          if (searchParameters.search().isPresent()) {
            return account.getApplicantName().toLowerCase(Locale.ROOT).contains(searchParameters.search().get().toLowerCase(Locale.ROOT));
          }
          if (searchParameters.searchDate().isPresent() && !searchParameters.searchDate().isEmpty() && account.getApplicantDateOfBirth().isPresent()) {
            LocalDate localDate =
              LocalDate.parse(searchParameters.searchDate().get(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return account.getApplicantDateOfBirth().get().equals(localDate);
          }
          return false;
        }).collect(ImmutableList.toImmutableList());
    }
    catch(DateTimeParseException e)
    {
      LOGGER.warn("Unformatted Date Entered - " + searchParameters.searchDate().get());
    }
    return allAccounts;
  }
}
