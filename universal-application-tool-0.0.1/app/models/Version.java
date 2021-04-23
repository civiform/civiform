package models;

import com.google.common.collect.ImmutableList;
import io.ebean.annotation.UpdatedTimestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import play.data.validation.Constraints;

/**
 * A version object has a lifecycle stage (active, draft, obsolete, deleted) and a list of programs
 * and questions. There is exactly one active version at any given time, and at most one draft -
 * there are an arbitrary number of obsolete and deleted versions. Obsolete versions may be reverted
 * to - deleted ones will not be displayed under any circumstances.
 */
@Entity
@Table(name = "versions")
public class Version extends BaseModel {
  @Constraints.Required private LifecycleStage lifecycleStage;

  @ManyToMany(mappedBy = "versions")
  private List<Question> questions;

  @ManyToMany(mappedBy = "versions")
  private List<Program> programs;

  @UpdatedTimestamp private Instant submitTime;

  public Version() {
    this(LifecycleStage.DRAFT);
  }

  public Version(LifecycleStage lifecycleStage) {
    this.lifecycleStage = lifecycleStage;
  }

  public void setLifecycleStage(LifecycleStage lifecycleStage) {
    this.lifecycleStage = lifecycleStage;
  }

  public ImmutableList<Program> getPrograms() {
    return ImmutableList.copyOf(programs);
  }

  public ImmutableList<Question> getQuestions() {
    return ImmutableList.copyOf(questions);
  }

  public LifecycleStage getLifecycleStage() {
    return lifecycleStage;
  }

  public Instant getSubmitTime() {
    return this.submitTime;
  }

  /**
   * If a program by the given name exists, return it. A maximum of one program by a given name can
   * exist in a version.
   */
  public Optional<Program> getProgramByName(String name) {
    return getPrograms().stream()
        .filter(p -> p.getProgramDefinition().adminName().equals(name))
        .findAny();
  }

  /**
   * If a question by the given name exists, return it. A maximum of one question by a given name
   * can exist in a version.
   */
  public Optional<Question> getQuestionByName(String name) {
    return getQuestions().stream()
        .filter(q -> q.getQuestionDefinition().getName().equals(name))
        .findAny();
  }
}
