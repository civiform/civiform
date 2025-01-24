package repository;

import io.ebean.DB;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;

public abstract class BaseRepository {
  public Transaction beginSerializedTransaction() {
    return DB.getDefault().beginTransaction(TxIsolation.SERIALIZABLE);
  }
}
