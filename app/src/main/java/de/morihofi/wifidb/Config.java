package de.morihofi.wifidb;

public class Config {
    //192.168.178.43:8080/wifidb-server
    public static final String masterserver = "tomcat.rs1.morihofi.de/wifidb-server"; //without / at the end
    public static final Boolean masterserver_usetls = false;

    public static String getSecureCharIfNeeded(){

        if (masterserver_usetls){
            return "s";
        }else{
            return "";
        }

    }

}
