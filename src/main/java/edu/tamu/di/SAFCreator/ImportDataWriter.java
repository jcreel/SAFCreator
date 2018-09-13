package edu.tamu.di.SAFCreator;

import java.io.File;
import java.lang.reflect.Method;
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
		String cancelledMessage = "Cancelled writing SAF.\n";

		for (Item item : batch.getItems())
		{
			if (isCancelled()) {
				console.append(cancelledMessage);
				return null;
			}

			if (batch.isIgnoredRow(++itemCount)) {
				File directory = new File(batch.getOutputSAFDir().getAbsolutePath() + File.separator + itemCount);
				directory.delete();

				console.append("\tSkipped item (row " + itemCount + "), because of verification failure.\n");
				publish(new ImportDataOperator.Updates(itemCount-1, totalItems));
				continue;
			}

			if (isCancelled()) {
				console.append(cancelledMessage);
				return null;
			}

			boolean hasError = false;
			Method method;
			List<Problem> problems = null;
			try {
				method = this.getClass().getMethod("isCancelled");
				problems = item.writeItemSAF(this, method);

				for(Verifier.Problem problem : problems)
				{
					if (isCancelled()) {
						console.append(cancelledMessage);
						return null;
					}

					console.append("\t" + problem.toString()+"\n");
					if (problem.isError()) {
						hasError = true;
						noErrors = false;
					}
					if (problem.isFlagged()) {
						flags.appendRow(problem.getFlag());
					}
				}
			} catch (NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}

			if (isCancelled()) {
				console.append(cancelledMessage);
				return null;
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
			console.append(cancelledMessage);
			return null;
		}

		console.append("Done writing SAF data.\n");
		return noErrors;
	}
}
