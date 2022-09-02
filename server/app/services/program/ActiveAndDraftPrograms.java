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

/**
 * A data class storing the current active and draft programs. For efficient querying of information
 * about current active / draft programs. Lifespan should be measured in milliseconds - seconds at
 * the maximum - within one request serving path - because it does not have any mechanism for a
 * refresh.
 */
public class ActiveAndDraftPrograms {

  private final ImmutableList<ProgramDefinition> activePrograms;
  private final ImmutableList<ProgramDefinition> draftPrograms;
  private final ImmutableMap<String, Pair<Optional<ProgramDefinition>, Optional<ProgramDefinition>>>
      versionedByName;
  private final int activeSize;
  private final int draftSize;

  public ActiveAndDraftPrograms(ProgramService service, Version active, Version draft) {
    // TODO(clouser): This has N+1 query behavior.
    ImmutableMap<String, ProgramDefinition> activeToName =
        checkNotNull(active).getPrograms().stream()
            .map(program -> getProgramDefinition(checkNotNull(service), program.id))
            .collect(
                ImmutableMap.toImmutableMap(ProgramDefinition::adminName, Function.identity()));

    ImmutableMap<String, ProgramDefinition> draftToName =
        checkNotNull(draft).getPrograms().stream()
            .map(program -> getProgramDefinition(checkNotNull(service), program.id))
            .collect(
                ImmutableMap.toImmutableMap(ProgramDefinition::adminName, Function.identity()));

    activePrograms = activeToName.values().asList();
    draftPrograms = draftToName.values().asList();
    activeSize = activeToName.size();
    draftSize = draftToName.size();
    versionedByName =
        Sets.union(activeToName.keySet(), draftToName.keySet()).stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    Function.identity(),
                    programName -> {
                      return Pair.create(
                          Optional.ofNullable(activeToName.get(programName)),
                          Optional.ofNullable(draftToName.get(programName)));
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

  public int getActiveSize() {
    return activeSize;
  }

  public int getDraftSize() {
    return draftSize;
  }

  public boolean anyDraft() {
    return getDraftSize() > 0;
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
