package repository;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Provider;
import io.ebean.DB;
import io.ebean.Database;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import models.Version;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

public final class ExportServiceRepository {
  private final Database database;
  private final Provider<VersionRepository> versionRepositoryProvider;

  @Inject
  public ExportServiceRepository(Provider<VersionRepository> versionRepositoryProvider) {
    this.database = DB.getDefault();
    this.versionRepositoryProvider = checkNotNull(versionRepositoryProvider);
  }

  public ImmutableMap<Long, String> getMultiSelectedHeaders(QuestionDefinition questionDefinition) {
    if (!questionDefinition.getQuestionType().equals(QuestionType.CHECKBOX)) {
      throw new RuntimeException("The Question Type is not checkbox");
    }
    String questionName = questionDefinition.getName();
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

    Version activeVersion = versionRepositoryProvider.get().getActiveVersion();
    MultiOptionQuestionDefinition currentQuestion =
        (MultiOptionQuestionDefinition)
            versionRepositoryProvider
                .get()
                .getQuestionByNameForVersion(questionName, activeVersion)
                .get()
                .getQuestionDefinition();
    currentQuestion.getOptions().stream()
        .forEach(
            e -> {
              if (!combinedList.containsKey(e.id())) {
                combinedList.put(e.id(), e.adminName());
              }
            });
    return ImmutableMap.<Long, String>builder().putAll(combinedList).build();
  }
}
