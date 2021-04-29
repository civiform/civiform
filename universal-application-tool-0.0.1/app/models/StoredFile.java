package models;

import java.net.URL;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import play.data.validation.Constraints;

/** The ebean mapped class for a file stored in AWS S3 */
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

  @Constraints.Required String name;

  public URL getPresignedURL() {
    return presignedURL;
  }

  public void setPresignedURL(URL presignedURL) {
    this.presignedURL = presignedURL;
  }

  @Transient URL presignedURL;
}
