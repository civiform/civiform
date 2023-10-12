package services.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import repository.ExportServiceRepository;

public final class MultiSelectQuestionHeaderService {

  private Map<String, ImmutableMap<Long, String>> multiSelectOptionHeaderMap;
  private final ExportServiceRepository exportServiceRepository;

  public Map<String, ImmutableMap<Long, String>> getMultiSelectOptionHeaderMap() {
    return ImmutableMap.copyOf(multiSelectOptionHeaderMap);
  }

  @Inject
  public MultiSelectQuestionHeaderService(ExportServiceRepository exportServiceRepository) {
    this.exportServiceRepository = checkNotNull(exportServiceRepository);
    this.multiSelectOptionHeaderMap = new HashMap<>();
  }

  public MultiSelectQuestionHeaderService(
      Map<String, ImmutableMap<Long, String>> multiSelectOptionHeaderMap,
      ExportServiceRepository exportServiceRepository) {
    if (multiSelectOptionHeaderMap == null) {
      this.multiSelectOptionHeaderMap = new HashMap<>();
    } else {
      this.multiSelectOptionHeaderMap = multiSelectOptionHeaderMap;
    }
    this.exportServiceRepository = checkNotNull(exportServiceRepository);
  }

  public Map<String, ImmutableMap<Long, String>> updateMap(String questionName) {
    if (!this.multiSelectOptionHeaderMap.containsKey(questionName)) {
      ImmutableMap<Long, String> headersForQuestion =
          exportServiceRepository.getMultiSelectedHeaders(questionName);
      Map<String, ImmutableMap<Long, String>> newMultiSelectMap =
          new ImmutableMap.Builder()
              .putAll(multiSelectOptionHeaderMap)
              .put(questionName, headersForQuestion)
              .build();
      this.multiSelectOptionHeaderMap = newMultiSelectMap;
    }
    return ImmutableMap.copyOf(multiSelectOptionHeaderMap);
  }
}
