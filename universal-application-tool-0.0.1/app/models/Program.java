package models;

import io.ebean.annotation.DbJsonB;
import io.ebean.text.json.EJson;
import java.io.IOException;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Table;
import play.data.validation.Constraints;

@Entity
@Table(name = "programs")
/** The ebeans mapped class for the program object. */
public class Program {
  private static final long serialVersionUID = 1L;

  public Map<String, Object> getObject() {
    return object;
  }

  public void setObject(Map<String, Object> object) {
    this.object = object;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  @Constraints.Required @DbJsonB
  // When we build an object that Jackson can deserialize, we replace Map<String, Object> with that
  // type.
  // For now, this will be automatically deserialized - with subobjects being "Map<String, Object>"
  // and lists being List<Object>.
  Map<String, Object> object;

  @Constraints.Required String name;

  @Constraints.Required long version;

  // This is where we write methods on the program - possibly resurfacing methods on the Jackson
  // object.

  public String objectAsJsonString() throws IOException {
    return EJson.write(object);
  }
}
