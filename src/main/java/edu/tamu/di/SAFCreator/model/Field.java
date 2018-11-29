package edu.tamu.di.SAFCreator.model;

import org.apache.commons.lang3.StringEscapeUtils;

public class Field extends CellDatumImpl {
    private SchematicFieldSet schema;
    private FieldLabel label;
    // private String element;
    // private String language;
    // private String qualifier=null;
    private String value;

    public FieldLabel getLabel() {
        return label;
    }

    public SchematicFieldSet getSchema() {
        return schema;
    }

    // public String getElement() {
    // return element;
    // }
    // public void setElement(String element) {
    // this.element = element;
    // }
    // public String getQualifier() {
    // return qualifier;
    // }
    // public void setQualifier(String qualifier) {
    // this.qualifier = qualifier;
    // }
    public String getValue() {
        return value;
    }

    public String getXMLSnippet() {
        if (value != "") {
            return "<dcvalue element=\"" + label.getElement() + "\""
                    + (label.getQualifier() == null ? "" : " qualifier=\"" + label.getQualifier() + "\"")
                    + (label.getLanguage() == null ? "" : " language=\"" + label.getLanguage() + "\"") + ">"
                    + StringEscapeUtils.escapeXml(value) + "</dcvalue>";
        } else {
            return "";
        }
    }

    public void setLabel(FieldLabel label) {
        this.label = label;
    }

    public void setSchema(SchematicFieldSet schema) {
        this.schema = schema;
    }

    public void setValue(String value) {
        this.value = value;
    }
    // public String getLanguage() {
    // return language;
    // }
    // public void setLanguage(String language) {
    // this.language = language;
    // }

}
