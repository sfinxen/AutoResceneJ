package se.sfinxen.autorescenej;

import se.sfinxen.autorescenej.json.JSONArray;
import se.sfinxen.autorescenej.json.JSONObject;

/**
 * Representation of SRRDB's json search-result
 * 
 * @author sfinxen
 *
 */
public class SRRResult
{
  private String release;
  private String date;
  private boolean hasNFO;
  private boolean hasSRS;
  
  public SRRResult(JSONObject json)
  {
    JSONArray array = json.getJSONArray("results");
    JSONObject result = (JSONObject)array.get(0);
    
    parseJSON(result);
  }
  
  private void parseJSON(JSONObject result)
  {
    this.release = result.get("release").toString();
    this.date = result.get("date").toString();
    this.hasNFO = result.get("hasNFO").equals("yes") ? true : false;
    this.hasSRS = result.get("hasSRS").equals("yes") ? true : false;
  }
  
  public String getRelease()
  {
    return this.release;
  }
  
  public String getDate()
  {
    return this.date;
  }
  
  public boolean hasNFO()
  {
    return this.hasNFO;
  }
  
  public boolean hasSRS()
  {
    return this.hasSRS;
  }
}
