package models;

import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by AnhQuan on 8/14/2016.
 */
@Entity
public class Customer extends Model {
    @ManyToOne
    public User user;
    public boolean isLocationManager;
    @ManyToMany
    public List<Location> locations;

    public Customer() {
    }

    public Customer(User user, boolean isLocationManager) {
        this.user = user;
        this.isLocationManager = isLocationManager;
        locations=new ArrayList<Location>();
    }
    public void addLocation(Location location){
        locations.add(location);
    }
}
