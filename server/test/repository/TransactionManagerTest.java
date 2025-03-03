package repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import io.ebean.DB;
import io.ebean.SerializableConflictException;
import io.ebean.Transaction;
import io.ebean.TxScope;
import io.ebean.annotation.TxIsolation;
import java.util.function.Supplier;
import models.AccountModel;
import org.junit.Before;
import org.junit.Test;
import services.ErrorAnd;

public class TransactionManagerTest extends ResetPostgres {

  TransactionManager transactionManager;
  AccountRepository accountRepo;

  @Before
  public void setUp() {
    transactionManager = instanceOf(TransactionManager.class);
    accountRepo = instanceOf(AccountRepository.class);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void executeInTransaction_runsWorkSupplier() {
    Supplier<String> mockWork = mock(Supplier.class);
    when(mockWork.get()).thenReturn("work");
    Supplier<String> mockOnFailure = mock(Supplier.class);
    when(mockOnFailure.get()).thenReturn("onFailure");

    transactionManager.executeInTransaction(mockWork, mockOnFailure);

    verify(mockWork).get();
    verify(mockOnFailure, times(0)).get();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void executeInTransaction_runsFailureSupplier() {
    Supplier<String> mockWork = mock(Supplier.class);
    when(mockWork.get())
        .thenThrow(
            new SerializableConflictException("Simulate a concurrency issue", new Exception()));
    Supplier<String> mockOnFailure = mock(Supplier.class);
    when(mockOnFailure.get()).thenReturn("onFailure");

    transactionManager.executeInTransaction(mockWork, mockOnFailure);

    verify(mockWork).get();
    verify(mockOnFailure).get();
  }

  @Test
  public void executeInTransaction_modifiesEntitySuccessfully() {
    AccountModel account = new AccountModel().setEmailAddress("initial@test.com");
    account.insert();

    Supplier<ErrorAnd<AccountModel, String>> modifyAccount =
        () -> {
          AccountModel accountToModify = accountRepo.lookupAccount(account.id).get();
          accountToModify.setEmailAddress("updated@test.com");
          accountToModify.save();
          return ErrorAnd.of(accountToModify);
        };
    Supplier<ErrorAnd<AccountModel, String>> onFailure =
        () -> ErrorAnd.error(ImmutableSet.of("error"));

    ErrorAnd<AccountModel, String> result =
        transactionManager.executeInTransaction(modifyAccount, onFailure);
    account.refresh();

    assertThat(result.hasResult()).isTrue();
    assertThat(result.isError()).isFalse();
    assertThat(account.getEmailAddress()).isEqualTo("updated@test.com");
  }

  @Test
  public void executeInTransaction_rollsBackTransactionSuccessfully() {
    AccountModel account = new AccountModel().setEmailAddress("initial@test.com");
    account.insert();

    Supplier<ErrorAnd<AccountModel, String>> modifyAccount =
        () -> {
          AccountModel accountToModify = accountRepo.lookupAccount(account.id).get();
          accountToModify.setEmailAddress("updated@test.com");
          accountToModify.save();
          throw new SerializableConflictException("Simulate a concurrency issue", new Exception());
        };
    Supplier<ErrorAnd<AccountModel, String>> onFailure =
        () -> ErrorAnd.error(ImmutableSet.of("error"));

    ErrorAnd<AccountModel, String> result =
        transactionManager.executeInTransaction(modifyAccount, onFailure);
    account.refresh();

    assertThat(result.hasResult()).isFalse();
    assertThat(result.isError()).isTrue();
    assertThat(account.getEmailAddress()).isEqualTo("initial@test.com");
  }

  /** Simulate when the work() supplier contains another transaction. */
  @Test
  public void executeInTransaction_transactionInsideSupplierRollsBackIfWrappedTransactionFails() {
    Supplier<String> work =
        () -> {
          try (Transaction innerTransaction =
              DB.beginTransaction(TxScope.required().setIsolation(TxIsolation.SERIALIZABLE))) {
            new AccountModel().insert();
            // Assert that from within this inner transaction, we see the new account
            assertThat(DB.find(AccountModel.class).findCount()).isEqualTo(1);
            innerTransaction.commit();
          }

          // Assert that, back in the outer transaction, we see the new account
          assertThat(DB.find(AccountModel.class).findCount()).isEqualTo(1);
          throw new SerializableConflictException("Simulate a concurrency issue", new Exception());
        };

    transactionManager.executeInTransaction(work, () -> "error");

    // Outside of both transactions, everything should be rolled back.
    assertThat(DB.find(AccountModel.class).findCount()).isEqualTo(0);
  }

  /** Simulate when we use the {@link TransactionManager} from inside another transaction. */
  @Test
  public void executeInTransaction_workInSupplierRollsBackIfOuterTransactionFails() {
    Supplier<String> work =
        () -> {
          new AccountModel().insert();
          // Assert that from within this inner transaction, we see the new account
          assertThat(DB.find(AccountModel.class).findCount()).isEqualTo(1);
          return "done";
        };

    try (Transaction outerTransaction =
        DB.beginTransaction(TxScope.required().setIsolation(TxIsolation.SERIALIZABLE))) {
      transactionManager.executeInTransaction(work, () -> "error");
      // Assert that, back in the outer transaction, we see the new account
      assertThat(DB.find(AccountModel.class).findCount()).isEqualTo(1);
      outerTransaction.rollback();
    }

    // Outside of both transactions, everything should be rolled back.
    assertThat(DB.find(AccountModel.class).findCount()).isEqualTo(0);
  }

  @Test
  public void executeInTransaction_innerTransactionWithRequiresNewIsIndependent() {
    Supplier<String> work =
        () -> {
          // Check initial number of accounts in the outer transaction's snapshot
          assertThat(DB.find(AccountModel.class).findCount()).isEqualTo(0);

          try (Transaction innerTransaction =
              DB.beginTransaction(TxScope.requiresNew().setIsolation(TxIsolation.SERIALIZABLE))) {
            new AccountModel().insert();

            // Assert that from within this inner transaction, we see the new account
            assertThat(DB.find(AccountModel.class).findCount()).isEqualTo(1);
            innerTransaction.commit();
          }

          // Assert that, back in the outer transaction, we still see the original 0 accounts
          // because we're working from an earlier snapshot.
          assertThat(DB.find(AccountModel.class).findCount()).isEqualTo(0);
          return "done";
        };

    transactionManager.executeInTransaction(work, () -> "error");

    // Outside of both transactions, we should see the new account
    assertThat(DB.find(AccountModel.class).findCount()).isEqualTo(1);
  }
}

// what happens if the inner work is in a different thread?
