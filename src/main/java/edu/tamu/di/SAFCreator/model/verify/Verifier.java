package edu.tamu.di.SAFCreator.model.verify;

import java.util.List;

import javax.swing.JTextArea;

import edu.tamu.di.SAFCreator.model.Batch;
import edu.tamu.di.SAFCreator.model.FlagPanel;
import edu.tamu.di.SAFCreator.model.Problem;

public interface Verifier extends VerifierProperty {

    public List<Problem> verify(Batch batch);

    public List<Problem> verify(Batch batch, JTextArea console, FlagPanel flagPanel);
}
