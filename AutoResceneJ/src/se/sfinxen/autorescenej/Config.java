package se.sfinxen.autorescenej;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Representation of configfile
 * 
 * @author sfinxen
 *
 */
public class Config
{
  private Long minSize;
  private String srrPath;
  private String srsPath;
  private boolean resample;
  private boolean verbose;
  private String searchURL;
  private String downloadURL;
  private boolean pyVerbose;
  
  public Config()
  {
    this.readConfigFile();
  }
  
  private void readConfigFile()
  {
    String configFile = "autorescenej.cfg";
    
    if (!new File(configFile).exists())
    {
      System.out.println("Configuration file not found!");
      throw new RuntimeException();
    }
    
    try
    {
      InputStream in = new FileInputStream(configFile);
      
      Properties properties = new Properties();
      properties.load(in);
      
      this.parseConfig(properties);
      
      in.close();
    }
    catch (IOException ex)
    {
      System.out.println("Could not read configuration file!");
      throw new RuntimeException();
    }
  }
  
  private void parseConfig(Properties properties)
  {
    this.minSize = Long.parseLong(properties.getProperty("minSize", "100"));
    this.srrPath = properties.getProperty("srrPath", "C:\\").replace("\\", "/");
    this.srsPath = properties.getProperty("srsPath", "C:\\").replace("\\", "/");
    
    if (!this.srrPath.endsWith("/"))
    {
      this.srrPath = this.srrPath + "/";
    }
    if (!this.srsPath.endsWith("/"))
    {
      this.srsPath = this.srsPath + "/";
    }
    
    this.resample = Boolean.parseBoolean(properties.getProperty("resample", "true"));
    this.verbose = Boolean.parseBoolean(properties.getProperty("verbose", "true"));
    this.pyVerbose = Boolean.parseBoolean(properties.getProperty("pyverbose", "true"));
    this.searchURL = properties.getProperty("searchURL", "http://www.srrdb.com/api/search/archive-crc:");
    this.downloadURL = properties.getProperty("downloadURL", "http://www.srrdb.com/download/srr/");
  }
  
  // Getters
  public Long getMinSize()
  {
    return this.minSize;
  }
  
  public String getSRRPath()
  {
    return this.srrPath;
  }
  
  public String getSRSPath()
  {
    return this.srsPath;
  }
  
  public boolean isResample()
  {
    return this.resample;
  }
  
  public boolean isVerbose()
  {
    return this.verbose;
  }

  public String getSearchURL()
  {
    return this.searchURL;
  }
  
  public String getDownloadURL()
  {
    return this.downloadURL;
  }
  
  public boolean isPyVerbose()
  {
    return pyVerbose;
  }
}
