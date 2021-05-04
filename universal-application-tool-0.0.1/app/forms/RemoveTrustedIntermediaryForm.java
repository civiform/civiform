package forms;

public class RemoveTrustedIntermediaryForm {
  private Long accountId;

  public RemoveTrustedIntermediaryForm(Long accountId) {
    this.accountId = accountId;
  }

  public RemoveTrustedIntermediaryForm() {
    accountId = -1L;
  }

  public Long getAccountId() {
    return accountId;
  }

  public void setAccountId(Long accountId) {
    this.accountId = accountId;
  }
}
