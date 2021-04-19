package models;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import play.data.validation.Constraints;

@Entity
@Table(name = "versions")
public class Version extends BaseModel {
  @Constraints.Required private LifecycleStage lifecycleStage;

  @ManyToMany(mappedBy = "versions")
  private List<Question> questions;

  @ManyToMany(mappedBy = "versions")
  private List<Program> programs;

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

  public Optional<Program> getProgramByName(String name) {
    return getPrograms().stream()
        .filter(p -> p.getProgramDefinition().adminName().equals(name))
        .findAny();
  }

  public Optional<Question> getQuestionByName(String name) {
    return getQuestions().stream()
        .filter(q -> q.getQuestionDefinition().getName().equals(name))
        .findAny();
  }
}
