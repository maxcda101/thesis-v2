package controllers;

import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.internal.com.fasterxml.jackson.databind.util.JSONPObject;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import entities.Response;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jobs.SendFCM;
import models.*;
import play.Logger;
import play.data.validation.Required;
import play.db.jpa.JPA;
import play.mvc.Controller;
import stateful.Stateful;
import token.JWTEncoder;
import token.TokenGenerator;
import token.TokenOptions;
import utils.DateFormat;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.*;

/**
 * Created by AnhQuan on 9/6/2016.
 */
public class ApiMobile extends Controller {
    public static void index() {
        renderJSON("Mobile api");
    }

    public static void getListDataNode(@Required Long idLocation){
        if (validation.hasErrors()) {
            renderJSON(new Response(2, "Validate error"));
        }
        Date date = new Date(new Date().getTime() - 15 * 60000);
        String sdate = "'" + DateFormat.DateToString(date) + "'";

        List<Data> listValue = new ArrayList<Data>();
        EntityManager em = JPA.em();

        List<Sensor> listSensor = Location.getSensor(idLocation,0);
        List<Node> listNode = Node.getAllNodeByLocation(idLocation);
        for(Node node : listNode){
            for(Sensor sensor: listSensor){
                String sql = "SELECT * FROM Data WHERE node_id=" + node.id + " and sensor_id=" + sensor.id + " and timeCreate >" + sdate + " and typeData_id=" + 3 + " order by timeCreate desc limit 1";
                Query query = em.createNativeQuery(sql, Data.class);
                List<Data> listData = query.getResultList();
                if (!listData.isEmpty()) {
                    Data data = listData.get(0);
                    data.node.root=null;
                    data.typeData=null;
                    data.timeCreate=null;
                    data.timeReceived=null;
                    listValue.add(data);
                }else{
                    Data data=new Data();
                    data.node=node;
                    data.sensor=sensor;
                    data.value=-1;
                    listValue.add(data);
                }
            }
        }
        renderJSON(listValue);
    }
    public static void getDataByDay(int day, int month, int year, @Required Long idSensor, @Required Long idNode, @Required Long idTypeData) {
        if (day == 0 || month == 0 || year == 0) {
            renderJSON(new Response(2,"Date format exception"));
        }
        renderJSON(Data.getDataByDay(day, month, year, idSensor,idNode,idTypeData));
    }
    public static void getDataNow(@Required Long idSensor, @Required Long idNode) {
        Date date = new Date(new Date().getTime() - 10 * 60000);
        String sdate = "'" + DateFormat.DateToString(date) + "'";
        Data data=Data.find(" sensor_id="+idSensor+" AND node_id="+idNode +" AND timeCreate >"+sdate+" AND  typeData_id=3 order by timeCreate desc" ).first();
        if(data==null){
            data=new Data();
            data.value=0;
            renderJSON(data);
        }else{
            data.node=null;
            data.sensor=null;
            data.typeData=null;
            data.timeReceived=null;
            renderJSON(data);
        }
    }
    public static void dataMedium(@Required Long idLocation) {
        if (validation.hasErrors()) {
            renderJSON(new Response(2, "Validate error"));
        }
        Date date = new Date(new Date().getTime() - 15 * 60000);
        String sdate = "'" + DateFormat.DateToString(date) + "'";

        List<Data> listValue = new ArrayList<Data>();
        EntityManager em = JPA.em();

        List<Sensor> listSensor = Location.getSensor(idLocation,0);
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
//        ApiEmbed.sendNotification(1L,title, body);
        new SendFCM(1L,title, body).doJob();
        renderJSON("ok ");
    }
    public static void register(@Required String email,@Required String password,@Required String name,String address,String phone,String codeLocation){
        if (validation.hasErrors()) {
            renderJSON(new Response(2, "Validate error"));
        }
        if(User.find("byEmail",email).fetch().size()>0){
            renderJSON(new Response(2,"Email existed"));
        }

        User user=new User(email,password,name,address,phone,false);
        List<KeyLocation> listLocation=KeyLocation.find("byCode",codeLocation).fetch();
        if(listLocation==null||listLocation.size()==0){
            renderJSON(new Response(2,"Code location invalid"));
        }
        KeyLocation keyLocation= listLocation.get(0);
        Customer customer=new Customer(user,false);
        customer.addLocation(keyLocation.location);
        user.save();
        customer.save();
        renderJSON(new Response(1,"Register sucess"));
    }
    public static void loginAdmin(@Required String email,@Required String password){
        if (validation.hasErrors()) {
            renderJSON(new Response(2, "Validate error"));
        }
        List<User> listUser=User.find("byEmail",email).fetch();
        if(listUser.size()==0){
            renderJSON(new Response(2,"Email or password invalid"));
        }
        User user=listUser.get(0);
        if(!user.isManager){
            renderJSON(new Response(2,"Error permission"));
        }
        //
        renderJSON(new Response(1,createToken(user,true)));;
    }
    public static void loginCustomer(@Required String email,@Required String password){
        if (validation.hasErrors()) {
            renderJSON(new Response(2, "Validate error"));
        }
        List<User> listUser=User.find("byEmailAndPassword",email,password).fetch();
        if(listUser.size()==0){
            renderJSON(new Response(2,"Email or password invalid"));
        }
        User user=listUser.get(0);
        Customer customer=Customer.find("byUser_id",user.getId()).first();
        renderJSON(new Response(1,createToken(customer.user,customer.isLocationManager)));
    }
    public static void refreshToken(String token){
        JWTVerifier jwtVerifier=new JWTVerifier(Stateful.getInstance().keyFirebase);
        Map<String, Object> decoded=null;
        try {
            decoded= jwtVerifier.verify(token);
            // Do something with decoded information like UserId
            //   Logger.info("Name: %s",decoded.get("name"));
        } catch (Exception e) {
            renderJSON(new Response(2,"Unauthorized: Token validation failed"));
        }
        Date expireTime=new Date(Long.parseLong(decoded.get("exp")+""));
        if(expireTime.getTime()<new Date().getTime()/1000){
            renderJSON(new Response(2,"Expired time"));
        }
        Gson gson = new Gson();
        JsonObject jsonObject=gson.fromJson(decoded.get("d")+"",JsonObject.class);
        Long id=Long.parseLong(jsonObject.get("uid")+"");
        User user= User.findById(id);
        renderJSON(new Response(1,createToken(user,user.isManager)));

    }
    public static void getLocation(@Required String uid){
        if (validation.hasErrors()) {
            renderJSON(new Response(2, "Validate error"));
        }
        Customer customer=Customer.find("byUser_id",uid).first();
        renderJSON(new Response(1,customer.locations));
    }
    public static void getInforUser(@Required String uid){
//        User user=User.find("byId",uid).first();
//        Customer customer=Customer.find("byUser_id",user.getId()).first();
//        user.password=null;
//        user.isManager=
    }
    private static String createToken(User user, boolean isLocationManager){
        int hour=Integer.parseInt(Stateful.getInstance().expireTime);
        Long timeExpire=new Date().getTime()+ hour*3600000;
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("uid",user.id+"");
        payload.put("email",user.email);
        payload.put("name", user.name);
        payload.put("isManager",isLocationManager);

        TokenGenerator tokenGenerator = new TokenGenerator(Stateful.getInstance().keyFirebase);
        TokenOptions tokenOptions=new TokenOptions();
        tokenOptions.setExpires(new Date(timeExpire));
        String token = tokenGenerator.createToken(payload,tokenOptions);

        return token;
    }


}
