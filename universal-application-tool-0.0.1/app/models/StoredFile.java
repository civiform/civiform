package models;

import javax.persistence.Entity;
import javax.persistence.Table;
import play.data.validation.Constraints;

/** The EBean mapped class for a file stored in AWS S3 */
@Entity
@Table(name = "files")
public class StoredFile extends BaseModel {
  private static final long serialVersionUID = 1L;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUserFileName() {
    return userFileName;
  }

  public void setUserFileName(String userFileName) {
    this.userFileName = userFileName;
  }

  @Constraints.Required String name;
  String userFileName;
}
