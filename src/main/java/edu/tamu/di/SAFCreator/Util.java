package edu.tamu.di.SAFCreator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {
	
	public static String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }
	
	public static int regexMatchCounter(String REGEX, String INPUT) 
	{
	       Pattern p = Pattern.compile(REGEX);
	       Matcher m = p.matcher(INPUT); // get a matcher object
	       int count = 0;
	       while(m.find()) {
	           count++;
//	           System.out.println("Match number "+count);
//	           System.out.println("start(): "+m.start());
//	           System.out.println("end(): "+m.end());
	       }
	       return count;
	}
	
	
	protected static String removeLanguage(String string)
	{
		return string.split("\\[")[0].trim();
	}
	
	protected static String getLanguage(String string)
	{
		return string.contains("[")?string.split("\\[")[1].replace("]",""):null;
	}
	
	protected static String getSchemaName(String string)
	{
		string = removeLanguage(string);
		return string.split("\\.")[0].trim();
	}
	
	protected static String getElementName(String string)
	{
		string = removeLanguage(string);
		
		//if qualified label...
		if(string.lastIndexOf('.') != string.indexOf('.'))
		{
			String name = string.substring(string.indexOf('.')+1, string.lastIndexOf('.'));
			name = name.trim();
			//System.out.println("Got name:"+name);
			return name;
		}
		else // unqualified label...
		{
			return string.substring(string.indexOf('.')+1);
		}
	}
	
	protected static String getElementQualifier(String string)
	{
		string = removeLanguage(string);
		
		//if qualified label...
		if(string.lastIndexOf('.') != string.indexOf('.'))
		{	
			String qualifier = string.substring(string.lastIndexOf('.')+1);
			qualifier = qualifier.trim();
			//System.out.println("Got qualifier: "+qualifier);
			return qualifier;
		}
		else // unqualified label...
		{
			return null;
		}
	}
	
	
	/**
	  * Change the contents of text file in its entirety, overwriting any
	  * existing text.
	  *
	  * This style of implementation throws all exceptions to the caller.
	  *
	  * @param aFile is an existing file which can be written to.
	  * @throws IllegalArgumentException if param does not comply.
	  * @throws FileNotFoundException if the file does not exist.
	  * @throws IOException if problem encountered during write.
	  */
	  static public void setFileContents(File aFile, String aContents)
	                                 throws FileNotFoundException, IOException {
	    if (aFile == null) {
	      throw new IllegalArgumentException("File should not be null.");
	    }
	    if (!aFile.exists()) {
	      throw new FileNotFoundException ("File does not exist: " + aFile);
	    }
	    if (!aFile.isFile()) {
	      throw new IllegalArgumentException("Should not be a directory: " + aFile);
	    }
	    if (!aFile.canWrite()) {
	      throw new IllegalArgumentException("File cannot be written: " + aFile);
	    }

	    //use buffering
	    Writer output = new OutputStreamWriter(new FileOutputStream(aFile), Charset.forName("UTF-8"));
	    try {
	      //Writer always uses UTF-8 as XML is dependent on this!
	      output.write( aContents );
	    }
	    finally {
	      output.close();
	    }
	  }
	
	
}
