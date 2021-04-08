package services.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import models.LifecycleStage;
import models.Question;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDListBox;
import org.junit.Test;
import services.Path;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

public class PdfMapAssistantTest {
  public Question makeFakeQuestionWithName(String name) throws UnsupportedQuestionTypeException {
    QuestionDefinition questionDefinition =
        new QuestionDefinitionBuilder()
            .setName(name)
            .setDescription("fake question")
            .setQuestionText(ImmutableMap.of())
            .setQuestionHelpText(ImmutableMap.of())
            .setQuestionType(QuestionType.TEXT)
            .setLifecycleStage(LifecycleStage.ACTIVE)
            .setPath(Path.create("$.applicant.fake.path"))
            .build();
    return new Question(questionDefinition);
  }

  @Test
  public void testMapping() throws IOException, UnsupportedQuestionTypeException {
    // Check that the form is as expected.
    File basePdf = new File("test/services/export/base.pdf");
    assertThat(basePdf.canRead()).isTrue();
    PDDocument doc = PDDocument.load(basePdf);
    PDField formfield = doc.getDocumentCatalog().getAcroForm().getField("formfield");
    assertThat(formfield).isNotNull();
    assertThat(formfield.getValueAsString()).isEmpty();

    PDDocument newDoc =
        PdfMapAssistant.convertToMapPdf(
            doc,
            ImmutableList.of(makeFakeQuestionWithName("foo"), makeFakeQuestionWithName("bar")));
    doc.close();

    PDField newField = newDoc.getDocumentCatalog().getAcroForm().getField("formfield");
    assertThat(newField).isNotNull();
    assertThat(newField).isInstanceOf(PDListBox.class);
    assertThat(((PDListBox) newField).getOptions())
        .hasSameElementsAs(ImmutableList.of("foo", "bar"));
    assertThat(newField.isRequired()).isTrue();
    newDoc.close();
  }
}
