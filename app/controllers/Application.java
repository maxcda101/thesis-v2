package controllers;

import modules.paginate.ModelPaginator;
import play.*;
import play.data.binding.As;
import play.db.jpa.JPA;
import play.mvc.*;

import java.text.SimpleDateFormat;
import java.util.*;

import models.*;
import utils.DateFormat;

import javax.persistence.EntityManager;
import javax.persistence.Query;

public class Application extends Controller {

    @Before(unless = "login")
    private static void checkAuthentification() {
        boolean success = true;
        if (session.contains("uId")) {
            List<Location> listLocation = Location.getLocations(0, 10);
            renderArgs.put("listLocation", listLocation);
            if (!session.contains("idLocation")) {
                session.put("idLocation", listLocation.get(0).id);
            }
        } else {
            login(null, null);
        }
    }

    public static void selectLocation(Long idLocation) {
        session.put("idLocation", idLocation);
        index();
    }

    public static void index() {
        Location location = Location.findById(Long.parseLong(session.get("idLocation")));

        Date date = new Date(new Date().getTime() - 15 * 60000);
        String sdate = "'" + DateFormat.DateToString(date) + "'";

        List<Data> listValue = new ArrayList<Data>();

        List<Sensor> listSensor = Location.getSensor(location.getId(), 0);
        List<Node> listNode = Node.getAllNodeByLocation(location.getId());
        for (Sensor sensor : listSensor) {
            float total = 0;
            int i = 0;
            for (Node node : listNode) {
                List<Data> listData = Data.find(" node_id = ? and sensor_id = ? and timeCreate> ? and typeData_id = 3 order by timeCreate desc", node.id, sensor.id, date).fetch();
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

        float temp = 0, humi = 0, air = 0;
        for (Data data : listValue) {
            if (data.sensor.id == 1l) {
                air = data.value;
            } else if (data.sensor.id == 2l) {
                temp = data.value;
            } else if (data.sensor.id == 3L) {
                humi = data.value;
            }
        }
        render(temp, humi, air, location);
    }

    public static void test() {
        render(renderArgs);
    }

    public static void login(String email, String password) {
        String notification = null;
        if (email == null || password == null) {
            render();
        } else if ("".equals(email) || "".equals(password)) {
            notification = "Bạn chưa nhập email hoặc mật khẩu";
            render(notification);
        } else {
            User user = User.find("byEmailAndPasswordAndIsManager", email, password, true).first();
            if (user == null) {
                notification = "Email hoặc mật khẩu không chính xác";
                render(notification);
            } else {
                session.put("uId", user.id);
                session.put("name", user.name);
                index();
            }
        }

        render();
    }

    public static void logout() {
        session.clear();
        login(null, null);
    }

    public static void analytic(@As("dd-MM-yyyy") Date startDate, @As("dd-MM-yyyy") Date endDate, Long idNode) {
        String notification=null;
        if(startDate==null){
            startDate=new Date();
        }
        if(endDate==null){
            endDate=new Date();
        }
        if (startDate.compareTo(endDate) >= 0) {
            notification = "Thời gian bắt đầu và kết thúc không hợp lệ.";
            render(startDate, endDate, notification);
        }
        List<Node> listNode = Node.getAllNodeByLocation(Long.parseLong(session.get("idLocation")));
        if (idNode == null || idNode == 0l) {
            idNode = listNode.get(0).id;
        }
        ModelPaginator data = null;
        data = new ModelPaginator(Data.class, "node_id = ? and typeData_id = ? and DATE(timeCreate) between ? and ?",idNode,3, startDate, endDate).orderBy("id desc");
        Logger.info("xxxxxxxxxx: "+data.size());
        render(startDate,endDate,data,listNode);
    }

    public static void chart(@As("yyyy-DD-mm") Date date, Long idNode) {
        if (date == null) {
            date = new Date();
        }
        List<Node> listNode = Node.getAllNodeByLocation(Long.parseLong(session.get("idLocation")));
        if (idNode == null || idNode == 0l) {
            idNode = listNode.get(0).id;
        }
        List<Data> tempMedium = Data.getDataByDay(date.getDate(), date.getMonth(), date.getYear(), 2l, idNode, 1l);
        List<Data> tempMin = Data.getDataByDay(date.getDate(), date.getMonth(), date.getYear(), 2l, idNode, 4l);
        List<Data> tempMax = Data.getDataByDay(date.getDate(), date.getMonth(), date.getYear(), 2l, idNode, 5l);

        List<Data> airMedium = Data.getDataByDay(date.getDate(), date.getMonth(), date.getYear(), 1l, idNode, 1l);
        List<Data> airMin = Data.getDataByDay(date.getDate(), date.getMonth(), date.getYear(), 1l, idNode, 4l);
        List<Data> airMax = Data.getDataByDay(date.getDate(), date.getMonth(), date.getYear(), 1l, idNode, 5l);

        List<Data> humiMedium = Data.getDataByDay(date.getDate(), date.getMonth(), date.getYear(), 3l, idNode, 1l);
        List<Data> humiMin = Data.getDataByDay(date.getDate(), date.getMonth(), date.getYear(), 3l, idNode, 4l);
        List<Data> humiMax = Data.getDataByDay(date.getDate(), date.getMonth(), date.getYear(), 3l, idNode, 5l);
        render(listNode, date, tempMedium, tempMin, tempMax, airMedium, airMin, airMax, humiMedium, humiMin, humiMax);
    }

    public static void root() {
        render();
    }

    public static void node() {
        render();
    }

    public static void user() {
        render();
    }


}