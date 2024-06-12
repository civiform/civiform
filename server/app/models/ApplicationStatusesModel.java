package models;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.WhenCreated;
import java.time.Instant;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import play.data.validation.Constraints;
import services.program.StatusDefinitions;

/** An EBean class that stores all the statuses related to programs. */
@Entity
@Table(name = "application_statuses")
public class ApplicationStatusesModel extends BaseModel {
  private static final long serialVersionUID = 1L;
  @Constraints.Required private String programName;

  @Constraints.Required @DbJson private StatusDefinitions statusDefinitions;
  @WhenCreated private Instant createTime;
  @Constraints.Required private StatusDefinitionsLifecycleStage statusDefinitionsLifecycleStage;

  public ApplicationStatusesModel(
      String programName,
      StatusDefinitions statusDefinitions,
      StatusDefinitionsLifecycleStage statusDefinitionsLifecycleStage) {
    this.programName = checkNotNull(programName);
    this.statusDefinitions = checkNotNull(statusDefinitions);
    this.statusDefinitionsLifecycleStage = checkNotNull(statusDefinitionsLifecycleStage);
  }

  @VisibleForTesting
  public ApplicationStatusesModel setCreateTimeForTest(String createTimeString) {
    this.createTime = Instant.parse(createTimeString);
    return this;
  }

  public String getProgramName() {
    return programName;
  }

  public StatusDefinitions getStatusDefinitions() {
    return statusDefinitions;
  }

  public Instant getCreateTime() {
    return createTime;
  }

  public StatusDefinitionsLifecycleStage getStatusDefinitionsLifecycleStage() {
    return statusDefinitionsLifecycleStage;
  }
}
