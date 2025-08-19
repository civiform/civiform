package models;

import io.ebean.annotation.WhenCreated;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "Account_deletion_records")
public class AccountDeletionRecords extends BaseModel {
  private static final long serialVersionUID = 1L;
  private long accountId;
  private List<String> applicantIds;
  private String deletionTrigger;
  @WhenCreated private Instant deleteTime;

  public AccountDeletionRecords(long accountId, List<String> applicantIds, String deletionTrigger) {
    this.accountId = accountId;
    this.applicantIds = applicantIds;
    this.deletionTrigger = deletionTrigger;
  }
}
