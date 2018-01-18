package edu.tamu.di.SAFCreator.model;

public class StubLabel extends CellDatumImpl implements ColumnLabel
{

	@Override
	public boolean isHandle()
	{
		return false;
	}

	@Override
	public boolean isField()
	{
		return false;
	}

	@Override
	public boolean isFile()
	{
		return false;
	}
}
