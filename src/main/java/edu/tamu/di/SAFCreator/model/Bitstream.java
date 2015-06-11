package edu.tamu.di.SAFCreator.model;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class Bitstream extends CellDatumImpl
{
	private Bundle bundle;
	private File source;
	private String relativePath;
	private File destination;
	private String readPolicyGroupName = null;

	public Bundle getBundle() {
		return bundle;
	}

	public void setBundle(Bundle bundle) {
		this.bundle = bundle;
	}

	public File getSource() {
		return source;
	}

	public void setSource(File source) {
		this.source = source;
	}

	public File getDestination() {
		return destination;
	}

//	public void setDestination(File destination) {
//		this.destination = destination;
//	}

	public String getReadPolicyGroupName() {
		return readPolicyGroupName;
	}

	public void setReadPolicyGroupName(String readPolicyGroupName) {
		this.readPolicyGroupName = readPolicyGroupName;
	}
	
	public String getContentsManifestLine()
	{
		String line = getRelativePath() + "\tBUNDLE:" + bundle.getName() + (readPolicyGroupName==null?"\n":"\tpermissions:-r "+readPolicyGroupName)+"\n"; 
		return line;
	}
	
	public void copyMe()
	{
		try {
			FileUtils.copyFile(source, destination);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setRelativePath(String value) {
		relativePath = value;
		destination = new File(bundle.getItem().getSAFDirectory()+"/"+relativePath);
		
	}
	
	public String getRelativePath()
	{
		return relativePath;
	}

}
