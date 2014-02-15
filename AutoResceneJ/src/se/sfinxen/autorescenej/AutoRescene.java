package se.sfinxen.autorescenej;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.CRC32;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import se.sfinxen.autorescenej.json.JSONException;
import se.sfinxen.autorescenej.json.JSONObject;
import se.sfinxen.autorescenej.json.JSONParser;

/**
 * AutoRescene-class, it's where the magic happens
 * 
 * @author sfinxen
 *
 */
public class AutoRescene
{
  private Config config;
  private String searchPath;
  private Collection<File> srrCandidateList = new ArrayList<File>();
  private SRRResult srrResult;
  private File currentFile;
  
  /**
   * Constructor runs all methods
   * 
   * @param path Path to scan
   */
  public AutoRescene(String path)
  {
    this.config = new Config();
    this.fixSearchPath(path);
    this.findSRRCandidates(new File(searchPath));
    this.createCRC();
  }
  
  /**
   * Fixes searchpath, replaces \ with / and adds trailing /
   * 
   * @param path Searchpath to fix
   */
  private void fixSearchPath(String path)
  {
    this.searchPath = path.replace("\\", "/");
    
    if (!this.searchPath.endsWith("/"))
    {
      this.searchPath = path + "/";
    }
  }
  
  /**
   * Recursive scanning of path to find all candidates for rescening
   * 
   * @param path Path to scan for candidates
   */
  private void findSRRCandidates(File path)
  {
    File[] files = path.listFiles();
    
    if (files != null)
    {
      for (File file : files)
      {
        if (file.isDirectory())
        {
          findSRRCandidates(file);
        }
        else
        {
          String extension = FilenameUtils.getExtension(file.getName());
          long fileSizeMB = (file.length() / 1024) / 1024;
          
          if ((extension.equalsIgnoreCase("mp4") || extension.equalsIgnoreCase("avi") || extension.equalsIgnoreCase("mkv")) && fileSizeMB > config.getMinSize())
          {
            srrCandidateList.add(file);
          }
        }
      }
    }
  }
  
  /**
   * Recursive scanning to find all SRS-files in newly created directory
   * Runs resample on all found candidates
   * 
   * @param path Directory to scan
   * @param sampleDir Directory to put sample in
   * @throws IOException
   */
  private void findSRSCandidates(File path, String sampleDir)
  {
    File[] files = path.listFiles();
    
    if (files != null)
    {
      for (File file : files)
      {
        if (file.isDirectory())
        {
          findSRSCandidates(file, sampleDir);
        }
        else
        {
          String extension = FilenameUtils.getExtension(file.getName());
          
          if (extension.equalsIgnoreCase("srs"))
          {
            resample(file, sampleDir);
          }
        }
      }
    }
  }
  
  /**
   * Loop through filelist and create checksum 
   * 
   */
  private void createCRC()
  {
    for (File file : srrCandidateList)
    {
      currentFile = file;
      
      String crc = "";
      
      try
      {
        InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
        CRC32 crcMaker = new CRC32();
        
        byte[] buffer = new byte[2^16];
        int bytesRead;
        while((bytesRead = inputStream.read(buffer)) != -1)
        {
            crcMaker.update(buffer, 0, bytesRead);
        }
        
        inputStream.close();
        
        crc = Long.toHexString(crcMaker.getValue());
        
        while (crc.length() < 8)
        {
          crc = "0" + crc;
        }
        
        this.print("Found candidate: " + file.getName() + " [" + crc + "]");
      }
      catch (IOException e)
      {
        this.print("Could not create CRC for file: " + file.getName() + ", skipping.");
        continue;
      }
      
      if (checkSRR(crc)) {
        File srrFile = getSRR();
        
        if (srrFile != null)
        {
          rescene(srrFile);
        }
      }
    }
  }
  
  /**
   * Checks if SRR-file exists in SRR-DB
   * 
   * @param crc CRC-checksum of file
   * @throws MalformedURLException
   * @throws JSONException
   * @throws IOException
   */
  private boolean checkSRR(String crc)
  {
    JSONObject json = new JSONParser().getJSONFromUrl(config.getSearchURL() + crc);
    
    if (json.get("resultsCount").toString().equals("0"))
    {
      this.print("No SRR found, skipping " + currentFile.getName() + ".");
      
      return false;
    }
    else
    {
      this.srrResult = new SRRResult(json);
      
      this.print("SRR found for " + currentFile.getName() + ", fetching..");
      
      return true;
    }
  }
  
  /**
   * Downloads SRR-file
   * 
   * @throws MalformedURLException
   * @throws IOException
   */
  private File getSRR()
  {
    File srrFile = new File(this.searchPath + this.srrResult.getRelease() + ".srr");
    
    try
    {
      FileUtils.copyURLToFile(new URL(config.getDownloadURL() + srrResult.getRelease()), srrFile);
      
      return srrFile;
    }
    catch (MalformedURLException e)
    {
      this.print("Bad URL, could not get file, skipping.");
      
      return null;
    }
    catch (IOException e)
    {
      this.print("Could not create SRR file, skipping.");
      
      return null;
    }
  }
  
  /**
   * Do the rescening
   * 
   * @param srrFile The SRR-file to use
   * @throws IOException
   */
  private void rescene(File srrFile)
  {
    String runPath = config.getSRRPath() + "srr.exe -y -p -r -i \"" + this.searchPath + "\" -o \"" + this.searchPath + this.srrResult.getRelease() + "/" + "\" \"" + srrFile.getAbsolutePath() + "\"";
    
    System.out.println("Rebuilding " + this.srrResult.getRelease());
    
    Process p;
    try
    {
      p = Runtime.getRuntime().exec(runPath);
    
      BufferedReader pOutput = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line;
      
      while ((line = pOutput.readLine()) != null)
      {
        if (config.isPyVerbose())
        {
          System.out.println(line);
        }
      }
      
      srrFile.delete();

    }
    catch (IOException e)
    {
      this.print("Could not rescene, skipping.");
    }
    
    if (config.isResample() && this.srrResult.hasSRS())
    {
      findSRSCandidates(new File(this.searchPath + this.srrResult.getRelease()), this.searchPath + this.srrResult.getRelease() + "/Sample/");
    }
  }
  
  /**
   * Does the resampling
   * 
   * @param srsFile The SRS-file to use
   * @param sampleDir Location to rebuild sample
   * @throws IOException
   */
  private void resample(File srsFile, String sampleDir)
  {
    String srsRunPath = config.getSRSPath() + "srs.exe \"" + srsFile.getAbsolutePath() + "\" -y -o \"" + sampleDir + "\" \"" + currentFile.getAbsolutePath() + "\"";
    
    try
    {
    Process p = Runtime.getRuntime().exec(srsRunPath);
    
    this.print("Rebuilding sample");
    
    BufferedReader pOutput = new BufferedReader(new InputStreamReader(p.getInputStream()));
    String line;
    while ((line = pOutput.readLine()) != null)
    {
      if (config.isPyVerbose())
      {
        System.out.println(line);
      }
    }
    
    srsFile.delete();
    }
    catch (IOException ex)
    {
      this.print("Could not resample, skipping.");
    }
  }
  
  /**
   * Prints string if verbose
   * 
   * @param str String to echo
   */
  private void print(String str)
  {
    if (config.isVerbose())
    {
      System.out.println(str);
    }
  }
}