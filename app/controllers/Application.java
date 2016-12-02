package controllers;

import modules.paginate.ModelPaginator;
import org.joda.time.MutableDateTime;
import play.*;
import play.data.binding.As;
import play.data.validation.Required;
import play.db.jpa.JPA;
import play.mvc.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

import models.*;
import utils.DateFormat;

import javax.persistence.EntityManager;
import javax.persistence.Query;

public class Application extends Controller {

    @Before(unless = {"login", "logout"})
    private static void checkAuthentification() {
        boolean success = true;
        if (session.contains("uId")) {
            List<Location> listLocation = null;
            boolean isManager = false;
            if (session.get("role").equals("manager")) {
                listLocation = Location.getLocations(0, 100);
                isManager = true;
            } else if (session.get("role").equals("customer")) {
                Customer customer = Customer.find("byUser_id", session.get("uId")).first();
                listLocation = customer.locations;
                isManager = false;
            }
            if (!session.contains("idLocation")) {
                session.put("idLocation", listLocation.get(0).id);
            }

            renderArgs.put("isManager", isManager);
            renderArgs.put("listLocation", listLocation);
            renderArgs.put("location", Location.findById(Long.parseLong(session.get("idLocation"))));
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
            User user = User.find("byEmailAndPassword", email, password).first();
            if (user == null) {
                notification = "Email hoặc mật khẩu không chính xác";
                render(notification);
            } else {
                session.put("uId", user.id);
                session.put("name", user.name);
                if (user.isManager) {
                    session.put("role", "manager");
                } else {
                    session.put("role", "customer");
                }
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
        if (listNode.size() == 0) {
            index();
        }
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


        for (int i = x; i <= y; i++) {
            labels.add(objTimeStart.getDayOfMonth() + "/" + objTimeStart.getMonthOfYear());
            List<Data> data1 = Data.find("node=? AND sensor = ? AND typeData = ? AND year(timeCreate)=? AND dayofyear(timeCreate)=? ", node, sensor, typeMedium, objTimeStart.getYear(), objTimeStart.getDayOfYear()).fetch();
            dataMedium.add(handerData(data1, 1));
            List<Data> data2 = Data.find("node=? AND sensor = ? AND typeData = ? AND year(timeCreate)=? AND dayofyear(timeCreate)=? ", node, sensor, typeMax, objTimeStart.getYear(), objTimeStart.getDayOfYear()).fetch();
            dataMax.add(handerData(data2, 2));
            List<Data> data3 = Data.find("node=? AND sensor = ? AND typeData = ? AND year(timeCreate)=? AND dayofyear(timeCreate)=? ", node, sensor, typeMin, objTimeStart.getYear(), objTimeStart.getDayOfYear()).fetch();
            dataMin.add(handerData(data3, 3));

            objTimeStart.addDays(1);
        }
        render(startDate, endDate, notification, listNode, sensorList, dataMedium, dataMax, dataMin, labels, idSensor);

    }

    public static void realtimeNode(Long idNode) {
        List<Node> listNode = Node.getAllNodeByLocation(Long.parseLong(session.get("idLocation")));
        if (listNode.size() == 0) {
            index();
        }
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
            nodes = Node.find("byRoot", Root.findById(idRoot)).fetch();
        }
        List<Sensor> sensors;
        List<List<Sensor>> listSensor = new ArrayList<List<Sensor>>();

        for (Node node : nodes) {
            Query query = JPA.em().createNativeQuery("SELECT distinct Sensor.id, Sensor.description, Sensor.name, Sensor.type FROM Sensor inner join Data on Data.sensor_id=Sensor.id where Data.node_id=?", Sensor.class);
            query.setParameter(1, node.id);
            sensors = query.getResultList();
            listSensor.add(sensors);
        }
        Logger.info("xxxxxxxxxxxx: " + listSensor.size());

        render(roots, nodes, listSensor);
    }

    public static void hardware() {
        Location location = Location.findById(Long.parseLong(session.get("idLocation")));
        List<Sensor> sensors = location.sensors;
        render(sensors);
    }

    public static void user() {
        Location location = Location.findById(Long.parseLong(session.get("idLocation")));
        Query query = JPA.em().createNativeQuery("SELECT * FROM Customer INNER JOIN Customer_Location ON Customer_Location.Customer_id= Customer.id Where Customer_Location.locations_id=? ", Customer.class);
        query.setParameter(1, location.id);
        List<Customer> customers = query.getResultList();

        render(customers);
    }

    public static void reportDay(@As("dd-MM-yyyy") Date date) {
        if (date == null) {
            date = new Date();
        }
        MutableDateTime objTime = new MutableDateTime(date);

        TypeData typeMedium = TypeData.findById(1l);
        TypeData typeNow = TypeData.findById(3l);

        Sensor sensorAir = Sensor.findById(1l);
        Sensor sensorTemp = Sensor.findById(2l);
        Sensor sensorHumi = Sensor.findById(3l);

        List<Node> listNodes = Node.getAllNodeByLocation(Long.parseLong(session.get("idLocation")));
        if(listNodes.size()==0){
            index();
        }
        String listNode="";
        for (Node node : listNodes) {
            listNode=listNode+ node.id+",";
        }
        listNode=listNode.substring(0, listNode.length()-1);
        List<Data> airMedium = Data.find("node_id IN (?) AND dayofyear(timeCreate) = ? AND typeData = ? AND sensor = ?",listNode, objTime.getDayOfYear(), typeMedium, sensorAir).fetch();
        Data airMin = Data.find("node_id IN (?) AND dayofyear(timeCreate) = ? AND typeData = ? AND sensor = ? order by value asc", listNode, objTime.getDayOfYear(), typeNow, sensorAir).first();
        Data airMax = Data.find("node_id IN (?) AND dayofyear(timeCreate) = ? AND typeData = ? AND sensor = ? order by value desc", listNode, objTime.getDayOfYear(), typeNow, sensorAir).first();

        List<Data> tempMedium = Data.find("node_id IN (?) AND dayofyear(timeCreate) = ? AND typeData = ? AND sensor = ?", listNode, objTime.getDayOfYear(), typeMedium, sensorTemp).fetch();
        Data tempMin = Data.find("node_id IN (?) AND dayofyear(timeCreate) = ? AND typeData = ? AND sensor = ? order by value asc", listNode, objTime.getDayOfYear(), typeNow, sensorTemp).first();
        Data tempMax = Data.find("node_id IN (?) AND dayofyear(timeCreate) = ? AND typeData = ? AND sensor = ? order by value desc", listNode, objTime.getDayOfYear(), typeNow, sensorTemp).first();

        List<Data> humiMedium = Data.find("node_id IN (?) AND dayofyear(timeCreate) = ? AND typeData = ? AND sensor = ?", listNode, objTime.getDayOfYear(), typeMedium, sensorHumi).fetch();
        Data humiMin = Data.find("node_id IN (?) AND dayofyear(timeCreate) = ? AND typeData = ? AND sensor = ? order by value asc", listNode, objTime.getDayOfYear(), typeNow, sensorHumi).first();
        Data humiMax = Data.find("node_id IN (?) AND dayofyear(timeCreate) = ? AND typeData = ? AND sensor = ? order by value desc", listNode, objTime.getDayOfYear(), typeNow, sensorHumi).first();

        java.text.DateFormat dateFormat = new SimpleDateFormat("HH:mm");
        String sairMedium = null;
        String sairMax = null;
        String sairMin = null;
        String airTotal = null;
        if (airMedium.size() > 0 && airMax != null) {
            float fair = handerData(airMedium, 1);
            sairMedium = "Chất lượng không khí trung bình trong ngày ở mức " + fair + "/100";
            sairMin = "Chất lượng không khí tốt nhất trong ngày ở mức " + airMin.value + "/100 vào lúc " + dateFormat.format(airMin.timeCreate);
            sairMax = "Chất lượng không khí cao nhất trong ngày ở mức " + airMax.value + "/100 vào lúc " + dateFormat.format(airMax.timeCreate);
            airTotal = "";
            if (fair < 30) {
                airTotal = "Trung bình chất lượng không khí trong ngày ở mức tốt cho sức khỏe (từ 0 đến 30)";
                if (airMax.value > 30) {
                    airTotal += ", tuy nhiên có một số thời điểm chất lượng không khi bị ô nhiễm";
                }
            } else {
                airTotal = "Chất lượng không khí trung bình ở mức bị ô nhiễm và không tốt cho sức khỏe";
            }
        }
        String stempMedium = null;
        String stempMax = null;
        String stempMin = null;
        String tempTotal = null;
        if (tempMedium.size() > 0 && tempMax != null && tempMin != null) {

            float ftemp = handerData(tempMedium, 1);
            stempMedium = "Nhiệt độ trung bình trong ngày ở mức " + ftemp + "°C";
            stempMax = "Nhiệt độ cao nhất trong ngày ở mức " + tempMax.value + "°C vào lúc " + dateFormat.format(tempMax.timeCreate);
            stempMin = "Nhiệt độ thấp nhất trong ngày ở mức " + tempMin.value + "°C vào lúc " + dateFormat.format(tempMin.timeCreate);
            tempTotal = "";
            if (ftemp < 22) {
                tempTotal = "Nhiệt độ trung bình trong ngày thấp hơn ngưỡng tốt cho sức khỏe (22°C)";
            } else if (ftemp > 27) {
                tempTotal = "Nhiệt độ trung bình trong ngày cao hơn ngưỡng tốt cho sức khỏe (27°C)";
            } else {
                tempTotal = "Nhiệt độ trung bình trong ngày ở ngưỡng tốt cho sức khỏe (22°C đến 27°C)";
                if (tempMin.value < 22 && tempMax.value > 27) {
                    tempTotal += ", tuy nhiên biên độ nhiệt trong ngày chênh lệch khá cao";
                } else {
                    if (tempMin.value < 22) {
                        tempTotal += ", tuy nhiên có một số thời điểm nhiệt độ thấp hơn mức tốt cho sức khỏe";
                    }
                    if (tempMax.value > 27) {
                        tempTotal += ", tuy nhiên có một số thời điểm nhiệt độ cao hơn mức tốt cho sức khỏe";
                    }
                }
            }
        }
        String shumiMedium = null;
        String shumiMax = null;
        String shumiMin = null;
        String humiTotal = null;
        if (humiMedium.size() > 0 && humiMax != null && humiMin != null) {

            float fhumi = handerData(humiMedium, 1);
            shumiMedium = "Độ ẩm trung bình trong ngày ở mức " + fhumi + "%";
            shumiMax = "Độ ẩm cao nhất trong ngày ở mức " + humiMax.value + "% vào lúc " + dateFormat.format(humiMax.timeCreate);
            shumiMin = "Độ ẩm thấp nhất trong ngày ở mức " + humiMin.value + "% vào lúc " + dateFormat.format(humiMin.timeCreate);
            humiTotal = "";
            if (fhumi < 50) {
                humiTotal = "Độ ẩm trung bình trong ngày thấp hơn ngưỡng tốt cho sức khỏe (50%)";
            } else if (fhumi > 70) {
                humiTotal = "Độ ẩm trung bình trong ngày cao hơn ngưỡng tốt cho sức khỏe (70%)";
            } else {
                humiTotal = "Độ ẩm trung bình trong ngày ở ngưỡng tốt cho sức khỏe (50% đến 70%)";
                if (humiMin.value < 50 && humiMax.value > 70) {
                    humiTotal += ", tuy nhiên biên độ độ ẩm trong ngày chênh lệch khá cao";
                } else {
                    if (humiMin.value < 50) {
                        humiTotal += ", tuy nhiên có một số thời điểm độ ẩm thấp hơn mức tốt cho sức khỏe";
                    }
                    if (humiMax.value > 70) {
                        humiTotal += ", tuy nhiên có một số thời điểm độ ẩm cao hơn mức tốt cho sức khỏe";
                    }
                }
            }
        }
        render(date, sairMedium,sairMin, sairMax, airTotal, stempMedium, stempMax, stempMin, tempTotal, shumiMedium, shumiMax, shumiMin, humiTotal);
    }

    public static void reportWeek(@As("dd-MM-yyyy") Date date) {
        if (date == null) {
            MutableDateTime objTime = new MutableDateTime(new Date());
            objTime.setSecondOfDay(0);
            objTime.addDays(-7);
            date=objTime.toDate();
        }

        TypeData typeMedium = TypeData.findById(1l);
        TypeData typeNow = TypeData.findById(3l);

        Sensor sensorAir = Sensor.findById(1l);
        Sensor sensorTemp = Sensor.findById(2l);
        Sensor sensorHumi = Sensor.findById(3l);


        MutableDateTime objTime = new MutableDateTime(date);
        objTime.setSecondOfDay(0);
        Date startDate=objTime.toDate();
        objTime.addDays(7);
        Date endDate=objTime.toDate();


        List<Node> listNodes = Node.getAllNodeByLocation(Long.parseLong(session.get("idLocation")));
        if(listNodes.size()==0){
            index();
        }
        String listNode="";
        for (Node node : listNodes) {
            listNode=listNode+ node.id+",";
        }
        listNode=listNode.substring(0, listNode.length()-1);

        List<Data> airMedium = Data.find("node_id IN (?) AND (timeCreate between ? and ?) AND typeData = ? AND sensor = ?", listNode, startDate,endDate, typeMedium, sensorAir).fetch();
        Data airMin = Data.find("node_id IN (?) AND (timeCreate between ? and ?) AND typeData = ? AND sensor = ? order by value asc", listNode, startDate,endDate, typeNow, sensorAir).first();
        Data airMax = Data.find("node_id IN (?) AND (timeCreate between ? and ?) AND typeData = ? AND sensor = ? order by value desc", listNode, startDate,endDate, typeNow, sensorAir).first();

        List<Data> tempMedium = Data.find("node_id IN (?) AND (timeCreate between ? and ?) AND typeData = ? AND sensor = ?", listNode, startDate,endDate, typeMedium, sensorTemp).fetch();
        Data tempMin = Data.find("node_id IN (?) AND (timeCreate between ? and ?) AND typeData = ? AND sensor = ? order by value asc", listNode, startDate,endDate, typeNow, sensorTemp).first();
        Data tempMax = Data.find("node_id IN (?) AND (timeCreate between ? and ?) AND typeData = ? AND sensor = ? order by value desc", listNode, startDate,endDate, typeNow, sensorTemp).first();

        List<Data> humiMedium = Data.find("node_id IN (?) AND (timeCreate between ? and ?) AND typeData = ? AND sensor = ?", listNode, startDate,endDate, typeMedium, sensorHumi).fetch();
        Data humiMin = Data.find("node_id IN (?) AND (timeCreate between ? and ?) AND typeData = ? AND sensor = ? order by value asc", listNode, startDate,endDate, typeNow, sensorHumi).first();
        Data humiMax = Data.find("node_id IN (?) AND (timeCreate between ? and ?) AND typeData = ? AND sensor = ? order by value desc", listNode, startDate,endDate, typeNow, sensorHumi).first();

        java.text.DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyy HH:mm");
        String sairMedium = null;
        String sairMmin = null;
        String sairMax = null;
        String airTotal = null;
        if (airMedium != null && airMax != null) {
            float fair = handerData(airMedium, 1);
            sairMedium = "Chất lượng không khí trung bình trong tuần ở mức " + fair + "/100";
            sairMmin = "Chất lượng không khí tốt nhất trong tuần ở mức " + airMin.value + "/100 vào lúc " + dateFormat.format(airMin.timeCreate);
            sairMax = "Chất lượng không khí cao nhất trong tuần ở mức " + airMax.value + "/100 vào lúc " + dateFormat.format(airMax.timeCreate);
            airTotal = "";
            if (fair < 30) {
                airTotal = "Trung bình chất lượng không khí trong tuần ở mức tốt cho sức khỏe (từ 0 đến 30)";
                if (airMax.value > 30) {
                    airTotal += ", tuy nhiên có một số thời điểm chất lượng không khi bị ô nhiễm";
                }
            } else {
                airTotal = "Chất lượng không khí trung bình ở mức bị ô nhiễm và không tốt cho sức khỏe";
            }
        }
        String stempMedium = null;
        String stempMax = null;
        String stempMin = null;
        String tempTotal = null;
        if (tempMedium.size() > 0 && tempMax != null && tempMin != null) {

            float ftemp = handerData(tempMedium, 1);
            stempMedium = "Nhiệt độ trung bình trong tuần ở mức " + ftemp + "°C";
            stempMax = "Nhiệt độ cao nhất trong tuần ở mức " + tempMax.value + "°C vào lúc " + dateFormat.format(tempMax.timeCreate);
            stempMin = "Nhiệt độ thấp nhất trong tuần ở mức " + tempMin.value + "°C vào lúc " + dateFormat.format(tempMin.timeCreate);
            tempTotal = "";
            if (ftemp < 22) {
                tempTotal = "Nhiệt độ trung bình trong tuần thấp hơn ngưỡng tốt cho sức khỏe (22°C)";
            } else if (ftemp > 27) {
                tempTotal = "Nhiệt độ trung bình trong tuần cao hơn ngưỡng tốt cho sức khỏe (27°C)";
            } else {
                tempTotal = "Nhiệt độ trung bình trong tuần ở ngưỡng tốt cho sức khỏe (22°C đến 27°C)";
                if (tempMin.value < 22 && tempMax.value > 27) {
                    tempTotal += ", tuy nhiên biên độ nhiệt trong tuần chênh lệch khá cao";
                } else {
                    if (tempMin.value < 22) {
                        tempTotal += ", tuy nhiên có một số thời điểm nhiệt độ thấp hơn mức tốt cho sức khỏe";
                    }
                    if (tempMax.value > 27) {
                        tempTotal += ", tuy nhiên có một số thời điểm nhiệt độ cao hơn mức tốt cho sức khỏe";
                    }
                }
            }
        }
        String shumiMedium = null;
        String shumiMax = null;
        String shumiMin = null;
        String humiTotal = null;
        if (humiMedium.size() > 0 && humiMax != null && humiMin != null) {

            float fhumi = handerData(humiMedium, 1);
            shumiMedium = "Độ ẩm trung bình trong tuần ở mức " + fhumi + "%";
            shumiMax = "Độ ẩm cao nhất trong tuần ở mức " + humiMax.value + "% vào lúc " + dateFormat.format(humiMax.timeCreate);
            shumiMin = "Độ ẩm thấp nhất trong tuần ở mức " + humiMin.value + "% vào lúc " + dateFormat.format(humiMin.timeCreate);
            humiTotal = "";
            if (fhumi < 50) {
                humiTotal = "Độ ẩm trung bình trong tuần thấp hơn ngưỡng tốt cho sức khỏe (50%)";
            } else if (fhumi > 70) {
                humiTotal = "Độ ẩm trung bình trong tuần cao hơn ngưỡng tốt cho sức khỏe (70%)";
            } else {
                humiTotal = "Độ ẩm trung bình trong tuần ở ngưỡng tốt cho sức khỏe (50% đến 70%)";
                if (humiMin.value < 50 && humiMax.value > 70) {
                    humiTotal += ", tuy nhiên biên độ độ ẩm trong tuần chênh lệch khá cao";
                } else {
                    if (humiMin.value < 50) {
                        humiTotal += ", tuy nhiên có một số thời điểm độ ẩm thấp hơn mức tốt cho sức khỏe";
                    }
                    if (humiMax.value > 70) {
                        humiTotal += ", tuy nhiên có một số thời điểm độ ẩm cao hơn mức tốt cho sức khỏe";
                    }
                }
            }
        }
        render(date, startDate, endDate, sairMedium,sairMmin, sairMax, airTotal, stempMedium, stempMax, stempMin, tempTotal, shumiMedium, shumiMax, shumiMin, humiTotal);

    }
    public static void locations(){
        List<Location> locations=Location.findAll();
        render(locations);
    }
    public static void editRole(Long idCustomer){
        if(!session.get("role").equals("manager")){
            user();
        }
        Customer customer=Customer.findById(idCustomer);
        if(customer!=null){
            if(customer.isLocationManager){
                customer.isLocationManager=false;
            }else{
                customer.isLocationManager=true;
            }
            customer.save();
        }
        user();
    }
    public static void download(){
        File file1 = new File("public/download/app.apk");
        if (file1.exists()) {
            InputStream is = null;
            try {
                is = new FileInputStream(file1);
            } catch (FileNotFoundException e) {
                renderJSON("Lỗi tải tập tin");
            }
            renderBinary(is, "Air Quality Monitoring.apk");
        } else {
            renderJSON("Tập tin không tồn tại");
        }
    }

    private static List<Data> convertData(List<Data> listOld) {
        List<Data> listNew = new ArrayList<Data>();

        Data data = new Data();
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