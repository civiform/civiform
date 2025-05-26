package services.apibridge;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.google.common.collect.ImmutableList;
import models.BridgeDefinitionItem;
import org.junit.Test;
import services.Path;
import services.applicant.ApplicantData;

public class RequestPayloadMapperTest {
  @Test
  public void map_success() {
    var applicantData = new ApplicantData();
    applicantData.putString(Path.create("applicant.householdaddress.zip"), "98056");
    applicantData.putLong(Path.create("applicant.sclaccount.number"), 123);

    var inputFields =
        ImmutableList.<BridgeDefinitionItem>builder()
            .add(
                BridgeDefinitionItem.builder()
                    .questionName("householdaddress.zip")
                    .externalName("zipCode")
                    .build())
            .add(
                BridgeDefinitionItem.builder()
                    .questionName("sclaccount.number")
                    .externalName("accountNumber")
                    .build())
            .build();

    var requestPayload = RequestPayloadMapper.map(applicantData, getRequestSchema(), inputFields);

    assertThat(requestPayload).isNotNull();
    assertThat(requestPayload.containsKey("zipCode")).isTrue();
    assertThat(requestPayload.containsKey("accountNumber")).isTrue();
    assertThat(requestPayload.get("zipCode")).isEqualTo("98056");
    assertThat(requestPayload.get("accountNumber")).isEqualTo(123L);
  }

  private String getRequestSchema() {
    return """
           {
             "$id" : "https://example.com/schemas/applicant.json",
             "type" : "object",
             "title" : "Applicant Information",
             "$schema" : "https://json-schema.org/draft/2020-12/schema",
             "required" : [ "accountNumber", "zipCode" ],
             "properties" : {
               "zipCode" : {
                 "type" : "string",
                 "description" : "Zip Code"
               },
               "accountNumber" : {
                 "type" : "number",
                 "description" : "Account Number"
               }
             },
             "description" : "Schema for housing assistance applicant information",
             "additionalProperties" : false
           }
           """;
  }
}
