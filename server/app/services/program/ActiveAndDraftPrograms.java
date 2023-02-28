package services.program;

import static com.google.common.base.Preconditions.checkNotNull;

import akka.japi.Pair;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Optional;
import java.util.function.Function;
import models.Version;
import repository.VersionRepository;

/**
 * A data class storing the current active and draft programs. For efficient querying of information
 * about current active / draft programs. Lifespan should be measured in milliseconds - seconds at
 * the maximum - within one request serving path - because it does not have any mechanism for a
 * refresh.
 */
public final class ActiveAndDraftPrograms {

  private final ImmutableList<ProgramDefinition> activePrograms;
  private final ImmutableList<ProgramDefinition> draftPrograms;
  private final ImmutableMap<String, Pair<Optional<ProgramDefinition>, Optional<ProgramDefinition>>>
      versionedByName;

  /**
   * Queries the existing active and draft versions and builds a snapshotted view of the program
   * state.
   */
  public static ActiveAndDraftPrograms buildFromCurrentVersions(
      ProgramService service, VersionRepository repository) {
    return new ActiveAndDraftPrograms(
        service, repository.getActiveVersion(), repository.getDraftVersion());
  }

  private ActiveAndDraftPrograms(ProgramService service, Version active, Version draft) {
    // Note: Building this lookup has N+1 query behavior since a call to getProgramDefinition does
    // an additional database lookup in order to sync the set of questions associated with the
    // program.
    ImmutableMap<String, ProgramDefinition> activeNameToProgram =
        checkNotNull(active).getPrograms().stream()
            .map(program -> getProgramDefinition(checkNotNull(service), program.id))
            .collect(
                ImmutableMap.toImmutableMap(ProgramDefinition::adminName, Function.identity()));

    ImmutableMap<String, ProgramDefinition> draftNameToProgram =
        checkNotNull(draft).getPrograms().stream()
            .map(program -> getProgramDefinition(checkNotNull(service), program.id))
            .collect(
                ImmutableMap.toImmutableMap(ProgramDefinition::adminName, Function.identity()));

    this.activePrograms = activeNameToProgram.values().asList();
    this.draftPrograms = draftNameToProgram.values().asList();
    this.versionedByName =
        Sets.union(activeNameToProgram.keySet(), draftNameToProgram.keySet()).stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    Function.identity(),
                    programName -> {
                      return Pair.create(
                          Optional.ofNullable(activeNameToProgram.get(programName)),
                          Optional.ofNullable(draftNameToProgram.get(programName)));
                    }));
  }

  public ImmutableList<ProgramDefinition> getActivePrograms() {
    return activePrograms;
  }

  public ImmutableList<ProgramDefinition> getDraftPrograms() {
    return draftPrograms;
  }

  public ImmutableSet<String> getProgramNames() {
    return versionedByName.keySet();
  }

  public Optional<ProgramDefinition> getActiveProgramDefinition(String name) {
    return versionedByName.get(name).first();
  }

  public Optional<ProgramDefinition> getDraftProgramDefinition(String name) {
    return versionedByName.get(name).second();
  }

  /** Returns the most recent version of the specified program, which may be active or a draft. */
  public ProgramDefinition getMostRecentProgramDefinition(String name) {
    return getDraftProgramDefinition(name).orElseGet(getActiveProgramDefinition(name)::get);
  }

  /**
   * Returns the most recent versions of all the programs, which may be a mix of active and draft.
   */
  public ImmutableList<ProgramDefinition> getMostRecentProgramDefinitions() {
    return getProgramNames().stream()
        .map(this::getMostRecentProgramDefinition)
        .collect(ImmutableList.toImmutableList());
  }

  public boolean anyDraft() {
    return draftPrograms.size() > 0;
  }

  private ProgramDefinition getProgramDefinition(ProgramService service, long id) {
    try {
      return service.getProgramDefinition(id);
    } catch (ProgramNotFoundException e) {
      // This is not possible because we query with existing program ids.
      throw new RuntimeException(e);
    }
  }
}
