package models;

import com.google.common.collect.ImmutableList;
import io.ebean.annotation.DbArray;
import io.ebean.annotation.WhenModified;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import play.data.validation.Constraints;

/**
 * An EBean mapped class that stores a reference object for coordinating the CiviForm data model.
 *
 * <p>A version object has a {@code LifecycleStage} (active, draft, obsolete, deleted) and a list of
 * programs and questions. There is exactly one active version at any given time, and at most one
 * draft - there are an arbitrary number of obsolete and deleted versions. Obsolete versions may be
 * reverted to - deleted ones will not be displayed under any circumstances.
 *
 * <p>Versions synchronize the CiviForm admin-configured data model, i.e. {@code Question}s and
 * {@code Programs}s, along with the resident and trusted intermediary-provided answers stored in
 * {@code ApplicantData}, {@code Applicant}, and {@code Application}.
 */
@Entity
@Table(name = "versions")
public class Version extends BaseModel {

  @Constraints.Required private LifecycleStage lifecycleStage;

  @ManyToMany(mappedBy = "versions")
  private List<Question> questions;

  /**
   * A tombstoned question or program is a question or program that will not be copied to the next
   * version published. It is set on the current draft version. Questions / Programs are listed here
   * by name rather than by ID.
   */
  @DbArray private List<String> tombstonedQuestionNames = new ArrayList<>();

  @ManyToMany(mappedBy = "versions")
  private List<Program> programs;

  /**
   * A tombstoned question or program is a question or program that will not be copied to the next
   * version published. It is set on the current draft version. Questions / Programs are listed here
   * by name rather than by ID.
   */
  @DbArray private List<String> tombstonedProgramNames = new ArrayList<>();

  @WhenModified private Instant submitTime;

  public Version() {
    this(LifecycleStage.DRAFT);
  }

  public Version(LifecycleStage lifecycleStage) {
    this.lifecycleStage = lifecycleStage;
  }

  public Version setLifecycleStage(LifecycleStage lifecycleStage) {
    this.lifecycleStage = lifecycleStage;
    return this;
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

  public ImmutableList<String> getTombstonedProgramNames() {
    if (this.tombstonedProgramNames == null) {
      return ImmutableList.of();
    }
    return ImmutableList.copyOf(this.tombstonedProgramNames);
  }

  public ImmutableList<String> getTombstonedQuestionNames() {
    if (this.tombstonedQuestionNames == null) {
      return ImmutableList.of();
    }
    return ImmutableList.copyOf(this.tombstonedQuestionNames);
  }

  /** Returns true if the question is not meant to be copied to the next version. */
  public boolean questionIsTombstoned(String questionName) {
    return this.getTombstonedQuestionNames().contains(questionName);
  }

  /** Returns true if the program is not meant to be copied to the next version. */
  public boolean programIsTombstoned(String programName) {
    return this.getTombstonedProgramNames().contains(programName);
  }

  public boolean addTombstoneForQuestion(Question question) {
    if (this.tombstonedQuestionNames == null) {
      this.tombstonedQuestionNames = new ArrayList<>();
    }
    if (this.questionIsTombstoned(question.getQuestionDefinition().getName())) {
      return false;
    }
    this.tombstonedQuestionNames.add(question.getQuestionDefinition().getName());
    return true;
  }

  public boolean removeTombstoneForQuestion(Question question) {
    if (this.tombstonedQuestionNames == null) {
      this.tombstonedQuestionNames = new ArrayList<>();
    }
    return this.tombstonedQuestionNames.remove(question.getQuestionDefinition().getName());
  }
}
