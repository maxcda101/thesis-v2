package controllers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import entities.DataEmbed;
import entities.Response;
import jobs.SendFCM;
import jobs.UpdateData;
import models.Data;
import play.Logger;
import play.cache.Cache;
import play.data.validation.Required;
import play.libs.F;
import play.libs.WS;
import play.mvc.Controller;
import stateful.Stateful;
import sun.rmi.runtime.Log;
import utils.DateFormat;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by AnhQuan on 9/6/2016.
 */
public class ApiEmbed extends Controller {
    public static void index() {
        renderJSON("Api embed");
    }

    public static void pushData(@Required Long idNode, @Required Long idSensor, @Required Float value, @Required String timeRec) {
        if (validation.hasErrors()) {
            renderJSON(new Response(2, "Validate error"));
        }
        Data data = new Data(value, DateFormat.StringToDate(timeRec), new Date(), idNode, idSensor, 3L);
        Logger.info("Push data: " + data.node.name + "|" + data.sensor.name + ": " + value);
        //check Data for notification
        int rank = 0;
        if (data.sensor.id == 1) {
            if (data.value > 0 && data.value <= 30) {
                rank = 3;
            } else if (30 < data.value && data.value <= 70) {
                rank = 2;
            } else if (70 < data.value) {
                rank = 1;
            } else {
                rank = 0;
            }
        }
        if (data.sensor.id == 1 && (Cache.get(data.node.id + "") == null || !Cache.get(data.node.id + "").equals(rank))) {
            Logger.info("Cache air: " + Cache.get(data.node.id + ""));
            Cache.set(data.node.id + "", rank);
            SendFCM sendFCM = null;

            java.text.DateFormat format=new SimpleDateFormat("HH:mm");
            switch (rank) {
                case 3:
                    sendFCM = new SendFCM(data.node.root.location.id, format.format(new Date())+" Thông báo " + data.node.name, "Không khí trong lành");
                    break;
                case 2:
                    sendFCM = new SendFCM(data.node.root.location.id,format.format(new Date())+ " Cảnh báo " + data.node.name, "Không khí đang bị ô nhiễm mức thấp: " + data.value);
                    break;
                case 1:
                    sendFCM = new SendFCM(data.node.root.location.id,format.format(new Date())+ " Cảnh báo " + data.node.name, "Không khí đang bị ô nhiễm mắc cao: " + data.value);
                    break;
                case 0:
                    sendFCM = new SendFCM(data.node.root.location.id,format.format(new Date())+ " Cảnh báo " + data.node.name, "Mất tín hiệu");
            }
            sendFCM.now();
        }
        if (data.save() != null) {
            renderJSON(new Response(1, "Success"));
        } else {
            renderJSON(new Response(2, "Error"));
        }
    }

    public static void updateData(@Required String datas) {
        Logger.info("Syschonize data ");
        if (validation.hasErrors()) {
            renderJSON(new Response(2, "Validate error"));
        }
        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<DataEmbed>>() {
        }.getType();
        List<DataEmbed> listDataEmbed;
        try {
            listDataEmbed = gson.fromJson(datas, listType);
            new UpdateData(listDataEmbed).now();
        } catch (Exception e) {
            Logger.error(e,"Error");
            renderJSON(new Response(2, "Json format exepttion"));

        }
        Logger.info("Syschonize success ");
        renderJSON(new Response(1, "Success"));

    }
}
