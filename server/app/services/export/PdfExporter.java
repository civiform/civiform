package services.export;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import javax.inject.Inject;
import models.Application;
import services.applicant.AnswerData;
import services.applicant.ApplicantService;
import services.applicant.ReadOnlyApplicantProgramService;

/** PdfExporter is meant to generate PDF files. The functionality is not fully implemented yet. */
public class PdfExporter {
  private final ApplicantService applicantService;
  private final Clock clock;

  @Inject
  PdfExporter(ApplicantService applicantService, Clock clock) {
    this.applicantService = checkNotNull(applicantService);
    this.clock = checkNotNull(clock);
  }
  /**
   * Generates a byte array containing all the values present in the List of AnswerData using
   * itextPDF.This function creates the output document in memory as a byte[] and is part of the
   * inMemoryPDF object. The InMemoryPdf object is passed back to the AdminController Class to
   * generate the required PDF.
   */
  public InMemoryPdf export(Application application) throws DocumentException, IOException {
    ReadOnlyApplicantProgramService roApplicantService =
        applicantService
            .getReadOnlyApplicantProgramService(application)
            .toCompletableFuture()
            .join();
    ImmutableList<AnswerData> answers = roApplicantService.getSummaryData();
    InMemoryPdf inMemoryPdf = new InMemoryPdf();
    String applicantNameWithApplicationId =
        String.format("%s (%d)", application.getApplicantData().getApplicantName(), application.id);
    String filename =
        String.format("%s-%s.pdf", applicantNameWithApplicationId, clock.instant().toString());
    inMemoryPdf.byteArray =
        buildPDF(
            answers,
            applicantNameWithApplicationId,
            application.getProgram().getProgramDefinition().adminName());
    inMemoryPdf.fileName = filename;
    return inMemoryPdf;
  }

  private byte[] buildPDF(
      ImmutableList<AnswerData> answers, String applicantNameWithApplicationId, String programName)
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
              "Program Name : " + programName, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15));
      document.add(applicant);
      document.add(program);
      document.add(Chunk.NEWLINE);
      for (AnswerData answerData : answers) {
        Paragraph question =
            new Paragraph(
                answerData.questionDefinition().getName(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
        Paragraph answer =
            new Paragraph(answerData.answerText(), FontFactory.getFont(FontFactory.HELVETICA, 11));
        LocalDate date =
            Instant.ofEpochMilli(answerData.timestamp())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        Paragraph time =
            new Paragraph("Answered on : " + date, FontFactory.getFont(FontFactory.HELVETICA, 10));
        time.setAlignment(Paragraph.ALIGN_RIGHT);
        document.add(question);
        document.add(answer);
        document.add(time);
      }
    } catch (DocumentException e) {
      throw e;
    } finally {
      document.close();
      writer.close();
      byteArrayOutputStream.close();
    }
    return byteArrayOutputStream.toByteArray();
  }

  public static class InMemoryPdf {
    private byte[] byteArray;
    private String fileName;

    public byte[] getByteArray() {
      return byteArray;
    }

    public String getFileName() {
      return fileName;
    }
  }
}
