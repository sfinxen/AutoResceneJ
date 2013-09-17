package se.sfinxen.autorescenej.json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;

public class JSONParser
{
  static InputStream is = null;
  static JSONObject jObj = null;
  static String json = "";
  
  public JSONParser()
  {
    
  }
  
  public JSONObject getJSONFromUrl(String url)
  {
    try
    {
      HttpClient httpClient = HttpClientBuilder.create().build();
      HttpPost httpPost = new HttpPost(url);
      
      HttpResponse httpResponse = httpClient.execute(httpPost);
      HttpEntity httpEntity = httpResponse.getEntity();
      is = httpEntity.getContent();
    }
    catch (UnsupportedEncodingException e)
    {
      e.printStackTrace();
    }
    catch (ClientProtocolException e)
    {
      e.printStackTrace();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    
    try
    {
      BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
      StringBuilder sb = new StringBuilder();
      String line = null;
      
      while ((line = reader.readLine()) != null)
      {
        sb.append(line + "\n");
      }
      
      is.close();
      json = sb.toString();
    }
    catch (Exception e)
    {
      System.out.println("Error converting result " + e.toString());
    }
    
    try
    {
      jObj = new JSONObject(json);
    }
    catch (JSONException e)
    {
      System.out.println("Error parsing data " + e.toString());
    }
    
    return jObj;
  }
}