package services.question;

import akka.japi.Pair;
import com.google.auto.value.AutoValue;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import models.Program;
import models.Question;
import models.Version;
import repository.VersionRepository;
import services.DeletionStatus;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
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
  ;
  private final ImmutableMap<
          String, Pair<Optional<QuestionDefinition>, Optional<QuestionDefinition>>>
      versionedByName;
  private final ImmutableMap<String, DeletionStatus> deletionStatusByName;
  private final ImmutableMap<Long, ImmutableSet<ProgramDefinition>> referencingDraftProgramsById;
  private final ImmutableMap<Long, ImmutableSet<ProgramDefinition>> referencingActiveProgramsById;
  private final boolean draftVersionHasAnyEdits;

  /**
   * Queries the existing active and draft versions and builds a snapshotted view of the question
   * state.
   */
  public static ActiveAndDraftQuestions buildFromCurrentVersions(VersionRepository repository) {
    return new ActiveAndDraftQuestions(
        repository.getActiveVersion(),
        repository.getDraftVersion(),
        repository.previewPublishNewSynchronizedVersion());
  }

  private ActiveAndDraftQuestions(Version active, Version draft, Version withDraftEdits) {
    ImmutableMap<String, QuestionDefinition> activeNameToQuestion =
        active.getQuestions().stream()
            .map(Question::getQuestionDefinition)
            .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getName, Function.identity()));
    this.activeQuestions = activeNameToQuestion.values().asList();

    ImmutableMap<String, QuestionDefinition> draftNameToQuestion =
        draft.getQuestions().stream()
            .map(Question::getQuestionDefinition)
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
    this.referencingActiveProgramsById = buildReferencingProgramsMap(active);
    this.referencingDraftProgramsById =
        draftVersionHasAnyEdits ? buildReferencingProgramsMap(withDraftEdits) : ImmutableMap.of();

    ImmutableSet<String> tombstonedQuestionNames =
        ImmutableSet.copyOf(
            Sets.union(
                ImmutableSet.copyOf(draft.getTombstonedQuestionNames()),
                ImmutableSet.copyOf(active.getTombstonedQuestionNames())));
    ImmutableMap<String, Long> latestDefinitionId =
        versionedByName.entrySet().stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    Map.Entry::getKey,
                    entry -> {
                      Optional<QuestionDefinition> draftQ = entry.getValue().second();
                      Optional<QuestionDefinition> activeQ = entry.getValue().first();
                      return draftQ.orElseGet(activeQ::get).getId();
                    }));
    this.deletionStatusByName =
        versionedByName.keySet().stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    Functions.identity(),
                    questionName -> {
                      ImmutableMap<Long, ImmutableSet<ProgramDefinition>> referencesToExamine =
                          draftVersionHasAnyEdits
                              ? referencingDraftProgramsById
                              : referencingActiveProgramsById;
                      long questionId = latestDefinitionId.get(questionName);
                      if (!referencesToExamine
                          .getOrDefault(questionId, ImmutableSet.of())
                          .isEmpty()) {
                        return DeletionStatus.NOT_DELETABLE;
                      }
                      return tombstonedQuestionNames.contains(questionName)
                          ? DeletionStatus.PENDING_DELETION
                          : DeletionStatus.DELETABLE;
                    }));
  }

  /**
   * Inspects the provided version and returns a map who's key is the question name and value is a
   * set of programs that reference the given question in this version.
   */
  private static ImmutableMap<Long, ImmutableSet<ProgramDefinition>> buildReferencingProgramsMap(
      Version version) {
    Map<Long, Set<ProgramDefinition>> result = Maps.newHashMap();
    for (Program program : version.getPrograms()) {
      ImmutableList<Long> programQuestionIds =
          program.getProgramDefinition().blockDefinitions().stream()
              .map(BlockDefinition::programQuestionDefinitions)
              .flatMap(ImmutableList::stream)
              .map(ProgramQuestionDefinition::id)
              .collect(ImmutableList.toImmutableList());
      for (Long questionId : programQuestionIds) {
        if (!result.containsKey(questionId)) {
          result.put(questionId, Sets.newHashSet());
        }
        result.get(questionId).add(program.getProgramDefinition());
      }
    }
    return result.entrySet().stream()
        .collect(
            ImmutableMap.toImmutableMap(Entry::getKey, e -> ImmutableSet.copyOf(e.getValue())));
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

  public ReferencingPrograms getReferencingPrograms(long questionId) {
    return ReferencingPrograms.builder()
        .setActiveReferences(
            referencingActiveProgramsById.getOrDefault(questionId, ImmutableSet.of()))
        .setDraftReferences(
            referencingDraftProgramsById.getOrDefault(questionId, ImmutableSet.of()))
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
