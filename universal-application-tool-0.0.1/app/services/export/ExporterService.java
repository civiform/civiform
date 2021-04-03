package services.export;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.inject.Inject;
import models.Application;
import models.Program;
import services.Path;
import services.program.BlockDefinition;
import services.program.Column;
import services.program.ColumnType;
import services.program.CsvExportConfig;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramQuestionDefinition;
import services.program.ProgramService;
import services.question.ScalarType;

public class ExporterService {
  private final ExporterFactory exporterFactory;
  private final ProgramService programService;

  @Inject
  public ExporterService(ExporterFactory exporterFactory, ProgramService programService) {
    this.exporterFactory = Preconditions.checkNotNull(exporterFactory);
    this.programService = Preconditions.checkNotNull(programService);
  }

  /**
   * Return a string containing the CSV of all the applicants for a particular program.
   *
   * @throws ProgramNotFoundException If the program ID refers to a program that does not exist.
   */
  public String getProgramCsv(long programId) throws ProgramNotFoundException {
    ImmutableList<Application> applications = programService.getProgramApplications(programId);
    ProgramDefinition program = programService.getProgramDefinition(programId);
    CsvExporter csvExporter;
    if (program.exportDefinitions().stream()
        .anyMatch(exportDefinition -> exportDefinition.csvConfig().isPresent())) {
      csvExporter = exporterFactory.csvExporter(program.toProgram());
    } else {
      csvExporter = exporterFactory.csvExporter(generateDefaultCsvConfig(program.toProgram()));
    }

    try {
      OutputStream inMemoryBytes = new ByteArrayOutputStream();
      Writer writer = new OutputStreamWriter(inMemoryBytes, StandardCharsets.UTF_8);
      for (Application application : applications) {
        csvExporter.export(application, writer);
      }
      writer.close();
      return inMemoryBytes.toString();
    } catch (IOException e) {
      // Since it's an in-memory writer, this shouldn't happen.  Catch so that callers don't
      // have to deal with it.
      throw new RuntimeException(e);
    }
  }

  /**
   * Produce the default CSV config for a given program. The default config includes all the
   * questions, the application id, and the application submission time.
   */
  public CsvExportConfig generateDefaultCsvConfig(Program program) {
    // Need to get the actual program with filled-in blocks.
    ProgramDefinition programDefinition;
    try {
      programDefinition = programService.getProgramDefinition(program.id);
    } catch (ProgramNotFoundException e) {
      // This can't happen because we have the model object already, but maintain the exception
      // throw so that it's findable / fixable if it does ever happen.
      throw new RuntimeException(e);
    }
    ImmutableList.Builder<Column> columnBuilder = new ImmutableList.Builder<>();
    // First add the ID and submit time columns.
    columnBuilder.add(Column.builder().setHeader("ID").setColumnType(ColumnType.ID).build());
    columnBuilder.add(
        Column.builder().setHeader("Submit time").setColumnType(ColumnType.SUBMIT_TIME).build());
    // Next add one column for each scalar entry in every column.
    for (BlockDefinition block : programDefinition.blockDefinitions()) {
      for (ProgramQuestionDefinition question : block.programQuestionDefinitions()) {
        for (Map.Entry<Path, ScalarType> entry :
            question.getQuestionDefinition().getScalars().entrySet()) {
          String finalSegment = entry.getKey().keyName();
          // These are the two metadata fields in every answer - we don't need to report them.
          if (!finalSegment.equals("updated_in_program") && !finalSegment.equals("updated_at")) {
            columnBuilder.add(
                Column.builder()
                    // e.g. "name.first".
                    .setHeader(question.getQuestionDefinition().getName() + "." + finalSegment)
                    .setJsonPath(entry.getKey())
                    .setColumnType(ColumnType.APPLICANT)
                    .build());
          }
        }
      }
    }
    return new CsvExportConfig() {
      @Override
      public ImmutableList<Column> columns() {
        return columnBuilder.build();
      }
    };
  }
}
