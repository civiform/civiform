package repository;

import io.ebean.DB;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.Supplier;

public class TransactionManager<T> {
  private Supplier<T> work;
  private Supplier<T> onFailure;

  public static <T> TransactionManager<T> newTransactionContext() {
    return new TransactionManager<>();
  }

  public TransactionManager<T> work(Supplier<T> work) {
    this.work = checkNotNull(work);
    return this;
  }

  public TransactionManager<T> onFailure(Supplier<T> onFailure) {
    this.onFailure = checkNotNull(onFailure);
    return this;
  }

  public T execute() {
    checkNotNull(this.work);
    checkNotNull(this.onFailure);
    try (Transaction transaction = DB.beginTransaction(TxIsolation.SERIALIZABLE)) {
      T result = work.get();
      transaction.commit();
      return result;
    } catch (Exception e) {
      return onFailure.get();
    }
  }

  public static <T> T executeInTransaction(Supplier<T> work, Supplier<T> onFailure) {
    try (Transaction transaction = DB.beginTransaction(TxIsolation.SERIALIZABLE)) {
      T result = work.get();
      transaction.commit();
      return result;
    } catch (Exception e) {
      return onFailure.get();
    }
  }
}
