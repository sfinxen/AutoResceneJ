package se.sfinxen.autorescenej;

import java.io.File;

/**
 * Main class checks given filepath and starts the autorescene magic
 * 
 * @author sfinxen
 *
 */
public class Main
{
  public static void main(String args[])
  {
    String helptext = "Usage: autorescenej <path>\n Configurations to be made in autorescenej.cfg";
    
    if (args.length == 1)
    {
      File file = new File(args[0]);
      
      if (file.exists() && file.isDirectory())
      {
        try
        {
          new AutoRescene(args[0]);
        }
        catch (RuntimeException ex)
        {
          errorExit();
        }
      }
      else
      {
        System.out.println(helptext);
      }
    }
    else
    {
      System.out.println(helptext);
    }
  }
  
  public static void errorExit()
  {
    System.out.println("Program has run in to a problem and shut down!");
    System.exit(0);
  }
}