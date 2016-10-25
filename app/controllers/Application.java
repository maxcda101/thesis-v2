package controllers;

import modules.paginate.ModelPaginator;
import org.joda.time.MutableDateTime;
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
                List<Data> listData = Data.find("node = ? and sensor = ? and timeCreate> ? and typeData =? order by timeCreate desc", node, sensor, date,TypeData.findById(3l)).fetch();
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

    public static void analytic(@As("dd-MM-yyyy") Date startDate, @As("dd-MM-yyyy") Date endDate, Long idNode, String commandCode) {
        String notification = null;
        if (startDate == null) {
            startDate = new Date();
        }
        if (endDate == null) {
            endDate = new Date();
        }
        if (startDate.compareTo(endDate) > 0) {
            notification = "Thời gian bắt đầu và kết thúc không hợp lệ.";
            render(startDate, endDate, notification);
        }
        List<Node> listNode = Node.getAllNodeByLocation(Long.parseLong(session.get("idLocation")));
        if (idNode == null || idNode == 0l) {
            idNode = listNode.get(0).id;
        }

        ModelPaginator data = null;

        if (commandCode != null && ("action".equalsIgnoreCase(commandCode))) {
            data = new ModelPaginator(Data.class, "node_id = ? and typeData_id = ? and DATE(timeCreate) between ? and ?", idNode, 3, startDate, endDate).orderBy("id desc");
        }

        render(startDate, endDate, data, listNode);
    }

    public static void chart(@As("dd-MM-yyyy") Date date, Long idNode) {
        if (date == null) {
            date = new Date();
        }
        List<Node> listNode = Node.getAllNodeByLocation(Long.parseLong(session.get("idLocation")));
        if (idNode == null || idNode == 0l) {
            idNode = listNode.get(0).id;
        }
        Node node=Node.findById(idNode);
        MutableDateTime timeObj = new MutableDateTime(date);
        int day = timeObj.getDayOfMonth();
        int month = timeObj.getMonthOfYear();
        int year = timeObj.getYear();
        List<Data> tempMedium = Data.getDataByDay(day, month, year, 2l, idNode, 1l);
        List<Data> tempMin = Data.getDataByDay(day, month, year, 2l, idNode, 4l);
        List<Data> tempMax = Data.getDataByDay(day, month, year, 2l, idNode, 5l);

        List<Data> airMedium = Data.getDataByDay(day, month, year, 1l, idNode, 1l);
        List<Data> airMin = Data.getDataByDay(day, month, year, 1l, idNode, 4l);
        List<Data> airMax = Data.getDataByDay(day, month, year, 1l, idNode, 5l);

        List<Data> humiMedium = Data.getDataByDay(day, month, year, 3l, idNode, 1l);
        List<Data> humiMin = Data.getDataByDay(day, month, year, 3l, idNode, 4l);
        List<Data> humiMax = Data.getDataByDay(day, month, year, 3l, idNode, 5l);


        tempMedium = convertData(tempMedium);
        tempMin = convertData(tempMin);
        tempMax = convertData(tempMax);
        airMedium = convertData(airMedium);
        airMin = convertData(airMin);
        airMax = convertData(airMax);
        humiMedium = convertData(humiMedium);
        humiMin = convertData(humiMin);
        humiMax = convertData(humiMax);

        MutableDateTime objectTime=new MutableDateTime(new Date());
        objectTime.addMinutes(-10);
        Date dateNow=objectTime.toDate();
        Data temp=Data.find("node = ? AND sensor = ? AND timeCreate > ? AND typeData= ? order by timeCreate desc", node, Sensor.findById(2l), dateNow, TypeData.findById(3l)).first();
        Data humi=Data.find("node = ? AND sensor = ? AND timeCreate > ? AND typeData= ? order by timeCreate desc", node, Sensor.findById(3l), dateNow, TypeData.findById(3l)).first();
        Data air=Data.find("node = ? AND sensor = ? AND timeCreate > ? AND typeData= ? order by timeCreate desc", node, Sensor.findById(1l), dateNow, TypeData.findById(3l)).first();
        if(temp==null){
            temp=new Data();
            temp.value=-1;
        }
        if(humi==null){
            humi=new Data();
            humi.value=-1;
        }
        if(air==null){
            air=new Data();
            air.value=-1;
        }

        render(temp, humi, air, node,listNode, date, tempMedium, tempMin, tempMax, airMedium, airMin, airMax, humiMedium, humiMin, humiMax);
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

    private static List<Data> convertData(List<Data> listOld) {
        List<Data> listNew = new ArrayList<Data>();

        Data data = new Data(0, null);
        for (int i = 0; i < 24; i++) {
            listNew.add(data);
        }
        for (Data d : listOld) {
            int index = d.timeCreate.getHours();
            listNew.remove(index);
            listNew.add(index, d);
        }
        return listNew;

    }

}