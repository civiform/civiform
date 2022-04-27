package services.program;

import static com.google.common.base.Preconditions.checkNotNull;

import akka.japi.Pair;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Optional;
import models.Version;

/**
 * A data class storing the current active and draft programs. For efficient querying of information
 * about current active / draft programs which does not hit the database. Lifespan should be
 * measured in milliseconds - seconds at the maximum - within one request serving path - because it
 * does not have any mechanism for a refresh.
 */
public class ActiveAndDraftPrograms {

  private final ImmutableList<ProgramDefinition> activePrograms;
  private final ImmutableList<ProgramDefinition> draftPrograms;
  private final ImmutableMap<String, Pair<Optional<ProgramDefinition>, Optional<ProgramDefinition>>>
      versionedByName;
  private final int activeSize;
  private final int draftSize;

  public ActiveAndDraftPrograms(ProgramService service, Version active, Version draft) {
    ImmutableMap.Builder<String, ProgramDefinition> activeToName = ImmutableMap.builder();
    ImmutableMap.Builder<String, ProgramDefinition> draftToName = ImmutableMap.builder();
    checkNotNull(draft).getPrograms().stream()
        .map(program -> getProgramDefinition(checkNotNull(service), program.id))
        .forEach(program -> draftToName.put(program.adminName(), program));
    checkNotNull(active).getPrograms().stream()
        .map(program -> getProgramDefinition(checkNotNull(service), program.id))
        .forEach(program -> activeToName.put(program.adminName(), program));
    ImmutableMap<String, ProgramDefinition> activeNames = activeToName.build();
    ImmutableMap<String, ProgramDefinition> draftNames = draftToName.build();
    activePrograms = activeNames.values().asList();
    draftPrograms = draftNames.values().asList();
    activeSize = activeNames.size();
    draftSize = draftNames.size();
    ImmutableMap.Builder<String, Pair<Optional<ProgramDefinition>, Optional<ProgramDefinition>>>
        versionedByNameBuilder = ImmutableMap.builder();
    for (String name : Sets.union(activeNames.keySet(), draftNames.keySet())) {
      versionedByNameBuilder.put(
          name,
          Pair.create(
              Optional.ofNullable(activeNames.get(name)),
              Optional.ofNullable(draftNames.get(name))));
    }
    versionedByName = versionedByNameBuilder.build();
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
