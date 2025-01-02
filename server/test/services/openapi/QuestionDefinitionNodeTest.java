package services.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.TextQuestionDefinition;

public class QuestionDefinitionNodeTest {
  @Test
  public void default_empty_settings() {
    QuestionDefinitionNode node = QuestionDefinitionNode.createRootNode();
    assertThat(node.getQuestionDefinition()).isNull();
    assertThat(node.getChildren()).isEmpty();
  }

  @Test
  public void single_top_level_child_exists() {
    var question1 = createQuestion();

    QuestionDefinitionNode node = QuestionDefinitionNode.createRootNode();
    node.addQuestionDefinition(question1);

    var children = node.getChildren();
    assertThat(children).isNotEmpty();
    assertThat(children.stream().map(QuestionDefinitionNode::getQuestionDefinition))
        .containsExactly(question1);
  }

  @Test
  public void multiple_top_level_children_exists() {
    var question1 = createQuestion();
    var question2 = createQuestion();

    QuestionDefinitionNode node = QuestionDefinitionNode.createRootNode();
    node.addQuestionDefinition(question1);
    node.addQuestionDefinition(question2);

    var children = node.getChildren();
    assertThat(children).isNotEmpty();
    assertThat(children.stream().map(QuestionDefinitionNode::getQuestionDefinition))
        .containsExactly(question1, question2);
  }

  @Test
  public void contains_empty_enumerator() {
    var question1 = createQuestion();
    var enumerator1 = createEnumerator();

    QuestionDefinitionNode node = QuestionDefinitionNode.createRootNode();
    node.addQuestionDefinition(question1);
    node.addQuestionDefinition(enumerator1);

    var children = node.getChildren();
    assertThat(children).isNotEmpty();
    assertThat(children.stream().map(QuestionDefinitionNode::getQuestionDefinition))
        .containsExactly(question1, enumerator1);

    var firstEnumeratorChild =
        children.stream().filter(x -> x.getQuestionDefinition().isEnumerator()).findFirst().get();

    assertThat(firstEnumeratorChild.getChildren()).isEmpty();
  }

  @Test
  public void contains_enumerator_with_expected_children() {
    var question1 = createQuestion();
    var enumerator1 = createEnumerator();
    var question2 = createQuestion(Optional.of(enumerator1.getId()));
    var question3 = createQuestion(Optional.of(enumerator1.getId()));
    var question4 = createQuestion();

    QuestionDefinitionNode node = QuestionDefinitionNode.createRootNode();
    node.addQuestionDefinition(question1);
    node.addQuestionDefinition(enumerator1);
    node.addQuestionDefinition(question2);
    node.addQuestionDefinition(question3);
    node.addQuestionDefinition(question4);

    var children = node.getChildren();
    assertThat(children).isNotEmpty();
    assertThat(children.stream().map(QuestionDefinitionNode::getQuestionDefinition))
        .containsExactly(question1, enumerator1, question4);

    var firstEnumeratorChild =
        children.stream().filter(x -> x.getQuestionDefinition().isEnumerator()).findFirst().get();

    assertThat(firstEnumeratorChild.getChildren()).isNotEmpty();
    assertThat(
            firstEnumeratorChild.getChildren().stream()
                .map(QuestionDefinitionNode::getQuestionDefinition))
        .containsExactly(question2, question3);
  }

  @Test
  public void contains_deeply_nested_enumerator_with_expected_children() {
    var question1 = createQuestion();
    var enumerator1 = createEnumerator();
    var question2 = createQuestion(Optional.of(enumerator1.getId()));
    var question3 = createQuestion(Optional.of(enumerator1.getId()));
    var enumerator2 = createEnumerator(Optional.of(enumerator1.getId()));
    var question4 = createQuestion(Optional.of(enumerator2.getId()));

    QuestionDefinitionNode node = QuestionDefinitionNode.createRootNode();
    node.addQuestionDefinition(question1);
    node.addQuestionDefinition(enumerator1);
    node.addQuestionDefinition(question2);
    node.addQuestionDefinition(question3);
    node.addQuestionDefinition(enumerator2);
    node.addQuestionDefinition(question4);

    var rootChildren = node.getChildren();
    assertThat(rootChildren).isNotEmpty();
    assertThat(rootChildren.stream().map(QuestionDefinitionNode::getQuestionDefinition))
        .containsExactly(question1, enumerator1);

    var firstEnumerator =
        rootChildren.stream()
            .filter(x -> x.getQuestionDefinition().isEnumerator())
            .findFirst()
            .get();
    var firstEnumeratorChildren = firstEnumerator.getChildren();

    assertThat(firstEnumeratorChildren).isNotEmpty();
    assertThat(firstEnumeratorChildren.stream().map(QuestionDefinitionNode::getQuestionDefinition))
        .containsExactly(question2, question3, enumerator2);

    var secondEnumerator =
        firstEnumeratorChildren.stream()
            .filter(x -> x.getQuestionDefinition().isEnumerator())
            .findFirst()
            .get();
    var secondEnumeratorChildren = secondEnumerator.getChildren();

    assertThat(secondEnumeratorChildren).isNotEmpty();
    assertThat(secondEnumeratorChildren.stream().map(QuestionDefinitionNode::getQuestionDefinition))
        .containsExactly(question4);
  }

  private static QuestionDefinition createQuestion() {
    return createQuestion(Optional.empty());
  }

  private static QuestionDefinition createQuestion(Optional<Long> enumeratorId) {
    QuestionDefinitionConfig.Builder builder =
        QuestionDefinitionConfig.builder()
            .setName("Sample Text Question")
            .setDescription("description")
            .setQuestionText(LocalizedStrings.withDefaultValue("What is your favorite color?"))
            .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"));

    enumeratorId.ifPresent(builder::setEnumeratorId);

    return new TextQuestionDefinition(builder.build()).withPopulatedTestId();
  }

  private static QuestionDefinition createEnumerator() {
    return createEnumerator(Optional.empty());
  }

  private static QuestionDefinition createEnumerator(Optional<Long> enumeratorId) {
    QuestionDefinitionConfig.Builder builder =
        QuestionDefinitionConfig.builder()
            .setName("Sample Enumerator Question")
            .setDescription("description")
            .setQuestionText(
                LocalizedStrings.withDefaultValue("List all members of your household."))
            .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"));

    enumeratorId.ifPresent(builder::setEnumeratorId);

    return new EnumeratorQuestionDefinition(
            builder.build(), LocalizedStrings.withDefaultValue("household member"))
        .withPopulatedTestId();
  }
}
