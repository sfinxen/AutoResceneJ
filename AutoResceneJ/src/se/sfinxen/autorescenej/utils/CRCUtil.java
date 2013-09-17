package se.sfinxen.autorescenej.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;

/**
 * CRCUtil creates CRC32 from file
 * 
 * @author sfinxen
 *
 */
public class CRCUtil
{
  public static String createCRC(File file) throws IOException
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
    
    return Long.toHexString(crcMaker.getValue());
  }
}