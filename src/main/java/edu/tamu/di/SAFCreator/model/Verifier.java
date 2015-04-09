package edu.tamu.di.SAFCreator.model;

import java.util.List;

public interface Verifier 
{
	public boolean generatesError();
	public String prettyName();
	
	public class Problem 
	{
		private int rownumber;
		private char columnletter;
		private boolean error;
		private String note;
		
		
		public Problem(int rownumber, char columnletter, boolean error, String note)
		{
			this.rownumber = rownumber;
			this.columnletter = columnletter;
			this.error = error;
			this.note = note;
		}
		
		@Override
		public String toString()
		{
			return (error?"ERROR at ":"WARNING at ") + "column " + columnletter + " row " + rownumber + ": " + note;
		}
		
		public boolean isError()
		{
			return error;
		}
		
	}
	


	public List<Problem> verify(Batch batch);

}
