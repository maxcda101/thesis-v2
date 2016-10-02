package models;

import play.db.jpa.JPA;
import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.ManyToOne;
import javax.persistence.Query;
import java.util.Date;
import java.util.List;

/**
 * Created by AnhQuan on 9/6/2016.
 */
@Entity
public class Data extends Model {
    public Date timeCreate;
    public Date timeReceived;
    public float value;
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
    public Data(float value, Date timeCreate, Date timeReceived, Long  idNode, Long idSensor, Long idTypeData) {
        this.node=Node.findById(idNode);
        this.sensor=Sensor.findById(idSensor);
        this.typeData=TypeData.findById(idTypeData);

        this.value = value;
        this.timeCreate = timeCreate;
        this.timeReceived = timeReceived;
    }
    public Data(float value, Sensor sensor){
        this.value=value;
        this.sensor=sensor;
    }
    public static List<Data> getDataByDay(int day, int month, int year, Long idSensor, Long idNode, Long idTypeData){
        String first=year+"-"+month+"-"+day;
        String last=year+"-"+month+"-"+(day+1);

        EntityManager em = JPA.em();
        String sql="SELECT * FROM Data where '"+first+"' <timeCreate and timeCreate < '"+last+"' and sensor_id="+idSensor +" and node_id="+idNode+" and typeData_id="+idTypeData;
        Query query = em.createNativeQuery(sql,Data.class);
        List<Data> result = (List<Data>) query.getResultList();
        return result;
    }

}
