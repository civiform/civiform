package models;

import io.ebean.annotation.WhenCreated;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "Application_deletion_records")
public class ApplicationDeletionRecords extends BaseModel {
  private static final long serialVersionUID = 1L;
  private long applicationId;
  private String programName;
  private String deletionTrigger;
  @WhenCreated private Instant deleteTime;

  public ApplicationDeletionRecord(long applicationId, String programName, String deletionTrigger) {
    this.applicationId = applicationId;
    this.programName = programName;
    this.deletionTrigger = deletionTrigger;
    deleteTime = Instant.now();
  }
}
