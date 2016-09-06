package models;

import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

/**
 * Created by AnhQuan on 8/10/2016.
 */
@Entity
public class Root extends Model {
    public String name;
    public String description;
    @ManyToOne
    public Location location;

    public Root() {
    }

    public Root(String name, String description, Location location) {
        this.name = name;
        this.description = description;
        this.location = location;
    }
}
