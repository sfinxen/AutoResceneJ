package se.sfinxen.autorescenej;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import se.sfinxen.autorescenej.json.JSONException;
import se.sfinxen.autorescenej.json.JSONObject;
import se.sfinxen.autorescenej.json.JSONParser;
import se.sfinxen.autorescenej.utils.CRCUtil;

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
   * Constructor strips away backslashes and starts the fun
   * 
   * @param path Path to scan
   */
  public AutoRescene(String path)
  {
    this.config = new Config();
    
    this.searchPath = path.replace("\\", "/");
    
    if (!this.searchPath.endsWith("/"))
    {
      this.searchPath = path + "/";
    }
    
    this.findSRRCandidates(new File(searchPath));
    this.createCRC();
  }
  
  /**
   * Find all candidates for rescening
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
   * Find all SRS-files in newly created directory
   * 
   * @param path Directory to scan
   * @param sampleDir Directory to put sample in
   * @throws IOException
   */
  private void findSRSCandidates(File path, String sampleDir) throws IOException
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
            doResample(file, sampleDir);
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
      
      try
      {
        String crc = CRCUtil.createCRC(file);
        
        this.print("Found candidate: " + file.getName() + " [" + crc + "]");
        
        checkSRR(crc);
      }
      catch (IOException e)
      {
        this.print("Could not create CRC for file: " + file.getName() + ", skipping.");
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
  private void checkSRR(String crc) throws MalformedURLException, JSONException, IOException
  {
    JSONObject json = new JSONParser().getJSONFromUrl(config.getSearchURL() + crc);
    
    if (json.get("resultsCount").toString().equals("0"))
    {
      this.print("No SRR found, skipping " + currentFile.getName() + ".");
    }
    else
    {
      this.srrResult = new SRRResult(json);
      
      this.print("SRR found for " + currentFile.getName() + ", fetching..");
      
      getSRR();
    }
  }
  
  /**
   * Downloads SRR-file
   * 
   * @throws MalformedURLException
   * @throws IOException
   */
  private void getSRR() throws MalformedURLException, IOException
  {
    File srrFile = new File(this.searchPath + this.srrResult.getRelease() + ".srr");
    FileUtils.copyURLToFile(new URL(config.getDownloadURL() + srrResult.getRelease()), srrFile);
    
    doRescene(srrFile);
  }
  
  /**
   * Do the rescening
   * 
   * @param srrFile The SRR-file to use
   * @throws IOException
   */
  private void doRescene(File srrFile) throws IOException
  {
    String runPath = config.getSRRPath() + "srr.exe -y -p -r -i " + this.searchPath + " -o " + this.searchPath + this.srrResult.getRelease() + "/" + " " + srrFile.getAbsolutePath();
    
    System.out.println("Rebuilding " + this.srrResult.getRelease());
    
    Process p = Runtime.getRuntime().exec(runPath);
    
    BufferedReader pOutput = new BufferedReader(new InputStreamReader(
        p.getInputStream()));
    String line;
    while ((line = pOutput.readLine()) != null)
    {
      if (config.isPyVerbose())
      {
        System.out.println(line);
      }
    }
    
    srrFile.delete();
    
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
  private void doResample(File srsFile, String sampleDir) throws IOException
  {
    String srsRunPath = config.getSRSPath() + "srs.exe " + srsFile.getAbsolutePath() + " -y -o " + sampleDir + " " + currentFile.getAbsolutePath();
    
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