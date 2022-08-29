package services.ti;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import models.Account;

/**
 * Holds list of account after filtering out the account list per the SearchParamaters
 *
 * <p>If the filtering attempt was successful, contains the filtered accounts.
 *
 * <p>If the filtering attempt was not successful, contains all the accounts of the TIGroup along
 * with the error message of why the filtering failed.
 */
public final class TrustedIntermediarySearchResult {

  private final Optional<ImmutableList<Account>> accounts;
  private final Optional<String> errorMessage;

  private TrustedIntermediarySearchResult(
      Optional<ImmutableList<Account>> accounts, Optional<String> errorMessage) {
    this.accounts = accounts;
    this.errorMessage = errorMessage;
  }

  public static TrustedIntermediarySearchResult success(ImmutableList<Account> searchResult) {
    return new TrustedIntermediarySearchResult(
        Optional.of(searchResult), /* errorMessage= */ Optional.empty());
  }

  public static TrustedIntermediarySearchResult fail(
      ImmutableList<Account> allAccount, String errorMessage) {
    return new TrustedIntermediarySearchResult(Optional.of(allAccount), Optional.of(errorMessage));
  }

  public boolean isSuccessful() {
    return errorMessage.isEmpty();
  }

  public Optional<ImmutableList<Account>> getAccounts() {
    return accounts;
  }

  public Optional<String> getErrorMessage() {
    return errorMessage;
  }
}
