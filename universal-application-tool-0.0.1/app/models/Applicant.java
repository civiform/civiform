package models;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.ebean.annotation.DbJson;
import java.io.IOException;
import javax.persistence.Entity;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import play.data.validation.Constraints;
import services.applicant.ApplicantData;

/** The ebean mapped class that represents an individual applicant */
@Entity
@Table(name = "applicants")
public class Applicant extends BaseModel {
  private static final long serialVersionUID = 1L;
  private static final String EMPTY_APPLICANT_DATA_JSON = "{ \"applicant\": {}, \"metadata\": {} }";
  private ApplicantData applicantData;

  @Constraints.Required
  @DbJson
  private String object;

  public ApplicantData getApplicantData() {
    if (object == null) {
      this.object = EMPTY_APPLICANT_DATA_JSON;
    }

    if (this.applicantData == null) {
      DocumentContext context = JsonPath.parse(object);
      this.applicantData = new ApplicantData(context);
    }

    return applicantData;
  }

  @PrePersist
  public void synchronizeObject() throws IOException {
    this.object = objectAsJsonString();
  }

  private String objectAsJsonString() throws IOException {
    return getApplicantData().asJsonString();
  }
}
