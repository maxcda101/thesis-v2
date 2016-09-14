package models;

import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

/**
 * Created by AnhQuan on 9/13/2016.
 */
@Entity
public class KeyLocation extends Model {
    @ManyToOne
    public Location location;
    public String code;

    public KeyLocation() {

    }

    public KeyLocation(Location location, String key) {

        this.location = location;
        this.code = key;
    }
}
