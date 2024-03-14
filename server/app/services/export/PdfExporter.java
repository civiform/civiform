package services.export;

import static com.google.common.base.Preconditions.checkNotNull;

import annotations.BindingAnnotations.Now;
import com.google.common.collect.ImmutableList;
import com.google.inject.Provider;
import com.itextpdf.text.Anchor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.typesafe.config.Config;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import javax.inject.Inject;
import models.ApplicationModel;
import services.DateConverter;
import services.applicant.AnswerData;
import services.applicant.ApplicantService;
import services.applicant.ReadOnlyApplicantProgramService;
import services.program.EligibilityDefinition;
import services.program.ProgramBlockDefinitionNotFoundException;
import services.program.ProgramDefinition;

/** PdfExporter is meant to generate PDF files. */
public final class PdfExporter {
  private final ApplicantService applicantService;
  private final Provider<LocalDateTime> nowProvider;
  private final String baseUrl;
  private final DateConverter dateConverter;

  @Inject
  PdfExporter(
      ApplicantService applicantService,
      @Now Provider<LocalDateTime> nowProvider,
      Config configuration,
      DateConverter dateConverter) {
    this.applicantService = checkNotNull(applicantService);
    this.nowProvider = checkNotNull(nowProvider);
    this.baseUrl = checkNotNull(configuration).getString("base_url");
    this.dateConverter = checkNotNull(dateConverter);
  }

  /**
   * Generates a byte array containing all the values present in the List of AnswerData using
   * itextPDF.This function creates the output document in memory as a byte[] and is part of the
   * inMemoryPDF object. The InMemoryPdf object is passed back to the AdminController Class to
   * generate the required PDF.
   */
  public InMemoryPdf export(
      ApplicationModel application, boolean showEligibilityText, boolean includeHiddenBlocks)
      throws DocumentException, IOException {
    ReadOnlyApplicantProgramService roApplicantService =
        applicantService
            .getReadOnlyApplicantProgramService(application)
            .toCompletableFuture()
            .join();

    ImmutableList<AnswerData> answersOnlyActive = roApplicantService.getSummaryDataOnlyActive();
    ImmutableList<AnswerData> answersOnlyHidden = ImmutableList.<AnswerData>of();
    if (includeHiddenBlocks) {
      answersOnlyHidden = roApplicantService.getSummaryDataOnlyHidden();
    }

    // We expect a name to be present at this point. However, if it's not, we use a placeholder
    // rather than throwing an error here.
    String applicantName =
        application.getApplicantData().getApplicantName().orElse("name-unavailable");
    String applicantNameWithApplicationId = String.format("%s (%d)", applicantName, application.id);
    String filename = String.format("%s-%s.pdf", applicantNameWithApplicationId, nowProvider.get());
    byte[] bytes =
        buildPDF(
            answersOnlyActive,
            answersOnlyHidden,
            applicantNameWithApplicationId,
            application.getProgram().getProgramDefinition(),
            application.getLatestStatus(),
            getSubmitTime(application.getSubmitTime()),
            showEligibilityText);
    return new InMemoryPdf(bytes, filename);
  }

  private String getSubmitTime(Instant submitTime) {
    return submitTime == null
        ? "Application submitted without submission time marked."
        : dateConverter.renderDateTimeHumanReadable(submitTime);
  }

  private byte[] buildPDF(
      ImmutableList<AnswerData> answersOnlyActive,
      ImmutableList<AnswerData> answersOnlyHidden,
      String applicantNameWithApplicationId,
      ProgramDefinition programDefinition,
      Optional<String> statusValue,
      String submitTime,
      boolean showEligibilityText)
      throws DocumentException, IOException {
    ByteArrayOutputStream byteArrayOutputStream = null;
    PdfWriter writer = null;
    Document document = null;

    try {
      byteArrayOutputStream = new ByteArrayOutputStream();
      document = new Document();
      writer = PdfWriter.getInstance(document, byteArrayOutputStream);
      document.open();

      Paragraph applicant =
          new Paragraph(
              applicantNameWithApplicationId, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16));
      Paragraph program =
          new Paragraph(
              "Program Name : " + programDefinition.adminName(),
              FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15));
      document.add(applicant);
      document.add(program);
      Paragraph status =
          new Paragraph(
              "Status: " + statusValue.orElse("none"),
              FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
      document.add(status);
      Paragraph submitTimeInformation =
          new Paragraph(
              "Submit Time: " + submitTime, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
      document.add(submitTimeInformation);
      document.add(Chunk.NEWLINE);
      boolean isEligibilityEnabledInProgram = programDefinition.hasEligibilityEnabled();
      for (AnswerData answerData : answersOnlyActive) {
        Paragraph question =
            new Paragraph(
                answerData.questionDefinition().getName(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
        final Paragraph answer;
        if (answerData.encodedFileKey().isPresent()) {
          String encodedFileKey = answerData.encodedFileKey().get();
          String fileLink =
              controllers.routes.FileController.adminShow(programDefinition.id(), encodedFileKey)
                  .url();
          Anchor anchor = new Anchor(answerData.answerText());
          anchor.setReference(baseUrl + fileLink);
          answer = new Paragraph();
          answer.add(anchor);
        } else {
          answer =
              new Paragraph(
                  answerData.answerText(), FontFactory.getFont(FontFactory.HELVETICA, 11));
        }
        LocalDate date =
            Instant.ofEpochMilli(answerData.timestamp())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        Paragraph time =
            new Paragraph("Answered on : " + date, FontFactory.getFont(FontFactory.HELVETICA, 10));
        time.setAlignment(Paragraph.ALIGN_RIGHT);
        Paragraph eligibility = new Paragraph();
        if (showEligibilityText && isEligibilityEnabledInProgram) {
          try {
            Optional<EligibilityDefinition> eligibilityDef =
                programDefinition.getBlockDefinition(answerData.blockId()).eligibilityDefinition();
            if (eligibilityDef
                .map(
                    definition ->
                        definition
                            .predicate()
                            .getQuestions()
                            .contains(answerData.questionDefinition().getId()))
                .orElse(false)) {

              String eligibilityText =
                  answerData.isEligible() ? "Meets eligibility" : "Doesn't meet eligibility";
              eligibility =
                  new Paragraph(eligibilityText, FontFactory.getFont(FontFactory.HELVETICA, 10));
              eligibility.setAlignment(Paragraph.ALIGN_RIGHT);
            }
          } catch (ProgramBlockDefinitionNotFoundException e) {
            throw new RuntimeException(e);
          }
        }

        document.add(question);
        document.add(answer);
        document.add(time);
        if (!eligibility.isEmpty()) {
          document.add(eligibility);
        }
      }
      if (!answersOnlyHidden.isEmpty()) {
        document.add(Chunk.NEWLINE);
        Paragraph hiddenText =
            new Paragraph(
                "Hidden Questions : ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15));
        document.add(hiddenText);
        document.add(Chunk.NEWLINE);
        for (AnswerData answerData : answersOnlyHidden) {
          Paragraph question =
              new Paragraph(
                  answerData.questionDefinition().getName(),
                  FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
          final Paragraph answer;
          answer =
              new Paragraph(
                  answerData.answerText(), FontFactory.getFont(FontFactory.HELVETICA, 11));
          document.add(question);
          document.add(answer);
        }
      }
    } finally {
      document.close();
      writer.close();
      byteArrayOutputStream.close();
    }
    return byteArrayOutputStream.toByteArray();
  }

  public static final class InMemoryPdf {
    private final byte[] byteArray;
    private final String fileName;

    InMemoryPdf(byte[] byteArray, String fileName) {
      this.byteArray = byteArray;
      this.fileName = fileName;
    }

    public byte[] getByteArray() {
      return byteArray;
    }

    public String getFileName() {
      return fileName;
    }
  }
}
