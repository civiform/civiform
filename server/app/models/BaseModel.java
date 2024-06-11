package models;

import io.ebean.Model;
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
}
