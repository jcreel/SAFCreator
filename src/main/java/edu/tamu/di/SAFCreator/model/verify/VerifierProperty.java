package edu.tamu.di.SAFCreator.model.verify;

public interface VerifierProperty {
    public boolean generatesError();

    public boolean isEnabled();

    public boolean isSwingWorker();

    public String prettyName();

    public void setEnabled(boolean enabled);
}
