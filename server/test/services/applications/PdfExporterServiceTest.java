package services.applications;

import static org.assertj.core.api.Assertions.assertThat;
import static services.export.PdfExporterTest.APPLICATION_ONE_STRING;
import static services.export.PdfExporterTest.getPdfLines;

import com.google.common.base.Splitter;
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
import services.export.AbstractExporterTest;
import services.export.PdfExporter;

public class PdfExporterServiceTest extends AbstractExporterTest {

  @Before
  public void createTestData() throws Exception {
    createFakeQuestions();
    createFakeProgram();
    createFakeApplications();
  }

  @Test
  public void generateApplicationPdf() throws IOException {
    PdfExporterService service = instanceOf(PdfExporterService.class);

    String applicantName = "name-unavailable";
    String applicantNameWithApplicationId =
        String.format("%s (%d)", applicantName, applicationOne.id);
    PdfExporter.InMemoryPdf result =
        service.generateApplicationPdf(
            applicationOne,
            /* showEligibilityText= */ true,
            /* includeHiddenBlocks= */ false,
            /* isAdmin= */ true);
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
          .isEqualTo("http://localhost:9000/admin/applicant-files/my-file-key");
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
  public void generateProgramPreviewPdf() throws IOException {
    PdfExporterService service = instanceOf(PdfExporterService.class);

    PdfExporter.InMemoryPdf result =
        service.generateProgramPreviewPdf(
            fakeProgram.getProgramDefinition(), getFakeQuestionDefinitions());

    List<String> linesFromPdf = getPdfLines(result);
    assertThat(linesFromPdf).isNotEmpty();
    assertThat(linesFromPdf.get(0))
        .isEqualTo(fakeProgram.getProgramDefinition().localizedName().getDefault());
    // More assertions about the PDF content will be in PdfExporterTest, since PdfExporter is the
    // class that actually builds the PDF.
  }
}
