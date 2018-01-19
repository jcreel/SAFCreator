package edu.tamu.di.SAFCreator.model;

import java.util.List;

import javax.swing.SwingWorker;

public abstract class VerifierBackground extends SwingWorker<List<Verifier.Problem>, VerifierBackground.VerifierUpdates> implements Verifier
{
	VerifierBackground nextVerifier = null;

	public class VerifierUpdates {
		private int processed;
		private int total;

		public VerifierUpdates() {
			processed = 0;
			total = 0;
		}

		public VerifierUpdates(int processed, int total) {
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
