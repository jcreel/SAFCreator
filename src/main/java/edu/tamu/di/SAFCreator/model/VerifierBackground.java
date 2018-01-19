package edu.tamu.di.SAFCreator.model;

import java.util.List;

import javax.swing.SwingWorker;

public abstract class VerifierBackground extends SwingWorker<List<Verifier.Problem>, Integer> implements Verifier
{
	VerifierBackground nextVerifier = null;

	public VerifierBackground getNextVerifier() {
		return nextVerifier;
	}

	public void setNextVerifier(VerifierBackground verifier) {
		nextVerifier = verifier;
	}

	@Override
	public boolean isSwingWorker()
	{
		return true;
	}

	@Override
	protected List<Problem> doInBackground()
	{
		return null;
	}

	@Override
	protected void done() {
		if (nextVerifier == null) {
			return;
		}

		if (nextVerifier.isSwingWorker()) {
			nextVerifier.execute();
		}
	}
}
