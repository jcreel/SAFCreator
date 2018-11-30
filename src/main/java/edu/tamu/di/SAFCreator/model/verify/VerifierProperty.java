package edu.tamu.di.SAFCreator.model.verify;

public interface VerifierProperty {
    public boolean generatesError();

    public boolean getActivated();

    public boolean isSwingWorker();

    public String prettyName();

    public void setActivated(boolean activated);
}
