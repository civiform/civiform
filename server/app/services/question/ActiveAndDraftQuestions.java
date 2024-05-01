package services.question;

import akka.japi.Pair;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Optional;
import java.util.function.Function;
import models.VersionModel;
import repository.VersionRepository;
import services.DeletionStatus;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;

/**
 * A data class storing the current active and draft questions. For efficient querying of
 * information about current active / draft questions which does not hit the database. Lifespan
 * should be measured in milliseconds - seconds at the maximum - within one request serving path -
 * because it does not have any mechanism for a refresh.
 */
public final class ActiveAndDraftQuestions {

  private final ImmutableList<QuestionDefinition> activeQuestions;
  private final ImmutableList<QuestionDefinition> draftQuestions;
  private final ImmutableMap<
          String, Pair<Optional<QuestionDefinition>, Optional<QuestionDefinition>>>
      versionedByName;
  private final ImmutableMap<String, DeletionStatus> deletionStatusByName;
  private final ImmutableMap<String, ImmutableSet<ProgramDefinition>>
      referencingDraftProgramsByName;
  private final ImmutableMap<String, ImmutableSet<ProgramDefinition>>
      referencingActiveProgramsByName;
  private final boolean draftVersionHasAnyEdits;

  /**
   * Queries the existing active and draft versions and builds a snapshotted view of the question
   * state.
   */
  public static ActiveAndDraftQuestions buildFromCurrentVersions(VersionRepository repository) {
    return new ActiveAndDraftQuestions(repository);
  }

  private ActiveAndDraftQuestions(VersionRepository repository) {
    VersionModel active = repository.getActiveVersion();
    VersionModel draft = repository.getDraftVersionOrCreate();
    VersionModel withDraftEdits = repository.previewPublishNewSynchronizedVersion();
    ImmutableMap<String, QuestionDefinition> activeNameToQuestion =
        repository.getQuestionDefinitionsForVersion(active).stream()
            .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getName, Function.identity()));
    this.activeQuestions = activeNameToQuestion.values().asList();

    ImmutableMap<String, QuestionDefinition> draftNameToQuestion =
        repository.getQuestionDefinitionsForVersion(draft).stream()
            .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getName, Function.identity()));
    this.draftQuestions = draftNameToQuestion.values().asList();

    versionedByName =
        Sets.union(activeNameToQuestion.keySet(), draftNameToQuestion.keySet()).stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    Function.identity(),
                    name -> {
                      return Pair.create(
                          Optional.ofNullable(activeNameToQuestion.get(name)),
                          Optional.ofNullable(draftNameToQuestion.get(name)));
                    }));

    this.draftVersionHasAnyEdits = draft.hasAnyChanges();
    this.referencingActiveProgramsByName = repository.buildReferencingProgramsMap(active);
    this.referencingDraftProgramsByName = repository.buildReferencingProgramsMap(withDraftEdits);

    ImmutableSet<String> tombstonedQuestionNames =
        ImmutableSet.copyOf(
            Sets.union(
                ImmutableSet.copyOf(draft.getTombstonedQuestionNames()),
                ImmutableSet.copyOf(active.getTombstonedQuestionNames())));
    this.deletionStatusByName =
        versionedByName.keySet().stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    questionName -> questionName,
                    questionName -> {
                      ImmutableMap<String, ImmutableSet<ProgramDefinition>> referencesToExamine =
                          draftVersionHasAnyEdits
                              ? referencingDraftProgramsByName
                              : referencingActiveProgramsByName;
                      if (!referencesToExamine
                          .getOrDefault(questionName, ImmutableSet.of())
                          .isEmpty()) {
                        return DeletionStatus.NOT_DELETABLE;
                      }
                      return tombstonedQuestionNames.contains(questionName)
                          ? DeletionStatus.PENDING_DELETION
                          : DeletionStatus.DELETABLE;
                    }));
  }

  public ImmutableList<QuestionDefinition> getActiveQuestions() {
    return activeQuestions;
  }

  public ImmutableList<QuestionDefinition> getDraftQuestions() {
    return draftQuestions;
  }

  public DeletionStatus getDeletionStatus(String questionName) {
    return deletionStatusByName.getOrDefault(questionName, DeletionStatus.NOT_ACTIVE);
  }

  public ImmutableSet<String> getQuestionNames() {
    return versionedByName.keySet();
  }

  public Optional<QuestionDefinition> getActiveQuestionDefinition(String name) {
    return versionedByName.containsKey(name) ? versionedByName.get(name).first() : Optional.empty();
  }

  public Optional<QuestionDefinition> getDraftQuestionDefinition(String name) {
    return versionedByName.containsKey(name)
        ? versionedByName.get(name).second()
        : Optional.empty();
  }

  public ReferencingPrograms getReferencingPrograms(String name) {
    return ReferencingPrograms.builder()
        .setActiveReferences(referencingActiveProgramsByName.getOrDefault(name, ImmutableSet.of()))
        .setDraftReferences(referencingDraftProgramsByName.getOrDefault(name, ImmutableSet.of()))
        .build();
  }

  public boolean draftVersionHasAnyEdits() {
    return draftVersionHasAnyEdits;
  }

  /** Contains sets of programs in the active and draft versions that reference a given question. */
  @AutoValue
  public abstract static class ReferencingPrograms {

    /** Returns a set of references to the question in the DRAFT version. */
    public abstract ImmutableSet<ProgramDefinition> draftReferences();

    /** Returns a set of references to the question in the ACTIVE version. */
    public abstract ImmutableSet<ProgramDefinition> activeReferences();

    static Builder builder() {
      return new AutoValue_ActiveAndDraftQuestions_ReferencingPrograms.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder setDraftReferences(ImmutableSet<ProgramDefinition> v);

      abstract Builder setActiveReferences(ImmutableSet<ProgramDefinition> v);

      abstract ReferencingPrograms build();
    }
  }
}
