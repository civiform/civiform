package models;

import io.ebean.annotation.DbJsonB;
import java.util.Map;
import javax.persistence.Entity;
import play.data.validation.Constraints;

@Entity
public class Question extends BaseModel {
  private static final long serialVersionUID = 1;

  @Constraints.Required @DbJsonB
  // When we build an object that Jackson can deserialize, we replace Map<String, Object> with that
  // type.
  // For now, this will be automatically deserialized - with subobjects being "Map<String, Object>"
  // and lists
  // being List<Object>.
  public Map<String, Object> object;

  // Play will autogenerate getters and setters, unless it detects that any have been written.
}
