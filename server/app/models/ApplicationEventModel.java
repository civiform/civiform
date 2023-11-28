package models;

import static com.google.common.base.Preconditions.checkNotNull;

import io.ebean.annotation.DbJson;
import io.ebean.annotation.WhenCreated;
import java.time.Instant;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import play.data.validation.Constraints;
import services.application.ApplicationEventDetails;

@Entity
@Table(name = "application_events")
public final class ApplicationEventModel extends BaseModel {

  // The Application the event is on.
  @ManyToOne private ApplicationModel application;
  // The {@code ApplicationEventDetails.Type} of the event.
  @Constraints.Required private ApplicationEventDetails.Type eventType;

  // The Account that triggered the event.
  @ManyToOne
  @JoinColumn(name = "creator_id")
  private AccountModel creator;

  // Details of the event specific to the eventType.
  @Constraints.Required @DbJson private ApplicationEventDetails details;
  @WhenCreated private Instant createTime;

  /**
   * Creates a representation of a single event happening to an Application.
   *
   * <p>The only time that 'creator' should ever be empty is when CiviForm is automatically
   * injecting an event, such as setting the status of an application to the default status.
   *
   * @param creator the Account that created the event.
   */
  public ApplicationEventModel(
      ApplicationModel application,
      Optional<AccountModel> creator,
      ApplicationEventDetails details) {
    this.application = checkNotNull(application);
    this.creator = checkNotNull(creator).orElse(null);
    this.details = checkNotNull(details);
    this.eventType = details.eventType();
  }

  public ApplicationModel getApplication() {
    return application;
  }

  public ApplicationEventModel setApplication(ApplicationModel application) {
    this.application = checkNotNull(application);
    return this;
  }

  public ApplicationEventDetails.Type getEventType() {
    return eventType;
  }

  public ApplicationEventModel setEventType(ApplicationEventDetails.Type eventType) {
    this.eventType = checkNotNull(eventType);
    return this;
  }

  public Optional<AccountModel> getCreator() {
    return Optional.ofNullable(creator);
  }

  public ApplicationEventModel setCreator(AccountModel creator) {
    this.creator = checkNotNull(creator);
    return this;
  }

  public ApplicationEventDetails getDetails() {
    return details;
  }

  public ApplicationEventModel setDetails(ApplicationEventDetails details) {
    this.details = checkNotNull(details);
    return this;
  }

  public Instant getCreateTime() {
    return createTime;
  }
}
