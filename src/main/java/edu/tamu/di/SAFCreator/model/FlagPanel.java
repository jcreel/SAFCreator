package edu.tamu.di.SAFCreator.model;

import java.io.BufferedWriter;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 * An alternative (and limited) error handling class for displaying and exporting specific errors.
 */
public class FlagPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private UrlFlagPanelTableModel model;
	private JTable table;
	private JScrollPane scrollPane;

	/**
	 * Custom variant of default table model.
	 */
	private class UrlFlagPanelTableModel extends DefaultTableModel {
		private static final long serialVersionUID = 1L;

		@Override
	    public boolean isCellEditable(int row, int col) {
			return false;
		}
	}

	/**
	 * Initialize the class.
	 */
	public FlagPanel() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		model = new UrlFlagPanelTableModel();
		model.setColumnIdentifiers(Flag.ColumnNames);
		table = new JTable(model);
		table.setPreferredScrollableViewportSize(getMaximumSize());

		scrollPane = new JScrollPane(table);
		add(scrollPane);
	}

	/**
	 * Removes all rows from the table.
	 */
	public void clear() {
		while (model.getRowCount() > 0) {
			model.removeRow(model.getRowCount() - 1);
		}
	}

	/**
	 * Append a flag to the table.
	 *
	 * @param flag the row to append.
	 */
	public void appendRow(Flag flag) {
		model.addRow(flag.toObject());
	}

	/**
	 * Retrieve the current row by row number.
	 *
	 * @param rowNumber row to retrieve.
	 *
	 * @return the row.
	 */
	public Flag getRow(int rowNumber) {
		if (rowNumber < 0 || rowNumber >= table.getRowCount()) {
			return null;
		}
		String flag = (String) table.getValueAt(rowNumber, Flag.Columns.FLAG.ordinal());
		String description = (String) table.getValueAt(rowNumber, Flag.Columns.DESCRIPTION.ordinal());
		String authority = (String) table.getValueAt(rowNumber, Flag.Columns.AUTHORITY.ordinal());
		String url = (String) table.getValueAt(rowNumber, Flag.Columns.URL.ordinal());
		String column = (String) table.getValueAt(rowNumber, Flag.Columns.COLUMN.ordinal());
		String row = (String) table.getValueAt(rowNumber, Flag.Columns.ROW.ordinal());

		return new Flag(flag, description, authority, url, column, row);
	}

	/**
	 * Retrieve the currently selected row.
	 *
	 * @return the row.
	 */
	public Flag getSelected() {
		return getRow(table.getSelectedRow());
	}

	/**
	 * Generate a CSV representation of entire data table.
	 *
	 * @param writer output stream to write to.
	 *
	 * @throws IOException
	 */
	public void exportToCSV(BufferedWriter writer) throws IOException {
		staticExportToCSV(table, writer);
	}

	/**
	 * Retrieve the total number of rows.
	 *
	 * @return the number of rows.
	 */
	public int getRowCount() {
		return table.getRowCount();
	}

	/**
	 * Check to see if there are any rows.
	 *
	 * @return true if there are no rows, false otherwise.
	 */
	public boolean empty() {
		return table.getRowCount() == 0;
	}

	/**
	 * Generate a CSV representation of entire data table.
	 *
	 * The CSVPrinter is a static class, so a static implementation is needed.
	 *
	 * @param table CSV source data.
	 * @param writer output stream.
	 *
	 * @throws IOException
	 */
	private static void staticExportToCSV(JTable table, BufferedWriter writer) throws IOException {
		CSVFormat format = CSVFormat.RFC4180.withHeader(Flag.ColumnNames);
		CSVPrinter printer = new CSVPrinter(writer, format);

		String flag = null;
		String description = null;
		String authority = null;
		String url = null;
		String column = null;
		String row = null;

		int length = table.getRowCount();
		int i = 0;
		while (i < length) {
			flag = (String) table.getValueAt(i, Flag.Columns.FLAG.ordinal());
			description = (String) table.getValueAt(i, Flag.Columns.DESCRIPTION.ordinal());
			authority = (String) table.getValueAt(i, Flag.Columns.AUTHORITY.ordinal());
			url = (String) table.getValueAt(i, Flag.Columns.URL.ordinal());
			column = (String) table.getValueAt(i, Flag.Columns.COLUMN.ordinal());
			row = (String) table.getValueAt(i, Flag.Columns.ROW.ordinal());

			printer.printRecord(flag, description, authority, url, column, row);
			printer.flush();
			i++;
		}
		printer.close();
	}
}
