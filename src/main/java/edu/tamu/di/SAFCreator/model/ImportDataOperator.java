package edu.tamu.di.SAFCreator.model;

import javax.swing.JTextArea;

import edu.tamu.di.SAFCreator.model.Batch;
import edu.tamu.di.SAFCreator.model.FlagPanel;

public interface ImportDataOperator
{
	public class Updates {
		private int processed;
		private int total;

		public Updates() {
			processed = 0;
			total = 0;
		}

		public Updates(int processed, int total) {
			this.processed = processed;
			this.total = total;
		}

		public int getProcessed() {
			return processed;
		}

		public int getTotal() {
			return total;
		}

		public void setProcessed(int processed) {
			this.processed = processed;
		}

		public void setTotal(int total) {
			this.total = total;
		}
	}

	public Batch getBatch();

	public void setBatch(Batch batch);

	public void setConsole(JTextArea console);

	public void setFlags(FlagPanel flags);

	public JTextArea getConsole();

	public FlagPanel getFlags();
}
