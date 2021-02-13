package models;

import java.io.File;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import play.data.validation.Constraints;

/** The ebean mapped class for a file stored in AWS S3 */
@Entity
@Table(name = "files")
public class File extends BaseModel {
  private static final long serialVersionUID = 1L;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Constraints.Required String name;

  @Transient @Constraints.Required File file;
}
