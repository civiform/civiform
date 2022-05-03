package services.export;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfDocument;
import com.itextpdf.text.pdf.PdfWriter;
import models.Applicant;

import models.Application;
import repository.ProgramRepository;
import services.Path;
import services.applicant.AnswerData;
import services.applicant.ApplicantService;
import services.applicant.ReadOnlyApplicantProgramService;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;
/** PdfExporter is meant to generate PDF files. The functionality is not fully implemented yet. */
public class PdfExporter {
  private final ApplicantService applicantService;

  @Inject
  PdfExporter(ApplicantService applicantService) {
    this.applicantService = checkNotNull(applicantService);
  }

  public byte[] export(Application application) throws DocumentException, IOException {
    ReadOnlyApplicantProgramService roApplicantService =
            applicantService
                    .getReadOnlyApplicantProgramService(application)
                    .toCompletableFuture()
                    .join();
    ImmutableList<AnswerData> answers = roApplicantService.getSummaryData();

    String applicantNameWithApplicationId =
            String.format("%s (%d)", application.getApplicantData().getApplicantName(), application.id);
    return buildPDF(answers,applicantNameWithApplicationId,application.getProgram().getProgramDefinition().adminName());
  }
  /**
   * Write a PDF containing all the values present in the List of AnswerData using itextPDF
   * This function creates the output document in memory due as byte[] and written to output document using
   * ContentType as "application/pdf"
   */
  private byte[] buildPDF(ImmutableList<AnswerData> answers, String applicantNameWithApplicationId, String programName) throws IOException, DocumentException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    Document document = new Document();
    PdfWriter writer = PdfWriter.getInstance(document, byteArrayOutputStream);
    document.open();

    Paragraph applicant = new Paragraph(applicantNameWithApplicationId,FontFactory.getFont(FontFactory.HELVETICA_BOLD,16));
    Paragraph program = new Paragraph("Program Name : "  + programName,FontFactory.getFont(FontFactory.HELVETICA_BOLD,15));
    document.add(applicant);
    document.add(program);
    document.add(Chunk.NEWLINE);
    for(AnswerData answerData : answers)
    {
      Paragraph question = new Paragraph(answerData.questionDefinition().getName(),
              FontFactory.getFont(FontFactory.HELVETICA_BOLD,12));
      Paragraph answer = new Paragraph(answerData.answerText(),
              FontFactory.getFont(FontFactory.HELVETICA,11));
      LocalDate date =
              Instant.ofEpochMilli(answerData.timestamp()).atZone(ZoneId.systemDefault()).toLocalDate();
      Paragraph time = new Paragraph("Answered on : " + date,FontFactory.getFont(FontFactory.HELVETICA,10));
              time.setAlignment(Paragraph.ALIGN_RIGHT);
      document.add(question);
      document.add(answer);
      document.add(time);
    }
    document.close();
    writer.close();
    byteArrayOutputStream.close();
    return byteArrayOutputStream.toByteArray();
  }
}