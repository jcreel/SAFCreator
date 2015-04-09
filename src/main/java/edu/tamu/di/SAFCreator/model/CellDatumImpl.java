package edu.tamu.di.SAFCreator.model;

public class CellDatumImpl implements CellDatum 
{

	
	private char column;
	private int row;

	public char getColumn() {
		return column;
	}
	
	public void setColumn(char column)
	{
		this.column = column;
	}

	public int getRow() {
		return row;
	}
	
	public void setRow(int row)
	{
		this.row = row;
	}

}
