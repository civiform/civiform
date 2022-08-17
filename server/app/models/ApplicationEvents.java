package models;

import io.ebean.annotation.DbJson;
import io.ebean.annotation.WhenCreated;
import java.time.Instant;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import play.data.validation.Constraints;
import services.application.ApplicationEventDetails;

@Entity
@Table(name = "application_events")
public class ApplicationEvents extends BaseModel {
  @ManyToOne private Application application;
  @Constraints.Required private ApplicationEventDetails.Type eventType;
  // The Account that triggered the event.
  // @ManyToMany private Account actor;
  // eventType specific details of the event.
  @Constraints.Required @DbJson private ApplicationEventDetails details;
  @WhenCreated private Instant createTime;
}
