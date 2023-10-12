package repository;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import io.ebean.DB;
import io.ebean.Database;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import services.question.types.MultiOptionQuestionDefinition;

public class ExportServiceRepository {
  private final Database database;
  private final VersionRepository versionRepository;

  @Inject
  public ExportServiceRepository(VersionRepository versionRepository) {
    this.database = DB.getDefault();
    this.versionRepository = checkNotNull(versionRepository);
  }

  public ImmutableMap<Long, String> getMultiSelectedHeaders(String questionName) {
    Map<Long, String> alloptionsMap = new HashMap<>();
    database
        .sqlQuery(
            "SELECT DISTINCT jsonb_array_elements(q.question_options)->>'adminName'AS"
                + " AdminName,jsonb_array_elements(q.question_options)->>'id' AS Id FROM questions"
                + " q where name = :currentQuestion::varchar")
        .setParameter("currentQuestion", questionName)
        .findList()
        .stream()
        .forEach(row -> alloptionsMap.put(row.getLong("id"), row.getString("AdminName")));
    Set<Long> allSelectedOptions = new HashSet<>();
    database
        .sqlQuery(
            "select json_array_elements(((object #>>"
                + " '{}')::jsonb)::json#>'{applicant, "
                + questionName
                + " ,selections}') AS selections from"
                + " applications;")
        .findList()
        .forEach(
            row ->
                allSelectedOptions.add(
                    Long.parseLong(row.getString("selections").replaceAll("^\"|\"$", ""))));
    Map<Long, String> combinedList = new HashMap<>();
    alloptionsMap.keySet().stream()
        .forEach(
            e -> {
              if (allSelectedOptions.contains(e)) {
                combinedList.put(e, alloptionsMap.get(e));
              }
            });

    MultiOptionQuestionDefinition currentQuestion =
        (MultiOptionQuestionDefinition)
            versionRepository
                .getActiveVersion()
                .getQuestionByName(questionName)
                .get()
                .getQuestionDefinition();
    currentQuestion.getOptions().stream()
        .forEach(
            e -> {
              if (!combinedList.containsKey(e.id())) {
                combinedList.put(e.id(), e.adminName());
              }
            });
    // LinkedHashSet<String> allHeaders = new LinkedHashSet<>();
    // combinedList.keySet().stream().sorted().forEach(e -> allHeaders.add(combinedList.get(e)));
    // allHeaders.stream().forEach(e -> System.out.println("_____ "+ e));
    System.out.println("******Calling it " + 1);

    // combinedList.keySet().stream().sorted().forEach(e -> allHeaders.add(combinedList.get(e)));
    return ImmutableMap.<Long, String>builder().putAll(combinedList).build();
  }
}
