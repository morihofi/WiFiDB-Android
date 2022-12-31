package de.morihofi.wifidb.utils;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class Tools {

    public static String getHTTPMessageFromStatusCode(int code){
        String message = "";

        switch(code){
            case 100:
                message = "Continue";
                break;
            case 101:
                message = "Switching Protocols";
                break;
            case 200:
                message = "OK";
                break;
            case 201:
                message = "Created";
                break;
            case 202:
                message = "Accepted";
                break;
            case 203:
                message = "Non-authoritative Information";
                break;
            case 204:
                message = "No Content";
                break;
            case 205:
                message = "Reset Content";
                break;
            case 206:
                message = "Partial Content";
                break;
            case 300:
                message = "Multiple Choices";
                break;
            case 301:
                message = "Moved Permanently";
                break;
            case 302:
                message = "Found";
                break;
            case 303:
                message = "See Other";
                break;
            case 304:
                message = "Not Modified";
                break;
            case 305:
                message = "Use Proxy";
                break;
            case 306:
                message = "Unused";
                break;
            case 307:
                message = "Temporary Redirect";
                break;
            case 400:
                message = "Bad Request";
                break;
            case 401:
                message = "Unauthorized";
                break;
            case 402:
                message = "Payment Required";
                break;
            case 403:
                message = "Forbidden";
                break;
            case 404:
                message = "Not Found";
                break;
            case 405:
                message = "Method Not Allowed";
                break;
            case 406:
                message = "Not Acceptable";
                break;
            case 407:
                message = "Proxy Authentication Required";
                break;
            case 408:
                message = "Request Timeout";
                break;
            case 409:
                message = "Conflict";
                break;
            case 410:
                message = "Gone";
                break;
            case 411:
                message = "Length Required";
                break;
            case 412:
                message = "Precondition Failed";
                break;
            case 413:
                message = "Request Entity Too Large";
                break;
            case 414:
                message = "Request-url Too Long";
                break;
            case 415:
                message = "Unsupported Media Type";
                break;
            case 417:
                message = "Expectation Failed";
                break;
            case 500:
                message = "Internal Server Error";
                break;
            case 501:
                message = "Not Implemented";
                break;
            case 502:
                message = "Bad Gateway";
                break;
            case 503:
                message = "Service Unavailable";
                break;
            case 504:
                message = "Gateway Timeout";
                break;
            case 505:
                message = "HTTP Version Not Supported";
                break;

            default:
                message = "Unknown";
                break;
        }

        return message;
    }


    public static void acceptAllCertificates(){
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    @Override
                    public void checkClientTrusted(X509Certificate[] arg0, String arg1)
                            throws CertificateException {}

                    @Override
                    public void checkServerTrusted(X509Certificate[] arg0, String arg1)
                            throws CertificateException {}
                }
        };

        SSLContext sc=null;
        try {
            sc = SSLContext.getInstance("SSL");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        // Create all-trusting host name verifier
        HostnameVerifier validHosts = new HostnameVerifier() {
            @Override
            public boolean verify(String arg0, SSLSession arg1) {
                return true;
            }
        };
        // All hosts will be valid
        HttpsURLConnection.setDefaultHostnameVerifier(validHosts);
    }
}
