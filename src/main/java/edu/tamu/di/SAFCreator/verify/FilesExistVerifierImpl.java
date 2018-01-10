package edu.tamu.di.SAFCreator.verify;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

import edu.tamu.di.SAFCreator.model.Batch;
import edu.tamu.di.SAFCreator.model.Bitstream;
import edu.tamu.di.SAFCreator.model.Bundle;
import edu.tamu.di.SAFCreator.model.Item;
import edu.tamu.di.SAFCreator.model.Verifier;

public class FilesExistVerifierImpl implements Verifier {

	public List<Problem> verify(Batch batch) 
	{
		List<Problem> missingFiles = new ArrayList<Problem>();
		
		if( ! batch.getIgnoreFiles())
		{
			for(Item item : batch.getItems())
			{
				for(Bundle bundle : item.getBundles())
				{
					for(Bitstream bitstream : bundle.getBitstreams())
					{
						URI source = bitstream.getSource();
						if (source.isAbsolute() && !source.getScheme().toString().equalsIgnoreCase("file"))
						{
							HttpURLConnection conn = null;

							try {
								conn = (HttpURLConnection) source.toURL().openConnection();
								conn.setRequestMethod("HEAD");
								conn.getInputStream().close();

								int response = conn.getResponseCode();

								if (response != 200) {
									Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumn(), generatesError(), "Unable to validate file URL " + source.toString() + ", HTTP Response Code: " + response + ".");
									missingFiles.add(missingFile);
								}
							} catch (MalformedURLException e) {
								Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumn(), generatesError(), "Source file URL " + source.toString() + " is invalid, reason: " + e.getMessage() + ".");
								missingFiles.add(missingFile);
							} catch (IOException e) {
								Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumn(), generatesError(), "Source file URL " + source.toString() + " had a connection error, message: " + e.getMessage() + ".");
								missingFiles.add(missingFile);
							} finally {
								if (conn != null) {
									conn.disconnect();
								}
							}
						} else {
							File file = new File(bitstream.getSource().getPath());

							if(!file.exists())
							{
								Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumn(), generatesError(), "Source file " + file.getAbsolutePath() + " not found.");
								missingFiles.add(missingFile);
							}
						}
					}
				}
			}
		}
		
		return missingFiles;
	}

	public boolean generatesError() 
	{
		return true;
	}
	
	public String prettyName()
	{
		return "Content Files Exist Verifier";
	}

}
