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
		private Flag flag = null;
		
		
		public Problem(boolean error, String note)
		{
			this.rownumber = null;
			this.columnletter = null;
			this.error = error;
			this.note = note;
			this.flag = null;
		}

		public Problem(int rownumber, char columnletter, boolean error, String note)
		{
			this.rownumber = rownumber;
			this.columnletter = columnletter;
			this.error = error;
			this.note = note;
			this.flag = null;
		}

		public Problem(int rownumber, char columnletter, boolean error, String note, Flag flag)
		{
			this.rownumber = rownumber;
			this.columnletter = columnletter;
			this.error = error;
			this.note = note;
			this.flag = flag;
		}
		
		@Override
		public String toString()
		{
			String flagged = "";
			if (flag != null) {
				flagged = "Flagged ";
			}
			if (rownumber == null) {
				return flagged + (error?"ERROR":"WARNING") + ": " + note;
			}

			return flagged + (error?"ERROR at ":"WARNING at ") + "column " + columnletter + " row " + rownumber + ":\n\t" + note;
		}
		
		public boolean isError()
		{
			return error;
		}

		public boolean isFlagged() {
			return flag != null;
		}

		public Flag getFlag() {
			return flag;
		}
	}
	

	public List<Problem> verify(Batch batch);
	public List<Problem> verify(Batch batch, JTextArea console, FlagPanel flagPanel);
}
