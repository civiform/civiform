package services.question;

import akka.japi.Pair;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    return new ActiveAndDraftQuestions(
        repository.getActiveVersion(),
        repository.getDraftVersion(),
        repository.previewPublishNewSynchronizedVersion());
  }

  private ActiveAndDraftQuestions(Version active, Version draft, Version withDraftEdits) {
    ImmutableMap<String, QuestionDefinition> activeNames =
        active.getQuestions().stream()
            .map(Question::getQuestionDefinition)
            .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getName, qd -> qd));
    ImmutableMap<String, QuestionDefinition> draftNames =
        draft.getQuestions().stream()
            .map(Question::getQuestionDefinition)
            .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getName, qd -> qd));
    versionedByName =
        Sets.union(activeNames.keySet(), draftNames.keySet()).stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    name -> name,
                    name -> {
                      return Pair.create(
                          Optional.ofNullable(activeNames.get(name)),
                          Optional.ofNullable(draftNames.get(name)));
                    }));

    draftVersionHasAnyEdits = draft.hasAnyChanges();
    referencingActiveProgramsByName = buildReferencingProgramsMap(active);
    referencingDraftProgramsByName =
        draftVersionHasAnyEdits ? buildReferencingProgramsMap(withDraftEdits) : ImmutableMap.of();

    ImmutableSet<String> tombstonedQuestionNames =
        ImmutableSet.copyOf(
            Sets.union(
                ImmutableSet.copyOf(draft.getTombstonedQuestionNames()),
                ImmutableSet.copyOf(active.getTombstonedQuestionNames())));
    deletionStatusByName =
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

  /**
   * Inspects the provided version and returns a map who's key is the question name and value is a
   * set of programs that reference the given question in this version.
   */
  private static ImmutableMap<String, ImmutableSet<ProgramDefinition>> buildReferencingProgramsMap(
      Version version) {
    // Different versions of a question can have distinct IDs while still
    // retaining the same "name". A given program has a reference only to a specific
    // question ID. This map allows us to easily cache the mapping from a question ID
    // to a logical question "name".
    ImmutableMap<Long, String> questionIdToNameLookup =
        version.getQuestions().stream()
            .map(Question::getQuestionDefinition)
            .collect(
                ImmutableMap.toImmutableMap(
                    QuestionDefinition::getId, QuestionDefinition::getName));
    Map<String, Set<ProgramDefinition>> result = Maps.newHashMap();
    for (Program program : version.getPrograms()) {
      ImmutableList<String> programQuestionNames =
          program.getProgramDefinition().blockDefinitions().stream()
              .map(BlockDefinition::programQuestionDefinitions)
              .flatMap(ImmutableList::stream)
              .map(ProgramQuestionDefinition::id)
              .filter(questionIdToNameLookup::containsKey)
              .map(questionIdToNameLookup::get)
              .collect(ImmutableList.toImmutableList());
      for (String questionName : programQuestionNames) {
        if (!result.containsKey(questionName)) {
          result.put(questionName, Sets.newHashSet());
        }
        result.get(questionName).add(program.getProgramDefinition());
      }
    }
    return result.entrySet().stream()
        .collect(
            ImmutableMap.toImmutableMap(e -> e.getKey(), e -> ImmutableSet.copyOf(e.getValue())));
  }

  public DeletionStatus getDeletionStatus(String questionName) {
    return this.deletionStatusByName.getOrDefault(questionName, DeletionStatus.NOT_ACTIVE);
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
