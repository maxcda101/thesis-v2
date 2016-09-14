package stateful;

import play.Play;

/**
 * Created by AnhQuan on 9/7/2016.
 */
public class Stateful {
    public String urlFirebaseMessage;
    public String keyFirebaseMessage;
    public String keyFirebase;
    public String expireTime;
    public static Stateful instance=getInstance();

    public static Stateful getInstance() {
        if (instance==null) instance=new Stateful();
        return instance;
    }

    private Stateful() {
        if(urlFirebaseMessage==null){
            urlFirebaseMessage= Play.configuration.getProperty("fcm.url");
        }
        if(keyFirebaseMessage==null){
            keyFirebaseMessage=Play.configuration.getProperty("fcm.key");
        }
        if(keyFirebase==null){
            keyFirebase=Play.configuration.getProperty("firebase.key");
        }
        if(expireTime==null){
            expireTime=Play.configuration.getProperty("expireTime");
        }
    }
}
