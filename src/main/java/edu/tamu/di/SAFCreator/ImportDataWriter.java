package edu.tamu.di.SAFCreator;

import java.io.File;
import java.util.List;

import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import edu.tamu.di.SAFCreator.model.Batch;
import edu.tamu.di.SAFCreator.model.FlagPanel;
import edu.tamu.di.SAFCreator.model.Item;
import edu.tamu.di.SAFCreator.model.Verifier;
import edu.tamu.di.SAFCreator.model.Verifier.Problem;

public class ImportDataWriter extends SwingWorker<Boolean, ImportDataWriter.WriterUpdates>
{
	private Batch batch = null;
	private JTextArea console = null;
	private FlagPanel flags = null;

	public class WriterUpdates {
		private int processed;
		private int total;

		public WriterUpdates() {
			processed = 0;
			total = 0;
		}

		public WriterUpdates(int processed, int total) {
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

	public Batch getBatch() {
		return batch;
	}

	public void setBatch(Batch batch) {
		this.batch = batch;
	}

	public void setConsole(JTextArea console) {
		this.console = console;
	}

	public void setFlags(FlagPanel flags) {
		this.flags = flags;
	}

	public JTextArea getConsole() {
		return console;
	}

	public FlagPanel getFlags() {
		return flags;
	}

	@Override
	protected Boolean doInBackground()
	{
		boolean noErrors = true;
		int itemCount = 0;
		int totalItems = batch.getItems().size();
		for(Item item : batch.getItems())
		{
			if (batch.isIgnoredRow(++itemCount)) {
				File directory = new File(batch.getOutputSAFDir().getAbsolutePath() + File.separator + itemCount);
				directory.delete();

				console.append("\tSkipped item (row " + itemCount + "), because of verification failure.\n");
				publish(new ImportDataWriter.WriterUpdates(itemCount, totalItems));
				continue;
			}

			boolean hasError = false;
			List<Problem> problems = item.writeItemSAF();
			for(Verifier.Problem problem : problems)
			{
				console.append(problem.toString()+"\n");
				if (problem.isError()) {
					hasError = true;
					noErrors = false;
				}
				if (problem.isFlagged()) {
					flags.appendRow(problem.getFlag());
				}
			}

			if (hasError) {
				console.append("\tFailed to write item (row " + itemCount + ") " + item.getSAFDirectory() + ".\n");
			} else {
				console.append("\tWrote item (row " + itemCount + ") " + item.getSAFDirectory() + ".\n");
			}

			if (isCancelled()) {
				console.append("Cancelled writing SAF.\n");
				return (Boolean) null;
			}

			publish(new ImportDataWriter.WriterUpdates(itemCount, totalItems));
		}

		console.append("Done writing SAF data.\n");
		return noErrors;
	}
}
