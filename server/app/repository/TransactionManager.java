package repository;

import io.ebean.DB;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import java.util.function.Supplier;

public class TransactionManager {
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
