package models;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

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

  public ImmutableList<Account> getManagedAccounts() {
    return ImmutableList.copyOf(managedAccounts);
  }

  public String getName() {
    return this.name;
  }

  public String getDescription() {
    return this.description;
  }
}
