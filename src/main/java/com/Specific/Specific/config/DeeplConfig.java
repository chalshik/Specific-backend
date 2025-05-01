package com.Specific.Specific.config;

public class DeeplConfig {
    private static String APIKEY = "83504ff5-0621-4b81-81c5-d050c0f67f9b:fx";
    private static final String API_URL = "https://api-free.deepl.com/v2/translate";
     static public String getAPIKEY() {
        return APIKEY;
    }
    static public String getApiUrl(){
         return API_URL;
    }
}
