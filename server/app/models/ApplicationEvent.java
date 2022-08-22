package models;

import io.ebean.annotation.DbJson;
import io.ebean.annotation.WhenCreated;
import java.time.Instant;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import play.data.validation.Constraints;
import services.application.ApplicationEventDetails;

@Entity
@Table(name = "application_events")
public class ApplicationEvent extends BaseModel {

  // The Application the event is on.
  @ManyToOne private Application application;
  // The {@code ApplicationEventDetails.Type} of the event.
  @Constraints.Required private ApplicationEventDetails.Type eventType;
  // The Account that triggered the event.
  @ManyToOne
  @JoinColumn(name = "actor_id")
  private Account actor;
  // Details of the event specific to the eventType.
  @Constraints.Required @DbJson private ApplicationEventDetails details;
  @WhenCreated private Instant createTime;

  public ApplicationEvent(
      Application application,
      Account actor,
      ApplicationEventDetails.Type eventType,
      ApplicationEventDetails details) {
    this.application = application;
    this.actor = actor;
    this.eventType = eventType;
    this.details = details;
  }

  public Application getApplication() {
    return application;
  }

  public ApplicationEvent setApplication(Application application) {
    this.application = application;
    return this;
  }

  public ApplicationEventDetails.Type getEventType() {
    return eventType;
  }

  public ApplicationEvent setEventType(ApplicationEventDetails.Type eventType) {
    this.eventType = eventType;
    return this;
  }

  public Account getActor() {
    return actor;
  }

  public ApplicationEvent setActor(Account actor) {
    this.actor = actor;
    return this;
  }

  public ApplicationEventDetails getDetails() {
    return details;
  }

  public ApplicationEvent setDetails(ApplicationEventDetails details) {
    this.details = details;
    return this;
  }

  public Instant getCreateTime() {
    return createTime;
  }
}
