package views.admin.apibridge.programbridge.fragments;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.Builder;
import models.ApiBridgeConfigurationModel;
import models.ApiBridgeConfigurationModel.ApiBridgeDefinitionItem;
import services.applicant.question.Scalar;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import views.admin.BaseViewModel;
import views.admin.apibridge.programbridge.ProgramSchemaField;

/** Holds data to render the program api bridge edit form */
@Builder
public record ProgramBridgeEditPartialViewModel(
    Long programId,
    ApiBridgeConfigurationModel bridgeConfiguration,
    JsonNode requestSchema,
    JsonNode responseSchema,
    ImmutableList<ApiBridgeDefinitionItem> inputFields,
    ImmutableList<ApiBridgeDefinitionItem> outputFields,
    ImmutableList<QuestionDefinition> questions)
    implements BaseViewModel {
  public String getSaveUrl() {
    return controllers.admin.apibridge.routes.ProgramBridgeController.save(programId()).url();
  }

  /**
   * Builds list used to render all the fields needed for mapping external and internal questions to
   * the request schema
   */
  public ImmutableList<ProgramSchemaField> getRequestSchema() {
    return getSchema(requestSchema(), inputFields);
  }

  /**
   * Builds list used to render all the fields needed for mapping external and internal questions to
   * the response schema
   */
  public ImmutableList<ProgramSchemaField> getResponseSchema() {
    return getSchema(responseSchema(), outputFields);
  }

  /**
   * Builds list used to render all the fields needed for mapping external and internal questions
   */
  private ImmutableList<ProgramSchemaField> getSchema(
      JsonNode jsonNode, ImmutableList<ApiBridgeDefinitionItem> items) {
    return jsonNode
        .get("properties")
        .propertyStream()
        .map(
            property -> {
              var optionalBridgeDefinitionItem =
                  items.stream()
                      .filter(x -> x.externalName().equals(property.getKey()))
                      .findFirst();

              String questionName =
                  optionalBridgeDefinitionItem.map(x -> x.questionName()).orElse("");
              String questionScalar =
                  optionalBridgeDefinitionItem.map(x -> x.questionScalar().name()).orElse("");

              return ProgramSchemaField.builder()
                  .externalName(property.getKey())
                  .title(property.getValue().get("title").textValue())
                  .description(property.getValue().get("description").textValue())
                  .type(property.getValue().get("type").textValue())
                  .questionName(questionName)
                  .questionScalar(questionScalar)
                  .build();
            })
        .collect(ImmutableList.toImmutableList());
  }

  /** Gets the list of scalars for the specified question or empty list */
  public ImmutableSet<Scalar> getScalarInfo(String questionName) {
    try {
      var question =
          questions().stream().filter(x -> x.getQuestionNameKey().equals(questionName)).findFirst();

      if (question.isEmpty()) {
        return ImmutableSet.of();
      }

      return Scalar.getScalars(question.get().getQuestionType());
    } catch (InvalidQuestionTypeException | UnsupportedQuestionTypeException e) {
      throw new RuntimeException(e);
    }
  }
}
