package edu.tamu.di.SAFCreator.model;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.tamu.di.SAFCreator.Util;

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
	
	private void writeContents()
	{
		String contentsString = "";
		for(Bundle bundle : bundles)
		{
			for(Bitstream bitstream : bundle.getBitstreams())
			{
				contentsString += bitstream.getContentsManifestLine();
				if( ! batch.getIgnoreFiles())
				{
					bitstream.copyMe();
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
			System.err.println("Unable to write to missing contents file for item directory " + getSAFDirectory());
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Error writing contents file for item directory " + getSAFDirectory());
			e.printStackTrace();
		}
	}
	
	private void writeHandle()
	{
	        File handleFile = new File(itemDirectory.getAbsolutePath() + "/handle");
	        try {
                    if(!handleFile.exists())
                    {
                            handleFile.createNewFile();
                    }
                    Util.setFileContents(handleFile, getHandle());
                } catch (FileNotFoundException e) {
                        System.err.println("Unable to write to missing handle file for item directory " + getSAFDirectory());
                        e.printStackTrace();
                } catch (IOException e) {
                        System.err.println("Error writing handle file for item directory " + getSAFDirectory());
                        e.printStackTrace();
                }
	}
	
	private void writeMetadata()
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
				System.err.println("Unable to write to missing metadata file " + metadataFile.getAbsolutePath());
				e.printStackTrace();
			} catch (IOException e) {
				System.err.println("Unable to create metadata file " + metadataFile.getAbsolutePath());
				e.printStackTrace();
			}
		}
	}
	
	public void writeItemSAF()
	{
		writeContents();
		writeMetadata();
		if(getHandle() != null) writeHandle();
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
