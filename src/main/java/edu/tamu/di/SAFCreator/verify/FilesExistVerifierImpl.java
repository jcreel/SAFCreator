package edu.tamu.di.SAFCreator.verify;

import java.util.ArrayList;
import java.util.List;

import edu.tamu.di.SAFCreator.model.Batch;
import edu.tamu.di.SAFCreator.model.Bitstream;
import edu.tamu.di.SAFCreator.model.Bundle;
import edu.tamu.di.SAFCreator.model.Item;
import edu.tamu.di.SAFCreator.model.Verifier;

public class FilesExistVerifierImpl implements Verifier {

	public List<Problem> verify(Batch batch) 
	{
		List<Problem> missingFiles = new ArrayList<Problem>();
		
		for(Item item : batch.getItems())
		{
			for(Bundle bundle : item.getBundles())
			{
				for(Bitstream bitstream : bundle.getBitstreams())
				{
					if(!bitstream.getSource().exists())
					{
						Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumn(), generatesError(), "Source file " + bitstream.getSource().getAbsolutePath() + " not found.");
						missingFiles.add(missingFile);
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
