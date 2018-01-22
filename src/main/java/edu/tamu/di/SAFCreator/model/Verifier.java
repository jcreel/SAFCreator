package edu.tamu.di.SAFCreator.model;

import java.util.List;

import javax.swing.JTextArea;

public interface Verifier 
{
	public boolean generatesError();
	public String prettyName();
	public boolean isSwingWorker();
	
	public class Problem 
	{
		private Integer rownumber = null;
		private Character columnletter = null;
		private boolean error;
		private String note;
		
		
		public Problem(boolean error, String note)
		{
			this.rownumber = null;
			this.columnletter = null;
			this.error = error;
			this.note = note;
		}

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
			if (rownumber == null) {
				return (error?"ERROR":"WARNING") + ": " + note;
			}

			return (error?"ERROR at ":"WARNING at ") + "column " + columnletter + " row " + rownumber + ": " + note;
		}
		
		public boolean isError()
		{
			return error;
		}
	}
	

	public List<Problem> verify(Batch batch);
	public List<Problem> verify(Batch batch, JTextArea console);
}
