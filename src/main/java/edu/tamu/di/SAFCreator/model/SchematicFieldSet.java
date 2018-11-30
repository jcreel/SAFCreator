package edu.tamu.di.SAFCreator.model;

import java.util.ArrayList;
import java.util.List;

public class SchematicFieldSet {
    private Item item;
    private String schemaName;
    private List<Field> fields = new ArrayList<Field>();


    public void addField(Field field) {
        fields.add(field);
    }

    public List<Field> getFields() {
        return fields;
    }

    public String getFilename() {
        return schemaName.equals("dc") ? "dublin_core.xml" : "metadata_" + schemaName + ".xml";
    }

    public Item getItem() {
        return item;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getXML() {
        String xmlString = "<dublin_core" + (schemaName.equals("dc") ? "" : " schema=\"" + schemaName + "\"") + ">\n";
        for (Field field : fields) {
            xmlString += "\t" + field.getXMLSnippet() + "\n";
        }
        xmlString += "</dublin_core>";

        return xmlString;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

}
