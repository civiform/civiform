package models;

import io.ebean.annotation.DbJsonB;
import io.ebean.text.json.EJson;
import java.io.IOException;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import play.data.validation.Constraints;

@Entity
@Table(name = "programs")
/** The ebeans mapped class for the program object. */
public class Program extends BaseModel {

  @Constraints.Required public String name;

  @Constraints.Required @DbJsonB public BlockContainer blocks;
}
