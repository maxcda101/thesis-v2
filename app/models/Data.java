package models;

import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.util.Date;

/**
 * Created by AnhQuan on 9/6/2016.
 */
@Entity
public class Data extends Model {
    public float value;
    public Date timeCreate;
    public Date timeReceived;
    @ManyToOne
    public Node node;
    @ManyToOne
    public Sensor sensor;
    @ManyToOne
    public TypeData typeData;

    public Data() {
    }

    public Data(float value, Date timeCreate, Date timeReceived, Node node, Sensor sensor, TypeData typeData) {
        this.value = value;
        this.timeCreate = timeCreate;
        this.timeReceived = timeReceived;
        this.node = node;
        this.sensor = sensor;
        this.typeData = typeData;
    }
}
