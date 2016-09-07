package entities;

import models.Data;
import models.Node;
import models.Sensor;
import models.TypeData;
import utils.DateFormat;

import java.util.Date;

/**
 * Created by AnhQuan on 9/6/2016.
 */
public class DataEmbed{
    public Long id;
    public Long idNode;
    public Long idSensor;
    public float value;
    public String timeRec;
    public String timeSent;

    public Data convertData(){
        return new Data(value, DateFormat.StringToDate(timeRec), new Date(),idNode,idSensor, 3L);
    }
}
