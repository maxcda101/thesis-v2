package controllers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import entities.DataEmbed;
import entities.Response;
import models.Data;
import play.Logger;
import play.data.validation.Required;
import play.libs.F;
import play.libs.WS;
import play.mvc.Controller;
import stateful.Stateful;
import utils.DateFormat;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by AnhQuan on 9/6/2016.
 */
public class ApiEmbed extends Controller {
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
        if(data.sensor.id==1){
            switch ((int)data.value){
                case 2: sendNotification(data.node.root.location.id,"Cảnh báo node "+data.node.name,"Không khí đang bị ô nhiễm mức 2/3");
                    break;
                case 1: sendNotification(data.node.root.location.id,"Cảnh báo node "+data.node.name, "Không khí đang bị ô nhiễm mắc cao 1/3");
                    break;
                case 0: sendNotification(data.node.root.location.id,"Cảnh báo node "+data.node.name, "Mất tín hiệu");
            }
        }

        if (data.save() != null) {
            renderJSON(new Response(1, "Success"));
        } else {
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

    public static void sendNotification(Long idLocation, String title, String body) {
        String json = "{\n" +
                "  \"condition\": \"'"+idLocation+"' in topics\",\n" +
                "  \"notification\": {\n" +
                "        \"title\": \"" + title + "\",\n" +
                "        \"text\": \"" + body + "\"\n" +
                "      }\n" +
                "}";
        F.Promise<WS.HttpResponse> promise = WS.url(Stateful.instance.urlFirebaseMessage)
                .setHeader("Authorization", "key=" + Stateful.instance.keyFirebaseMessage)
                .setHeader("Content-Type", "application/json").body(json).postAsync();
        await(promise, new F.Action<WS.HttpResponse>() {

            @Override
            public void invoke(WS.HttpResponse result) {
                //
                JsonElement jsonElement = result.getJson();
                if (!jsonElement.isJsonNull()) {
                    JsonObject object = jsonElement.getAsJsonObject();
                    renderJSON(new Response(1, "Success"));
                } else {

                }
            }
        });
    }
}
