package jobs;

import models.Data;
import play.Logger;
import play.db.jpa.JPA;
import play.jobs.Every;
import play.jobs.Job;
import play.jobs.On;
import utils.DateFormat;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Date;
import java.util.List;

/**
 * Created by AnhQuan on 9/16/2016.
 */
//@On("0 00 * * * ?")//every hour h:00:00
public class HanderOneHour extends Job {
    @Override
    public void doJob() {
        Logger.info("doJob ONE HOUR: ", DateFormat.DateToString(new Date()));
        String timeOld = DateFormat.DateToString(new Date(new Date().getTime() - 1000 * 3600));
        String timeNew = DateFormat.DateToString(new Date());

        EntityManager em = JPA.em();
        Query query = em.createNativeQuery("SELECT distinct sensor_id FROM Data where '" + timeOld + "' < timeCreate and timeCreate < '" + timeNew + "' and typeData_id=3");
        List<Number> listSensor = (List<Number>) query.getResultList();

        query = em.createNativeQuery("SELECT Distinct node_id FROM Data where '" + timeOld + "' < timeCreate and timeCreate < '" + timeNew + "' and typeData_id=3");
        List<Number> listNode = (List<Number>) query.getResultList();

        for (Number i : listNode) {
            long idNode = i.longValue();
            for (Number j : listSensor) {
                long idSensor = j.longValue();
                query = em.createNativeQuery("SELECT * FROM Data where node_id=" + idNode + " and sensor_id=" + idSensor + " and '" + timeOld + "' < timeCreate and timeCreate < '" + timeNew + "' and typeData_id=3", Data.class);
                List<Data> listData = (List<Data>) query.getResultList();
                float value = calculateMedium(listData);
                if (value != 0) {
                    Data data = new Data(value, new Date(), new Date(), idNode, idSensor, 1L);
                    data.save();
                }
                value = minValue(listData);
                if (value != 0) {
                    Data data = new Data(value, new Date(), new Date(), idNode, idSensor, 4L);
                    data.save();
                }
                value = maxValue(listData);
                if (value != 0) {
                    Data data = new Data(value, new Date(), new Date(), idNode, idSensor, 5L);
                    data.save();
                }
            }
        }
    }

    private float minValue(List<Data> list) {
        if (list == null || list.size() == 0) {
            return 0;
        } else {
            float value = list.get(0).value;
            for (Data data : list) {
                if (data.value < value) {
                    value = data.value;
                }
            }
            return value;
        }
    }

    private float maxValue(List<Data> list) {
        if (list == null || list.size() == 0) {
            return 0;
        } else {
            float value = list.get(0).value;
            for (Data data : list) {
                if (data.value > value) {
                    value = data.value;
                }
            }
            return value;
        }
    }

    private float calculateMedium(List<Data> list) {
        float value = 0;
        for (Data data : list) {
            value += data.value;
        }
        if (list.size() > 0) {
            return value / list.size();
        } else {
            return 0;
        }
    }
}
