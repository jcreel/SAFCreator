package edu.tamu.di.SAFCreator.model;

import java.util.List;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import edu.tamu.di.SAFCreator.model.verify.VerifierProperty;

public class VerifierTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;

    public static final String COLUMN_VERIFIER = "Verifier";
    public static final String COLUMN_GENERATES_ERRORS = "Generates Errors";
    public static final String COLUMN_ACTIVATED = "Activated";

    public static final String VERIFIER_ACTIVE = "Active";
    public static final String VERIFIER_INACTIVE = "Inactive";

    private Vector<String> columnNames;
    private Vector<Vector<Object>> rowData;

    private Vector<String> verifierNames;
    private Vector<VerifierProperty> verifierProperties;


    public VerifierTableModel(List<VerifierProperty> verifiers) {
        columnNames = new Vector<String>();
        rowData = new Vector<Vector<Object>>();

        verifierNames = new Vector<String>();
        verifierProperties = new Vector<VerifierProperty>();

        columnNames.add(COLUMN_VERIFIER);
        columnNames.add(COLUMN_GENERATES_ERRORS);
        columnNames.add(COLUMN_ACTIVATED);

        for (VerifierProperty verifier : verifiers) {
            Vector<Object> row = new Vector<Object>();

            row.add(verifier.prettyName());
            row.add(verifier.generatesError());
            row.add(verifier.getActivated() ? VERIFIER_ACTIVE : VERIFIER_INACTIVE);

            verifierNames.add(verifier.getClass().getName());
            verifierProperties.add(verifier);

            rowData.add(row);
        }
    }

    @Override
    public int getColumnCount() {
        return columnNames.size();
    }

    @Override
    public String getColumnName(int col) {
        return columnNames.get(col);
    }

    @Override
    public int getRowCount() {
        return rowData.size();
    }

    @Override
    public Object getValueAt(int row, int col) {
        Object object = null;
        if (row >= 0 && row < rowData.size()) {
            object = rowData.get(row).get(col);
        }
        return object;
    }

    public VerifierProperty getVerifierPropertyAt(int row) {
        VerifierProperty verifierProperty = null;
        if (row >= 0 && row < verifierProperties.size()) {
            verifierProperty = verifierProperties.get(row);
        }
        return verifierProperty;
    }

    public VerifierProperty getVerifierWithName(String name) {
        VerifierProperty verifierProperty = null;
        int row = 0;
        String verifierName = null;

        for (; row < verifierProperties.size(); row++) {
            verifierName = verifierNames.get(row);
            if (verifierName.equals(name)) {
                break;
            }
        }

        if (row < rowData.size()) {
            verifierProperty = verifierProperties.get(row);
        }

        return verifierProperty;
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        rowData.get(row).set(column, value);
        if (column == 2) {
            getVerifierPropertyAt(row).setActivated(value.equals(VERIFIER_ACTIVE));
        }
        fireTableCellUpdated(row, column);
    }

}
