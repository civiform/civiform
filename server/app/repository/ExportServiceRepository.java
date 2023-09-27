package repository;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
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

  public ImmutableList<String> getCsvHeaders(String questionName) {
    questionName = questionName.isEmpty() ? "color" : questionName;
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
                + " '{}')::jsonb)::json#>'{applicant,color,selections}') AS selections from"
                + " applications;")
        // .setParameter("currentQuestion",questionName)
        .findList()
        .forEach(
            row ->
                allSelectedOptions.add(
                    Long.parseLong(row.getString("selections").replaceAll("^\"|\"$", ""))));

    Set<String> allHeaders = new HashSet<>();
    alloptionsMap.keySet().stream()
        .forEach(
            key -> {
              if (allSelectedOptions.contains(key)) {
                allHeaders.add(alloptionsMap.get(key));
              }
            });

    MultiOptionQuestionDefinition q =
        (MultiOptionQuestionDefinition)
            versionRepository
                .getActiveVersion()
                .getQuestionByName(questionName)
                .get()
                .getQuestionDefinition();
    q.getOptions().stream()
        .forEach(
            e -> {
              if (!allHeaders.contains(e.adminName())) {
                allHeaders.add(e.adminName());
              }
            });
    return ImmutableList.<String>builder().addAll(allHeaders).build();
  }
}
