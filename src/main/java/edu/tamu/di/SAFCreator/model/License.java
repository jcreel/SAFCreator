package edu.tamu.di.SAFCreator.model;

import java.io.File;
import java.io.IOException;

import edu.tamu.di.SAFCreator.Util;

public class License 
{
	private String filename;
	private String bundleName;
	private String licenseText;
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public String getBundleName() {
		return bundleName;
	}
	public void setBundleName(String bundleName) {
		this.bundleName = bundleName;
	}
	public String getLicenseText() {
		return licenseText;
	}
	public void setLicenseText(String licenseText) {
		this.licenseText = licenseText;
	}
	
	public String getContentsManifestLine() {
		
		return filename + "\tBUNDLE:" + bundleName + "\n";
	}
	
	public void writeToItem(Item item) 
	{
		File licenseFile = new File(item.getSAFDirectory() + "/" + filename);
	
		try 
		{
			if(!licenseFile.exists())
			{
			
				licenseFile.createNewFile();
			}
			
			Util.setContents(licenseFile, licenseText);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
