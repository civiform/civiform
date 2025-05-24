package repository;

import static com.google.common.base.Preconditions.checkNotNull;

import io.ebean.DB;
import io.ebean.SerializableConflictException;
import io.ebean.Transaction;
import io.ebean.TxScope;
import io.ebean.annotation.TxIsolation;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class for managing workflows that span multiple repositories, but should still be wrapped in a
 * transaction to prevent race conditions and data integrity errors.
 *
 * <p>Warning: This class does not work across Async calls, they will not see the wrapping
 * transaction due to transactions being stored thread-local. <a
 * href="https://github.com/civiform/civiform/wiki/Concurrency-and-Transactions#wrapping-logic-in-transactions">Details
 * here</a>
 *
 * <p>See https://www.postgresql.org/docs/current/transaction-iso.html#XACT-SERIALIZABLE for details
 * on transactions.
 */
public final class TransactionManager {
  private static final Logger logger = LoggerFactory.getLogger(TransactionManager.class);

  /**
   * Run the supplied code in a {@link TxIsolation#SERIALIZABLE} transaction.
   *
   * <p>Notes:
   *
   * <ul>
   *   <li>If there is already an active transaction, it will be used, and any changes won't be
   *       committed until the outer transaction commits. Otherwise, a new transaction will be
   *       created.
   *   <li>If the transaction fails, any database changes will be rolled back, but any other side
   *       effects from the provided functions will persist.
   *   <li>If there is an outer transaction, it must have been created with a {@link TxScope} to
   *       avoid a runtime casting error.
   * </ul>
   *
   * @param synchronousWork the synchronous {@link Supplier} to run inside a transaction
   * @param <T> the return type of the suppliers
   */
  public <T> T execute(Supplier<T> synchronousWork) {
    return execute(TxScope.required(), synchronousWork);
  }

  // This is the main execution of caller code.  Other methods are wrappers
  // of this.
  private <T> T execute(TxScope scope, Supplier<T> synchronousWork) {
    checkNotNull(synchronousWork);
    try (Transaction transaction =
        DB.beginTransaction(scope.setIsolation(TxIsolation.SERIALIZABLE))) {
      T result = synchronousWork.get();
      transaction.commit();
      return result;
    }
  }

  /** Calls {@link #execute(Supplier)} but accepts a {@link Runnable}. */
  public void execute(Runnable synchronousWork) {
    execute(TxScope.required(), synchronousWork);
  }

  private void execute(TxScope scope, Runnable synchronousWork) {
    execute(
        scope,
        () -> {
          synchronousWork.run();
          return null;
        });
  }

  /**
   * Calls {@link #execute(Supplier)} returning its result, if it fails due to a concurrent
   * transaction the failure handler is called and its result is returned.
   *
   * @param onSerializationFailure {@link Supplier} to run in the event of a failure due to a
   *     conflict with a concurrent transaction
   */
  public <T> T execute(Supplier<T> synchronousWork, Supplier<T> onSerializationFailure) {
    checkNotNull(synchronousWork);
    checkNotNull(onSerializationFailure);
    try {
      return execute(synchronousWork);
    } catch (SerializableConflictException e) {
      logger.info(
          "Concurrent transaction occurred, falling back to failure handler: {}", e.getMessage());
      return onSerializationFailure.get();
    }
  }

  /** Calls {@link #execute(Supplier)} but makes two attempts before failing. */
  public <T> T executeWithRetry(Supplier<T> synchronousWork) {
    checkNotNull(synchronousWork);
    Optional<T> returnValue = Optional.empty();
    try {
      returnValue = Optional.of(execute(synchronousWork));
    } catch (SerializableConflictException sce) {
      // Log the exception and retry, allowing subsequent exceptions to be
      // surfaced.
      logRetriedException(sce);
    }

    return returnValue.orElseGet(() -> execute(synchronousWork));
  }

  /** Calls {@link #execute(Runnable)} but makes two attempts before failing. */
  public void executeWithRetry(Runnable synchronousWork) {
    checkNotNull(synchronousWork);
    try {
      execute(synchronousWork);
      return;
    } catch (SerializableConflictException sce) {
      // Log the exception and retry, allowing subsequent exceptions to be
      // surfaced.
      logRetriedException(sce);
    }

    execute(synchronousWork);
  }

  /** Throws {@code IllegalStateException} if a transaction is not present.
   * <p>
   * It would be cleaner if this were a method annotation but I/shane was
   * unable to get that to work.
   */
  public static void throwIfTransactionNotPresent() {
    if (DB.getDefault().currentTransaction() == null) {
      throw new IllegalStateException("A database transaction is required but not present");
    }
  }

  private void logRetriedException(SerializableConflictException sce) {
    logger.info(
        "DB concurrency collision occurred and retried. In isolation "
            + "this is working as intended. If there are more it may indicate a "
            + "coding error or a more severe issue.",
        sce);
  }
}
