package services.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import com.google.common.base.Splitter;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import org.junit.Test;



public class PdfExporterTest extends AbstractExporterTest {

    @Test
    public void validatePDFExport() throws IOException, DocumentException {
        createFakeQuestions();
        createFakeProgram();
        createFakeApplications();

        PdfExporter exporter = instanceOf(PdfExporter.class);

        byte[] result = exporter.export(applicationOne);
        PdfReader pdfReader = new PdfReader(result);

        StringBuilder textFromPDF = new StringBuilder();
        int pages = pdfReader.getNumberOfPages();
        for (int i = 1; i < pages; i++) {
            textFromPDF.append(PdfTextExtractor.getTextFromPage(pdfReader, i));
        }
        pdfReader.close();

        assertThat(textFromPDF).isNotNull();
        List<String> linesFromPDF = Splitter.on('\n').splitToList(textFromPDF.toString());

        assertThat(textFromPDF).isNotNull();
        String name = applicationOne.getApplicantData().getApplicantName() == null? "<Anonymous Applicant>" : applicationOne.getApplicantData().getApplicantName();
        String programName = applicationOne.getProgram().getProgramDefinition().adminName();
        assertThat(linesFromPDF.get(0)).isEqualTo(name + " " +  "("+applicationOne.id+")");
        assertThat(linesFromPDF.get(1)).isEqualTo("Program Name : " + programName);

        List<String> linesFromStaticString  = Splitter.on("\n").splitToList(applicationOneString);

        for(int i=3;i<linesFromPDF.size();i++) {
            assertThat(linesFromPDF.get(i)).isEqualTo(linesFromStaticString.get(i));
        }

    }
    public static String applicationOneString =
            "<Anonymous Applicant> (48)\n" +
                    "Program Name : Fake Program\n" +
                    " \n" +
                    "applicant Email address\n" +
                    "one@example.com\n" +
                    "Answered on : 1969-12-31\n" +
                    "applicant address\n" +
                    "street st\n" +
                    "apt 100\n" +
                    "city, AB 54321\n" +
                    "Answered on : 1969-12-31\n" +
                    "applicant birth date\n" +
                    "01/01/1980\n" +
                    "Answered on : 1969-12-31\n" +
                    "applicant favorite color\n" +
                    "Some Value \" containing ,,, special characters\n" +
                    "Answered on : 1969-12-31\n" +
                    "applicant file\n" +
                    "-- my-file-key UPLOADED (click to download) --\n" +
                    "Answered on : 1969-12-31\n" +
                    "applicant household members\n" +
                    "item1\n" +
                    "item2\n" +
                    "Answered on : 1969-12-31\n" +
                    "applicant ice cream\n" +
                    "strawberry\n" +
                    "Answered on : 1969-12-31\n" +
                    "applicant id\n" +
                    "012\n" +
                    "Answered on : 1969-12-31\n" +
                    "applicant monthly income\n" +
                    "1234.56\n" +
                    "Answered on : 1969-12-31\n" +
                    "applicant name\n" +
                    "Alice Appleton\n" +
                    "Answered on : 1969-12-31\n" +
                    "kitchen tools\n" +
                    "toaster\n" +
                    "pepper grinder\n" +
                    "Answered on : 1969-12-31\n" +
                    "number of items applicant can juggle\n" +
                    "123456\n" +
                    "Answered on : 1969-12-31\n" +
                    "radio\n" +
                    "winter\n";
}

