package edu.tamu.di.SAFCreator.model;

public class FieldLabel extends CellDatumImpl implements ColumnLabel
{
	private String schema;
	private String element;
	private String qualifier;
	private String language;
	
	public String getSchema() {
		return schema;
	}
	public void setSchema(String schema) {
		this.schema = schema;
	}
	public String getElement() {
		return element;
	}
	public void setElement(String element) {
		this.element = element;
	}
	public String getQualifier() {
		return qualifier;
	}
	public void setQualifier(String qualifier) {
		this.qualifier = qualifier;
	}
	public String getLanguage() {
		return language;
	}
	public void setLanguage(String language) {
		this.language = language;
	}
	public boolean isHandle() {
	    return true;
	}
    public boolean isField() {
		return true;
	}
	public boolean isFile() {
		return false;
	}

}
