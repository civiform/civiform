package services.apibridge;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import models.BridgeDefinitionItem;
import org.junit.Test;
import services.Path;
import services.applicant.ApplicantData;

public class ResponsePayloadMapperTest {
  @Test
  public void map_success() {
    var applicantData = new ApplicantData();

    var outputFields =
        ImmutableList.<BridgeDefinitionItem>builder()
            .add(
                BridgeDefinitionItem.builder()
                    .questionName("sclaccount.number")
                    .externalName("accountNumber")
                    .build())
            .add(
                BridgeDefinitionItem.builder()
                    .questionName("valid_scl_account.selection")
                    .externalName("isValid")
                    .build())
            .build();

    var payload =
        ImmutableMap.<String, Object>builder()
            .put("accountNumber", 1234)
            .put("isValid", true)
            .build();

    var newApplicantData =
        ResponsePayloadMapper.map(applicantData, getResponseSchema(), payload, outputFields);

    assertThat(newApplicantData).isNotNull();
    assertThat(newApplicantData.readString(Path.create("applicant").join("sclaccount.number")))
        .get()
        .isEqualTo("1234");
    assertThat(
            newApplicantData.readString(
                Path.create("applicant").join("valid_scl_account.selection")))
        .get()
        .isEqualTo("0");
  }

  private String getResponseSchema() {
    return """
             {
               "$id" : "https://civiform.us/schemas/application-status.json",
               "type" : "object",
               "title" : "Application Status",
               "$schema" : "https://json-schema.org/draft/2020-12/schema",
               "required" : [ "accountNumber, isValid" ],
               "properties" : {
                 "isValid" : {
                   "type" : "boolean",
                   "description" : "Has valid account"
                 },
                 "accountNumber" : {
                   "type" : "number",
                   "description" : "Account Number"
                 }
               },
               "description" : "Schema for housing application status response",
               "additionalProperties" : false
             }
           """;
  }
}
