package services.program;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import models.DisplayMode;
import models.VersionModel;
import org.apache.pekko.japi.Pair;
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

  enum ActiveAndDraftProgramsType {
    IN_USE,
    DISABLED
  }

  private static final EnumSet<ActiveAndDraftProgramsType> allProgramTypes =
      EnumSet.allOf(ActiveAndDraftProgramsType.class);

  /**
   * Queries the existing active and draft versions and builds a snapshotted view of the program
   * state. Since a ProgramService argument is included, we will get the full program definition,
   * which includes the question definitions.
   */
  public static ActiveAndDraftPrograms buildFromCurrentVersionsSynced(
      ProgramService service, VersionRepository repository) {
    return new ActiveAndDraftPrograms(repository, Optional.of(service), allProgramTypes);
  }

  /**
   * Queries the existing active and draft versions and builds a snapshotted view of the program
   * state. These programs won't include the question definition, since ProgramService is not
   * provided.
   */
  public static ActiveAndDraftPrograms buildFromCurrentVersionsUnsynced(
      VersionRepository repository) {
    return new ActiveAndDraftPrograms(repository, Optional.empty(), allProgramTypes);
  }

  /**
   * Queries the existing active and draft versions of disabled programs and builds a snapshotted
   * view of their state. Like its counterpart, this does not include question definitions due to
   * the absence of ProgramService.
   */
  public static ActiveAndDraftPrograms buildDisabledProgramsFromCurrentVersionsUnsynced(
      VersionRepository repository) {
    return new ActiveAndDraftPrograms(
        repository, Optional.empty(), EnumSet.of(ActiveAndDraftProgramsType.DISABLED));
  }

  /**
   * Queries the existing active and draft versions of non-disabled programs and builds a
   * snapshotted view of the program state. These programs won't include the question definition,
   * since ProgramService is not provided.
   */
  public static ActiveAndDraftPrograms buildInUseProgramFromCurrentVersionsUnsynced(
      VersionRepository repository) {
    return new ActiveAndDraftPrograms(
        repository, Optional.empty(), EnumSet.of(ActiveAndDraftProgramsType.IN_USE));
  }

  private ImmutableMap<String, ProgramDefinition> mapNameToProgramWithFilter(
      VersionRepository repository,
      Optional<ProgramService> service,
      VersionModel versionModel,
      Optional<DisplayMode> excludeDisplayMode) {
    return repository.getProgramsForVersion(checkNotNull(versionModel)).stream()
        .map(
            program ->
                service.isPresent()
                    ? getFullProgramDefinition(service.get(), program.id)
                    : program.getProgramDefinition())
        .filter(
            program ->
                excludeDisplayMode.isPresent()
                    ? program.displayMode() != excludeDisplayMode.get()
                    : true)
        .collect(ImmutableMap.toImmutableMap(ProgramDefinition::adminName, Function.identity()));
  }

  private ImmutableMap<String, ProgramDefinition> mapNameToProgram(
      VersionRepository repository, Optional<ProgramService> service, VersionModel versionModel) {
    return mapNameToProgramWithFilter(repository, service, versionModel, Optional.empty());
  }

  private ActiveAndDraftPrograms(
      VersionRepository repository,
      Optional<ProgramService> service,
      EnumSet<ActiveAndDraftProgramsType> types) {
    VersionModel active = repository.getActiveVersion();
    VersionModel draft = repository.getDraftVersionOrCreate();
    // Note: Building this lookup has N+1 query behavior since a call to getProgramDefinition does
    // an additional database lookup in order to sync the set of questions associated with the
    // program.

    // Active, non-disabled programs.
    ImmutableMap<String, ProgramDefinition> activeNameToProgram =
        mapNameToProgramWithFilter(repository, service, active, Optional.of(DisplayMode.DISABLED));

    // All active programs (including disabled).
    ImmutableMap<String, ProgramDefinition> activeNameToProgramAll =
        mapNameToProgram(repository, service, active);

    // Draft, non-disabled programs.
    ImmutableMap<String, ProgramDefinition> draftNameToProgram =
        mapNameToProgramWithFilter(repository, service, draft, Optional.of(DisplayMode.DISABLED));

    // All draft programs (including disabled).
    ImmutableMap<String, ProgramDefinition> draftNameToProgramAll =
        mapNameToProgram(repository, service, draft);

    if (types.containsAll(allProgramTypes)) {
      this.activePrograms = activeNameToProgramAll.values().asList();
      this.draftPrograms = draftNameToProgramAll.values().asList();
      this.versionedByName =
          createVersionedByNameMap(activeNameToProgramAll, draftNameToProgramAll);
    } else if (types.contains(ActiveAndDraftProgramsType.DISABLED)) {
      // Disabled active programs.
      ImmutableMap<String, ProgramDefinition> disabledActiveNameToProgram =
          filterMapNameToProgram(activeNameToProgramAll, activeNameToProgram);
      // Disabled draft programs.
      ImmutableMap<String, ProgramDefinition> disabledDraftNameToProgram =
          filterMapNameToProgram(draftNameToProgramAll, draftNameToProgram);

      this.activePrograms = disabledActiveNameToProgram.values().asList();
      this.draftPrograms = disabledDraftNameToProgram.values().asList();
      this.versionedByName =
          createVersionedByNameMap(disabledActiveNameToProgram, disabledDraftNameToProgram);
    } else if (types.contains(ActiveAndDraftProgramsType.IN_USE)) {
      this.activePrograms = activeNameToProgram.values().asList();
      this.draftPrograms = draftNameToProgram.values().asList();
      this.versionedByName = createVersionedByNameMap(activeNameToProgram, draftNameToProgram);
    } else {
      throw new IllegalArgumentException("Unsupported ActiveAndDraftProgramsType: " + types);
    }
  }

  /**
   * Returns an ImmutableMap containing all key-value pairs from `fullProgramMap` whose keys are not
   * present in `excludeProgramMap`. In other words, this filters out any entries that are shared
   * between the two maps.
   *
   * @param fullProgramMap The complete map of program definitions.
   * @param excludeProgramMap The map containing entries to exclude.
   * @return A new ImmutableMap with the filtered entries.
   */
  private ImmutableMap<String, ProgramDefinition> filterMapNameToProgram(
      ImmutableMap<String, ProgramDefinition> fullProgramMap,
      ImmutableMap<String, ProgramDefinition> excludeProgramMap) {
    return fullProgramMap.entrySet().stream()
        .filter(entry -> !excludeProgramMap.containsKey(entry.getKey()))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * Creates an ImmutableMap that associates each program name with a Pair containing: - An Optional
   * of the ProgramDefinition from the `activeNameToProgram` map, if present. - An Optional of the
   * ProgramDefinition from the `draftNameToProgram` map, if present.
   *
   * <p>This allows lookup to see if a program exists in either the active or draft state, and to
   * access its programDefinition if it does.
   *
   * @param activeNameToProgram A map of active program names to their ProgramDefinition.
   * @param draftNameToProgram A map of draft program names to their ProgramDefinition.
   * @return An ImmutableMap where keys are program names and values are Pairs of Optional
   *     ProgramDefinitions.
   */
  private ImmutableMap<String, Pair<Optional<ProgramDefinition>, Optional<ProgramDefinition>>>
      createVersionedByNameMap(
          ImmutableMap<String, ProgramDefinition> activeNameToProgram,
          ImmutableMap<String, ProgramDefinition> draftNameToProgram) {
    Set<String> allProgramNames =
        Sets.union(activeNameToProgram.keySet(), draftNameToProgram.keySet());

    return allProgramNames.stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Function.identity(),
                programName ->
                    Pair.create(
                        Optional.ofNullable(activeNameToProgram.get(programName)),
                        Optional.ofNullable(draftNameToProgram.get(programName)))));
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
    if (!versionedByName.containsKey(name)) {
      return Optional.empty();
    }

    return versionedByName.get(name).first();
  }

  public Optional<ProgramDefinition> getDraftProgramDefinition(String name) {
    if (!versionedByName.containsKey(name)) {
      return Optional.empty();
    }

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

  private ProgramDefinition getFullProgramDefinition(ProgramService service, long id) {
    try {
      return service.getFullProgramDefinition(id);
    } catch (ProgramNotFoundException e) {
      // This is not possible because we query with existing program ids.
      throw new RuntimeException(e);
    }
  }
}
