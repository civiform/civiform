package services.export;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import repository.VersionRepository.PublishProgramPreview;
import services.DeletionStatus;
import services.program.ProgramDefinition;
import services.question.ActiveAndDraftQuestions;
import services.question.ActiveAndDraftQuestions.ReferencingPrograms;
import services.question.LocalizedQuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

/**
 * Exports the CiviForm question bank to CSV format.
 *
 * <p>Produces a UTF-8 CSV string containing one row per question, sorted alphabetically by admin
 * name, with columns for metadata such as question text, type, answer options, referencing
 * programs, and lifecycle status.
 */
@Singleton
public final class QuestionBankCsvExporter {

  private static final String[] HEADERS = {
    "Admin ID",
    "Question Text",
    "Question Help Text",
    "Admin Description",
    "Question Type",
    "Answer Options",
    "Universal",
    "Eligibility Question",
    "Programs",
    "Status",
    "Last Modified"
  };

  @Inject
  QuestionBankCsvExporter() {}

  /**
   * Exports the question bank to a CSV string.
   *
   * @param activeAndDraftQuestions a snapshot of the current active and draft questions
   * @return a UTF-8 CSV string
   */
  public String export(ActiveAndDraftQuestions activeAndDraftQuestions) {
    StringWriter stringWriter = new StringWriter();
    CSVFormat format = CSVFormat.DEFAULT.builder().setHeader(HEADERS).get();

    try (CSVPrinter printer = new CSVPrinter(stringWriter, format)) {
      ImmutableList<String> sortedNames =
          activeAndDraftQuestions.getQuestionNames().stream()
              .sorted()
              .collect(ImmutableList.toImmutableList());

      for (String questionName : sortedNames) {
        Optional<QuestionDefinition> draftDef =
            activeAndDraftQuestions.getDraftQuestionDefinition(questionName);
        Optional<QuestionDefinition> activeDef =
            activeAndDraftQuestions.getActiveQuestionDefinition(questionName);

        // Use draft if available, otherwise active.
        QuestionDefinition questionDef;
        if (draftDef.isPresent()) {
          questionDef = draftDef.get();
        } else if (activeDef.isPresent()) {
          questionDef = activeDef.get();
        } else {
          continue;
        }

        printer.printRecord(
            questionDef.getName(),
            questionDef.getQuestionText().getDefault(),
            questionDef.getQuestionHelpText().isEmpty()
                ? ""
                : questionDef.getQuestionHelpText().getDefault(),
            questionDef.getDescription(),
            questionDef.getQuestionType().getLabel(),
            getAnswerOptions(questionDef),
            questionDef.isUniversal() ? "Yes" : "No",
            activeAndDraftQuestions.isUsedInEligibility(questionName) ? "Yes" : "No",
            getPrograms(activeAndDraftQuestions, questionName),
            getStatus(activeAndDraftQuestions, questionName, draftDef, activeDef),
            questionDef.getLastModifiedTime().map(Instant::toString).orElse(""));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return stringWriter.toString();
  }

  private static String getAnswerOptions(QuestionDefinition questionDef) {
    QuestionType type = questionDef.getQuestionType();
    if (type != QuestionType.CHECKBOX
        && type != QuestionType.DROPDOWN
        && type != QuestionType.RADIO_BUTTON
        && type != QuestionType.YES_NO) {
      return "";
    }

    MultiOptionQuestionDefinition multiOptionDef = (MultiOptionQuestionDefinition) questionDef;
    ImmutableList<LocalizedQuestionOption> options = multiOptionDef.getOptionsForDefaultLocale();
    return options.stream()
        .map(LocalizedQuestionOption::optionText)
        .collect(Collectors.joining("; "));
  }

  private static String getPrograms(
      ActiveAndDraftQuestions activeAndDraftQuestions, String questionName) {
    ReferencingPrograms refs = activeAndDraftQuestions.getReferencingPrograms(questionName);
    TreeSet<String> programNames = new TreeSet<>();

    for (ProgramDefinition programDef : refs.activeReferences()) {
      programNames.add(programDef.adminName());
    }
    for (PublishProgramPreview preview : refs.draftReferences()) {
      programNames.add(preview.adminName());
    }

    return String.join("; ", programNames);
  }

  private static String getStatus(
      ActiveAndDraftQuestions activeAndDraftQuestions,
      String questionName,
      Optional<QuestionDefinition> draftDef,
      Optional<QuestionDefinition> activeDef) {
    String status;
    if (draftDef.isPresent() && activeDef.isPresent()) {
      status = "Active (with draft)";
    } else if (draftDef.isPresent()) {
      status = "Draft";
    } else {
      status = "Active";
    }

    DeletionStatus deletionStatus = activeAndDraftQuestions.getDeletionStatus(questionName);
    if (deletionStatus == DeletionStatus.PENDING_DELETION) {
      status += " (pending deletion)";
    }

    return status;
  }
}
