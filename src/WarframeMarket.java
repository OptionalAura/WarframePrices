import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.http.HttpClient;

public class WarframeMarket {

    /**
     * Program entry point
     * @param args
     */
    public static void main(String[] args){
        Application app = new Application();
        app.start();
    }
}