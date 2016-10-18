package jobs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import entities.Response;
import play.jobs.Job;
import play.libs.F;
import play.libs.WS;
import stateful.Stateful;

/**
 * Created by AnhQuan on 9/16/2016.
 */
public class SendFCM extends Job {
    private static Long idLocation;
    private static String title;
    private static String body;

    public SendFCM(Long idLocation, String title, String body) {
        this.idLocation = idLocation;
        this.title = title;
        this.body = body;
    }

    @Override
    public void doJob() {
        String json = "{\n" +
                "  \"condition\": \"'" + idLocation + "' in topics\",\n" +
                "  \"notification\": {\n" +
                "        \"title\": \"" + title + "\",\n" +
                "        \"text\": \"" + body + "\"\n" +
                "      }\n" +
                "}";
        F.Promise<WS.HttpResponse> promise = WS.url(Stateful.instance.urlFirebaseMessage)
                .setHeader("Authorization", "key=" + Stateful.instance.keyFirebaseMessage)
                .setHeader("Content-Type", "application/json").body(json).postAsync();
    }
}
