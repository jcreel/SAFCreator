package edu.tamu.di.SAFCreator.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class Batch {

	public static enum BatchStatus { UNVERIFIED, FAILED_VERIFICATION, VERIFIED };
	
	private String name;
	private BatchStatus status;
	private File inputFilesDir;
	private File outputSAFDir;
	private List<Item> items = new ArrayList<Item>();
	private License license;
	private List<ColumnLabel> labels = new ArrayList<ColumnLabel>();
	private Boolean ignoreFiles = false;
	
	public void setLicense(String filename, String bundleName, String licenseText)
	{
		license = new License();
		license.setFilename(filename);
		license.setBundleName(bundleName);
		license.setLicenseText(licenseText);
	}
	
	public void unsetLicense()
	{
		this.license = null;
	}
	
	public License getLicense()
	{
		return license;
	}
	
	/**
	 * @return The user supplied name of this batch
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 * Set the user supplied name of this batch.
	 * 
	 * @param name
	 *            The new name.
	 */
	public void setName(String name)
	{
		this.name = name;
	}
	
	/**
	 * @return The current evaluation status of the batch.
	 */
	public BatchStatus getStatus()
	{
		return status;
	}
	
	
	/**
	 * Set the evaluation status of the batch.
	 */
	public void setStatus(BatchStatus status)
	{
		this.status = status;
	}
	
	/**
	 * @return The base directory from where to begin looking for all associated
	 *         file content for this batch.
	 */
	public File getinputFilesDir()
	{
		return inputFilesDir;
	}
	
	/**
	 * Set the base directory from where to begin searching for any associated
	 * files.
	 * 
	 * @param directory
	 *            The new base directory.
	 */
	public void setinputFilesDir(File directory)
	{
		inputFilesDir = directory;
	}
	
	public void setinputFilesDir(String directoryName)
	{
		inputFilesDir = new File(directoryName);
	}

	
		
	/**
	 * @return A list of all items associated with this batch.
	 */
	public List<Item> getItems()
	{
		return items;
	}
	
	/**
	 * Add a new item to the list of items contained within this
	 * batch.
	 * 
	 */
	public void addItem(Item item)
	{
		items.add(item);
	}

	public File getOutputSAFDir() {
		return outputSAFDir;
	}

	public void setOutputSAFDir(File outputSAFDir) {
		this.outputSAFDir = outputSAFDir;
	}
	
	public void setOutputSAFDir(String outputSAFDirName) {
		this.outputSAFDir = new File(outputSAFDirName);
	}

	public List<ColumnLabel> getLabels() {
		return labels;
	}

	public void setLabels(List<ColumnLabel> labels) {
		this.labels = labels;
	}

	public void restrictItemsToGroup(String groupName) 
	{
		for(Item item : items)
		{
			for(Bundle bundle : item.getBundles())
			{
				for(Bitstream bitstream : bundle.getBitstreams())
				{
					bitstream.setReadPolicyGroupName(groupName);
				}
			}
		}
		
	}

	public Boolean getIgnoreFiles() {
		return ignoreFiles;
	}

	public void setIgnoreFiles(Boolean ignoreFiles) {
		this.ignoreFiles = ignoreFiles;
	}
	
}
