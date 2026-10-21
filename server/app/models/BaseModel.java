package models;

import io.ebean.Model;
import io.ebean.bean.EntityBean;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * Superclass for all CiviForm models. All database interaction is brokered through subclasses of
 * {@code BaseModel}.
 *
 * <p>Properties defined here are available in all models.
 */
@MappedSuperclass
public class BaseModel extends Model {

  @Id public Long id;

  /**
   * Marks this bean as dirty so that a save runs the update, and its @PreUpdate hooks, even if no
   * persisted property has changed.
   *
   * <p>This does the same thing as {@link io.ebean.Database#markAsDirty(Object)}, but without
   * looking up the default Database. Models are often built in memory where no Database is
   * registered, such as in unit tests.
   */
  @Override
  public void markAsDirty() {
    ((EntityBean) this)._ebean_getIntercept().setDirty(true);
  }
}
