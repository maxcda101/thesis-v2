package controllers;

import modules.paginate.ModelPaginator;
import org.joda.time.MutableDateTime;
import play.*;
import play.data.binding.As;
import play.data.validation.Required;
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
                List<Data> listData = Data.find("node = ? and sensor = ? and timeCreate> ? and typeData =? order by timeCreate desc", node, sensor, date, TypeData.findById(3l)).fetch();
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

    public static void analytic(@As("dd-MM-yyyy") Date startDate, @As("dd-MM-yyyy") Date endDate, @Required Long idNode, @Required String commandCode, @Required Long idSensor) {
        String notification = null;
        List<Sensor> sensorList = Location.getSensor(Long.parseLong(session.get("idLocation")), 0);
        List<Node> listNode = Node.getAllNodeByLocation(Long.parseLong(session.get("idLocation")));
        if (startDate == null) {
            startDate = new Date();
        }
        if (endDate == null) {
            endDate = new Date();
        }
        if (startDate.compareTo(endDate) > 0) {
            notification = "Thời gian bắt đầu và kết thúc không hợp lệ.";
            render(startDate, endDate, notification, listNode, sensorList);
        }
        if (validation.hasErrors()) {
            notification = "Vui lòng chọn Node và Sensor";
            render(startDate, endDate, notification, listNode, sensorList);
        }

        MutableDateTime objTime = new MutableDateTime(endDate);
        objTime.addDays(1);
        ModelPaginator data = null;

        if (commandCode != null && ("action".equalsIgnoreCase(commandCode))) {
            if (idSensor == -1) {
                data = new ModelPaginator(Data.class, "node = ? and typeData = ? and timeCreate between ? and ?", Node.findById(idNode), TypeData.findById(3l), startDate, objTime.toDate()).orderBy("timeCreate desc");
            } else {
                data = new ModelPaginator(Data.class, "sensor_id = ? and node_id = ? and typeData_id = ? and timeCreate between ? and ?", idSensor, idNode, 3, startDate, objTime.toDate()).orderBy("timeCreate desc");
            }
        }

        render(sensorList, startDate, endDate, data, listNode);
    }

    public static void dangerous(@As("dd-MM-yyyy") Date startDate, @As("dd-MM-yyyy") Date endDate, @Required Long idNode, @Required String commandCode, @Required Long idSensor) {
        String notification = null;
        List<Sensor> sensorList = Location.getSensor(Long.parseLong(session.get("idLocation")), 0);
        List<Node> listNode = Node.getAllNodeByLocation(Long.parseLong(session.get("idLocation")));
        if (startDate == null) {
            startDate = new Date();
        }
        if (endDate == null) {
            endDate = new Date();
        }
        if (startDate.compareTo(endDate) > 0) {
            notification = "Thời gian bắt đầu và kết thúc không hợp lệ.";
            render(startDate, endDate, notification, listNode, sensorList);
        }
        if (validation.hasErrors()) {
            notification = "Vui lòng chọn Node và Sensor";
            render(startDate, endDate, notification, listNode, sensorList);
        }

        MutableDateTime objTime = new MutableDateTime(endDate);
        objTime.addDays(1);
        ModelPaginator data = null;

        if (commandCode != null && ("action".equalsIgnoreCase(commandCode))) {
            if (idSensor == 1) {
                data = new ModelPaginator(Data.class, "value > 30 AND sensor_id = ? and node_id = ? and typeData_id = ? and timeCreate between ? and ?", idSensor, idNode, 3, startDate, objTime.toDate()).orderBy("timeCreate desc");
            } else if (idSensor == 2) {
                data = new ModelPaginator(Data.class, "(value < 22 OR value > 27) AND sensor_id = ? and node_id = ? and typeData_id = ? and timeCreate between ? and ?", idSensor, idNode, 3, startDate, objTime.toDate()).orderBy("timeCreate desc");
            } else if (idSensor == 3) {
                data = new ModelPaginator(Data.class, "(value < 50 OR value > 70) AND sensor_id = ? and node_id = ? and typeData_id = ? and timeCreate between ? and ?", idSensor, idNode, 3, startDate, objTime.toDate()).orderBy("timeCreate desc");
            }
        }

        render(sensorList, startDate, endDate, data, listNode);
    }

    public static void chart(@As("dd-MM-yyyy") Date date, Long idNode) {
        if (date == null) {
            date = new Date();
        }
        List<Node> listNode = Node.getAllNodeByLocation(Long.parseLong(session.get("idLocation")));
        if (idNode == null || idNode == 0l) {
            idNode = listNode.get(0).id;
        }
        Node node = Node.findById(idNode);
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


        render(node, listNode, date, tempMedium, tempMin, tempMax, airMedium, airMin, airMax, humiMedium, humiMin, humiMax);
    }

    public static void partOfTheDay(@As("dd-MM-yyyy") Date startDate, @As("dd-MM-yyyy") Date endDate, @Required Long idNode, @Required String commandCode, @Required Long idSensor) {
        String notification = null;
        List<Sensor> sensorList = Location.getSensor(Long.parseLong(session.get("idLocation")), 0);
        List<Node> listNode = Node.getAllNodeByLocation(Long.parseLong(session.get("idLocation")));
        if (startDate == null) {
            startDate = new Date();
        }
        if (endDate == null) {
            endDate = new Date();
        }
        MutableDateTime objTimeStart = new MutableDateTime(startDate);
        MutableDateTime objTimeEnd = new MutableDateTime(endDate);
        if (startDate.compareTo(endDate) > 0) {
            notification = "Thời gian bắt đầu và kết thúc không hợp lệ.";
            render(startDate, endDate, notification, listNode, sensorList);
        }
        if (validation.hasErrors()) {
            notification = "Vui lòng chọn Node và Sensor";
            render(startDate, endDate, notification, listNode, sensorList);
        }
        int x = objTimeStart.getDayOfYear();
        int y = objTimeEnd.getDayOfYear();

        List<Float> dataMedium = new ArrayList<Float>();
        List<Float> dataMin = new ArrayList<Float>();
        List<Float> dataMax = new ArrayList<Float>();
        List<String> labels = new ArrayList<String>();
        TypeData typeMedium = TypeData.find("name =?", "medium 1h").first();
        TypeData typeMax = TypeData.find("name =?", "max 1h").first();
        TypeData typeMin = TypeData.find("name =?", "min 1h").first();
        Sensor sensor = Sensor.findById(idSensor);
        Node node = Node.findById(idNode);
        objTimeStart.setHourOfDay(0);
        objTimeStart.setMinuteOfHour(0);

        for (int i = x; i <= y; i++) {
            Date s = objTimeStart.toDate();
            objTimeStart.addHours(8);
            Date e = objTimeStart.toDate();
            labels.add("Sang " + objTimeStart.getDayOfMonth() + "/" + objTimeStart.getMonthOfYear());
            List<Data> data1 = Data.find("node=? AND sensor = ? AND typeData = ? AND timeCreate between ? and ?", node, sensor, typeMedium, s, e).fetch();
            dataMedium.add(handerData(data1, 1));
            List<Data> data2 = Data.find("node=? AND sensor = ? AND typeData = ? AND timeCreate between ? and ?", node, sensor, typeMax, s, e).fetch();
            dataMax.add(handerData(data2, 2));
            List<Data> data3 = Data.find("node=? AND sensor = ? AND typeData = ? AND timeCreate between ? and ?", node, sensor, typeMin, s, e).fetch();
            dataMin.add(handerData(data3, 3));

            labels.add("Trưa " + objTimeStart.getDayOfMonth() + "/" + objTimeStart.getMonthOfYear());
            s = e;
            objTimeStart.addHours(8);
            e = objTimeStart.toDate();
            List<Data> data4 = Data.find("node=? AND sensor = ?AND typeData = ? AND timeCreate between ? and ?", node, sensor, typeMedium, s, e).fetch();
            dataMedium.add(handerData(data4, 1));
            List<Data> data5 = Data.find("node=? AND sensor = ?AND typeData = ? AND timeCreate between ? and ?", node, sensor, typeMax, s, e).fetch();
            dataMax.add(handerData(data5, 2));
            List<Data> data6 = Data.find("node=? AND sensor = ?AND typeData = ? AND timeCreate between ? and ?", node, sensor, typeMin, s, e).fetch();
            dataMin.add(handerData(data6, 3));

            labels.add("Tối " + objTimeStart.getDayOfMonth() + "/" + objTimeStart.getMonthOfYear());
            s = e;
            objTimeStart.addHours(8);
            e = objTimeStart.toDate();
            List<Data> data7 = Data.find("node=? AND sensor = ?AND typeData = ? AND timeCreate between ? and ?", node, sensor, typeMedium, s, e).fetch();
            dataMedium.add(handerData(data7, 1));
            List<Data> data8 = Data.find("node=? AND sensor = ?AND typeData = ? AND timeCreate between ? and ?", node, sensor, typeMax, s, e).fetch();
            dataMax.add(handerData(data8, 2));
            List<Data> data9 = Data.find("node=? AND sensor = ?AND typeData = ? AND timeCreate between ? and ?", node, sensor, typeMin, s, e).fetch();
            dataMin.add(handerData(data9, 3));
        }
        render(startDate, endDate, notification, listNode, sensorList, dataMedium, dataMax, dataMin, labels);

    }

    public static void realtimeNode(Long idNode) {
        List<Node> listNode = Node.getAllNodeByLocation(Long.parseLong(session.get("idLocation")));
        if (idNode == null || idNode == 0l) {
            idNode = listNode.get(0).id;
        }
        Node node = Node.findById(idNode);
        MutableDateTime objectTime = new MutableDateTime(new Date());
        objectTime.addMinutes(-10);
        Date dateNow = objectTime.toDate();
        Data temp = Data.find("node = ? AND sensor = ? AND timeCreate > ? AND typeData= ? order by timeCreate desc", node, Sensor.findById(2l), dateNow, TypeData.findById(3l)).first();
        Data humi = Data.find("node = ? AND sensor = ? AND timeCreate > ? AND typeData= ? order by timeCreate desc", node, Sensor.findById(3l), dateNow, TypeData.findById(3l)).first();
        Data air = Data.find("node = ? AND sensor = ? AND timeCreate > ? AND typeData= ? order by timeCreate desc", node, Sensor.findById(1l), dateNow, TypeData.findById(3l)).first();
        if (temp == null) {
            temp = new Data();
            temp.value = -1;
        }
        if (humi == null) {
            humi = new Data();
            humi.value = -1;
        }
        if (air == null) {
            air = new Data();
            air.value = -1;
        }
        render(temp, humi, air, node, listNode);
    }

    public static void root() {
        Location location = Location.findById(Long.parseLong(session.get("idLocation")));
        List<Root> roots = Root.find("byLocation", location).fetch();
        render(roots);
    }

    public static void node(Long idRoot) {
        Location location = Location.findById(Long.parseLong(session.get("idLocation")));
        List<Root> roots = Root.find("byLocation", location).fetch();
        List<Node> nodes = null;
        if (idRoot == null || idRoot == 0) {
            nodes = Node.getAllNodeByLocation(location.id);
        } else {
            nodes  = Node.find("byRoot", Root.findById(idRoot)).fetch();
        }
        List<Sensor> sensors;
        List<List<Sensor>> listSensor=new ArrayList<List<Sensor>>();

        for(Node node: nodes){
            Query query=JPA.em().createNativeQuery("SELECT distinct Sensor.id, Sensor.description, Sensor.name, Sensor.type FROM Sensor inner join Data on Data.sensor_id=Sensor.id where Data.node_id=?",Sensor.class);
            query.setParameter(1,node.id);
            sensors=query.getResultList();
            listSensor.add(sensors);
        }
        Logger.info("xxxxxxxxxxxx: "+listSensor.size());

        render(roots, nodes,listSensor);
    }

    public static void hardware() {
        Location location = Location.findById(Long.parseLong(session.get("idLocation")));
        List<Sensor> sensors=location.sensors;
        render(sensors);
    }

    public static void user() {
        Location location = Location.findById(Long.parseLong(session.get("idLocation")));
        Query query = JPA.em().createNativeQuery("SELECT * FROM Customer INNER JOIN Customer_Location ON Customer_Location.Customer_id= Customer.id Where Customer_Location.locations_id=? ", Customer.class);
        query.setParameter(1, location.id);
        List<Customer> customers = query.getResultList();

        render(customers);
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

    private static float handerData(List<Data> datas, int type) {
        float result = 0;
        if (datas == null || datas.size() == 0) {
            //nothing
        } else {

            if (type == 1) {
                for (Data data : datas) {
                    result += data.value;
                }
                result = result / datas.size();
            } else if (type == 2) {
                result = datas.get(0).value;
                for (Data data : datas) {
                    if (data.value > result) {
                        result = data.value;
                    }
                }
            } else if (type == 3) {
                result = datas.get(0).value;
                for (Data data : datas) {
                    if (data.value < result) {
                        result = data.value;
                    }
                }
            }
        }
        return result;
    }

}