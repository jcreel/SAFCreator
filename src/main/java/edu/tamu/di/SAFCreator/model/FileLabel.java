package edu.tamu.di.SAFCreator.model;

public class FileLabel extends CellDatumImpl implements ColumnLabel 
{
	private String bundleName;

	public FileLabel(String bundleName) {
		setBundleName(bundleName);
	}

	public String getBundleName() {
		return bundleName;
	}

	public void setBundleName(String bundleName) {
		this.bundleName = bundleName;
	}
	
	public boolean isHandle() {
	    return true;
	}
    
	public boolean isField() {
		return false;
	}

	public boolean isFile() {
		return true;
	}
}
