package models;

import io.ebean.annotation.DbJsonB;
import io.ebean.text.json.EJson;
import java.io.IOException;
import java.util.Map;
import javax.persistence.Entity;
import play.data.validation.Constraints;

@Entity
/** The ebeans mapped class that represents an individual applicant */
public class Applicant extends BaseModel {
  private static final long serialVersionUID = 1L;

  public Map<String, Object> getObject() {
    return object;
  }

  public void setObject(Map<String, Object> object) {
    this.object = object;
  }

  @Constraints.Required @DbJsonB
  // When we build an object that Jackson can deserialize, we replace Map<String, Object> with that
  // type.
  // For now, this will be automatically deserialized - with subobjects being "Map<String, Object>"
  // and lists being List<Object>.
  Map<String, Object> object;

  public String objectAsJsonString() throws IOException {
    return EJson.write(object);
  }
}
