package edu.tamu.di.SAFCreator.model;

import java.util.ArrayList;

public class Flag {
	public static enum Columns {
		FLAG, DESCRIPTION, AUTHORITY, URL, COLUMN, ROW, ACTION
	};

	public static String[] ColumnNames = { "Flag", "Description", "Authority", "URL", "Column", "Row", "Action" };

	public static String ACCESS_DENIED = "Access Denied";
	public static String BAD_SCHEMA_NAME = "Bad Schema Name";
	public static String FILE_ERROR = "File Error";
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
	public static String DELETE_FAILURE = "Delete Failure";

	private ArrayList<String> rowData;

	/**
	 * Initialize row with empty cells.
	 */
	public Flag() {
		rowData = new ArrayList<String>();
		rowData.add("");
		rowData.add("");
		rowData.add("");
		rowData.add("");
		rowData.add("");
		rowData.add("");
		rowData.add("");
	}

	/**
	 * Initialize row with pre-populated cells.
	 *
	 * @param flagCode
	 *            the flag code.
	 * @param flagName
	 *            the flag name.
	 * @param action
	 *            the action status associated with the batch.
	 */
	public Flag(String flagCode, String flagName, String action) {
		rowData = new ArrayList<String>();
		rowData.add(flagCode);
		rowData.add(flagName);
		rowData.add("");
		rowData.add("");
		rowData.add("");
		rowData.add("");
		rowData.add(action);
	}

	/**
	 * Initialize row with pre-populated cells.
	 *
	 * @param flagCode
	 *            the flag code.
	 * @param flagName
	 *            the flag name.
	 * @param action
	 *            the action status associated with the batch.
	 * @param bitstream
	 *            the bitstream to extract the information from.
	 */
	public Flag(String flagCode, String flagName, String action, Bitstream bitstream) {
		rowData = new ArrayList<String>();
		rowData.add(flagCode);
		rowData.add(flagName);
		rowData.add(bitstream.getSource().getAuthority());
		rowData.add(bitstream.getSource().toString());
		rowData.add(bitstream.getColumnLabel());
		rowData.add("" + bitstream.getRow());
		rowData.add(action);
	}

	/**
	 * Initialize row with pre-populated cells.
	 *
	 * @param flagCode
	 *            the flag code.
	 * @param flagName
	 *            the flag name.
	 * @param authority
	 *            the authority/hostname..
	 * @param url
	 *            the entire URL (including authority/hostname).
	 * @param column
	 *            column number/letter (of imported CSV file).
	 * @param row
	 *            row number (of imported CSV file).
	 * @param action
	 *            the action status associated with the batch.
	 */
	public Flag(String flagCode, String flagName, String authority, String url, String column, String row,
	        String action) {
		rowData = new ArrayList<String>();
		rowData.add(flagCode);
		rowData.add(flagName);
		rowData.add(authority);
		rowData.add(url);
		rowData.add(column);
		rowData.add(row);
		rowData.add(action);
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
		rowData.set(Columns.ACTION.ordinal(), "");
	}

	/**
	 * Retrieve data from a cell.
	 * 
	 * @param column
	 *            the Row.Columns column id.
	 *
	 * @return String of data for the specified column.
	 */
	public String getCell(Columns column) {
		return rowData.get(column.ordinal());
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
		returnData.set(Columns.ACTION.ordinal(), rowData.get(Columns.ACTION.ordinal()));

		return returnData;
	}

	/**
	 * Assign data to a cell.
	 *
	 * @param column
	 *            the Row.Columns column id.
	 * @param data
	 *            the data to assign.
	 */
	public void setCell(Columns column, String data) {
		rowData.set(column.ordinal(), data);
	}

	/**
	 * Assign data for the entire row.
	 *
	 * @param flagCode
	 *            the flag code.
	 * @param flagName
	 *            the flag name.
	 * @param authority
	 *            the authority/hostname.
	 * @param url
	 *            the entire URL (including authority/hostname).
	 * @param column
	 *            column number/letter (of imported CSV file).
	 * @param row
	 *            row number (of imported CSV file).
	 * @param action
	 *            the action status associated with the batch.
	 */
	public void setRow(String flagCode, String flagName, String authority, String url, String column, String row,
	        String action) {
		rowData.set(Columns.FLAG.ordinal(), flagCode);
		rowData.set(Columns.DESCRIPTION.ordinal(), flagName);
		rowData.set(Columns.AUTHORITY.ordinal(), authority);
		rowData.set(Columns.URL.ordinal(), url);
		rowData.set(Columns.COLUMN.ordinal(), column);
		rowData.set(Columns.ROW.ordinal(), row);
		rowData.set(Columns.ACTION.ordinal(), action);
	}

	/**
	 * Get the row as an object array.
	 *
	 * @return an object array of the row cells.
	 */
	public Object[] toObject() {
		return new Object[] { rowData.get(Columns.FLAG.ordinal()), rowData.get(Columns.DESCRIPTION.ordinal()),
		        rowData.get(Columns.AUTHORITY.ordinal()), rowData.get(Columns.URL.ordinal()),
		        rowData.get(Columns.COLUMN.ordinal()), rowData.get(Columns.ROW.ordinal()),
		        rowData.get(Columns.ACTION.ordinal()), };
	}
}
