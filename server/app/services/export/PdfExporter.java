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

import services.Path;
import services.applicant.AnswerData;
//import com.itextpdf.*;
/** PdfExporter is meant to generate PDF files. The functionality is not fully implemented yet. */
public class PdfExporter {


  /**
   * Write a PDF containing the filled-in base form to the provided writer. For the PDF to be valid,
   * the writer should contain no previous writes and should be closed immediately after this call.
   * This function marshals the output document in memory due to restrictions of the PDDocument
   * class.
   */
  public byte[] export(ImmutableList<AnswerData> answers, String applicantNameWithApplicationId, String programName) throws IOException, DocumentException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Document document = new Document();
    PdfWriter.getInstance(document, baos);
    document.open();

    document.addTitle(applicantNameWithApplicationId);
    document.addHeader("Program Name : " , programName);
    //List<String> lines = new ArrayList<>();
    for(AnswerData answerData : answers)
    {
      Paragraph question = new Paragraph(answerData.questionDefinition().getName(),
              FontFactory.getFont(FontFactory.HELVETICA_BOLD,14));
      Paragraph answer = new Paragraph(answerData.answerText(),
              FontFactory.getFont(FontFactory.HELVETICA,12));
      LocalDate date =
              Instant.ofEpochMilli(answerData.timestamp()).atZone(ZoneId.systemDefault()).toLocalDate();
      Paragraph time = new Paragraph("Answered on : " + date,FontFactory.getFont(FontFactory.HELVETICA,10));
              time.setAlignment(Paragraph.ALIGN_RIGHT);

              document.add(question);
              document.add(answer);
              document.add(time);

    }
    //document.
    /*for(String line: lines)
    {
      document.add(new Paragraph(line));
    }*/
    document.close();
    return baos.toByteArray();
  }
}