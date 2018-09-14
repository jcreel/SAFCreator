package edu.tamu.di.SAFCreator.model.verify;

import java.util.List;

import javax.swing.SwingWorker;

import edu.tamu.di.SAFCreator.model.Problem;

public abstract class VerifierBackground extends SwingWorker<List<Problem>, VerifierBackground.VerifierUpdates>
        implements Verifier {

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

    private boolean enabled;

    public VerifierBackground() {
        enabled = true;
    }

    public VerifierBackground(VerifierProperty settings) {
        this();
        enabled = settings.isEnabled();
    }

    /**
     * Provide a way to close active connections or similar situations on cancel.
     *
     * This must be provided in addition to cancel() because that method as provided by SwingWorker is a final method.
     */
    public abstract void doCancel();

    @Override
    protected List<Problem> doInBackground() {
        return null;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isSwingWorker() {
        return true;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
