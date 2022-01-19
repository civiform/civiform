package models;

import java.util.Optional;
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

  public Optional<String> getOriginalFileName() {
    if (originalFileName == null) {
      return Optional.empty();
    }
    return Optional.of(originalFileName);
  }

  public void setOriginalFileName(String originalFileName) {
    this.originalFileName = originalFileName;
  }

  @Constraints.Required String name;
  String originalFileName;
}
