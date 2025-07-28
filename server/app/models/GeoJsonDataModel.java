package models;

import io.ebean.Model;
import io.ebean.annotation.DbJsonB;
import io.ebean.annotation.WhenCreated;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import play.data.validation.Constraints;

@Entity
@Table(name = "geo_json_data")
@Getter
public class GeoJsonDataModel extends Model {

  @Constraints.Required @Setter @DbJsonB private String geoJson;

  @Constraints.Required @Setter private String endpoint;

  /**
   * The timestamp this data was last confirmed to be up to date via the API. If the data has
   * changed, a new row is created.
   */
  @Constraints.Required @Setter private Instant confirmTime;

  @WhenCreated private Instant createTime;
}
