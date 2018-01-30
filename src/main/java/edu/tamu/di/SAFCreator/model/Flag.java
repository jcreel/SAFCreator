package edu.tamu.di.SAFCreator.model;

import java.util.ArrayList;

public class Flag {
	public static enum Columns {FLAG, DESCRIPTION, AUTHORITY, URL, COLUMN, ROW};
	public static String[] ColumnNames = {"Flag", "Description", "Authority", "URL", "Column", "Row"};

	public static String ACCESS_DENIED = "Access Denied";
	public static String HTTP_FAILURE = "HTTP Failure";
	public static String INVALID_MIME = "Invalid File Type";
	public static String INVALID_FORMAT = "Invalid Format";
	public static String IO_FAILURE = "I/O Failure";
	public static String NOT_FOUND = "Not Found";
	public static String NO_UNIQUE_ID = "No Unique ID";
	public static String REDIRECT_FAILURE = "Redirect Failure";
	public static String REDIRECT_LIMIT = "Redirect Limit";
	public static String REDIRECT_LOOP = "Redirect Loop";
	public static String SERVICE_ERROR = "Server Error";
	public static String SERVICE_REJECTED = "Service Rejected";
	public static String SERVICE_TIMEOUT = "Service Timeout";
	public static String SERVICE_UNAVAILABLE = "Service Unavailable";
	public static String SSL_FAILURE = "SSL Failure";
	public static String SOCKET_ERROR = "Socket Error";

	private ArrayList<String> rowData;

	/**
	 * Initialize row with empty cells.
	 */
	public Flag() {
		this.rowData = new ArrayList<String>();
		this.rowData.add("");
		this.rowData.add("");
		this.rowData.add("");
		this.rowData.add("");
		this.rowData.add("");
		this.rowData.add("");
	}

	/**
	 * Initialize row with pre-populated cells.
	 *
	 * @param flagCode the flag code.
	 * @param flagName the flag name.
	 * @param authority the authority/hostname.
	 * @param url the entire URL (including authority/hostname).
	 * @param column column number/letter (of imported CSV file).
	 * @param row row number (of imported CSV file).
	 */
	public Flag(String flagCode, String flagName, String authority, String url, String column, String row) {
		this.rowData = new ArrayList<String>();
		this.rowData.add(flagCode);
		this.rowData.add(flagName);
		this.rowData.add(authority);
		this.rowData.add(url);
		this.rowData.add(column);
		this.rowData.add(row);
	}

	/**
	 * Assign data to a cell.
	 *
	 * @param column the Row.Columns column id.
	 * @param data the data to assign.
	 */
	public void setCell(Columns column, String data) {
		rowData.set(column.ordinal(), data);
	}

	/**
	 * Retrieve data from a cell.
	 * @param column the Row.Columns column id.
	 *
	 * @return String of data for the specified column.
	 */
	public String getCell(Columns column) {
		return rowData.get(column.ordinal());
	}

	/**
	 * Assign data for the entire row.
	 *
	 * @param flagCode the flag code.
	 * @param flagName the flag name.
	 * @param authority the authority/hostname.
	 * @param url the entire URL (including authority/hostname).
	 * @param column column number/letter (of imported CSV file).
	 * @param row row number (of imported CSV file).
	 */
	public void setRow(String flagCode, String flagName, String authority, String url, String column, String row) {
		rowData.set(Columns.FLAG.ordinal(), flagCode);
		rowData.set(Columns.DESCRIPTION.ordinal(), flagName);
		rowData.set(Columns.AUTHORITY.ordinal(), authority);
		rowData.set(Columns.URL.ordinal(), url);
		rowData.set(Columns.COLUMN.ordinal(), column);
		rowData.set(Columns.ROW.ordinal(), row);
	}

	/**
	 * Retrieve a copy of the row data.
	 *
	 * @return An array list of strings containing the row information.
	 */
	public ArrayList<String> getRow() {
		ArrayList<String> returnData = new ArrayList<String>();
		returnData.set(Columns.FLAG.ordinal(), rowData.get(Columns.FLAG.ordinal()));
		returnData.set(Columns.DESCRIPTION.ordinal(), rowData.get(Columns.DESCRIPTION.ordinal()));
		returnData.set(Columns.AUTHORITY.ordinal(), rowData.get(Columns.AUTHORITY.ordinal()));
		returnData.set(Columns.URL.ordinal(), rowData.get(Columns.URL.ordinal()));
		returnData.set(Columns.COLUMN.ordinal(), rowData.get(Columns.COLUMN.ordinal()));
		returnData.set(Columns.ROW.ordinal(), rowData.get(Columns.ROW.ordinal()));

		return returnData;
	}

	/**
	 * Resets all cells to empty strings.
	 */
	public void clear() {
		rowData.set(Columns.FLAG.ordinal(), "");
		rowData.set(Columns.DESCRIPTION.ordinal(), "");
		rowData.set(Columns.AUTHORITY.ordinal(), "");
		rowData.set(Columns.URL.ordinal(), "");
		rowData.set(Columns.COLUMN.ordinal(), "");
		rowData.set(Columns.ROW.ordinal(), "");
	}

	/**
	 * Get the row as an object array.
	 *
	 * @return an object array of the row cells.
	 */
	public Object[] toObject() {
		return new Object[] {
			rowData.get(Columns.FLAG.ordinal()),
			rowData.get(Columns.DESCRIPTION.ordinal()),
			rowData.get(Columns.AUTHORITY.ordinal()),
			rowData.get(Columns.URL.ordinal()),
			rowData.get(Columns.COLUMN.ordinal()),
			rowData.get(Columns.ROW.ordinal()),
		};
	}
}
