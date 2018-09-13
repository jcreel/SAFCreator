package edu.tamu.di.SAFCreator;

import java.io.File;
import java.util.List;

import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import edu.tamu.di.SAFCreator.model.Batch;
import edu.tamu.di.SAFCreator.model.FlagPanel;
import edu.tamu.di.SAFCreator.model.ImportDataOperator;
import edu.tamu.di.SAFCreator.model.Item;
import edu.tamu.di.SAFCreator.model.Verifier;
import edu.tamu.di.SAFCreator.model.Verifier.Problem;

public class ImportDataWriter extends SwingWorker<Boolean, ImportDataOperator.Updates> implements ImportDataOperator
{
	private Batch batch = null;
	private JTextArea console = null;
	private FlagPanel flags = null;

	@Override
	public Batch getBatch() {
		return batch;
	}

	@Override
	public void setBatch(Batch batch) {
		this.batch = batch;
	}

	@Override
	public void setConsole(JTextArea console) {
		this.console = console;
	}

	@Override
	public void setFlags(FlagPanel flags) {
		this.flags = flags;
	}

	@Override
	public JTextArea getConsole() {
		return console;
	}

	@Override
	public FlagPanel getFlags() {
		return flags;
	}

	@Override
	protected Boolean doInBackground()
	{
		boolean noErrors = true;
		int itemCount = 1;
		int totalItems = batch.getItems().size();

		for (Item item : batch.getItems())
		{
			if (isCancelled()) {
				break;
			}

			if (batch.isIgnoredRow(++itemCount)) {
				File directory = new File(batch.getOutputSAFDir().getAbsolutePath() + File.separator + itemCount);
				directory.delete();

				console.append("\tSkipped item (row " + itemCount + "), because of verification failure.\n");
				publish(new ImportDataOperator.Updates(itemCount-1, totalItems));
				continue;
			}

			boolean hasError = false;
			List<Problem> problems = item.writeItemSAF();
			for(Verifier.Problem problem : problems)
			{
				console.append("\t" + problem.toString()+"\n");
				if (problem.isError()) {
					hasError = true;
					noErrors = false;
				}
				if (problem.isFlagged()) {
					flags.appendRow(problem.getFlag());
				}
			}

			if (hasError) {
				batch.failedRow(itemCount);
				console.append("\tFailed to write item (row " + itemCount + ") " + item.getSAFDirectory() + ".\n");
			} else {
				console.append("\tWrote item (row " + itemCount + ") " + item.getSAFDirectory() + ".\n");
			}

			publish(new ImportDataOperator.Updates(itemCount-1, totalItems));
		}

		if (isCancelled()) {
			console.append("Cancelled writing SAF.\n");
			return null;
		}

		console.append("Done writing SAF data.\n");
		return noErrors;
	}
}
