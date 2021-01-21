package models;

import play.data.validation.Constraints;

import javax.persistence.Entity;

/** Person entity managed by Ebean */
@Entity
public class Person extends BaseModel {

  private static final long serialVersionUID = 1L;

  @Constraints.Required public String name;
}
