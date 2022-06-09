package models;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.StoredFileAcls;
import io.ebean.annotation.DbJsonB;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.Table;
import play.data.validation.Constraints;

/** The EBean mapped class for a file stored in AWS S3 */
@Entity
@Table(name = "files")
public class StoredFile extends BaseModel {
  private static final long serialVersionUID = 1L;

  /** ACLs for accessing this file. */
  @DbJsonB private StoredFileAcls acls;

  public StoredFile(StoredFileAcls acls) {
    this.acls = checkNotNull(acls);
  }

  public StoredFile() {
    this(new StoredFileAcls());
  }

  public StoredFileAcls getAcls() {
    return acls;
  }

  public StoredFile setAcls(StoredFileAcls acls) {
    this.acls = acls;
    return this;
  }

  public String getName() {
    return name;
  }

  public StoredFile setName(String name) {
    this.name = name;
    return this;
  }

  public Optional<String> getOriginalFileName() {
    return Optional.ofNullable(originalFileName);
  }

  public StoredFile setOriginalFileName(String originalFileName) {
    this.originalFileName = originalFileName;
    return this;
  }

  @Constraints.Required String name;
  String originalFileName;
}
