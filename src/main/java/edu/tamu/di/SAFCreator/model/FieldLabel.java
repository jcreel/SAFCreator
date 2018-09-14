package edu.tamu.di.SAFCreator.model;

public class FieldLabel extends CellDatumImpl implements ColumnLabel {
    private String schema;
    private String element;
    private String qualifier;
    private String language;

    public String getElement() {
        return element;
    }

    public String getLanguage() {
        return language;
    }

    public String getQualifier() {
        return qualifier;
    }

    public String getSchema() {
        return schema;
    }

    @Override
    public boolean isField() {
        return true;
    }

    @Override
    public boolean isFile() {
        return false;
    }

    @Override
    public boolean isHandle() {
        return true;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

}
