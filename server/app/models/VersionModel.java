package models;

import com.google.common.collect.ImmutableList;
import io.ebean.annotation.DbArray;
import io.ebean.annotation.WhenModified;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
public final class VersionModel extends BaseModel {

  @Constraints.Required private LifecycleStage lifecycleStage;

  @ManyToMany(mappedBy = "versions")
  @JoinTable(
      name = "versions_questions",
      joinColumns = @JoinColumn(name = "versions_id"),
      inverseJoinColumns = @JoinColumn(name = "questions_id"))
  private List<QuestionModel> questions;

  /**
   * A tombstoned question is a question that will not be copied to the next version published. It
   * is set on the current draft version. Questions are listed here by name rather than by ID.
   */
  @DbArray private List<String> tombstonedQuestionNames = new ArrayList<>();

  @ManyToMany(mappedBy = "versions")
  @JoinTable(
      name = "versions_programs",
      joinColumns = @JoinColumn(name = "versions_id"),
      inverseJoinColumns = @JoinColumn(name = "programs_id"))
  private List<ProgramModel> programs;

  /**
   * A tombstoned program is a program that will not be copied to the next version published. It is
   * set on the current draft version. Programs are listed here by name rather than by ID.
   */
  @DbArray private List<String> tombstonedProgramNames = new ArrayList<>();

  @WhenModified private Instant submitTime;

  public VersionModel() {
    this(LifecycleStage.DRAFT);
  }

  public VersionModel(LifecycleStage lifecycleStage) {
    this.lifecycleStage = lifecycleStage;
  }

  public LifecycleStage getLifecycleStage() {
    return lifecycleStage;
  }

  public VersionModel setLifecycleStage(LifecycleStage lifecycleStage) {
    this.lifecycleStage = lifecycleStage;
    return this;
  }

  public Instant getSubmitTime() {
    return this.submitTime;
  }

  public VersionModel addProgram(ProgramModel program) {
    this.programs.add(program);
    return this;
  }

  public boolean removeProgram(ProgramModel program) {
    return this.programs.remove(program);
  }

  public VersionModel addQuestion(QuestionModel question) {
    this.questions.add(question);
    return this;
  }

  public boolean removeQuestion(QuestionModel question) {
    return this.questions.remove(question);
  }

  /**
   * Returns all programs of a given version. Instead of calling this function directly,
   * getProgramsForVersion should be called, since that will implement caching.
   */
  public ImmutableList<ProgramModel> getPrograms() {
    return ImmutableList.copyOf(programs);
  }

  /**
   * Returns all questions of a given version. Instead of calling this function directly,
   * getQuestionsForVersion should be called, since that will implement caching.
   */
  public ImmutableList<QuestionModel> getQuestions() {
    return ImmutableList.copyOf(questions);
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
   */
  public boolean addTombstoneForQuestion(String questionName) {
    if (this.tombstonedQuestionNames == null) {
      this.tombstonedQuestionNames = new ArrayList<>();
    }
    if (this.questionIsTombstoned(questionName)) {
      return false;
    }
    return this.tombstonedQuestionNames.add(questionName);
  }

  /**
   * Marks the provided question as eligible for copying to the next version.
   *
   * @return true if the question previously was tombstoned and false otherwise.
   */
  public boolean removeTombstoneForQuestion(QuestionModel question) {
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
  public boolean addTombstoneForProgramForTest(ProgramModel program) {
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
  public boolean removeTombstoneForProgram(ProgramModel program) {
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
