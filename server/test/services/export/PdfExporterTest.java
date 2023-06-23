package services.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Splitter;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfArray;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfObject;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfString;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import java.io.IOException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class PdfExporterTest extends AbstractExporterTest {

  @Before
  public void createTestData() throws Exception {
    createFakeQuestions();
    createFakeProgram();
    createFakeApplications();
  }

  @Test
  public void validatePDFExport() throws IOException, DocumentException {
    PdfExporter exporter = instanceOf(PdfExporter.class);

    String applicantNameWithApplicationId =
        String.format(
            "%s (%d)", applicationOne.getApplicantData().getApplicantName(), applicationOne.id);
    PdfExporter.InMemoryPdf result = exporter.export(applicationOne);
    PdfReader pdfReader = new PdfReader(result.getByteArray());
    StringBuilder textFromPDF = new StringBuilder();

    int pages = pdfReader.getNumberOfPages();
    for (int pageNum = 1; pageNum < pages; pageNum++) {
      textFromPDF.append(PdfTextExtractor.getTextFromPage(pdfReader, pageNum));
      // Assertions to check if the URL is embedded for the FileUpload
      PdfDictionary pdfDictionary = pdfReader.getPageN(pageNum);
      PdfArray annots = pdfDictionary.getAsArray(PdfName.ANNOTS);
      PdfObject current = annots.getPdfObject(0);
      PdfDictionary currentPdfDictionary = (PdfDictionary) PdfReader.getPdfObject(current);
      assertThat(currentPdfDictionary.get(PdfName.SUBTYPE)).isEqualTo(PdfName.LINK);
      PdfDictionary AnnotationAction = currentPdfDictionary.getAsDict(PdfName.A);
      assertThat(AnnotationAction.get(PdfName.S)).isEqualTo(PdfName.URI);
      PdfString link = AnnotationAction.getAsString(PdfName.URI);
      assertThat(link.toString())
          .isEqualTo(
              "http://localhost:9000/admin/programs/"
                  + applicationOne.getProgram().id
                  + "/files/my-file-key");
    }

    pdfReader.close();
    assertThat(textFromPDF).isNotNull();
    List<String> linesFromPDF = Splitter.on('\n').splitToList(textFromPDF.toString());
    assertThat(textFromPDF).isNotNull();
    String programName = applicationOne.getProgram().getProgramDefinition().adminName();
    assertThat(linesFromPDF.get(0)).isEqualTo(applicantNameWithApplicationId);
    assertThat(linesFromPDF.get(1)).isEqualTo("Program Name : " + programName);
    assertThat(linesFromPDF.get(2)).isEqualTo("Status: " + STATUS_VALUE);
    List<String> linesFromStaticString = Splitter.on("\n").splitToList(APPLICATION_ONE_STRING);
    for (int lineNum = 4; lineNum < linesFromPDF.size(); lineNum++) {
      assertThat(linesFromPDF.get(lineNum)).isEqualTo(linesFromStaticString.get(lineNum));
    }
  }

  @Test
  public void validatePDFExport_OptionalFileUploadWithFile() throws IOException, DocumentException {
    createFakeProgramWithOptionalQuestion();
    PdfExporter exporter = instanceOf(PdfExporter.class);

    String applicantNameWithApplicationId =
        String.format(
            "%s (%d)", applicationFive.getApplicantData().getApplicantName(), applicationFive.id);
    PdfExporter.InMemoryPdf result = exporter.export(applicationFive);
    PdfReader pdfReader = new PdfReader(result.getByteArray());
    StringBuilder textFromPDF = new StringBuilder();

    textFromPDF.append(PdfTextExtractor.getTextFromPage(pdfReader, 1));
    // Assertions to check if the URL is embedded for the FileUpload
    PdfDictionary pdfDictionary = pdfReader.getPageN(1);
    PdfArray annots = pdfDictionary.getAsArray(PdfName.ANNOTS);
    PdfObject current = annots.getPdfObject(0);
    PdfDictionary currentPdfDictionary = (PdfDictionary) PdfReader.getPdfObject(current);
    assertThat(currentPdfDictionary.get(PdfName.SUBTYPE)).isEqualTo(PdfName.LINK);
    PdfDictionary AnnotationAction = currentPdfDictionary.getAsDict(PdfName.A);
    assertThat(AnnotationAction.get(PdfName.S)).isEqualTo(PdfName.URI);
    PdfString link = AnnotationAction.getAsString(PdfName.URI);
    assertThat(link.toString())
        .isEqualTo(
            "http://localhost:9000/admin/programs/"
                + applicationFive.getProgram().id
                + "/files/my-file-key");

    pdfReader.close();
    assertThat(textFromPDF).isNotNull();
    List<String> linesFromPDF = Splitter.on('\n').splitToList(textFromPDF.toString());
    assertThat(textFromPDF).isNotNull();
    String programName = applicationFive.getProgram().getProgramDefinition().adminName();
    assertThat(linesFromPDF.get(0)).isEqualTo(applicantNameWithApplicationId);
    assertThat(linesFromPDF.get(1)).isEqualTo("Program Name : " + programName);
    List<String> linesFromStaticString = Splitter.on("\n").splitToList(APPLICATION_FIVE_STRING);

    for (int i = 3; i < linesFromPDF.size(); i++) {
      assertThat(linesFromPDF.get(i)).isEqualTo(linesFromStaticString.get(i));
    }
  }

  @Test
  public void validatePDFExport_OptionalFileUploadWithoutFile()
      throws IOException, DocumentException {
    createFakeProgramWithOptionalQuestion();
    PdfExporter exporter = instanceOf(PdfExporter.class);

    String applicantNameWithApplicationId =
        String.format(
            "%s (%d)", applicationSix.getApplicantData().getApplicantName(), applicationSix.id);
    PdfExporter.InMemoryPdf result = exporter.export(applicationSix);
    PdfReader pdfReader = new PdfReader(result.getByteArray());
    StringBuilder textFromPDF = new StringBuilder();
    textFromPDF.append(PdfTextExtractor.getTextFromPage(pdfReader, 1));
    PdfDictionary pdfDictionary = pdfReader.getPageN(1);
    // Annots would be empty for no file cases
    PdfArray annots = pdfDictionary.getAsArray(PdfName.ANNOTS);
    assertThat(annots).isNull();
    pdfReader.close();
    assertThat(textFromPDF).isNotNull();
    List<String> linesFromPDF = Splitter.on('\n').splitToList(textFromPDF.toString());
    assertThat(textFromPDF).isNotNull();
    String programName = applicationSix.getProgram().getProgramDefinition().adminName();
    assertThat(linesFromPDF.get(0)).isEqualTo(applicantNameWithApplicationId);
    assertThat(linesFromPDF.get(1)).isEqualTo("Program Name : " + programName);

    List<String> linesFromStaticString = Splitter.on("\n").splitToList(APPLICATION_SIX_STRING);

    for (int i = 3; i < linesFromPDF.size(); i++) {
      assertThat(linesFromPDF.get(i)).isEqualTo(linesFromStaticString.get(i));
    }
  }

  @Test
  public void validatePDFExport_eligibility() throws IOException, DocumentException {
    createFakeProgramWithEligibilityPredicate();

    PdfExporter exporter = instanceOf(PdfExporter.class);

    String applicantNameWithApplicationId =
        String.format(
            "%s (%d)", applicationTwo.getApplicantData().getApplicantName(), applicationTwo.id);
    PdfExporter.InMemoryPdf result = exporter.export(applicationTwo);
    PdfReader pdfReader = new PdfReader(result.getByteArray());
    StringBuilder textFromPDF = new StringBuilder();
    textFromPDF.append(PdfTextExtractor.getTextFromPage(pdfReader, 1));
    pdfReader.close();
    assertThat(textFromPDF).isNotNull();
    List<String> linesFromPDF = Splitter.on('\n').splitToList(textFromPDF.toString());
    String programName = applicationTwo.getProgram().getProgramDefinition().adminName();
    assertThat(linesFromPDF.get(0)).isEqualTo(applicantNameWithApplicationId);
    assertThat(linesFromPDF.get(1)).isEqualTo("Program Name : " + programName);
    assertThat(textFromPDF).contains("Meets eligibility");
  }

  public static final String APPLICATION_SIX_STRING =
      "Optional.empty (653)\n"
          + "Program Name : Fake Optional Question Program\n"
          + "Status: none\n"
          + " \n"
          + "applicant name\n"
          + "Example Six\n"
          + "Answered on : 1969-12-31\n"
          + "applicant file\n"
          + "-- NO FILE SELECTED --\n"
          + "Answered on : 1969-12-31\n";
  public static final String APPLICATION_FIVE_STRING =
      "Optional.empty (558)\n"
          + "Program Name : Fake Optional Question Program\n"
          + "Status: none\n"
          + " \n"
          + "applicant name\n"
          + "Example Five\n"
          + "Answered on : 1969-12-31\n"
          + "applicant file\n"
          + "-- my-file-key UPLOADED (click to download) --\n"
          + "Answered on : 1969-12-31\n";

  public static final String APPLICATION_ONE_STRING =
      "Optional.empty (48)\n"
          + "Program Name : Fake Program\n"
          + "Status: "
          + STATUS_VALUE
          + "\n"
          + " \n"
          + "applicant Email address\n"
          + "one@example.com\n"
          + "Answered on : 1969-12-31\n"
          + "applicant address\n"
          + "street st\n"
          + "apt 100\n"
          + "city, AB 54321\n"
          + "Answered on : 1969-12-31\n"
          + "applicant birth date\n"
          + "01/01/1980\n"
          + "Answered on : 1969-12-31\n"
          + "applicant favorite color\n"
          + "Some Value \" containing ,,, special characters\n"
          + "Answered on : 1969-12-31\n"
          + "applicant file\n"
          + "-- my-file-key UPLOADED (click to download) --\n"
          + "Answered on : 1969-12-31\n"
          + "applicant household members\n"
          + "item1\n"
          + "item2\n"
          + "Answered on : 1969-12-31\n"
          + "applicant ice cream\n"
          + "strawberry\n"
          + "Answered on : 1969-12-31\n"
          + "applicant id\n"
          + "012\n"
          + "Answered on : 1969-12-31\n"
          + "applicant monthly income\n"
          + "1234.56\n"
          + "Answered on : 1969-12-31\n"
          + "applicant name\n"
          + "Alice Appleton\n"
          + "Answered on : 1969-12-31\n"
          + "applicant phone\n"
          + "+1 615-757-1010\n"
          + "Answered on : 1969-12-31\n"
          + "kitchen tools\n"
          + "toaster\n"
          + "pepper grinder\n"
          + "Answered on : 1969-12-31\n"
          + "number of items applicant can juggle\n"
          + "123456\n"
          + "Answered on : 1969-12-31\n"
          + "radio\n"
          + "winter\n";
}
