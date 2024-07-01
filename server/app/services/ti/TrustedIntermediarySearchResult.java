package services.ti;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import models.AccountModel;

/**
 * Holds list of account after filtering out the account list per the SearchParamaters
 *
 * <p>If the filtering attempt was successful, contains the filtered accounts.
 *
 * <p>If the filtering attempt was not successful, contains all the accounts of the TIGroup along
 * with the error message of why the filtering failed.
 */
public final record TrustedIntermediarySearchResult(
    ImmutableList<AccountModel> accounts, Optional<String> errorMessage) {

  public TrustedIntermediarySearchResult(
      ImmutableList<AccountModel> accounts, Optional<String> errorMessage) {
    this.accounts = accounts;
    this.errorMessage = errorMessage;
  }

  public TrustedIntermediarySearchResult(ImmutableList<AccountModel> accounts) {
    this(accounts, Optional.empty());
  }

  public boolean isSuccessful() {
    return errorMessage.isEmpty();
  }
}
