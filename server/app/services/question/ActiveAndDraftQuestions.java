package services.question;

import akka.japi.Pair;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
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
  private final Optional<ImmutableMap<String, ImmutableSet<ReferencingProgram>>>
      referencingDraftProgramsByName;
  private final ImmutableMap<String, ImmutableSet<ReferencingProgram>>
      referencingActiveProgramsByName;

  public ActiveAndDraftQuestions(VersionRepository repository) {
    Version active = repository.getActiveVersion();
    Version draft = repository.getDraftVersion();
    Version withEditsDraft = repository.previewPublishNewSynchronizedVersion();
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

    referencingActiveProgramsByName = buildReferencingProgramsMap(active);
    boolean draftHasEdits = draft.getPrograms().size() > 0 || draft.getQuestions().size() > 0;
    if (draftHasEdits) {
      referencingDraftProgramsByName = Optional.of(buildReferencingProgramsMap(withEditsDraft));
    } else {
      referencingDraftProgramsByName = Optional.empty();
    }

    deletionStatusByName =
        activeNames.keySet().stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    questionName -> questionName,
                    questionName -> {
                      if (draft.getTombstonedQuestionNames().contains(questionName)) {
                        return DeletionStatus.PENDING_DELETION;
                      } else if (isNotDeletable(
                          active, draft, activeNames, draftNames, questionName)) {
                        return DeletionStatus.NOT_DELETABLE;
                      } else {
                        return DeletionStatus.DELETABLE;
                      }
                    }));
  }

  private static ImmutableMap<String, ImmutableSet<ReferencingProgram>> buildReferencingProgramsMap(
      Version version) {
    ImmutableMap<Long, String> questionIdToNameLookup =
        version.getQuestions().stream()
            .map(Question::getQuestionDefinition)
            .collect(
                ImmutableMap.toImmutableMap(
                    QuestionDefinition::getId, QuestionDefinition::getName));
    Map<String, Set<ReferencingProgram>> result = Maps.newHashMap();
    for (Program program : version.getPrograms()) {
      ProgramDefinition programDefinition = program.getProgramDefinition();
      ImmutableList<Pair<String, BlockDefinition>> referencedQuestions =
          buildReferencedQuestions(programDefinition, questionIdToNameLookup);
      for (Pair<String, BlockDefinition> referencedQuestion : referencedQuestions) {
        if (!result.containsKey(referencedQuestion.first())) {
          result.put(referencedQuestion.first(), Sets.newHashSet());
        }
        result
            .get(referencedQuestion.first())
            .add(
                ReferencingProgram.builder()
                    .setProgramDefinition(programDefinition)
                    .setBlockDefinitionId(referencedQuestion.second().id())
                    .build());
      }
    }
    return result.entrySet().stream()
        .collect(
            ImmutableMap.toImmutableMap(e -> e.getKey(), e -> ImmutableSet.copyOf(e.getValue())));
  }

  private static ImmutableList<Pair<String, BlockDefinition>> buildReferencedQuestions(
      ProgramDefinition program, ImmutableMap<Long, String> questionIdToNameLookup) {
    ImmutableList.Builder<Pair<String, BlockDefinition>> resultBuilder = ImmutableList.builder();
    for (BlockDefinition block : program.blockDefinitions()) {
      for (ProgramQuestionDefinition pqd : block.programQuestionDefinitions()) {
        if (!questionIdToNameLookup.containsKey(pqd.id())) {
          continue;
        }
        String questionName = questionIdToNameLookup.get(pqd.id());
        resultBuilder.add(Pair.create(questionName, block));
      }
    }

    return resultBuilder.build();
  }

  private static boolean isNotDeletable(
      Version active,
      Version draft,
      ImmutableMap<String, QuestionDefinition> activeNames,
      ImmutableMap<String, QuestionDefinition> draftNames,
      String questionName) {
    return Streams.concat(active.getPrograms().stream(), draft.getPrograms().stream())
        .anyMatch(
            program -> {
              QuestionDefinition activeQuestion = activeNames.get(questionName);
              QuestionDefinition draftQuestion = draftNames.get(questionName);
              ProgramDefinition programDefinition = program.getProgramDefinition();
              if (activeQuestion != null && programDefinition.hasQuestion(activeQuestion)) {
                return true;
              } else return draftQuestion != null && programDefinition.hasQuestion(draftQuestion);
            });
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
        .setDraftReferences(
            referencingDraftProgramsByName.map(r -> r.getOrDefault(name, ImmutableSet.of())))
        .build();
  }

  /** Contains sets of programs in the active and draft versions that reference a given question. */
  @AutoValue
  public abstract static class ReferencingPrograms {

    /**
     * Returns a set of references to the question in the DRAFT version. This returns
     * Optional.empty() if there are no edited programs or questions in the DRAFT version.
     */
    public abstract Optional<ImmutableSet<ReferencingProgram>> draftReferences();

    /** Returns a set of references to the question in the ACTIVE version. */
    public abstract ImmutableSet<ReferencingProgram> activeReferences();

    private static Builder builder() {
      return new AutoValue_ActiveAndDraftQuestions_ReferencingPrograms.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder setDraftReferences(Optional<ImmutableSet<ReferencingProgram>> v);

      abstract Builder setActiveReferences(ImmutableSet<ReferencingProgram> v);

      abstract ReferencingPrograms build();
    }
  }

  /** Represents a specific program's reference to a given question. */
  @AutoValue
  public abstract static class ReferencingProgram {
    /** Returns the program that references the question. */
    public abstract ProgramDefinition programDefinition();

    /** Returns the block ID that references the question. */
    public abstract long blockDefinitionId();

    private static Builder builder() {
      return new AutoValue_ActiveAndDraftQuestions_ReferencingProgram.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder setProgramDefinition(ProgramDefinition value);

      abstract Builder setBlockDefinitionId(long value);

      abstract ReferencingProgram build();
    }
  }
}
