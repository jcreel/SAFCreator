package edu.tamu.di.SAFCreator.model;

public class HandleLabel extends CellDatumImpl implements ColumnLabel 
{
	public HandleLabel() {
	}

	public boolean isHandle() {
	        return true;
	}
	
	public boolean isField() {
		return false;
	}

	public boolean isFile() {
		return false;
	}
}
