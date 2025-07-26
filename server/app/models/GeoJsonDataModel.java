package models;

import io.ebean.Model;
import io.ebean.annotation.DbJsonB;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
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

  @WhenModified private Instant updateTime;

  @WhenCreated private Instant createTime;
}
