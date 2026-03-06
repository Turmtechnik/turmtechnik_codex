package tom.turmtechnik;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Looper;
import android.util.Log;

public class JsonSendAndRead {
    private String uri1 = "http://app.turmtechnik.com/json_update.php";
    private String uri2 = "http://app.turmtechnik.com/json_request.php";
    private String url_update = "http://app.turmtechnik.com/json.php?type=update";
    private String url_request = "http://app.turmtechnik.com/json.php?type=request";
    private String url_status = "http://app.turmtechnik.com/json.php?type=request&table=status";
    private String url_website_status = "http://app.turmtechnik.com/json.php?type=status";

    public void testWriteJson() {
        Thread t = new Thread() {

            public void run() {
                Looper.prepare();
                HttpClient client = new DefaultHttpClient();
                HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000);
                org.apache.http.HttpResponse response;
                JSONObject json = new JSONObject();
                JSONArray jsonArray = new JSONArray();


                //String andiString =

                try {
                    URI uri = new URI(uri1);
                    HttpPost post = new HttpPost(uri);
                    json.put("Jahr", "2014");
                    json.put("Monat", "Februar");
                    json.put("Tag", "Donnerstag");
                    jsonArray.put(json);
                    Log.e("send", "Array=" + jsonArray.toString());
                    StringEntity se = new StringEntity(jsonArray.toString());
                    //Log.e("sende gelesen von andi" , "= \n" + StaticVariable.gelesenVonJsonRequest) ;
                    //StringEntity se = new StringEntity(StaticVariable.gelesenVonJsonRequest) ;

                    se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                    post.setEntity(se);
                    response = client.execute(post);

                    if (response != null) {
                        InputStream in = response.getEntity().getContent();
                        String echo = in.toString();
                        Log.e("HTTP ECHO", "= " + echo);
                    }
                } catch (Exception e) {
                    Log.e("Exception", "=" + e.toString());
                }

                Looper.loop();
            }

        };

        t.start();
    }

    public String testReadJsonAndi() {
        StringBuilder builder = new StringBuilder();
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(uri2);
        try {
            org.apache.http.HttpResponse response = client.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            } else {
                Log.e("READ JSON", "Fehler beim download File");
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return builder.toString();
    }


    public String testReadJson() {
        StringBuilder builder = new StringBuilder();
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(url_website_status);
        try {
            org.apache.http.HttpResponse response = client.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            } else {
                Log.e("READ JSON", "Fehler beim download File");
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return builder.toString();
    }

    public void testSendMuster() {
        JSONObject jo = new JSONObject();
        try {
            jo.put("action", "Senden Test");
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            jo.put("par1", "par 1 gesendet");
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            jo.put("par2", "das ist par2");
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            jo.put("par3", "und noch par3");
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        URL url = null;
        try {
            url = new URL(url_update);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = null;
        try {
            httpPost = new HttpPost(url.toURI());
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Prepare JSON to send by setting the entity
        try {
            Log.e("jo", "=" + jo.toString());
            httpPost.setEntity(new StringEntity(jo.toString(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Set up the header types needed to properly transfer JSON
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Accept-Encoding", "application/json");
        httpPost.setHeader("Accept-Language", "en-US");

        // Execute POST
        try {
            org.apache.http.HttpResponse response = httpClient.execute(httpPost);
            Log.e("response", "=" + response.toString());
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }


}