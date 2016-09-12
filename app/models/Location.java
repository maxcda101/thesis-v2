package models;

import play.data.validation.Valid;
import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import java.util.List;

/**
 * Created by AnhQuan on 8/7/2016.
 */
@Entity
public class Location extends Model {
    @Valid
    public String name;
    public String address;
    //vi do
    public float latitude;
    //kinh do
    public float longitude;
    @Lob
    public String description;
    @ManyToMany
    public List<Sensor> sensors;
    public Location() {
    }
    public Location(String name, String address, float latitude, float longitude, String description) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
    }
    public static List<Location> getLocations(int start, int limit){
        if(limit<=0||limit >10){
            limit=10;
        }
        if(start<0){
            start=0;
        }
        return Location.find("").from(start).fetch(limit);
    }
}
