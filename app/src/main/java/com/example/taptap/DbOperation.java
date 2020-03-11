package com.example.taptap;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public class DbOperation extends AsyncTask<String, Void, String> {


    @Override
    protected String doInBackground(String... voids) {

        String result = "";
        String api_key = voids[0];
        String ec = voids[1];
        String fg1 = voids[2];
        String fg2 = voids[3];


        try {
            URL server = new URL("http://192.168.137.54");
            HttpURLConnection http = (HttpURLConnection) server.openConnection();
            http.setRequestMethod("POST");
            http.setDoInput(true);
            http.setDoOutput(true);

            OutputStream ops = http.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(ops,"UTF-8"));
            String data = URLEncoder.encode("api_key","UTF-8")+"="+ URLEncoder.encode(api_key,"UTF-8")
                    +"&&"+URLEncoder.encode("ec","UTF-8")+"="+URLEncoder.encode(ec,"UTF-8")
                    +"&&"+URLEncoder.encode("fg1","UTF-8")+"="+URLEncoder.encode(fg1,"UTF-8")
                    +"&&"+URLEncoder.encode("fg2","UTF-8")+"="+URLEncoder.encode(fg2,"UTF-8");
            Log.d("SecuGen",data);
            writer.write(data);
            writer.flush();
            writer.close();
            ops.close();

        }
        catch(MalformedURLException e) {
            result = e.getMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }
}
