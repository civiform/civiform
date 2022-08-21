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
import services.application.ApplicationEventDetails.Type;

@Entity
@Table(name = "application_events")
public class ApplicationEvent extends BaseModel {

  @ManyToOne private Application application;
  @Constraints.Required private Type eventType;
  // The Account that triggered the event.
  @ManyToOne
  @JoinColumn(name = "actor_id")
  private Account actor;
  // eventType specific details of the event.
  @Constraints.Required @DbJson private ApplicationEventDetails details;
  @WhenCreated private Instant createTime;

  public ApplicationEvent() {}

  public Account getActor() {
    return actor;
  }

  public ApplicationEvent setActor(Account actor) {
    this.actor = actor;
    return this;
  }

  public Application getApplication() {
    return application;
  }

  public ApplicationEvent setApplication(Application application) {
    this.application = application;
    return this;
  }

  public Type getEventType() {
    return eventType;
  }

  public ApplicationEvent setEventType(Type eventType) {
    this.eventType = eventType;
    return this;
  }

  public ApplicationEventDetails getDetails() {
    return details;
  }

  public ApplicationEvent setDetails(ApplicationEventDetails details) {
    this.details = details;
    return this;
  }

  public ApplicationEvent(
      Application application, Account actor, Type eventType, ApplicationEventDetails details) {
    this.application = application;
    this.actor = actor;
    this.eventType = eventType;
    this.details = details;
  }
  // public static ApplicationEvent create(Application application,Type eventType,
  // ApplicationEventDetails details) {
  //  ApplicationEvent event = new ApplicationEvent(application,eventType, details);
  //  event.save();
  //  return event;
  // }
}
