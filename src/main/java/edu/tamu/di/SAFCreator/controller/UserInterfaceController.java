package edu.tamu.di.SAFCreator.controller;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import javax.swing.JFileChooser;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import edu.tamu.di.SAFCreator.enums.ActionStatus;
import edu.tamu.di.SAFCreator.enums.FieldChangeStatus;
import edu.tamu.di.SAFCreator.enums.FlagColumns;
import edu.tamu.di.SAFCreator.model.Batch;
import edu.tamu.di.SAFCreator.model.Flag;
import edu.tamu.di.SAFCreator.model.Problem;
import edu.tamu.di.SAFCreator.model.VerifierTableModel;
import edu.tamu.di.SAFCreator.model.importData.ImportDataCleaner;
import edu.tamu.di.SAFCreator.model.importData.ImportDataProcessor;
import edu.tamu.di.SAFCreator.model.importData.ImportDataWriter;
import edu.tamu.di.SAFCreator.model.verify.VerifierBackground;
import edu.tamu.di.SAFCreator.model.verify.VerifierProperty;
import edu.tamu.di.SAFCreator.model.verify.impl.LocalFilesExistVerifierImpl;
import edu.tamu.di.SAFCreator.model.verify.impl.RemoteFilesExistVerifierImpl;
import edu.tamu.di.SAFCreator.model.verify.impl.ValidSchemaNameVerifierImpl;
import edu.tamu.di.SAFCreator.view.UserInterfaceView;

public class UserInterfaceController {
    private final UserInterfaceView userInterface;

    private final ImportDataProcessor processor;

    private ActionStatus actionStatus = ActionStatus.NONE_LOADED;

    private FieldChangeStatus itemProcessDelayFieldChangeStatus = FieldChangeStatus.NO_CHANGES;
    private FieldChangeStatus remoteFileTimeoutFieldChangeStatus = FieldChangeStatus.NO_CHANGES;
    private FieldChangeStatus userAgentFieldChangeStatus = FieldChangeStatus.NO_CHANGES;


    // file and directory names
    private String csvOutputFileName = "SAF-Flags.csv";
    private String metadataInputFileName;
    private String sourceDirectoryName;
    private String outputDirectoryName;


    // batch
    private Boolean batchVerified;
    private Batch batch;
    private boolean batchContinue;
    private final Map<String, VerifierBackground> verifiers;


    // swing background process handling
    private ImportDataWriter currentWriter;
    private ImportDataCleaner currentCleaner;
    private final List<VerifierBackground> currentVerifiers;
    private int currentVerifier = -1;


    public UserInterfaceController(final ImportDataProcessor processor, final UserInterfaceView userInterface) {
        this.processor = processor;
        this.userInterface = userInterface;

        batch = new Batch();
        currentVerifiers = new ArrayList<VerifierBackground>();
        batchContinue = false;
        verifiers = new HashMap<String, VerifierBackground>();

        createBatchListeners();
        createFlagListeners();
        createLicenseListeners();
        createSettingsListeners();
        createVerificationListeners();
    }

    public void cancelVerifyCleanup() {
        userInterface.getStatusIndicator().setText("Batch Status:\n Unverified");

        unlockVerifyButtons();
    }

    public void cancelWriteCleanup() {
        userInterface.getWriteSAFBtn().setText("Write SAF data now!");

        userInterface.getStatusIndicator().setText("Batch Status:\n Unverified");
        userInterface.getStatusIndicator().setForeground(Color.white);
        userInterface.getStatusIndicator().setBackground(Color.blue);

        userInterface.getActionStatusField().setText("Batch write cancelled.");
        userInterface.getActionStatusField().setForeground(Color.black);
        userInterface.getActionStatusField().setBackground(Color.orange);
    }

    public void createVerifiers() {
        VerifierProperty settings = null;

        currentVerifier = -1;
        currentVerifiers.clear();
        verifiers.clear();

        if (batch != null) {
            batch.clearIgnoredRows();
            batch.clearFailedRows();
        }

        settings = userInterface.getTableModel().getVerifierWithName(ValidSchemaNameVerifierImpl.class.getName());

        ValidSchemaNameVerifierImpl validSchemaVerifier = new ValidSchemaNameVerifierImpl(settings) {
            @Override
            public List<Problem> doInBackground() {
                userInterface.getStatusIndicator().setText("Batch Status:\n Unverified\n Schema Name?\n 0 / " + batch.getItems().size());
                batch.setAction(prettyName());
                return verify(batch, userInterface.getConsole(), userInterface.getFlagPanel());
            }

            @Override
            public void done() {
                if (isCancelled()) {
                    doCancel();
                    return;
                }

                List<Problem> problems = new ArrayList<Problem>();
                if (batchVerified == null) {
                    batchVerified = true;
                }

                try {
                    problems = get();
                } catch (InterruptedException | ExecutionException e) {
                    batchVerified = false;
                    e.printStackTrace();
                }

                batchContinue = batch.getRemoteBitstreamErrorContinue();
                for (Problem problem : problems) {
                    if (problem.isError()) {
                        if (problem.isFlagged()) {
                            batchVerified = false;
                            if (batch.hasIgnoredRows()) {
                                continue;
                            } else {
                                break;
                            }
                        } else {
                            batchContinue = false;
                            batchVerified = false;
                            break;
                        }
                    }
                }

                currentVerifier++;
                if (currentVerifier < currentVerifiers.size()) {
                    VerifierBackground verifier = currentVerifiers.get(currentVerifier);
                    verifier.execute();
                } else {
                    if (batchVerified) {
                        transitionToVerifySuccess();
                    } else {
                        if (batchContinue) {
                            transitionToVerifySuccessIgnoreErrors();
                        } else {
                            transitionToVerifyFailed();
                        }
                    }

                    currentVerifier = -1;
                    currentVerifiers.clear();
                    unlockVerifyButtons();
                }
            }

            @Override
            protected void process(List<VerifierBackground.VerifierUpdates> updates) {
                if (updates.size() == 0) {
                    return;
                }

                VerifierBackground.VerifierUpdates update = updates.get(updates.size() - 1);
                if (update != null && update.getTotal() > 0) {
                    userInterface.getStatusIndicator().setText("Batch Status:\n Unverified\n Schema Name?\n " + update.getProcessed() + " / " + update.getTotal());
                }
            }
        };

        verifiers.put(ValidSchemaNameVerifierImpl.class.getName(), validSchemaVerifier);

        settings = userInterface.getTableModel().getVerifierWithName(LocalFilesExistVerifierImpl.class.getName());

        LocalFilesExistVerifierImpl localFileExistsVerifier = new LocalFilesExistVerifierImpl(settings) {
            @Override
            public List<Problem> doInBackground() {
                userInterface.getStatusIndicator().setText("Batch Status:\n Unverified\n Local File Exists?\n 0 / " + batch.getItems().size());
                batch.setAction(prettyName());
                return verify(batch, userInterface.getConsole(), userInterface.getFlagPanel());
            }

            @Override
            public void done() {
                if (isCancelled()) {
                    doCancel();
                    return;
                }

                List<Problem> problems = new ArrayList<Problem>();
                if (batchVerified == null) {
                    batchVerified = true;
                }

                try {
                    problems = get();
                } catch (InterruptedException | ExecutionException e) {
                    batchVerified = false;
                    e.printStackTrace();
                }

                batchContinue = batch.getRemoteBitstreamErrorContinue();
                for (Problem problem : problems) {
                    if (problem.isError()) {
                        if (problem.isFlagged()) {
                            batchVerified = false;
                            if (batch.hasIgnoredRows()) {
                                continue;
                            } else {
                                break;
                            }
                        } else {
                            batchContinue = false;
                            batchVerified = false;
                            break;
                        }
                    }
                }

                currentVerifier++;
                if (currentVerifier < currentVerifiers.size()) {
                    VerifierBackground verifier = currentVerifiers.get(currentVerifier);
                    verifier.execute();
                } else {
                    if (batchVerified) {
                        transitionToVerifySuccess();
                    } else {
                        if (batchContinue) {
                            transitionToVerifySuccessIgnoreErrors();
                        } else {
                            transitionToVerifyFailed();
                        }
                    }

                    currentVerifier = -1;
                    currentVerifiers.clear();
                    unlockVerifyButtons();
                }
            }

            @Override
            protected void process(List<VerifierBackground.VerifierUpdates> updates) {
                if (updates.size() == 0) {
                    return;
                }

                VerifierBackground.VerifierUpdates update = updates.get(updates.size() - 1);
                if (update != null && update.getTotal() > 0) {
                    userInterface.getStatusIndicator().setText("Batch Status:\n Unverified\n Local File Exists?\n " + update.getProcessed() + " / " + update.getTotal());
                }
            }
        };

        verifiers.put(LocalFilesExistVerifierImpl.class.getName(), localFileExistsVerifier);

        settings = userInterface.getTableModel().getVerifierWithName(RemoteFilesExistVerifierImpl.class.getName());

        RemoteFilesExistVerifierImpl remoteFileExistsVerifier = new RemoteFilesExistVerifierImpl(settings) {
            @Override
            public List<Problem> doInBackground() {
                userInterface.getStatusIndicator().setText("Batch Status:\n Unverified\n Remote File Exists?\n 0 / " + batch.getItems().size());
                batch.setAction(prettyName());
                return verify(batch, userInterface.getConsole(), userInterface.getFlagPanel());
            }

            @Override
            public void done() {
                if (isCancelled()) {
                    doCancel();
                    return;
                }

                List<Problem> problems = new ArrayList<Problem>();
                if (batchVerified == null) {
                    batchVerified = true;
                }

                try {
                    problems = get();
                } catch (InterruptedException | ExecutionException e) {
                    batchVerified = false;
                    e.printStackTrace();
                }

                batchContinue = batch.getRemoteBitstreamErrorContinue();
                for (Problem problem : problems) {
                    if (problem.isError()) {
                        if (problem.isFlagged()) {
                            batchVerified = false;
                            if (batch.hasIgnoredRows()) {
                                continue;
                            } else {
                                break;
                            }
                        } else {
                            batchContinue = false;
                            batchVerified = false;
                            break;
                        }
                    }
                }

                currentVerifier++;
                if (currentVerifier < currentVerifiers.size()) {
                    VerifierBackground verifier = currentVerifiers.get(currentVerifier);
                    verifier.execute();
                } else {
                    if (batchVerified) {
                        transitionToVerifySuccess();
                    } else {
                        if (batchContinue) {
                            transitionToVerifySuccessIgnoreErrors();
                        } else {
                            transitionToVerifyFailed();
                        }
                    }

                    currentVerifier = -1;
                    currentVerifiers.clear();
                    unlockVerifyButtons();
                }
            }

            @Override
            protected void process(List<VerifierBackground.VerifierUpdates> updates) {
                if (updates.size() == 0) {
                    return;
                }

                VerifierBackground.VerifierUpdates update = updates.get(updates.size() - 1);
                if (update != null && update.getTotal() > 0) {
                    userInterface.getStatusIndicator().setText("Batch Status:\n Unverified\n Remote File Exists?\n " + update.getProcessed() + " / " + update.getTotal());
                }
            }
        };

        verifiers.put(RemoteFilesExistVerifierImpl.class.getName(), remoteFileExistsVerifier);
    }

    public void lockThreadSensitiveControls() {
        userInterface.getLoadBatchBtn().setEnabled(false);
        userInterface.getChooseInputFileBtn().setEnabled(false);
        userInterface.getChooseSourceDirectoryBtn().setEnabled(false);
        userInterface.getChooseOutputDirectoryBtn().setEnabled(false);
        userInterface.getVerifyCancelBtn().setEnabled(true);
        userInterface.getVerifyBatchBtn().setEnabled(false);

        if (actionStatus == ActionStatus.VERIFIED) {
            userInterface.getWriteSAFBtn().setEnabled(false);
        }

        if (actionStatus != ActionStatus.NONE_LOADED) {
            userInterface.getIgnoreFilesBox().setEnabled(false);
            userInterface.getContinueOnRemoteErrorBox().setEnabled(false);
            userInterface.getAllowSelfSignedBox().setEnabled(false);
            userInterface.getItemProcessDelayField().setEnabled(false);
            userInterface.getRemoteFileTimeoutField().setEnabled(false);
            userInterface.getUserAgentField().setEnabled(false);
        }
    }

    public void lockVerifyButtons() {
        userInterface.getVerifyBatchBtn().setText("Verifying..");
        lockThreadSensitiveControls();
    }

    public void transitionToCleaned() {
        userInterface.getWriteSAFBtn().setText("No Batch Loaded");
        userInterface.getWriteSAFBtn().setEnabled(false);
        userInterface.getActionStatusField().setForeground(Color.black);
        userInterface.getActionStatusField().setBackground(Color.green);
        userInterface.getActionStatusField().setText("Batch SAF written & failures removed.");
        userInterface.getStatusIndicator().setText("Batch status:\n Written & Cleaned");
        userInterface.getStatusIndicator().setForeground(Color.black);
        userInterface.getStatusIndicator().setBackground(Color.green);
        actionStatus = ActionStatus.CLEANED;
    }

    public void transitionToCleanedFailed() {
        userInterface.getActionStatusField().setForeground(Color.black);
        userInterface.getActionStatusField().setBackground(Color.red);
        userInterface.getActionStatusField().setText("Batch SAF written & clean failed.");
        userInterface.getStatusIndicator().setText("Batch status:\n Written & Clean Failed");
        userInterface.getStatusIndicator().setForeground(Color.black);
        userInterface.getStatusIndicator().setBackground(Color.red);
    }

    public void transitionToLoaded() {
        userInterface.getActionStatusField().setText("Your batch has not been verified.");
        userInterface.getActionStatusField().setForeground(Color.black);
        userInterface.getActionStatusField().setBackground(Color.red);
        actionStatus = ActionStatus.LOADED;
        userInterface.getStatusIndicator().setText("Batch Status:\n Unverified");
        userInterface.getStatusIndicator().setForeground(Color.white);
        userInterface.getStatusIndicator().setBackground(Color.blue);
        userInterface.getLoadBatchBtn().setText("Reload batch as specified");
        userInterface.getAddLicenseCheckbox().setSelected(false);
        userInterface.getWriteSAFBtn().setText("No batch loaded");
        userInterface.getWriteSAFBtn().setEnabled(false);
        userInterface.getVerifyBatchBtn().setEnabled(true);
        userInterface.getFlagsDownloadCsvBtn().setEnabled(true);
        userInterface.getFlagsReportSelectedBtn().setEnabled(true);

        batch.setIgnoreFiles(userInterface.getIgnoreFilesBox().isSelected());
        batch.setRemoteBitstreamErrorContinue(userInterface.getContinueOnRemoteErrorBox().isSelected());
        batch.setAllowSelfSigned(userInterface.getAllowSelfSignedBox().isSelected());
        batch.setItemProcessDelay(userInterface.getItemProcessDelayField().getText());
        batch.setUserAgent(userInterface.getUserAgentField().getText());

        userInterface.getFlagPanel().clear();

        userInterface.getIgnoreFilesBox().setEnabled(true);
        userInterface.getContinueOnRemoteErrorBox().setEnabled(true);
        userInterface.getAllowSelfSignedBox().setEnabled(true);
        userInterface.getItemProcessDelayField().setEnabled(true);
        userInterface.getRemoteFileTimeoutField().setEnabled(true);
        userInterface.getUserAgentField().setEnabled(true);

        batchContinue = false;
    }

    public void transitionToVerifyFailed() {
        userInterface.getConsole().append("Batch failed verification.\n");
        actionStatus = ActionStatus.FAILED_VERIFICATION;
        userInterface.getActionStatusField().setText("Your batch failed to verify.");
        userInterface.getWriteSAFBtn().setText("No valid batch.");
        userInterface.getActionStatusField().setBackground(Color.red);
        userInterface.getStatusIndicator().setText("Batch Status:\n Unverified");
    }

    public void transitionToVerifySuccess() {
        userInterface.getConsole().append("Batch verified.\n");

        userInterface.getVerifyBatchBtn().setEnabled(false);

        actionStatus = ActionStatus.VERIFIED;
        userInterface.getActionStatusField().setText("Your batch has been verified!");
        userInterface.getActionStatusField().setBackground(Color.green);

        userInterface.getStatusIndicator().setText("Batch Status:\nVerified");
        userInterface.getStatusIndicator().setForeground(Color.black);
        userInterface.getStatusIndicator().setBackground(Color.green);

        userInterface.getWriteSAFBtn().setEnabled(true);
        userInterface.getWriteSAFBtn().setText("Write SAF data now!");
    }

    public void transitionToVerifySuccessIgnoreErrors() {
        userInterface.getConsole().append("Batch failed verification.\n");

        userInterface.getVerifyBatchBtn().setEnabled(false);

        actionStatus = ActionStatus.VERIFIED;
        userInterface.getActionStatusField().setText("Your batch failed to verify, continuing.");
        userInterface.getActionStatusField().setBackground(Color.orange);

        userInterface.getStatusIndicator().setText("Batch Status:\nUnverified,\nContinuing Anyway");
        userInterface.getStatusIndicator().setForeground(Color.black);
        userInterface.getStatusIndicator().setBackground(Color.orange);

        userInterface.getWriteSAFBtn().setEnabled(true);
        userInterface.getWriteSAFBtn().setText("Write SAF data now!");
    }

    public void transitionToWritten() {
        userInterface.getStatusIndicator().setText("Batch status:\n Written");
        userInterface.getStatusIndicator().setForeground(Color.black);
        userInterface.getStatusIndicator().setBackground(Color.white);

        userInterface.getActionStatusField().setText("Your batch is written.");
        userInterface.getActionStatusField().setBackground(Color.green);

        userInterface.getWriteSAFBtn().setText("No Batch Loaded");
        userInterface.getWriteSAFBtn().setEnabled(false);

        userInterface.getActionStatusField().setText("Batch SAF written.");

        userInterface.getLoadBatchBtn().setText("Reload batch as specified");
        actionStatus = ActionStatus.WRITTEN;
    }

    public void transitionToWrittenFailed() {
        userInterface.getStatusIndicator().setText("Batch Status:\n verified\n write failed");
        userInterface.getStatusIndicator().setForeground(Color.black);
        userInterface.getStatusIndicator().setBackground(Color.red);

        userInterface.getActionStatusField().setText("Your batch had write failures.");
        userInterface.getActionStatusField().setBackground(Color.orange);

        userInterface.getWriteSAFBtn().setText("Clean SAF Data");
        userInterface.getWriteSAFBtn().setEnabled(true);

        userInterface.getActionStatusField().setText("Batch SAF written, with failures.");

        userInterface.getLoadBatchBtn().setText("Reload batch as specified");
        actionStatus = ActionStatus.WRITTEN;
    }

    public void transitionToWrittenIgnored() {
        userInterface.getStatusIndicator().setText("Batch Status:\n written\n with ignored");
        userInterface.getStatusIndicator().setForeground(Color.black);
        userInterface.getStatusIndicator().setBackground(Color.orange);

        userInterface.getActionStatusField().setText("Your batch has ignored rows.");
        userInterface.getActionStatusField().setBackground(Color.orange);

        userInterface.getWriteSAFBtn().setText("Clean SAF Data");
        userInterface.getWriteSAFBtn().setEnabled(true);

        userInterface.getActionStatusField().setText("Batch SAF written, with ignored.");

        userInterface.getLoadBatchBtn().setText("Reload batch as specified");
        actionStatus = ActionStatus.WRITTEN;
    }

    public void unlockThreadSensitiveControls() {
        userInterface.getVerifyCancelBtn().setEnabled(false);

        if (actionStatus == ActionStatus.LOADED || actionStatus == ActionStatus.FAILED_VERIFICATION) {
            userInterface.getVerifyBatchBtn().setEnabled(true);
        }

        userInterface.getLoadBatchBtn().setEnabled(true);
        userInterface.getChooseInputFileBtn().setEnabled(true);
        userInterface.getChooseSourceDirectoryBtn().setEnabled(true);
        userInterface.getChooseOutputDirectoryBtn().setEnabled(true);

        if (actionStatus == ActionStatus.VERIFIED) {
            userInterface.getWriteSAFBtn().setEnabled(true);
        } else if (actionStatus == ActionStatus.CLEANED) {
            userInterface.getWriteSAFBtn().setEnabled(false);
        }

        if (actionStatus != ActionStatus.NONE_LOADED) {
            userInterface.getIgnoreFilesBox().setEnabled(true);
            userInterface.getContinueOnRemoteErrorBox().setEnabled(true);
            userInterface.getAllowSelfSignedBox().setEnabled(true);
            userInterface.getItemProcessDelayField().setEnabled(true);
            userInterface.getRemoteFileTimeoutField().setEnabled(true);
            userInterface.getUserAgentField().setEnabled(true);
        }
    }

    public void unlockVerifyButtons() {
        userInterface.getVerifyBatchBtn().setText("Verify Batch");
        unlockThreadSensitiveControls();
    }

    private void createBatchListeners() {
        userInterface.getChooseInputFileBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (userInterface.getInputFileChooser().showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    userInterface.getConsole().append("You have selected this input CSV file: " + userInterface.getInputFileChooser().getSelectedFile().getName() + ".\n");
                    metadataInputFileName = userInterface.getInputFileChooser().getSelectedFile().getAbsolutePath();
                    userInterface.getInputFileNameField().setText(metadataInputFileName);

                    if (!actionStatus.equals(ActionStatus.NONE_LOADED)) {
                        userInterface.getConsole().append("Resetting due to new CSV file selection.\n");

                        batch = null;

                        userInterface.getStatusIndicator().setText("No batch loaded");
                        userInterface.getStatusIndicator().setForeground(Color.white);
                        userInterface.getStatusIndicator().setBackground(Color.blue);

                        userInterface.getStatusIndicator().setText("Please load a batch for processing.");
                        userInterface.getActionStatusField().setForeground(Color.white);
                        userInterface.getActionStatusField().setBackground(Color.blue);

                        userInterface.getIgnoreFilesBox().setEnabled(false);
                        userInterface.getContinueOnRemoteErrorBox().setEnabled(false);
                        userInterface.getAllowSelfSignedBox().setEnabled(false);
                        userInterface.getItemProcessDelayField().setEnabled(false);
                        userInterface.getRemoteFileTimeoutField().setEnabled(false);
                        userInterface.getUserAgentField().setEnabled(false);

                        userInterface.getWriteSAFBtn().setEnabled(false);
                        userInterface.getWriteSAFBtn().setText("No batch loaded");

                        actionStatus = ActionStatus.NONE_LOADED;
                    }
                }
            }
        });

        userInterface.getChooseSourceDirectoryBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (userInterface.getSourceDirectoryChooser().showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    userInterface.getConsole().append("You have selected this source directory: " + userInterface.getSourceDirectoryChooser().getSelectedFile().getName() + "\n");
                    sourceDirectoryName = userInterface.getSourceDirectoryChooser().getSelectedFile().getAbsolutePath();
                    userInterface.getSourceDirectoryNameField().setText(sourceDirectoryName);
                }
            }
        });

        userInterface.getChooseOutputDirectoryBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (userInterface.getOutputDirectoryChooser().showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    userInterface.getConsole().append("You have selected this target directory: " + userInterface.getOutputDirectoryChooser().getSelectedFile().getName() + "\n");
                    outputDirectoryName = userInterface.getOutputDirectoryChooser().getSelectedFile().getAbsolutePath();
                    userInterface.getOutputDirectoryNameField().setText(outputDirectoryName);
                }
            }
        });

        userInterface.getLoadBatchBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (actionStatus.equals(ActionStatus.NONE_LOADED) || actionStatus.equals(ActionStatus.LOADED) || actionStatus.equals(ActionStatus.WRITTEN) || actionStatus.equals(ActionStatus.CLEANED) || actionStatus.equals(ActionStatus.VERIFIED) || actionStatus.equals(ActionStatus.FAILED_VERIFICATION)) {
                    // Attempt to load the batch and set status to LOADED if successful, NONE_LOADED if failing
                    userInterface.getConsole().append("\nLoading batch for " + metadataInputFileName + "...\n");
                    batch = processor.loadBatch(metadataInputFileName, sourceDirectoryName, outputDirectoryName, userInterface.getConsole());
                    if (batch == null) {
                        userInterface.getConsole().append("\nFAILED TO READ BATCH.\n\n");
                        actionStatus = ActionStatus.NONE_LOADED;

                        userInterface.getStatusIndicator().setText("Batch Status:\nFailed to Load");
                        userInterface.getStatusIndicator().setForeground(Color.black);
                        userInterface.getStatusIndicator().setBackground(Color.red);

                        userInterface.getActionStatusField().setText("Please load a batch for processing.");
                        userInterface.getActionStatusField().setForeground(Color.white);
                        userInterface.getActionStatusField().setBackground(Color.blue);

                        userInterface.getIgnoreFilesBox().setEnabled(false);
                        userInterface.getContinueOnRemoteErrorBox().setEnabled(false);
                        userInterface.getAllowSelfSignedBox().setEnabled(false);
                        userInterface.getItemProcessDelayField().setEnabled(false);
                        userInterface.getRemoteFileTimeoutField().setEnabled(false);
                        userInterface.getUserAgentField().setEnabled(false);
                    } else {
                        batch.setItemProcessDelay(userInterface.getItemProcessDelayField().getText());
                        batch.setRemoteFileTimeout(userInterface.getRemoteFileTimeoutField().getText());

                        userInterface.getConsole().append("\nBatch successfully loaded.\n\n");

                        transitionToLoaded();
                    }
                }
            }
        });

        userInterface.getWriteSAFBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {

                if (actionStatus.equals(ActionStatus.VERIFIED)) {
                    lockThreadSensitiveControls();

                    // Action Status Field may have "cancelled" text, so reset it before writing begins.
                    userInterface.getActionStatusField().setText("Your batch has been verified!");
                    userInterface.getActionStatusField().setBackground(Color.green);

                    userInterface.getWriteSAFBtn().setText("Writing..");
                    userInterface.getWriteCancelBtn().setEnabled(true);

                    userInterface.getConsole().append("Parsing CSV file and writing output to DC XML files...\n");
                    userInterface.getStatusIndicator().setText("Batch Status:\n Verified\n Written:\n 0 / " + batch.getItems().size());

                    currentWriter = new ImportDataWriter() {
                        @Override
                        public void done() {
                            currentWriter = null;
                            userInterface.getWriteCancelBtn().setEnabled(false);
                            unlockThreadSensitiveControls();

                            if (isCancelled()) {
                                cancelWriteCleanup();
                                return;
                            }

                            try {
                                if (get()) {
                                    if (batch.hasIgnoredRows()) {
                                        transitionToWrittenIgnored();
                                    } else {
                                        transitionToWritten();
                                    }
                                } else {
                                    transitionToWrittenFailed();
                                }
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        protected void process(List<ImportDataWriter.Updates> updates) {
                            if (updates.size() == 0) {
                                return;
                            }

                            ImportDataWriter.Updates update = updates.get(updates.size() - 1);
                            if (update != null && update.getTotal() > 0) {
                                userInterface.getStatusIndicator().setText("Batch Status:\n Verified\n Written:\n " + update.getProcessed() + " / " + update.getTotal());
                            }
                        }
                    };

                    batch.setAction("Writing");
                    currentWriter.setBatch(batch);
                    currentWriter.setConsole(userInterface.getConsole());
                    currentWriter.setFlags(userInterface.getFlagPanel());
                    currentWriter.execute();
                } else if (actionStatus.equals(ActionStatus.LOADED) || actionStatus.equals(ActionStatus.FAILED_VERIFICATION)) {
                    userInterface.getConsole().append("\nPlease verify the batch before writing.\n\n");
                } else if (actionStatus.equals(ActionStatus.NONE_LOADED)) {
                    userInterface.getConsole().append("\nNo loaded SAF data to write.\n\n");
                } else if (actionStatus.equals(ActionStatus.WRITTEN)) {
                    lockThreadSensitiveControls();
                    userInterface.getWriteSAFBtn().setText("Cleaning..");
                    userInterface.getWriteCancelBtn().setEnabled(true);

                    userInterface.getConsole().append("Deleting all folders with flagged errors...\n");
                    userInterface.getStatusIndicator().setText("Batch Status:\n Written\n Cleaned:\n 0 / " + batch.getItems().size());

                    currentCleaner = new ImportDataCleaner() {
                        @Override
                        public void done() {
                            currentCleaner = null;
                            userInterface.getWriteCancelBtn().setEnabled(false);
                            unlockThreadSensitiveControls();

                            if (isCancelled()) {
                                return;
                            }

                            try {
                                if (get()) {
                                    transitionToCleaned();
                                } else {
                                    transitionToCleanedFailed();
                                }
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        protected void process(List<ImportDataCleaner.Updates> updates) {
                            if (updates.size() == 0) {
                                return;
                            }

                            ImportDataCleaner.Updates update = updates.get(updates.size() - 1);
                            if (update != null && update.getTotal() > 0) {
                                userInterface.getStatusIndicator().setText("Batch Status:\n Written\n Cleaned:\n " + update.getProcessed() + " / " + update.getTotal());
                            }
                        }
                    };

                    batch.setAction("Cleaning");
                    currentCleaner.setBatch(batch);
                    currentCleaner.setConsole(userInterface.getConsole());
                    currentCleaner.setFlags(userInterface.getFlagPanel());
                    currentCleaner.execute();
                } else if (actionStatus.equals(ActionStatus.CLEANED)) {
                    userInterface.getConsole().append("\nSAF data is already written.\n\n");
                }
            }
        });

        userInterface.getWriteCancelBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentWriter != null) {
                    currentWriter.cancel(true);
                }

                if (currentCleaner != null) {
                    currentCleaner.cancel(true);
                }

                userInterface.getConsole().append("Cancelling write..\n");
            }
        });
    }

    private void createFlagListeners() {
        userInterface.getFlagsDownloadCsvBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (actionStatus.equals(ActionStatus.NONE_LOADED)) {
                    userInterface.getConsole().append("Unable to write to write CSV file, reason: SAF not loaded.\n");
                    return;
                }

                if (csvOutputFileName.isEmpty() || outputDirectoryName.isEmpty()) {
                    userInterface.getConsole().append("Unable to write to write CSV file, reason: SAF directory not defined.\n");
                    return;
                }

                if (userInterface.getFlagPanel().getRowCount() == 0) {
                    userInterface.getConsole().append("Unable to write to write CSV file, reason: No flags to output.\n");
                    return;
                }

                String csvFilePath = outputDirectoryName + File.separator + csvOutputFileName;
                try {
                    BufferedWriter writer = Files.newBufferedWriter(Paths.get(csvFilePath));
                    userInterface.getFlagPanel().exportToCSV(writer);
                    userInterface.getConsole().append("Wrote to CSV File: " + csvFilePath + ".\n");
                } catch (IOException e1) {
                    userInterface.getConsole().append("Unable to write CSV file " + csvFilePath + ", reason: " + e1.getMessage() + ".\n");
                }
            }
        });

        userInterface.getFlagsReportSelectedBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (userInterface.getFlagPanel().getRowCount() == 0) {
                    userInterface.getConsole().append("Flag list is empty.\n");
                    return;
                }

                Flag row = userInterface.getFlagPanel().getSelected();
                if (row == null) {
                    userInterface.getConsole().append("No valid row is selected.\n");
                    return;
                }

                Problem problem = new Problem(true, row.getCell(FlagColumns.DESCRIPTION));
                userInterface.getConsole().append("\nFlag Name: " + row.getCell(FlagColumns.FLAG) + "\n");
                userInterface.getConsole().append("Flag Description: " + problem.toString() + "\n");

                if (!row.getCell(FlagColumns.AUTHORITY).isEmpty()) {
                    userInterface.getConsole().append("Flag Authority: " + row.getCell(FlagColumns.AUTHORITY) + "\n");
                }

                if (!row.getCell(FlagColumns.URL).isEmpty()) {
                    userInterface.getConsole().append("Flag URL: " + row.getCell(FlagColumns.URL) + "\n");
                }

                if (row.getCell(FlagColumns.COLUMN).isEmpty()) {
                    if (!row.getCell(FlagColumns.ROW).isEmpty()) {
                        userInterface.getConsole().append("Flag Row: " + row.getCell(FlagColumns.ROW) + "\n");
                    }
                } else if (row.getCell(FlagColumns.ROW).isEmpty()) {
                    userInterface.getConsole().append("Flag Column: " + row.getCell(FlagColumns.COLUMN) + "\n");
                } else {
                    userInterface.getConsole().append("Flag Column, Row: " + row.getCell(FlagColumns.COLUMN) + ", " + row.getCell(FlagColumns.ROW) + "\n");
                }

                if (!row.getCell(FlagColumns.ACTION).isEmpty()) {
                    userInterface.getConsole().append("Flag Action: " + row.getCell(FlagColumns.ACTION) + "\n");
                }
            }
        });
    }

    private void createLicenseListeners() {
        userInterface.getAddLicenseCheckbox().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!actionStatus.equals(ActionStatus.NONE_LOADED) && !actionStatus.equals(ActionStatus.WRITTEN) && !actionStatus.equals(ActionStatus.CLEANED)) {
                    if (!userInterface.getAddLicenseCheckbox().isSelected()) {
                        // user clicked to disable the license
                        userInterface.getConsole().append("License disabled.\n");
                        userInterface.getLicenseTextField().setEditable(true);
                        batch.unsetLicense();
                    } else if (userInterface.getAddLicenseCheckbox().isSelected()) {
                        // user clicked to enable the license
                        userInterface.getConsole().append("License enabled.\n");
                        userInterface.getLicenseTextField().setEditable(false);
                        batch.setLicense(userInterface.getLicenseFilenameField().getText(), userInterface.getLicenseBundleNameField().getText(), userInterface.getLicenseTextField().getText());
                    }
                } else {
                    userInterface.getAddLicenseCheckbox().setSelected(false);
                    userInterface.getConsole().append("No active batch on which to change a license.\n");
                }
            }
        });

        userInterface.getLicenseTextField().addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!userInterface.getLicenseTextField().isEditable()) {
                    userInterface.getConsole().append("License affixed to items - disable license to edit the license text.\n");
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }
        });

        userInterface.getRestrictToGroupCheckbox().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!actionStatus.equals(ActionStatus.NONE_LOADED) && !actionStatus.equals(ActionStatus.WRITTEN) && !actionStatus.equals(ActionStatus.CLEANED)) {
                    if (!userInterface.getRestrictToGroupCheckbox().isSelected()) {
                        // user clicked to disable the restriction
                        userInterface.getConsole().append("Group restriction disabled.\n");
                        userInterface.getRestrictToGroupField().setEditable(true);
                        batch.restrictItemsToGroup(null);

                    } else if (userInterface.getRestrictToGroupCheckbox().isSelected()) {
                        // user clicked to enable the restriction
                        userInterface.getConsole().append("Group restriction to \"" + userInterface.getRestrictToGroupField().getText() + "\" enabled.\n");
                        userInterface.getRestrictToGroupField().setEditable(false);
                        batch.restrictItemsToGroup(userInterface.getRestrictToGroupField().getText());
                    }
                } else {
                    userInterface.getRestrictToGroupCheckbox().setSelected(false);
                    userInterface.getConsole().append("No active batch on which to apply group restrictions.\n");
                }
            }
        });

        userInterface.getRestrictToGroupField().addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!userInterface.getRestrictToGroupField().isEditable()) {
                    userInterface.getConsole().append("Restriction to group \"" + userInterface.getRestrictToGroupField().getText() + "\" affixed to items - disable restriction to edit the group name.\n");
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }
        });
    }

    private void createSettingsListeners() {
        userInterface.getIgnoreFilesBox().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (!userInterface.getIgnoreFilesBox().isEnabled()) {
                    // do nothing when field is disabled.
                    return;
                }

                if (userInterface.getIgnoreFilesBox().isSelected()) {
                    userInterface.getConsole().append("Bitstream (content) files will be ommitted from generated SAF.\n");
                    batch.setIgnoreFiles(true);
                } else {
                    userInterface.getConsole().append("Bitstream (content) files will be included in generated SAF.\n");
                    batch.setIgnoreFiles(false);
                }
            }
        });

        userInterface.getContinueOnRemoteErrorBox().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!userInterface.getContinueOnRemoteErrorBox().isEnabled()) {
                    // do nothing when field is disabled.
                    return;
                }

                if (userInterface.getContinueOnRemoteErrorBox().isSelected()) {
                    userInterface.getConsole().append("Allowing write even if the remote bitstream verification flags an error.\n");
                    batch.setRemoteBitstreamErrorContinue(true);
                } else {
                    userInterface.getConsole().append("Denying write if the remote bitstream verification flags an error.\n");
                    batch.setRemoteBitstreamErrorContinue(false);
                }
            }
        });

        userInterface.getAllowSelfSignedBox().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!userInterface.getIgnoreFilesBox().isEnabled()) {
                    // do nothing when field is disabled.
                    return;
                }

                if (userInterface.getAllowSelfSignedBox().isSelected()) {
                    userInterface.getConsole().append("Self-Signed SSL Certificates will always be allowed when generating SAF.\n");
                    batch.setAllowSelfSigned(true);
                }
                else {
                    userInterface.getConsole().append("Self-Signed SSL Certificates will not always be allowed when generating SAF.\n");
                    batch.setAllowSelfSigned(false);
                }
            }
        });

        userInterface.getItemProcessDelayField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                if (batch == null) {
                    return;
                }

                itemProcessDelayFieldChangeStatus = FieldChangeStatus.CHANGES;
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                insertUpdate(e);
            }
        });

        userInterface.getItemProcessDelayField().addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent event) {
            }

            @Override
            public void focusLost(FocusEvent event) {
                if (itemProcessDelayFieldChangeStatus == FieldChangeStatus.CHANGES) {
                    String text = userInterface.getItemProcessDelayField().getText();
                    int delay = batch.getItemProcessDelay();

                    // enforce a default value of 0 when text box is empty.
                    if (text.length() == 0) {
                        text = "0";
                        userInterface.getItemProcessDelayField().setText(text);
                    }

                    try {
                        delay = Integer.parseInt(text);
                    } catch (NumberFormatException e) {
                        userInterface.getConsole().append("The specified Item Process Delay is invalid, resetting.\n");
                        userInterface.getItemProcessDelayField().setText(String.valueOf(batch.getItemProcessDelay()));
                    }

                    if (delay < 0) {
                        userInterface.getConsole().append("The Item Process Delay may not be a negative integer, resetting.\n");
                        userInterface.getItemProcessDelayField().setText(String.valueOf(batch.getItemProcessDelay()));
                    } else {
                        batch.setItemProcessDelay(delay);
                        userInterface.getConsole().append("The Item Process Delay is now set to " + delay + " milliseconds.\n");
                    }

                    itemProcessDelayFieldChangeStatus = FieldChangeStatus.NO_CHANGES;
                }
            }
        });

        userInterface.getRemoteFileTimeoutField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                if (batch == null) {
                    return;
                }

                remoteFileTimeoutFieldChangeStatus = FieldChangeStatus.CHANGES;
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                insertUpdate(e);
            }
        });

        userInterface.getRemoteFileTimeoutField().addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent event) {
            }

            @Override
            public void focusLost(FocusEvent event) {
                if (remoteFileTimeoutFieldChangeStatus == FieldChangeStatus.CHANGES) {
                    String text = userInterface.getRemoteFileTimeoutField().getText();
                    int timeout = batch.getRemoteFileTimeout();

                    // enforce a default value of 0 when text box is empty.
                    if (text.length() == 0) {
                        text = "0";
                        userInterface.getRemoteFileTimeoutField().setText(text);
                    }

                    try {
                        timeout = Integer.parseInt(text);
                    } catch (NumberFormatException e) {
                        userInterface.getConsole().append("The specified Remote File Timeout is invalid, resetting.\n");
                        userInterface.getRemoteFileTimeoutField().setText(String.valueOf(batch.getRemoteFileTimeout()));
                    }

                    if (timeout < 0) {
                        userInterface.getConsole().append("The Remote File Timeout may not be a negative integer, resetting.\n");
                        userInterface.getRemoteFileTimeoutField().setText(String.valueOf(batch.getRemoteFileTimeout()));
                    } else {
                        batch.setRemoteFileTimeout(timeout);
                        userInterface.getConsole().append("The Remote File Timeout is now set to " + timeout + " milliseconds.\n");
                    }

                    remoteFileTimeoutFieldChangeStatus = FieldChangeStatus.NO_CHANGES;
                }
            }
        });

        userInterface.getUserAgentField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                if (batch == null) {
                    return;
                }

                userAgentFieldChangeStatus = FieldChangeStatus.CHANGES;
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                insertUpdate(e);
            }
        });

        userInterface.getUserAgentField().addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent event) {
            }

            @Override
            public void focusLost(FocusEvent event) {
                if (userAgentFieldChangeStatus == FieldChangeStatus.CHANGES) {
                    userInterface.getConsole().append("The user agent has been set to '" + userInterface.getUserAgentField().getText() + "'.\n");
                    itemProcessDelayFieldChangeStatus = FieldChangeStatus.NO_CHANGES;
                    batch.setUserAgent(userInterface.getUserAgentField().getText());
                }
            }
        });
    }

    private void createVerificationListeners() {

        userInterface.getVerifierTbl().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                switch (actionStatus) {
                case NONE_LOADED:
                case LOADED:
                case VERIFIED:
                case FAILED_VERIFICATION:
                    break;
                default:
                    return;
                }

                int row = userInterface.getVerifierTbl().rowAtPoint(evt.getPoint());
                int column = userInterface.getVerifierTbl().columnAtPoint(evt.getPoint());
                if (column == 2 && row >= 0 && row < userInterface.getVerifierTbl().getColumnModel().getColumnCount()) {
                    VerifierProperty verifier = userInterface.getTableModel().getVerifierPropertyAt(row);
                    if (verifier != null) {
                        userInterface.getVerifierTbl().setValueAt(verifier.getActivated() ? VerifierTableModel.VERIFIER_INACTIVE : VerifierTableModel.VERIFIER_ACTIVE, row, column);
                        userInterface.getConsole().append(verifier.prettyName() + " is now " + (verifier.getActivated() ? "Active" : "Inactive") + ".\n");

                        unlockVerifyButtons();

                        // the verification process must be when status is LOADED.
                        if (actionStatus != ActionStatus.NONE_LOADED && actionStatus != ActionStatus.LOADED) {
                            transitionToLoaded();
                        }
                    }
                }
            }
        });

        userInterface.getVerifyBatchBtn().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (actionStatus == ActionStatus.LOADED || actionStatus == ActionStatus.FAILED_VERIFICATION) {
                    lockVerifyButtons();
                    createVerifiers();
                    userInterface.getFlagPanel().clear();
                    batch.clearFailedRows();
                    batch.clearIgnoredRows();
                    batchVerified = null;

                    for (Entry<String, VerifierBackground> entry : verifiers.entrySet()) {
                        VerifierBackground verifier = entry.getValue();
                        if (verifier.getActivated() && verifier.isSwingWorker()) {
                            currentVerifiers.add(verifier);
                        }
                    }

                    if (currentVerifiers.size() == 0) {
                        userInterface.getConsole().append("No verifiers are enabled.\n");
                        unlockVerifyButtons();
                        transitionToVerifySuccess();
                    } else {
                        currentVerifier = 0;
                        currentVerifiers.get(currentVerifier).execute();
                    }
                } else {
                    userInterface.getConsole().append("Nothing new to verify.\n");
                }
            }
        });

        userInterface.getVerifyCancelBtn().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                userInterface.getConsole().append("Cancelling " + currentVerifiers.get(currentVerifier).prettyName() + "..\n");

                currentVerifier = currentVerifiers.size();
                while (currentVerifiers.size() > 0) {
                    VerifierBackground verifier = currentVerifiers.get(currentVerifiers.size() - 1);
                    currentVerifiers.remove(verifier);
                    verifier.cancel(true);
                    verifier.doCancel();
                }
                cancelVerifyCleanup();
            }
        });
    }
}
