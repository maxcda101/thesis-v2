package utils;

import play.libs.F;
import play.libs.WS;
import play.mvc.Controller;
import stateful.Stateful;

/**
 * Created by AnhQuan on 9/7/2016.
 */
public class FCM extends Controller{
    public static void sendNotification(String title, String body){
        String json="{\n" +
                "  \"condition\": \"'1' in topics\",\n" +
                "  \"notification\": {\n" +
                "        \"title\": \""+title+"\",\n" +
                "        \"text\": \""+body+"\"\n" +
                "      }\n" +
                "}";
        F.Promise<WS.HttpResponse> promise=WS.url(Stateful.instance.urlFirebaseMessage)
                .setHeader("Authorization","key="+Stateful.instance.keyFirebaseMessage)
                .setHeader("Content-Type","application/json").body(json).postAsync();
        await(promise, new F.Action<WS.HttpResponse>(){

            @Override
            public void invoke(WS.HttpResponse result) {
                //
//                result.getJson();
            }
        } );



    }
}
