package models;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import io.ebean.annotation.DbJsonB;
import io.ebean.annotation.WhenCreated;
import java.time.Instant;
import javax.persistence.Entity;
import javax.persistence.Table;
import play.data.validation.Constraints;

/**
 * An EBean mapped class that stores a snapshot of the admin-writeable CiviForm server settings.
 *
 * <p>Each time an admin updates the server settings using the admin UI, a SettingsGroup is saved.
 * The latest snapshot is used to provide settings for a given request to the server.
 */
@Entity
@Table(name = "civiform_settings")
public class SettingsGroupModel extends BaseModel {

  @DbJsonB private ImmutableMap<String, String> settings;

  @WhenCreated private Instant createTime;

  @Constraints.Required private String createdBy;

  public ImmutableMap<String, String> getSettings() {
    return settings;
  }

  public Instant getCreateTime() {
    return createTime;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  @VisibleForTesting
  public SettingsGroupModel setCreateTimeForTest(String createTimeString) {
    this.createTime = Instant.parse(createTimeString);
    return this;
  }

  public SettingsGroupModel(ImmutableMap<String, String> settings, String createdBy) {
    this.settings = checkNotNull(settings);
    this.createdBy = createdBy;
  }
}
