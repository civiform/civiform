package models;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.ebean.annotation.DbJsonB;
import io.ebean.text.json.EJson;
import java.io.IOException;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Table;
import play.data.validation.Constraints;
import com.jayway.jsonpath.internal.JsonContext;
import services.applicant.ApplicantData;

@Entity
/** The ebeans mapped class that represents an individual applicant */
@Table(name = "applicants")
public class Applicant extends BaseModel {
  private static final long serialVersionUID = 1L;

  public Applicant() {
    super();

    this.object = "{ \"applicant\": {}, \"metadata\": {} }";
  }

  public String getObject() {
    return object;
  }

  public void setObject(String object) {
    this.object = object;
  }

  @Constraints.Required String object;

  public ApplicantData getApplicantData() {
    DocumentContext context = JsonPath.parse(getObject());
    return new ApplicantData(context);
  }

  public String objectAsJsonString() throws IOException {
    return getApplicantData().asJsonString();
  }
}
