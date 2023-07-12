package models;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.ebean.annotation.DbArray;
import io.ebean.annotation.WhenModified;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import play.data.validation.Constraints;
import services.program.ProgramDefinition;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.QuestionDefinition;

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
public final class Version extends BaseModel {

  @Constraints.Required private LifecycleStage lifecycleStage;

  @ManyToMany(mappedBy = "versions")
  private List<Question> questions;

  /**
   * A tombstoned question is a question that will not be copied to the next version published. It
   * is set on the current draft version. Questions are listed here by name rather than by ID.
   */
  @DbArray private List<String> tombstonedQuestionNames = new ArrayList<>();

  @ManyToMany
  @JoinTable(name = "versions_programs")
  private List<Program> programs;

  /**
   * A tombstoned program is a program that will not be copied to the next version published. It is
   * set on the current draft version. Programs are listed here by name rather than by ID.
   */
  @DbArray private List<String> tombstonedProgramNames = new ArrayList<>();

  @WhenModified private Instant submitTime;

  public Version() {
    this(LifecycleStage.DRAFT);
  }

  public Version(LifecycleStage lifecycleStage) {
    this.lifecycleStage = lifecycleStage;
  }

  public LifecycleStage getLifecycleStage() {
    return lifecycleStage;
  }

  public Version setLifecycleStage(LifecycleStage lifecycleStage) {
    this.lifecycleStage = lifecycleStage;
    return this;
  }

  public Instant getSubmitTime() {
    return this.submitTime;
  }

  public Version addProgram(Program program) {
    this.programs.add(program);
    return this;
  }

  public boolean removeProgram(Program program) {
    return this.programs.remove(program);
  }

  public Version addQuestion(Question question) {
    this.questions.add(question);
    return this;
  }

  public boolean removeQuestion(Question question) {
    return this.questions.remove(question);
  }

  public ImmutableList<Program> getPrograms() {
    return ImmutableList.copyOf(programs);
  }

  public ImmutableList<Question> getQuestions() {
    return ImmutableList.copyOf(questions);
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

  /** Returns the names of all the programs. */
  public ImmutableSet<String> getProgramNames() {
    return getPrograms().stream()
        .map(Program::getProgramDefinition)
        .map(ProgramDefinition::adminName)
        .collect(ImmutableSet.toImmutableSet());
  }

  /** Returns the names of all the questions. */
  public ImmutableSet<String> getQuestionNames() {
    return getQuestions().stream()
        .map(Question::getQuestionDefinition)
        .map(QuestionDefinition::getName)
        .collect(ImmutableSet.toImmutableSet());
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

  /**
   * Attempts to mark the provided question as not eligible for copying to the next version.
   *
   * @return true if the question was successfully marked as tombstoned, false otherwise.
   * @throws QuestionNotFoundException if the question cannot be found in this version.
   */
  public boolean addTombstoneForQuestion(Question question) throws QuestionNotFoundException {
    String name = question.getQuestionDefinition().getName();
    if (!this.getQuestionNames().contains(name)) {
      throw new QuestionNotFoundException(question.getQuestionDefinition().getId());
    }
    if (this.tombstonedQuestionNames == null) {
      this.tombstonedQuestionNames = new ArrayList<>();
    }
    if (this.questionIsTombstoned(name)) {
      return false;
    }
    return this.tombstonedQuestionNames.add(name);
  }

  /**
   * Marks the provided question as eligible for copying to the next version.
   *
   * @return true if the question previously was tombstoned and false otherwise.
   */
  public boolean removeTombstoneForQuestion(Question question) {
    if (this.tombstonedQuestionNames == null) {
      this.tombstonedQuestionNames = new ArrayList<>();
    }
    return this.tombstonedQuestionNames.remove(question.getQuestionDefinition().getName());
  }

  /**
   * Attempts to mark the provided program as not eligible for copying to the next version.
   *
   * @return true if the program was successfully marked as tombstoned, false otherwise.
   */
  public boolean addTombstoneForProgramForTest(Program program) {
    if (this.tombstonedProgramNames == null) {
      this.tombstonedProgramNames = new ArrayList<>();
    }
    if (this.programIsTombstoned(program.getProgramDefinition().adminName())) {
      return false;
    }
    return this.tombstonedProgramNames.add(program.getProgramDefinition().adminName());
  }

  /**
   * Marks the provided program as eligible for copying to the next version.
   *
   * @return true if the program previously was tombstoned and false otherwise.
   */
  public boolean removeTombstoneForProgram(Program program) {
    if (this.tombstonedProgramNames == null) {
      this.tombstonedProgramNames = new ArrayList<>();
    }
    return this.tombstonedProgramNames.remove(program.getProgramDefinition().adminName());
  }

  /**
   * Returns whether any edits have been made within the version. Edits include marking a
   * question/program as tombstoned as well as creating a draft question/program.
   */
  public boolean hasAnyChanges() {
    return tombstonedQuestionNames.size() > 0
        || tombstonedProgramNames.size() > 0
        || questions.size() > 0
        || programs.size() > 0;
  }
}
