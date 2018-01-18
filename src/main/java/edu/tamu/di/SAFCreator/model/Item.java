package edu.tamu.di.SAFCreator.model;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.tamu.di.SAFCreator.Util;
import edu.tamu.di.SAFCreator.model.Verifier.Problem;

public class Item {
	private Batch batch;
	private List<SchematicFieldSet> schemata;
	private List<Bundle> bundles;
	private String handle;
	
	private File itemDirectory;
	
	public Item(int row, Batch batch)
	{
		this.batch = batch;
		schemata = new ArrayList<SchematicFieldSet>();
		bundles = new ArrayList<Bundle>();
		
		itemDirectory = new File(batch.getOutputSAFDir().getAbsolutePath() + File.separator + row);
		itemDirectory.mkdir();
		
		handle = null;

	}
	
	public Batch getBatch()
	{
		return batch;
	}
	
	public List<SchematicFieldSet> getSchemata()
	{
		return schemata;
	}
	
	public SchematicFieldSet getOrCreateSchema(String schemaName)
	{
		for(SchematicFieldSet schema : schemata)
		{
			if(schema.getSchemaName().equals(schemaName))
			{
				return schema;
			}
		}
		
		SchematicFieldSet schema = new SchematicFieldSet();
		schema.setSchemaName(schemaName);
		schemata.add(schema);
		return schema;
	}

	public List<Bundle> getBundles() {
		return bundles;
	}

	public Bundle getOrCreateBundle(String bundleName)
	{
		for(Bundle bundle : bundles)
		{
			if(bundle.getName().equals(bundleName))
			{
				return bundle;
			}
		}
		
		Bundle bundle = new Bundle();
		bundle.setName(bundleName);
		bundle.setItem(this);
		bundles.add(bundle);
		return bundle;
	}
	
	private void writeContents(List<Problem> problems)
	{
		String contentsString = "";
		for(Bundle bundle : bundles)
		{
			for(Bitstream bitstream : bundle.getBitstreams())
			{
				contentsString += bitstream.getContentsManifestLine();
				if( ! batch.getIgnoreFiles())
				{
					bitstream.copyMe(problems);
				}
			}
		}
		
		if (batch.getLicense() != null)
		{
			contentsString += batch.getLicense().getContentsManifestLine();
			batch.getLicense().writeToItem(this);
		}
		
		File contentsFile = new File(getSAFDirectory() + "/contents");
		try {
			if(!contentsFile.exists())
			{
				contentsFile.createNewFile();
			}
			Util.setFileContents(contentsFile, contentsString);
		} catch (FileNotFoundException e) {
			Problem problem = new Problem(true, "Unable to write to missing contents file for item directory " + getSAFDirectory() + ", reason: " + e.getMessage());
			problems.add(problem);
		} catch (IOException e) {
			Problem problem = new Problem(true, "Error writing contents file for item directory " + getSAFDirectory() + ", reason: " + e.getMessage());
			problems.add(problem);
		}
	}
	
	private void writeHandle(List<Problem> problems)
	{
	        File handleFile = new File(itemDirectory.getAbsolutePath() + "/handle");
	        try {
                    if(!handleFile.exists())
                    {
                            handleFile.createNewFile();
                    }
                    Util.setFileContents(handleFile, getHandle());
                } catch (FileNotFoundException e) {
					Problem problem = new Problem(true, "Unable to write to missing handle file for item directory " + getSAFDirectory() + ", reason: " + e.getMessage());
					problems.add(problem);
                } catch (IOException e) {
					Problem problem = new Problem(true, "Error writing handle file for item directory " + getSAFDirectory() + ", reason: " + e.getMessage());
					problems.add(problem);
                }
	}
	
	private void writeMetadata(List<Problem> problems)
	{
		for(SchematicFieldSet schema : schemata)
		{
			File metadataFile = new File(itemDirectory.getAbsolutePath() + "/" + schema.getFilename());
			
			try {
				if(!metadataFile.exists())
				{
					metadataFile.createNewFile();
				}
				Util.setFileContents(metadataFile, schema.getXML());
			} catch (FileNotFoundException e) {
				Problem problem = new Problem(true, "Unable to write to missing metadata file " + metadataFile.getAbsolutePath() + ", reason: " + e.getMessage());
				problems.add(problem);
			} catch (IOException e) {
				Problem problem = new Problem(true, "Unable to create metadata file " + metadataFile.getAbsolutePath() + ", reason: " + e.getMessage());
				problems.add(problem);
			}
		}
	}
	
	public List<Problem> writeItemSAF()
	{
		List<Problem> problems = new ArrayList<Problem>();
		writeContents(problems);
		writeMetadata(problems);
		if(getHandle() != null) writeHandle(problems);
		return problems;
	}

	public String getSAFDirectory() {
		return itemDirectory.getAbsolutePath();
	}

    public void setHandle(String handle) {
        this.handle = handle;        
    }
    
    public String getHandle() {
        return handle;
    }
}
