package models;

import static org.checkerframework.errorprone.com.google.common.base.Preconditions.checkNotNull;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.google.common.base.Preconditions;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.DbJsonB;
import play.data.validation.Constraints;
import services.program.StatusDefinitions;

import java.util.Optional;

/**
 * An EBean mapped class that represents status definition associated with a program There is a
 * boolean value, isArchieved associated which gives a clear indication whether the current status
 * associated with the program is active or not
 */
@Entity
@Table(name = "statuses")
public class StatusesModel extends BaseModel {

  @Constraints.Required private String name;
  @Constraints.Required @DbJson private StatusDefinitions statusDefinitions;
  @Constraints.Required private boolean isarchived;

  public StatusesModel(
      String name, StatusDefinitions statusDefinitions, boolean isarchieved) {
    this.name = checkNotNull(name);
    this.statusDefinitions = checkNotNull(statusDefinitions);
    this.isarchived = isarchieved;
    this.save();
  }

  public StatusDefinitions getStatusDefinitions() {
    return Preconditions.checkNotNull(this.statusDefinitions);
  }

  public Optional<StatusDefinitions.Status> getDefaultStatus() {
    return this.statusDefinitions.getStatuses().stream()
      .filter(StatusDefinitions.Status::computedDefaultStatus)
      .findFirst();
  }
}
