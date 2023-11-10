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
public final class TrustedIntermediarySearchResult {

  private final Optional<ImmutableList<AccountModel>> accounts;
  private final Optional<String> errorMessage;

  private TrustedIntermediarySearchResult(
      Optional<ImmutableList<AccountModel>> accounts, Optional<String> errorMessage) {
    this.accounts = accounts;
    this.errorMessage = errorMessage;
  }

  public static TrustedIntermediarySearchResult success(ImmutableList<AccountModel> searchResult) {
    return new TrustedIntermediarySearchResult(
        Optional.of(searchResult), /* errorMessage= */ Optional.empty());
  }

  public static TrustedIntermediarySearchResult fail(
      ImmutableList<AccountModel> allAccount, String errorMessage) {
    return new TrustedIntermediarySearchResult(Optional.of(allAccount), Optional.of(errorMessage));
  }

  public boolean isSuccessful() {
    return errorMessage.isEmpty();
  }

  public Optional<ImmutableList<AccountModel>> getAccounts() {
    return accounts;
  }

  public Optional<String> getErrorMessage() {
    return errorMessage;
  }
}
