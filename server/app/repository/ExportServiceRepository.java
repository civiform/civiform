package repository;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Provider;
import io.ebean.DB;
import io.ebean.Database;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import models.Application;
import models.LifecycleStage;
import models.Version;
import services.applicant.ApplicantData;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

/** Implements queries related to CSV exporting needs. */
public final class ExportServiceRepository {
  private final Database database;
  private final Provider<VersionRepository> versionRepositoryProvider;

  @Inject
  public ExportServiceRepository(Provider<VersionRepository> versionRepositoryProvider) {
    this.database = DB.getDefault();
    this.versionRepositoryProvider = checkNotNull(versionRepositoryProvider);
  }
  private void test(String programName)
  {
    List<Application> litsD = database.find(Application.class)
    .fetch("applicant.applicantData")
    .where().eq("program.name",programName)
    .in("lifecycle_stage", ImmutableList.of(LifecycleStage.ACTIVE, LifecycleStage.OBSOLETE))
      .findList();
    Set<Long> allSelectedOptions = new HashSet<>();
    litsD.stream().forEach(application -> {
      application.getApplicantData().g
    });
  }

  /**
   * This method creates There are two queries which are run here 1. To get all the
   * adminNames to optionIds for a given question 2. To get all the options which were ever picked
   * by an applicant (this is to filter out options accidentally introduced) The options are now
   * combined with the latest active version of the question and returned as an immutable map.
   * @param questionDefinition of a Checkbox question whose CSV Headers needs to be generated
   * @return a map of all the optionIds to its option's adminName string for a given multiselect question definition
   * @throws RuntimeException when the question is not of type Checkbox
   * @throws NoSuchElementException when the active version of the question is missing
   */
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

    allSelectedOptions.add(1L);
    test("test");
//    database
//      .sqlQuery(
//        "select jsonb_array_elements_text((object #>> '{}')::jsonb->'applicant'->:currentQuestion->'selections')::integer as id from applications")
//      .setParameter("currentQuestion", questionName)
//      .findList()
//      .forEach(
//        row ->
//          allSelectedOptions.add(row.getLong("id")));
    System.out.println("Print .....");
    allSelectedOptions.stream().forEach(e -> System.out.println("________ " + e));
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
