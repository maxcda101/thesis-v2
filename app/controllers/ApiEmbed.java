package controllers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import entities.DataEmbed;
import entities.Response;
import jobs.SendFCM;
import models.Data;
import play.Logger;
import play.data.validation.Required;
import play.libs.F;
import play.libs.WS;
import play.mvc.Controller;
import stateful.Stateful;
import utils.DateFormat;

import java.lang.reflect.Type;
import java.util.*;

/**
 * Created by AnhQuan on 9/6/2016.
 */
public class ApiEmbed extends Controller {
    private static Map<Long, Float> airQuality = new HashMap<Long, Float>();
    public static void index() {
        renderJSON("Api embed");
    }

    public static void pushData(@Required Long idNode, @Required Long idSensor, @Required Float value, @Required String timeRec) {
        Logger.info("Push data: " + timeRec + "|" + value);
        if (validation.hasErrors()) {
            renderJSON(new Response(2, "Validate error"));
        }
        Data data = new Data(value, DateFormat.StringToDate(timeRec), new Date(), idNode, idSensor, 3L);
        //check Data for notification
        if (data.sensor.id == 1 && (airQuality.get(data.node.id)==null ||airQuality.get(data.node.id)!=data.value)) {
            Logger.info("Map: "+airQuality.get(data.node.id));
            airQuality.put(data.node.id,data.value);
            switch ((int) data.value) {
                case 3:
                    SendFCM.sendNotification(data.node.root.location.id, "Thông báo node " + data.node.name, "Không khí đã hết ô nhiễm");
                    break;
                case 2:
                    SendFCM.sendNotification(data.node.root.location.id, "Cảnh báo node " + data.node.name, "Không khí đang bị ô nhiễm mức 2/3");
                    break;
                case 1:
                    SendFCM.sendNotification(data.node.root.location.id, "Cảnh báo node " + data.node.name, "Không khí đang bị ô nhiễm mắc cao 1/3");
                    break;
                case 0:
                    SendFCM.sendNotification(data.node.root.location.id, "Cảnh báo node " + data.node.name, "Mất tín hiệu");
            }
        }
        if(data.save()!=null){
            renderJSON(new Response(1, "Success"));
        }else{
            renderJSON(new Response(2, "Error"));
        }



    }

    public static void updateData(@Required String datas) {
        Logger.info("update data ");
        if (validation.hasErrors()) {
            renderJSON(new Response(2, "Validate error"));
        }
        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<DataEmbed>>() {
        }.getType();
        List<DataEmbed> listDataEmbed = gson.fromJson(datas, listType);
        for (DataEmbed dataEmbed : listDataEmbed) {
            try {
                dataEmbed.convertData().save();
            } catch (Exception e) {
                Logger.error(e.getMessage());
            }
        }
        Logger.info("update success ");
        renderJSON(new Response(1, "Success"));

    }
}
