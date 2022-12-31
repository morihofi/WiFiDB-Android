package de.morihofi.wifidb.utils;

import java.util.HashMap;

public class libgeo {
    public static HashMap<String, String> DDtoDMS(String dec)
    {
        // Converts decimal format to DMS ( Degrees / minutes / seconds )
        String[] vars = dec.split("\\.");
        Double deg = Double.valueOf(vars[0]);
        Double tempma = Double.valueOf("0." + vars[1]);

        tempma = tempma * 3600;
        Double min = Math.floor(tempma / 60);
        Double sec = tempma - (min*60);


        HashMap<String, String> retmap = new HashMap<>();
        retmap.put("deg", String.valueOf(deg));
        retmap.put("min", String.valueOf(min));
        retmap.put("sec", String.valueOf(sec));

        return retmap;
    }
    public static int DMStoDD(int deg, int min, int sec)
    {

        // Converting DMS ( Degrees / minutes / seconds ) to decimal format
        return deg+(((min*60)+(sec))/3600);
    }

    public static String  CoordinateString(Double lat,Double lon){
        HashMap<String, String> laa = DDtoDMS(String.valueOf(lat));
        HashMap<String, String> loa = DDtoDMS(String.valueOf(lon));

        String calclocN = laa.get("deg") + "°" + laa.get("min") + "'" + Math.round(Double.valueOf(laa.get("sec"))) + "\"N"; //$laa["deg"]."°".$laa["min"]."'".round($laa["sec"],1)."\"N";
        String calclocE = loa.get("deg") + "°" + loa.get("min") + "'" + Math.round(Double.valueOf(loa.get("sec"))) + "\"E";

        String calcloc = calclocN + " " + calclocE;

        return calcloc;
    }
}
