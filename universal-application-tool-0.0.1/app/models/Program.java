package models;

import io.ebean.annotation.DbJsonB;
import io.ebean.text.json.EJson;
import java.io.IOException;
import java.util.Map;
import javax.persistence.Entity;
import play.data.validation.Constraints;

@Entity
/** The ebeans mapped class for the program object. */
public class Program {
  private static final long serialVersionUID = 1;

  @Constraints.Required @DbJsonB
  // When we build an object that Jackson can deserialize, we replace Map<String, Object> with that
  // type.
  // For now, this will be automatically deserialized - with subobjects being "Map<String, Object>"
  // and lists being List<Object>.
  public Map<String, Object> object;

  @Constraints.Required public String name;

  @Constraints.Required public long version;

  // This is where we write methods on the program - possibly resurfacing methods on the Jackson
  // object.
  // Play will autogenerate getters and setters, unless it detects that any have been written.

  public String objectAsJsonString() throws IOException {
    return EJson.write(object);
  }
}
