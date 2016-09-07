package utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by AnhQuan on 9/6/2016.
 */
public class DateFormat {
    public static Date StringToDate(String date){
        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date datef=null;
        try {
             datef= dt.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return datef;
    }
    public static String DateToString(Date date){
        String newstring = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
        return newstring;
    }
}
