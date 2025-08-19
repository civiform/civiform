package models;

import java.time.Instant;

public class ApplicationDeletionRecord extends BaseModel {
  private static final long serialVersionUID = 1L;
  private long applicationId;
  private String programName;
  private String deletionTrigger;
  private Instant deleteTime;
}
