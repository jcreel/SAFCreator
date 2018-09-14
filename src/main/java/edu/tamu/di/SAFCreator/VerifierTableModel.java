package edu.tamu.di.SAFCreator;

import java.util.List;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import edu.tamu.di.SAFCreator.model.Verifier;

public class VerifierTableModel extends AbstractTableModel {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	Vector<String> columnNames;// new Vector<String>();
	Vector<Vector<Object>> rowData;// new Vector<Vector<Object>>();

	// String[] columnNames = {"Blah", "Foo"};
	// Object[][] data = {{1, 2}, {3, 4}};

	// public MyTableModel()
	// {
	// String[] columnNames = {"Blah", "Foo"};
	// Object[][] data = {{1, 2}, {3, 4}};
	// }

	public VerifierTableModel(List<Verifier> verifiers) {
		System.out.println("Called!");
		columnNames = new Vector<String>();
		rowData = new Vector<Vector<Object>>();

		columnNames.add("Verifier");
		columnNames.add("Activated");

		Vector<Vector<Object>> rowData = new Vector<Vector<Object>>();

		for (Verifier verifier : verifiers) {
			System.out.println("Adding verifier  " + verifier.getClass().getSimpleName());
			Vector<Object> row = new Vector<Object>();

			row.add(verifier.getClass().getName());
			row.add(true);

			rowData.add(row);
		}
		System.out.println(rowData.size() + " rows.");
	}

	@Override
	public int getColumnCount() {
		// System.out.println("columnData size " + columnNames.size());
		return columnNames.size();
	}

	@Override
	public String getColumnName(int col) {
		System.out.println("getting column name " + col + ": " + columnNames.get(col));
		return columnNames.get(col);
	}

	@Override
	public int getRowCount() {
		System.out.println("Now I claim " + rowData.size() + " rows, but columns stayed at " + getColumnCount());
		return rowData.size();
		// return 1;
	}

	@Override
	public Object getValueAt(int row, int col) {
		System.out.println("getting datum " + row + ", " + col + ":" + rowData.get(row).get(col));
		return rowData.get(row).get(col);
	}

	// public int getColumnCount() {
	// return columnNames.length;
	// }
	//
	// public int getRowCount() {
	// return data.length;
	// }
	//
	// public String getColumnName(int col) {
	// return columnNames[col];
	// }
	//
	// public Object getValueAt(int row, int col) {
	// return data[row][col];
	// }

	// @Override
	// public Class<? extends Object> getColumnClass(int c) {
	// return getValueAt(0, c).getClass();
	// }

	/*
	 * Don't need to implement this method unless your table's editable.
	 */
	// public boolean isCellEditable(int row, int col) {
	// //Note that the data/cell address is constant,
	// //no matter where the cell appears onscreen.
	// if (col > 1) {
	// return true;
	// } else {
	// return false;
	// }
	// }

	/*
	 * Don't need to implement this method unless your table's data can change.
	 */
	// public void setValueAt(Object value, int row, int col) {
	// rowData.get(row).set(col, value);
	// fireTableCellUpdated(row, col);
	// }

}
