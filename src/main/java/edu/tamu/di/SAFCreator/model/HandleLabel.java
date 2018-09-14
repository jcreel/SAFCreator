package edu.tamu.di.SAFCreator.model;

public class HandleLabel extends CellDatumImpl implements ColumnLabel {
	public HandleLabel() {
	}

	@Override
	public boolean isField() {
		return false;
	}

	@Override
	public boolean isFile() {
		return false;
	}

	@Override
	public boolean isHandle() {
		return true;
	}
}
