package edu.tamu.di.SAFCreator;

import java.util.List;

import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import edu.tamu.di.SAFCreator.model.Batch;
import edu.tamu.di.SAFCreator.model.Item;
import edu.tamu.di.SAFCreator.model.Verifier;
import edu.tamu.di.SAFCreator.model.Verifier.Problem;

public class ImportDataWriter extends SwingWorker<Boolean, ImportDataWriter.WriterUpdates>
{
	private Batch batch = null;
	private JTextArea console = null;

	public class WriterUpdates {
		private int processed = 0;
		private int total = 0;
		private JTextArea console = null;

		public int getProcessed() {
			return processed;
		}

		public int getTotal() {
			return total;
		}

		public JTextArea getConsole() {
			return console;
		}

		public void setProcessed(int processed) {
			this.processed = processed;
		}

		public void setTotal(int total) {
			this.total = total;
		}

		public void setConsole(JTextArea console) {
			this.console = console;
		}
	}

	public Batch getBatch() {
		return batch;
	}

	public void setBatch(Batch batch) {
		this.batch = batch;
	}

	public void setConsole(JTextArea console) {
		this.console = console;
	}

	public JTextArea getConsole() {
		return console;
	}

	@Override
	protected Boolean doInBackground()
	{
		boolean noErrors = true;
		for(Item item : batch.getItems())
		{
			boolean hasError = false;
			List<Problem> problems = item.writeItemSAF();
			for(Verifier.Problem problem : problems)
			{
				console.append(problem.toString()+"\n");
				if (problem.isError()) {
					hasError = true;
					noErrors = false;
				}
			}

			if (hasError) {
				console.append("\tFailed to write item " + item.getSAFDirectory() + "\n");
			} else {
				console.append("\tWrote item " + item.getSAFDirectory() + "\n");
			}

			if (isCancelled()) {
				console.append("Cancelled writing SAF.\n");
				return (Boolean) null;
			}
		}

		console.append("Done writing SAF data.\n");
		return noErrors;
	}
}
