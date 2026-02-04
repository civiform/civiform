package repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.ebean.DB;
import io.ebean.SerializableConflictException;
import io.ebean.Transaction;
import io.ebean.TxScope;
import io.ebean.annotation.TxIsolation;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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

  @Test
  public void execute_runnable_runsWorkSupplier() {
    String outerEmail = "outeremail@test.com";
    AccountModel account = new AccountModel().setEmailAddress("initial@test.com");
    account.insert();

    transactionManager.execute(
        () -> {
          AccountModel outerAccount = accountRepo.lookupAccount(account.id).orElseThrow();
          outerAccount.setEmailAddress(outerEmail);
          outerAccount.save();
        });

    account.refresh();
    assertThat(account.getEmailAddress()).isEqualTo(outerEmail);
  }

  @Test
  public void execute_runnable_rollsBackTransactionSuccessfully() {
    String innerEmail = "inneremail@test.com";
    AccountModel account = new AccountModel().setEmailAddress("initial@test.com");
    account.insert();

    Runnable modifyAccount =
        () -> {
          AccountModel outerAccount = accountRepo.lookupAccount(account.id).orElseThrow();

          // Update the account in a different Transaction (requiresNew)
          // before the current one finishes to trigger a serialization
          // exception in the outer transaction.
          try (Transaction innerTransaction =
              DB.beginTransaction(TxScope.requiresNew().setIsolation(TxIsolation.SERIALIZABLE))) {
            AccountModel innerAccount = accountRepo.lookupAccount(account.id).orElseThrow();
            innerAccount.setEmailAddress(innerEmail);
            innerAccount.save();
            innerTransaction.commit();
          }

          outerAccount.setEmailAddress("updated@test.com");
          outerAccount.save();
        };

    assertThrows(
        SerializableConflictException.class, () -> transactionManager.execute(modifyAccount));

    account.refresh();
    assertThat(account.getEmailAddress()).isEqualTo(innerEmail);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void execute_supplier_runsWorkSupplier() {
    Supplier<String> mockWork = mock(Supplier.class);
    when(mockWork.get()).thenReturn("work");
    Supplier<String> mockOnFailure = mock(Supplier.class);
    when(mockOnFailure.get()).thenReturn("onFailure");

    transactionManager.execute(mockWork, mockOnFailure);

    verify(mockWork).get();
    verify(mockOnFailure, times(0)).get();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void execute_supplier_runsFailureSupplier() {
    Supplier<String> mockWork = mock(Supplier.class);
    when(mockWork.get())
        .thenThrow(
            new SerializableConflictException("Simulate a concurrency issue", new Exception()));
    Supplier<String> mockOnFailure = mock(Supplier.class);
    when(mockOnFailure.get()).thenReturn("onFailure");

    transactionManager.execute(mockWork, mockOnFailure);

    verify(mockWork).get();
    verify(mockOnFailure).get();
  }

  @Test
  public void execute_supplier_modifiesEntitySuccessfully() {
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

    ErrorAnd<AccountModel, String> result = transactionManager.execute(modifyAccount, onFailure);
    account.refresh();

    assertThat(result.hasResult()).isTrue();
    assertThat(result.isError()).isFalse();
    assertThat(account.getEmailAddress()).isEqualTo("updated@test.com");
  }

  @Test
  public void execute_supplier_rollsBackTransactionSuccessfully() {
    String innerEmail = "inneremail@test.com";
    AccountModel account = new AccountModel().setEmailAddress("initial@test.com");
    account.insert();

    Supplier<ErrorAnd<AccountModel, String>> modifyAccount =
        () -> {
          AccountModel outerAccount = accountRepo.lookupAccount(account.id).orElseThrow();

          // Update the account in a different Transaction (requiresNew)
          // before the current one finishes to trigger a serialization
          // exception in the outer transaction.
          try (Transaction innerTransaction =
              DB.beginTransaction(TxScope.requiresNew().setIsolation(TxIsolation.SERIALIZABLE))) {
            AccountModel innerAccount = accountRepo.lookupAccount(account.id).orElseThrow();
            innerAccount.setEmailAddress(innerEmail);
            innerAccount.save();
            innerTransaction.commit();
          }

          outerAccount.setEmailAddress("updated@test.com");
          outerAccount.save();
          return ErrorAnd.of(outerAccount);
        };
    Supplier<ErrorAnd<AccountModel, String>> onFailure =
        () -> ErrorAnd.error(ImmutableSet.of("error"));

    ErrorAnd<AccountModel, String> result = transactionManager.execute(modifyAccount, onFailure);
    account.refresh();

    assertThat(result.hasResult()).isFalse();
    assertThat(result.isError()).isTrue();
    assertThat(account.getEmailAddress()).isEqualTo(innerEmail);
  }

  @Test
  public void executeWithRetry_supplier_throws() {
    // Force the workload to fail by running other transactions that conflict.

    List<String> innerEmails = ImmutableList.of("inneremail1@test.com", "inneremail2@test.com");
    AtomicReference<Integer> counter = new AtomicReference<>(0);
    AccountModel account = new AccountModel().setEmailAddress("initial@test.com");
    account.insert();

    Supplier<AccountModel> modifyAccount =
        () -> {
          AccountModel outerAccount = accountRepo.lookupAccount(account.id).orElseThrow();

          // Update the account in a different Transaction (requiresNew) before the current one
          // finishes to trigger a serialization/ exception in the outer transaction.
          // We need a different email each time as save() is a no-op if nothing changes.
          try (Transaction innerTransaction =
              DB.beginTransaction(TxScope.requiresNew().setIsolation(TxIsolation.SERIALIZABLE))) {
            AccountModel innerAccount = accountRepo.lookupAccount(account.id).orElseThrow();
            innerAccount.setEmailAddress(innerEmails.get(counter.getAndSet(counter.get() + 1)));
            innerAccount.save();
            innerTransaction.commit();
          }

          outerAccount.setEmailAddress("updated@test.com");
          outerAccount.save();
          return outerAccount;
        };

    assertThrows(
        SerializableConflictException.class,
        () -> transactionManager.executeWithRetry(modifyAccount));
    account.refresh();
    assertThat(innerEmails.get(1)).isEqualTo(account.getEmailAddress());
  }

  @Test
  public void executeWithRetry_supplier() {
    final String innerEmail = "inneremail@test.com";
    AtomicBoolean innerTransactionHasRun = new AtomicBoolean(false);
    // How many times setEmailAddress was successfully saved on outerAccount.
    // Helps verify the test setup works; in that .save() is throwing an
    // exception the first time.
    AtomicInteger outerSetNumberOfTime = new AtomicInteger();
    AccountModel initialAccount = new AccountModel().setEmailAddress("initial@test.com");
    initialAccount.insert();
    long accountId = initialAccount.id;

    Supplier<AccountModel> modifyAccount =
        () -> {
          AccountModel outerAccount = accountRepo.lookupAccount(accountId).orElseThrow();

          // Cause a concurrency error on the first run.
          if (!innerTransactionHasRun.getAndSet(true)) {
            // Update the account in a different Transaction (requiresNew)
            // before the current one finishes to trigger a serialization
            // exception in the outer transaction.
            try (Transaction innerTransaction =
                DB.beginTransaction(TxScope.requiresNew().setIsolation(TxIsolation.SERIALIZABLE))) {
              AccountModel innerAccount = accountRepo.lookupAccount(accountId).orElseThrow();
              innerAccount.setEmailAddress(innerEmail);
              innerAccount.save();
              innerTransaction.commit();
            }
          } else {
            // Verify the inner transaction change occurred and is visible on the retry.
            AccountModel innerAccount = accountRepo.lookupAccount(accountId).orElseThrow();
            assertThat(innerAccount.getEmailAddress()).isEqualTo(innerEmail);
          }

          outerAccount.setEmailAddress("updated@test.com");
          outerAccount.save();
          // The first time through save() will throw the exception due to
          // innerTransaction.  The second time will execute the entire method.
          // Counting here ensures that the test setup is correct.
          outerSetNumberOfTime.incrementAndGet();
          return outerAccount;
        };

    var outerAccount = transactionManager.executeWithRetry(modifyAccount);
    assertThat(outerAccount.getEmailAddress()).isEqualTo("updated@test.com");

    initialAccount.refresh();
    assertThat(initialAccount.getEmailAddress()).isEqualTo("updated@test.com");
    assertThat(outerSetNumberOfTime.get()).isEqualTo(1);
  }

  @Test
  public void executeWithRetry_runnable_throws() {
    // Force the workload to fail by running other transactions that conflict.

    List<String> innerEmails = ImmutableList.of("inneremail1@test.com", "inneremail2@test.com");
    AtomicReference<Integer> counter = new AtomicReference<>(0);
    AccountModel account = new AccountModel().setEmailAddress("initial@test.com");
    account.insert();

    Runnable modifyAccount =
        () -> {
          AccountModel outerAccount = accountRepo.lookupAccount(account.id).orElseThrow();

          // Update the account in a different Transaction (requiresNew) before the current one
          // finishes to trigger a serialization/ exception in the outer transaction.
          // We need a different email each time as save() is a no-op if nothing changes.
          try (Transaction innerTransaction =
              DB.beginTransaction(TxScope.requiresNew().setIsolation(TxIsolation.SERIALIZABLE))) {
            AccountModel innerAccount = accountRepo.lookupAccount(account.id).orElseThrow();
            innerAccount.setEmailAddress(innerEmails.get(counter.getAndSet(counter.get() + 1)));
            innerAccount.save();
            innerTransaction.commit();
          }

          outerAccount.setEmailAddress("updated@test.com");
          outerAccount.save();
        };

    assertThrows(
        SerializableConflictException.class,
        () -> transactionManager.executeWithRetry(modifyAccount));
    account.refresh();
    assertThat(innerEmails.get(1)).isEqualTo(account.getEmailAddress());
  }

  @Test
  public void executeWithRetry_runnable() {
    final String innerEmail = "inneremail@test.com";
    AtomicBoolean innerTransactionHasRun = new AtomicBoolean(false);
    // How many times setEmailAddress was successfully saved on outerAccount.
    // Helps verify the test setup works; in that .save() is throwing an
    // exception the first time.
    AtomicInteger outerSetNumberOfTime = new AtomicInteger();
    AccountModel initialAccount = new AccountModel().setEmailAddress("initial@test.com");
    initialAccount.insert();
    long accountId = initialAccount.id;

    transactionManager.executeWithRetry(
        () -> {
          AccountModel outerAccount = accountRepo.lookupAccount(accountId).orElseThrow();

          // Cause a concurrency error on the first run.
          if (!innerTransactionHasRun.getAndSet(true)) {
            // Update the account in a different Transaction (requiresNew)
            // before the current one finishes to trigger a serialization
            // exception in the outer transaction.
            try (Transaction innerTransaction =
                DB.beginTransaction(TxScope.requiresNew().setIsolation(TxIsolation.SERIALIZABLE))) {
              AccountModel innerAccount = accountRepo.lookupAccount(accountId).orElseThrow();
              innerAccount.setEmailAddress(innerEmail);
              innerAccount.save();
              innerTransaction.commit();
            }
          } else {
            // Verify the inner transaction change occurred and is visible on the retry.
            AccountModel innerAccount = accountRepo.lookupAccount(accountId).orElseThrow();
            assertThat(innerAccount.getEmailAddress()).isEqualTo(innerEmail);
          }

          outerAccount.setEmailAddress("updated@test.com");
          outerAccount.save();
          // The first time through save() will throw the exception due to
          // innerTransaction.  The second time will execute the entire method.
          // Counting here ensures that the test setup is correct.
          outerSetNumberOfTime.incrementAndGet();
        });

    initialAccount.refresh();
    assertThat(initialAccount.getEmailAddress()).isEqualTo("updated@test.com");
    assertThat(outerSetNumberOfTime.get()).isEqualTo(1);
  }

  /** Simulate when the work() supplier contains another transaction. */
  @Test
  public void execute_supplier_transactionInsideSupplierRollsBackIfWrappedTransactionFails() {
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

    transactionManager.execute(work, () -> "error");

    // Outside of both transactions, everything should be rolled back.
    assertThat(DB.find(AccountModel.class).findCount()).isEqualTo(0);
  }

  /** Simulate when we use the {@link TransactionManager} from inside another transaction. */
  @Test
  public void execute_supplier_workInSupplierRollsBackIfOuterTransactionFails() {
    Supplier<String> work =
        () -> {
          new AccountModel().insert();
          // Assert that from within this inner transaction, we see the new account
          assertThat(DB.find(AccountModel.class).findCount()).isEqualTo(1);
          return "done";
        };

    try (Transaction outerTransaction =
        DB.beginTransaction(TxScope.required().setIsolation(TxIsolation.SERIALIZABLE))) {
      transactionManager.execute(work, () -> "error");
      // Assert that, back in the outer transaction, we see the new account
      assertThat(DB.find(AccountModel.class).findCount()).isEqualTo(1);
      outerTransaction.rollback();
    }

    // Outside of both transactions, everything should be rolled back.
    assertThat(DB.find(AccountModel.class).findCount()).isEqualTo(0);
  }

  @Test
  public void execute_supplier_innerTransactionWithRequiresNewIsIndependent() {
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

    transactionManager.execute(work, () -> "error");

    // Outside of both transactions, we should see the new account
    assertThat(DB.find(AccountModel.class).findCount()).isEqualTo(1);
  }
}
