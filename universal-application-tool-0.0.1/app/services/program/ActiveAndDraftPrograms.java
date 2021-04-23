package services.program;

import akka.japi.Pair;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Optional;
import models.Program;
import models.Version;

/**
 * A data class storing the current active and draft programs. For efficient querying of information
 * about current active / draft programs which does not hit the database. Lifespan should be
 * measured in milliseconds - seconds at the maximum - within one request serving path - because it
 * does not have any mechanism for a refresh.
 */
public class ActiveAndDraftPrograms {

  private final ImmutableMap<String, Pair<Optional<ProgramDefinition>, Optional<ProgramDefinition>>>
      versionedByName;
  private final int activeSize;
  private final int draftSize;

  public ActiveAndDraftPrograms(Version active, Version draft) {
    ImmutableMap.Builder<String, ProgramDefinition> activeToName = ImmutableMap.builder();
    ImmutableMap.Builder<String, ProgramDefinition> draftToName = ImmutableMap.builder();
    draft.getPrograms().stream()
        .map(Program::getProgramDefinition)
        .forEach(program -> draftToName.put(program.adminName(), program));
    active.getPrograms().stream()
        .map(Program::getProgramDefinition)
        .forEach(program -> activeToName.put(program.adminName(), program));
    ImmutableMap<String, ProgramDefinition> activeNames = activeToName.build();
    ImmutableMap<String, ProgramDefinition> draftNames = draftToName.build();
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
}
