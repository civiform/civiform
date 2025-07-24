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

@Entity
@Table(name = "geojson_map_data")
@Getter
public class MapDataModel extends Model {

  @Setter @DbJsonB private String geojson;

  @Setter private String endpoint;

  @WhenModified private Instant updateTime;

  @WhenCreated private Instant createTime;
}
