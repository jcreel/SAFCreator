package edu.tamu.di.SAFCreator.verify;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextArea;

import edu.tamu.di.SAFCreator.model.Batch;
import edu.tamu.di.SAFCreator.model.Flag;
import edu.tamu.di.SAFCreator.model.FlagPanel;
import edu.tamu.di.SAFCreator.model.Item;
import edu.tamu.di.SAFCreator.model.VerifierBackground;
import edu.tamu.di.SAFCreator.model.VerifierProperty;

public class ValidHandleVerifierImpl extends VerifierBackground {
	public ValidHandleVerifierImpl() {
		super();
	}

	public ValidHandleVerifierImpl(VerifierProperty settings) {
		super(settings);
	}

	@Override
	public void doCancel() {
	}

	@Override
	public boolean generatesError() {
		return true;
	}

	@Override
	public String prettyName() {
		return "Syntactically Valid Schema Names Verifier";
	}

	@Override
	public List<Problem> verify(Batch batch) {
		return verify(batch, null, null);
	}

	@Override
	public List<Problem> verify(Batch batch, JTextArea console, FlagPanel flagPanel) {
		List<Problem> problems = new ArrayList<Problem>();
		int itemCount = 0;
		for (Item item : batch.getItems()) {
			itemCount++;

			String handle = item.getHandle();
			if (handle == null) {
				// no handle column is defined, so assume it is not wanted.
				break;
			}
			if (handle.isEmpty()) {
				Flag flag = new Flag(Flag.NO_UNIQUE_ID, "Undefined Handle for row " + itemCount + ".",
				        batch.getAction());
				Problem problem = new Problem(true, "Undefined Handle for row " + itemCount + ".");
				problem.setFlag(flag);
				problems.add(problem);
				batch.failedRow(itemCount);
				if (console != null) {
					console.append("\t" + problem.toString() + "\n");
				}
				if (flagPanel != null) {
					flagPanel.appendRow(flag);
				}
			}

			if (isCancelled()) {
				return problems;
			}
		}

		return problems;
	}
}
