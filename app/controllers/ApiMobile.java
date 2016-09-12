package controllers;

import models.Data;
import models.Node;
import models.Sensor;
import play.data.validation.Required;
import play.db.jpa.JPA;
import play.mvc.Controller;
import utils.DateFormat;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by AnhQuan on 9/6/2016.
 */
public class ApiMobile extends Controller {
    public static void index() {
        renderJSON("Mobile api");
    }

    public static void dataMedium(@Required Long idLocation) {
        Date date = new Date(new Date().getTime() - 15 * 60000);
        String sdate = "'" + DateFormat.DateToString(date) + "'";

        List<Data> listValue = new ArrayList<Data>();
        EntityManager em = JPA.em();

        List<Sensor> listSensor = Sensor.findAll();
        List<Node> listNode = Node.getAllNodeByLocation(idLocation);
        for (Sensor sensor : listSensor) {
            float total = 0;
            int i = 0;
            for (Node node : listNode) {
                String sql = "SELECT * FROM Data WHERE node_id=" + node.id + " and sensor_id=" + sensor.id + " and timeCreate >" + sdate + " and typeData_id=" + 3 + " order by timeCreate desc limit 1";
                Query query = em.createNativeQuery(sql, Data.class);
                List<Data> listData = query.getResultList();
                if (!listData.isEmpty()) {
                    Data data = listData.get(0);
                    total += data.value;
                    i++;
                }
            }
            if (i == 0) {
                listValue.add(new Data(-1, sensor));
            } else {
                listValue.add(new Data(total / i, sensor));
            }
        }
        renderJSON(listValue);
    }

    public static void sendFCM(String title, String body) {
        ApiEmbed.sendNotification(1L,title, body);
        renderJSON("ok ");
    }

}
