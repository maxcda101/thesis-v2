package models;

import play.db.jpa.Model;

import javax.persistence.Entity;

/**
 * Created by AnhQuan on 9/6/2016.
 */
@Entity
public class TypeData extends Model{
    public String name;
    public String description;
}
