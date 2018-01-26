package edu.tamu.di.SAFCreator.model;

public class CellDatumImpl implements CellDatum 
{
	private int column;
	private int row;

	public int getColumn() {
		return column;
	}

	public String getColumnLabel() {
		int dividend = column;
		String label = "";
		int modulo;
		char character;

		while (dividend > 0)
		{
			modulo = (dividend - 1) % 26;
			character = Character.valueOf((char) (65 + modulo));

			label = character + label;
			dividend = (int) ((dividend - modulo) / 26);
		}

		return label;
	}

	public void setColumn(int column)
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
