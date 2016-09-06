package models;

import play.db.jpa.Model;

import javax.persistence.Entity;
import java.util.List;

/**
 * Created by AnhQuan on 8/7/2016.
 */
@Entity
public class Sensor extends Model {
    public String name;
    public String description;

    public Sensor() {
    }
    public Sensor(String name, String description) {
        this.name = name;
        this.description = description;
    }
    public static List<Sensor> getSensors(int start, int limit){
        if(limit<=0||limit >10){
            limit=10;
        }
        if(start<0){
            start=0;
        }
        return Sensor.find("").from(start).fetch(limit);
    }
}
