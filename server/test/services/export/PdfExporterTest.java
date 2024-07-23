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
import services.applications.PdfExporterService;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;

public class PdfExporterTest extends AbstractExporterTest {

  @Before
  public void createTestData() throws Exception {
    createFakeQuestions();
    createFakeProgram();
    createFakeApplications();
  }

  @Test
  public void exportApplication() throws IOException, DocumentException {
    PdfExporter exporter = instanceOf(PdfExporter.class);

    String applicantName = "name-unavailable";
    String applicantNameWithApplicationId =
        String.format("%s (%d)", applicantName, applicationOne.id);
    PdfExporter.InMemoryPdf result =
        exporter.exportApplication(applicationOne, /* isAdmin= */ false);
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
              "http://localhost:9000/applicants/"
                  + applicationOne.getApplicantData().getApplicant().id
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
  public void exportApplication_optionalFileUploadWithFile_asAdmin()
      throws IOException, DocumentException {
    createFakeProgramWithOptionalQuestion();
    PdfExporter exporter = instanceOf(PdfExporter.class);

    // The applicant for this application has a value for the name, so ensure that it is reflected
    // in the generated filename, and later, in the PDF contents.
    assertThat(applicationFive.getApplicantData().getApplicantName().isPresent()).isTrue();
    String applicantName = applicationFive.getApplicantData().getApplicantName().get();
    String applicantNameWithApplicationId =
        String.format("%s (%d)", applicantName, applicationFive.id);
    PdfExporter.InMemoryPdf result =
        exporter.exportApplication(applicationFive, /* isAdmin= */ true);
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
        .isEqualTo("http://localhost:9000/admin/applicant-files/my-file-key");

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
  public void
      exportApplication_optionalFileUploadWithFile_asApplicantUsesDifferentLinkWithSameContent()
          throws IOException, DocumentException {
    createFakeProgramWithOptionalQuestion();
    PdfExporter exporter = instanceOf(PdfExporter.class);

    PdfExporter.InMemoryPdf result =
        exporter.exportApplication(applicationFive, /* isAdmin= */ false);
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
            "http://localhost:9000/applicants/"
                + applicationFive.getApplicantData().getApplicant().id
                + "/files/my-file-key");

    pdfReader.close();

    assertThat(textFromPDF).isNotNull();
    List<String> linesFromPDF = Splitter.on('\n').splitToList(textFromPDF.toString());
    List<String> linesFromStaticString = Splitter.on("\n").splitToList(APPLICATION_FIVE_STRING);

    for (int i = 3; i < linesFromPDF.size(); i++) {
      assertThat(linesFromPDF.get(i)).isEqualTo(linesFromStaticString.get(i));
    }
  }

  @Test
  public void exportApplication_optionalFileUploadWithoutFile()
      throws IOException, DocumentException {
    createFakeProgramWithOptionalQuestion();
    PdfExporter exporter = instanceOf(PdfExporter.class);

    String applicantName = "name-unavailable";
    String applicantNameWithApplicationId =
        String.format("%s (%d)", applicantName, applicationSix.id);
    PdfExporter.InMemoryPdf result =
        exporter.exportApplication(applicationSix, /* isAdmin= */ false);
    PdfReader pdfReader = new PdfReader(result.getByteArray());
    StringBuilder textFromPDF = new StringBuilder();
    textFromPDF.append(PdfTextExtractor.getTextFromPage(pdfReader, 1));
    PdfDictionary pdfDictionary = pdfReader.getPageN(1);
    // Annots would be empty for no file cases
    PdfArray annots = pdfDictionary.getAsArray(PdfName.ANNOTS);
    assertThat(annots).isNull();
    pdfReader.close();
    assertThat(textFromPDF).isNotNull();
    System.out.println(textFromPDF);
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
  public void exportApplication_hiddenQuestionIncludedForAdmins()
      throws IOException, DocumentException {
    createFakeProgramWithVisibilityPredicate();

    PdfExporter exporter = instanceOf(PdfExporter.class);
    String applicantName = "name-unavailable";
    String applicantNameWithApplicationId =
        String.format("%s (%d)", applicantName, applicationSeven.id);
    PdfExporter.InMemoryPdf result =
        exporter.exportApplication(applicationSeven, /* isAdmin= */ true);
    PdfReader pdfReader = new PdfReader(result.getByteArray());
    StringBuilder textFromPDF = new StringBuilder();
    String programName = applicationSeven.getProgram().getProgramDefinition().adminName();
    textFromPDF.append(PdfTextExtractor.getTextFromPage(pdfReader, 1));
    pdfReader.close();
    List<String> linesFromPDF = Splitter.on('\n').splitToList(textFromPDF.toString());

    assertThat(textFromPDF).isNotNull();
    assertThat(linesFromPDF.get(0)).isEqualTo(applicantNameWithApplicationId);
    assertThat(linesFromPDF.get(1)).isEqualTo("Program Name : " + programName);
    assertThat(textFromPDF).contains("Hidden Questions");
  }

  @Test
  public void exportApplication_eligibilityShowsForAdmins() throws IOException, DocumentException {
    createFakeProgramWithEligibilityPredicate();

    PdfExporter exporter = instanceOf(PdfExporter.class);

    String applicantName = "name-unavailable";
    String applicantNameWithApplicationId =
        String.format("%s (%d)", applicantName, applicationTwo.id);
    PdfExporter.InMemoryPdf result =
        exporter.exportApplication(applicationTwo, /* isAdmin= */ false);
    PdfReader pdfReader = new PdfReader(result.getByteArray());
    StringBuilder textFromPDF = new StringBuilder();
    textFromPDF.append(PdfTextExtractor.getTextFromPage(pdfReader, 1));
    pdfReader.close();
    assertThat(textFromPDF).isNotNull();
    List<String> linesFromPDF = Splitter.on('\n').splitToList(textFromPDF.toString());
    String programName = applicationTwo.getProgram().getProgramDefinition().adminName();
    assertThat(linesFromPDF.get(0)).isEqualTo(applicantNameWithApplicationId);
    assertThat(linesFromPDF.get(1)).isEqualTo("Program Name : " + programName);
    assertThat(textFromPDF).doesNotContain("Meets eligibility");
    PdfExporter.InMemoryPdf resultWithEligibility =
        exporter.exportApplication(applicationTwo, /* isAdmin= */ true);
    PdfReader pdfReaderTwo = new PdfReader(resultWithEligibility.getByteArray());
    StringBuilder textFromPDFTwo = new StringBuilder();
    textFromPDFTwo.append(PdfTextExtractor.getTextFromPage(pdfReaderTwo, 1));
    pdfReaderTwo.close();
    assertThat(textFromPDFTwo).isNotNull();
    List<String> linesFromPDFTwo = Splitter.on('\n').splitToList(textFromPDFTwo.toString());
    assertThat(linesFromPDFTwo.get(1)).isEqualTo("Program Name : " + programName);
    assertThat(textFromPDFTwo).contains("Meets eligibility");
  }

  @Test
  public void exportProgram_hasMainProgramInfo() throws IOException {
    PdfExporterService service = instanceOf(PdfExporterService.class);
    ProgramDefinition programDef = fakeProgram.getProgramDefinition();

    PdfExporter.InMemoryPdf result =
        service.generateProgramPreviewPdf(programDef, getFakeQuestionDefinitions());

    List<String> linesFromPdf = getPdfLines(result);
    assertThat(linesFromPdf).isNotEmpty();
    assertThat(linesFromPdf.get(0)).isEqualTo(programDef.localizedName().getDefault());
    assertThat(linesFromPdf.get(1)).isEqualTo("Admin name: " + programDef.adminName());
    assertThat(linesFromPdf.get(2))
        .isEqualTo("Admin description: " + programDef.adminDescription());
    assertThat(linesFromPdf.get(3)).contains("Time of export:");
    assertThat(linesFromPdf.get(4)).isEqualTo("Origin of export: http://localhost:9000");
  }

  @Test
  public void exportProgram_hasBlocksAndQuestions() throws IOException {
    PdfExporterService service = instanceOf(PdfExporterService.class);
    ProgramDefinition programDef = fakeProgram.getProgramDefinition();

    PdfExporter.InMemoryPdf result =
        service.generateProgramPreviewPdf(programDef, getFakeQuestionDefinitions());

    String pdfText = getPdfText(result);
    // For every block (which is every question, since our fake program creates one block per
    // question): Verify the PDF has the block name, question text, question help text, admin name,
    // admin description, and question type.
    for (int i = 0; i < fakeQuestions.size(); i++) {
      BlockDefinition block = programDef.blockDefinitions().get(i);
      QuestionDefinition questionDefinition = fakeQuestions.get(i).getQuestionDefinition();
      pdfText = assertContainsThenCrop(pdfText, "Screen " + (i + 1));
      pdfText = assertContainsThenCrop(pdfText, questionDefinition.getQuestionText().getDefault());
      if (!questionDefinition.getQuestionHelpText().isEmpty()) {
        pdfText =
            assertContainsThenCrop(pdfText, questionDefinition.getQuestionHelpText().getDefault());
      }
      pdfText =
          assertContainsThenCrop(
              pdfText,
              "isOptional: "
                  + block.programQuestionDefinitions().stream()
                      .filter(pqd -> pqd.id() == questionDefinition.getId())
                      .findFirst()
                      .get()
                      .optional());
      pdfText = assertContainsThenCrop(pdfText, "Admin name: " + questionDefinition.getName());
      pdfText =
          assertContainsThenCrop(
              pdfText, "Admin description: " + questionDefinition.getDescription());
      pdfText =
          assertContainsThenCrop(
              pdfText,
              "Question type: "
                  + fakeQuestions.get(i).getQuestionDefinition().getQuestionType().name());
    }
  }

  /**
   * Asserts that {@code expected} is contained in {@code actual}, then crops {@code actual} to just
   * be the substring after the first occurrence of {@code expected}.
   *
   * <p>This is used for asserting program info is in the PDF in the right order. Because the
   * program info is split across multiple pages and then concatenated together, there may be extra
   * spaces within the PDF string and we can't assert that lines are exactly equal to particular
   * values. Instead, we can only assert that all the information is in the PDF in the right order.
   */
  private String assertContainsThenCrop(String actual, String expected) {
    assertThat(actual).contains(expected);
    return actual.substring(actual.indexOf(expected) + expected.length());
  }

  @Test
  public void exportProgram_hasQuestionOptions() throws IOException {
    PdfExporterService service = instanceOf(PdfExporterService.class);
    ProgramDefinition programDef = fakeProgram.getProgramDefinition();

    PdfExporter.InMemoryPdf result =
        service.generateProgramPreviewPdf(programDef, getFakeQuestionDefinitions());

    String pdfText = getPdfText(result);

    // Dropdown options
    assertThat(pdfText).contains("- Chocolate");
    assertThat(pdfText).contains("- Strawberry");
    assertThat(pdfText).contains("- Vanilla");
    assertThat(pdfText).contains("- Coffee");

    // Checkbox options
    assertThat(pdfText).contains("- Toaster");
    assertThat(pdfText).contains("- Pepper Grinder");
    assertThat(pdfText).contains("- Garlic Press");

    // Radio button options
    assertThat(pdfText).contains("- Winter");
    assertThat(pdfText).contains("- Spring");
    assertThat(pdfText).contains("- Summer");
    assertThat(pdfText).contains("- Fall");
  }

  @Test
  public void exportProgram_hasEligibilityPredicate() throws IOException {
    createFakeProgramWithEligibilityPredicate();

    PdfExporterService service = instanceOf(PdfExporterService.class);
    ProgramDefinition programDef = fakeProgramWithEligibility.getProgramDefinition();

    PdfExporter.InMemoryPdf result =
        service.generateProgramPreviewPdf(programDef, getFakeQuestionDefinitions());

    String pdfText = getPdfText(result);
    assertThat(pdfText)
        .contains("Screen 1 is eligible if \"applicant favorite color\" text is equal to \"blue\"");
  }

  @Test
  public void exportProgram_hasVisibilityPredicate() throws IOException {
    createFakeProgramWithVisibilityPredicate();

    PdfExporterService service = instanceOf(PdfExporterService.class);
    ProgramDefinition programDef = fakeProgramWithVisibility.getProgramDefinition();

    PdfExporter.InMemoryPdf result =
        service.generateProgramPreviewPdf(programDef, getFakeQuestionDefinitions());

    String pdfText = getPdfText(result);
    assertThat(pdfText)
        .contains("Screen 2 is hidden if \"applicant favorite color\" text is equal to \"red\"");
  }

  @Test
  public void exportProgram_nestedEnumeratorQuestionsShown() throws IOException {
    createFakeProgramWithEnumeratorAndAnswerQuestions();

    PdfExporterService service = instanceOf(PdfExporterService.class);
    ProgramDefinition programDef = fakeProgramWithEnumerator.getProgramDefinition();

    PdfExporter.InMemoryPdf result =
        service.generateProgramPreviewPdf(programDef, getFakeQuestionDefinitions());

    String pdfText = getPdfText(result);
    assertThat(pdfText).contains("What is the $this's name?");
    assertThat(pdfText).contains("household members name");
    assertThat(pdfText).contains("What are the $this's jobs?");
    assertThat(pdfText).contains("household members jobs");
    assertThat(pdfText).contains("How many days has $this.parent worked at $this?");
    assertThat(pdfText).contains("household members days worked");
  }

  public static List<String> getPdfLines(PdfExporter.InMemoryPdf pdf) throws IOException {
    return Splitter.on('\n').splitToList(getPdfText(pdf));
  }

  private static String getPdfText(PdfExporter.InMemoryPdf pdf) throws IOException {
    PdfReader pdfReader = new PdfReader(pdf.getByteArray());
    StringBuilder textFromPdf = new StringBuilder();
    int pages = pdfReader.getNumberOfPages();
    for (int pageNum = 1; pageNum <= pages; pageNum++) {
      textFromPdf.append(PdfTextExtractor.getTextFromPage(pdfReader, pageNum));
    }
    pdfReader.close();
    return textFromPdf.toString();
  }

  public static final String APPLICATION_SIX_STRING =
      "Optional.empty (653)\n"
          + "Program Name : Fake Optional Question Program\n"
          + "Status: none\n"
          + "Submit Time: 2021/12/31 at 4:00 PM PST\n"
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
          + "Submit Time: 2021/12/31 at 4:00 PM PST\n"
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
          + "Submit Time: 2021/12/31 at 4:00 PM PST\n"
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
          + "applicant favorite season\n"
          + "Winter\n"
          + "Answered on : 1969-12-31\n"
          + "applicant file\n"
          + "-- my-file-key UPLOADED (click to download) --\n"
          + "Answered on : 1969-12-31\n"
          + "applicant household members\n"
          + "item1\n"
          + "item2\n"
          + "Answered on : 1969-12-31\n"
          + "applicant ice cream\n"
          + "Strawberry\n"
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
          + "Toaster\n"
          + "Pepper Grinder\n"
          + "Answered on : 1969-12-31\n"
          + "number of items applicant can juggle\n"
          + "123456\n"
          + "Answered on : 1969-12-31\n";
}
