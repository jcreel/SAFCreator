package edu.tamu.di.SAFCreator;

import java.awt.Color;
import java.awt.Dimension;
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
import java.util.Vector;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import edu.tamu.di.SAFCreator.enums.ActionStatus;
import edu.tamu.di.SAFCreator.enums.FieldChangeStatus;
import edu.tamu.di.SAFCreator.enums.FlagColumns;
import edu.tamu.di.SAFCreator.model.Batch;
import edu.tamu.di.SAFCreator.model.Flag;
import edu.tamu.di.SAFCreator.model.FlagPanel;
import edu.tamu.di.SAFCreator.model.Verifier;
import edu.tamu.di.SAFCreator.model.Verifier.Problem;
import edu.tamu.di.SAFCreator.model.VerifierBackground;
import edu.tamu.di.SAFCreator.model.VerifierProperty;
import edu.tamu.di.SAFCreator.verify.LocalFilesExistVerifierImpl;
import edu.tamu.di.SAFCreator.verify.RemoteFilesExistVerifierImpl;
import edu.tamu.di.SAFCreator.verify.ValidSchemaNameVerifierImpl;

public class ImporterGUI extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String defaultUserAgent = "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:57.0) Gecko/20100101 Firefox/57.0";

    private static final int defaultProcessDelay = 400;
    private static final int defaultRemoteFileTimeout = 10000;
    private static final String csvOutputFileName = "SAF-Flags.csv";
    private static String sourceDirectoryName;

    private static String outputDirectoryName;
    private static Boolean batchVerified = null;
    private Batch batch;
    private Map<String, VerifierBackground> verifiers = new HashMap<String, VerifierBackground>();

    private ActionStatus actionStatus = ActionStatus.NONE_LOADED;
    private FieldChangeStatus itemProcessDelayFieldChangeStatus = FieldChangeStatus.NO_CHANGES;
    private FieldChangeStatus remoteFileTimeoutFieldChangeStatus = FieldChangeStatus.NO_CHANGES;
    private FieldChangeStatus userAgentFieldChangeStatus = FieldChangeStatus.NO_CHANGES;

    // tabbed views
    private final JTabbedPane tabs = new JTabbedPane();
    private final JPanel mainTab = new JPanel();

    private final JPanel licenseTab = new JPanel();
    private final JPanel validationTab = new JPanel();
    private final JPanel advancedSettingsTab = new JPanel();
    private final JPanel flagTableTab = new JPanel();

    // Components of the Batch Detail tab
    private final JButton chooseInputFileBtn = new JButton("Select metadata CSV file");
    private final JButton chooseSourceDirectoryBtn = new JButton("Select source files directory");
    private final JButton chooseOutputDirectoryBtn = new JButton("Select SAF output directory");
    private final JFileChooser inputFileChooser = new JFileChooser(".");
    private final JFileChooser sourceDirectoryChooser = new JFileChooser(".");
    private final JFileChooser outputDirectoryChooser = new JFileChooser(".");
    private final JTextField inputFileNameField = new JTextField("", 42);
    private final JTextField sourceDirectoryNameField = new JTextField("", 40);
    private final JTextField outputDirectoryNameField = new JTextField("", 40);

    private final JTextField actionStatusField = new JTextField("Please load a batch for processing.");
    private final JButton loadBatchBtn = new JButton("Load specified batch now!");
    private final JButton writeSAFBtn = new JButton("No batch loaded");
    private final JButton writeCancelBtn = new JButton("Cancel");

    // Components of the License tab
    private final JPanel addLicenseFilePanel = new JPanel();
    private final JCheckBox addLicenseCheckbox = new JCheckBox("Add a license:");
    private final JLabel licenseFilenameFieldLabel = new JLabel("License bitstream filename:");
    private final JTextField licenseFilenameField = new JTextField("license.txt", 39);
    private final JLabel licenseBundleNameFieldLabel = new JLabel("License bundle name:");
    private final JTextField licenseBundleNameField = new JTextField("LICENSE", 42);

    // private final JLabel licenseTextFieldLabel = new JLabel("License text:");
    private final JTextArea licenseTextField = new JTextArea("This item is hereby licensed.", 6, 0);
    private final JScrollPane licenseTextScrollPane = new JScrollPane(licenseTextField);
    private final JCheckBox restrictToGroupCheckbox = new JCheckBox("Restrict read access to a group - Group name:");
    private final JTextField restrictToGroupField = new JTextField("member", 36);

    // Components of the Verify Batch tab
    private final JButton verifyBatchBtn = new JButton("Verify Batch");
    private final JButton verifyCancelBtn = new JButton("Cancel");
    private JTable verifierTbl = null;
    private List<String> verifierNamesMap = new ArrayList<String>();

    // Components of the Advanced Settings tab
    private final JCheckBox ignoreFilesBox = new JCheckBox("Omit bitstreams (content files) from generated SAF:");
    private final JLabel itemProcessDelayLabel = new JLabel("Item Processing Delay (in milliseconds):");
    private final JTextField itemProcessDelayField = new JTextField(String.valueOf(defaultProcessDelay), 10);
    private final JLabel remoteFileTimeoutLabel = new JLabel("Remote File Timeout (in milliseconds):");

    private final JTextField remoteFileTimeoutField = new JTextField(String.valueOf(defaultRemoteFileTimeout), 10);
    private final JLabel userAgentLabel = new JLabel("User agent:");
    private final JTextField userAgentField = new JTextField(defaultUserAgent, 48);
    private final JCheckBox continueOnRemoteErrorBox = new JCheckBox("Allow writing even if remote bitstream verification flags an error:");

    // Components of the Flag List tab
    private final FlagPanel flagPanel = new FlagPanel();
    private final JButton flagsDownloadCsvBtn = new JButton("Generate CSV");
    private final JButton flagsReportSelectedBtn = new JButton("Display Selected Row");

    // Components shown under any tab
    private final JPanel statusPanel = new JPanel();

    private final JTextArea statusIndicator = new JTextArea("No batch loaded", 2, 10);
    private final JTextArea console = new JTextArea(20, 50);
    private final JScrollPane scrollPane;

    private String metadataInputFileName;

    private final ImportDataProcessor processor;

    // swing background process handling
    private ImportDataWriter currentWriter = null;
    private ImportDataCleaner currentCleaner = null;
    private List<VerifierBackground> currentVerifiers = new ArrayList<VerifierBackground>();
    private Map<String, VerifierProperty> verifierSettings = new HashMap<String, VerifierProperty>();
    private int currentVerifier = -1;
    private boolean batchContinue = false;

    public ImporterGUI(final ImportDataProcessor processor) {
        this.processor = processor;

        createVerifierSettings();

        createBatchDetailsTab();

        createLicenseTab();

        createVerificationTab();

        createAdvancedSettingsTab();

        createFlagTableTab();

        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        // JMenuBar bar = new JMenuBar();
        // bar.add(new JMenu("menu"));
        // this.setJMenuBar(bar);

        // add the tabbed views
        getContentPane().add(tabs);

        // add the status info area present under all tabs
        console.setEditable(false);
        console.setLineWrap(true);
        scrollPane = new JScrollPane(console);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        statusIndicator.setBackground(Color.blue);
        statusIndicator.setForeground(Color.white);
        statusIndicator.setEditable(false);
        statusIndicator.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        scrollPane.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

        statusPanel.add(statusIndicator);
        statusPanel.add(scrollPane);
        getContentPane().add(statusPanel);

        initializeGUIState();

        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("DSpace Simple Archive Format Creator");

    }

    private void cancelVerifyCleanup() {
        statusIndicator.setText("Batch Status:\n Unverified");

        unlockVerifyButtons();
    }

    private void cancelWriteCleanup() {
        writeSAFBtn.setText("Write SAF data now!");

        statusIndicator.setText("Batch Status:\n Unverified");
        statusIndicator.setForeground(Color.white);
        statusIndicator.setBackground(Color.blue);

        actionStatusField.setText("Batch write cancelled.");
        actionStatusField.setForeground(Color.black);
        actionStatusField.setBackground(Color.orange);
    }

    private void createAdvancedSettingsTab() {
        advancedSettingsTab.setLayout(new BoxLayout(advancedSettingsTab, BoxLayout.Y_AXIS));
        advancedSettingsTab.add(ignoreFilesBox);
        advancedSettingsTab.add(continueOnRemoteErrorBox);

        tabs.addTab("Advanced Settings", advancedSettingsTab);

        ignoreFilesBox.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!ignoreFilesBox.isEnabled()) {
                    // checkbox mouse clicks still trigger even when set to disabled.
                    return;
                }

                if (ignoreFilesBox.isSelected()) {
                    console.append("Bitstream (content) files will be ommitted from generated SAF.\n");
                    batch.setIgnoreFiles(true);
                } else {
                    console.append("Bitstream (content) files will be included in generated SAF.\n");
                    batch.setIgnoreFiles(false);
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

        continueOnRemoteErrorBox.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!continueOnRemoteErrorBox.isEnabled()) {
                    // checkbox mouse clicks still trigger even when set to disabled.
                    return;
                }

                if (continueOnRemoteErrorBox.isSelected()) {
                    console.append("Allowing write even if the remote bitstream verification flags an error.\n");
                    batch.setRemoteBitstreamErrorContinue(true);
                } else {
                    console.append("Denying write if the remote bitstream verification flags an error.\n");
                    batch.setRemoteBitstreamErrorContinue(false);
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

        JPanel itemProcessDelayPanel = new JPanel();
        itemProcessDelayPanel.add(itemProcessDelayLabel);
        itemProcessDelayPanel.add(itemProcessDelayField);
        advancedSettingsTab.add(itemProcessDelayPanel);

        JPanel remoteFileTimeoutPanel = new JPanel();
        remoteFileTimeoutPanel.add(remoteFileTimeoutLabel);
        remoteFileTimeoutPanel.add(remoteFileTimeoutField);
        advancedSettingsTab.add(remoteFileTimeoutPanel);

        JPanel userAgentPanel = new JPanel();
        userAgentPanel.add(userAgentLabel);
        userAgentPanel.add(userAgentField);
        advancedSettingsTab.add(userAgentPanel);

        // initialize as disabled so that it will only be enable once a batch is assigned.
        ignoreFilesBox.setEnabled(false);
        continueOnRemoteErrorBox.setEnabled(false);
        itemProcessDelayField.setEnabled(false);
        itemProcessDelayField.getDocument().addDocumentListener(new DocumentListener() {
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

        itemProcessDelayField.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent event) {
            }

            @Override
            public void focusLost(FocusEvent event) {
                if (itemProcessDelayFieldChangeStatus == FieldChangeStatus.CHANGES) {
                    String text = itemProcessDelayField.getText();
                    int delay = batch.getItemProcessDelay();

                    // enforce a default value of 0 when text box is empty.
                    if (text.length() == 0) {
                        text = "0";
                        itemProcessDelayField.setText(text);
                    }

                    try {
                        delay = Integer.parseInt(text);
                    } catch (NumberFormatException e) {
                        console.append("The specified Item Process Delay is invalid, resetting.\n");
                        itemProcessDelayField.setText(String.valueOf(batch.getItemProcessDelay()));
                    }

                    if (delay < 0) {
                        console.append("The Item Process Delay may not be a negative integer, resetting.\n");
                        itemProcessDelayField.setText(String.valueOf(batch.getItemProcessDelay()));
                    } else {
                        batch.setItemProcessDelay(delay);
                        console.append("The Item Process Delay is now set to " + delay + " milliseconds.\n");
                    }

                    itemProcessDelayFieldChangeStatus = FieldChangeStatus.NO_CHANGES;
                }
            }
        });

        remoteFileTimeoutField.setEnabled(false);
        remoteFileTimeoutField.getDocument().addDocumentListener(new DocumentListener() {
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

        remoteFileTimeoutField.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent event) {
            }

            @Override
            public void focusLost(FocusEvent event) {
                if (remoteFileTimeoutFieldChangeStatus == FieldChangeStatus.CHANGES) {
                    String text = remoteFileTimeoutField.getText();
                    int timeout = batch.getRemoteFileTimeout();

                    // enforce a default value of 0 when text box is empty.
                    if (text.length() == 0) {
                        text = "0";
                        remoteFileTimeoutField.setText(text);
                    }

                    try {
                        timeout = Integer.parseInt(text);
                    } catch (NumberFormatException e) {
                        console.append("The specified Remote File Timeout is invalid, resetting.\n");
                        remoteFileTimeoutField.setText(String.valueOf(batch.getRemoteFileTimeout()));
                    }

                    if (timeout < 0) {
                        console.append("The Remote File Timeout may not be a negative integer, resetting.\n");
                        remoteFileTimeoutField.setText(String.valueOf(batch.getRemoteFileTimeout()));
                    } else {
                        batch.setRemoteFileTimeout(timeout);
                        console.append("The Remote File Timeout is now set to " + timeout + " milliseconds.\n");
                    }

                    remoteFileTimeoutFieldChangeStatus = FieldChangeStatus.NO_CHANGES;
                }
            }
        });

        userAgentField.setEnabled(false);
        userAgentField.getDocument().addDocumentListener(new DocumentListener() {
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

        userAgentField.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent event) {
            }

            @Override
            public void focusLost(FocusEvent event) {
                if (userAgentFieldChangeStatus == FieldChangeStatus.CHANGES) {
                    console.append("The user agent has been set to '" + userAgentField.getText() + "'.\n");
                    itemProcessDelayFieldChangeStatus = FieldChangeStatus.NO_CHANGES;
                    batch.setUserAgent(userAgentField.getText());
                }
            }
        });
    }

    private void createBatchDetailsTab() {
        sourceDirectoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        outputDirectoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        metadataInputFileName = "";
        sourceDirectoryName = "";
        outputDirectoryName = "";
        // metadataInputFileName = "/Users/jcreel/Development/SAF/testo.csv";
        // sourceDirectoryName = "/Users/jcreel/Development/SAF/testfiles";
        // outputDirectoryName = "/Users/jcreel/Development/SAF/test-output";
        //

        inputFileNameField.setText(metadataInputFileName);
        inputFileNameField.setEditable(false);
        sourceDirectoryNameField.setText(sourceDirectoryName);
        sourceDirectoryNameField.setEditable(false);
        outputDirectoryNameField.setText(outputDirectoryName);
        outputDirectoryNameField.setEditable(false);

        mainTab.setLayout(new BoxLayout(mainTab, BoxLayout.Y_AXIS));

        JPanel inputCSVPanel = new JPanel();
        inputCSVPanel.add(chooseInputFileBtn);
        inputCSVPanel.add(inputFileNameField);
        mainTab.add(inputCSVPanel);

        JPanel sourceFilesDirPanel = new JPanel();
        sourceFilesDirPanel.add(chooseSourceDirectoryBtn);
        sourceFilesDirPanel.add(sourceDirectoryNameField);
        mainTab.add(sourceFilesDirPanel);

        JPanel outputSAFDirPanel = new JPanel();
        outputSAFDirPanel.add(chooseOutputDirectoryBtn);
        outputSAFDirPanel.add(outputDirectoryNameField);
        mainTab.add(outputSAFDirPanel);

        JPanel writeButtonPanel = new JPanel();
        actionStatusField.setEditable(false);

        writeCancelBtn.setEnabled(false);
        writeSAFBtn.setEnabled(false);
        writeButtonPanel.add(loadBatchBtn);
        writeButtonPanel.add(actionStatusField);
        writeButtonPanel.add(writeSAFBtn);
        writeButtonPanel.add(writeCancelBtn);
        mainTab.add(writeButtonPanel);

        tabs.addTab("Batch Details", mainTab);

        chooseInputFileBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (inputFileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    console.append("You have selected this input CSV file: " + inputFileChooser.getSelectedFile().getName() + ".\n");
                    metadataInputFileName = inputFileChooser.getSelectedFile().getAbsolutePath();
                    inputFileNameField.setText(metadataInputFileName);

                    if (!actionStatus.equals(ActionStatus.NONE_LOADED)) {
                        console.append("Resetting due to new CSV file selection.\n");

                        batch = null;

                        statusIndicator.setText("No batch loaded");
                        statusIndicator.setForeground(Color.white);
                        statusIndicator.setBackground(Color.blue);

                        statusIndicator.setText("Please load a batch for processing.");
                        actionStatusField.setForeground(Color.white);
                        actionStatusField.setBackground(Color.blue);

                        ignoreFilesBox.setEnabled(false);
                        continueOnRemoteErrorBox.setEnabled(false);
                        itemProcessDelayField.setEnabled(false);
                        remoteFileTimeoutField.setEnabled(false);
                        userAgentField.setEnabled(false);

                        writeSAFBtn.setEnabled(false);
                        writeSAFBtn.setText("No batch loaded");

                        actionStatus = ActionStatus.NONE_LOADED;
                    }
                }
            }
        });

        chooseSourceDirectoryBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (sourceDirectoryChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    console.append("You have selected this source directory: " + sourceDirectoryChooser.getSelectedFile().getName() + "\n");
                    sourceDirectoryName = sourceDirectoryChooser.getSelectedFile().getAbsolutePath();
                    sourceDirectoryNameField.setText(sourceDirectoryName);
                }
            }
        });

        chooseOutputDirectoryBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (outputDirectoryChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    console.append("You have selected this target directory: " + outputDirectoryChooser.getSelectedFile().getName() + "\n");
                    outputDirectoryName = outputDirectoryChooser.getSelectedFile().getAbsolutePath();
                    outputDirectoryNameField.setText(outputDirectoryName);
                }
            }
        });

        loadBatchBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (actionStatus.equals(ActionStatus.NONE_LOADED) || actionStatus.equals(ActionStatus.LOADED)
                        || actionStatus.equals(ActionStatus.WRITTEN) || actionStatus.equals(ActionStatus.CLEANED)
                        || actionStatus.equals(ActionStatus.VERIFIED)
                        || actionStatus.equals(ActionStatus.FAILED_VERIFICATION)) {
                    // Attempt to load the batch and set status to LOADED if successful, NONE_LOADED if failing
                    console.append("\nLoading batch for " + metadataInputFileName + "...\n");
                    batch = processor.loadBatch(metadataInputFileName, sourceDirectoryName, outputDirectoryName,
                            console);
                    if (batch == null) {
                        console.append("\nFAILED TO READ BATCH.\n\n");
                        actionStatus = ActionStatus.NONE_LOADED;

                        statusIndicator.setText("Batch Status:\nFailed to Load");
                        statusIndicator.setForeground(Color.black);
                        statusIndicator.setBackground(Color.red);

                        actionStatusField.setText("Please load a batch for processing.");
                        actionStatusField.setForeground(Color.white);
                        actionStatusField.setBackground(Color.blue);

                        ignoreFilesBox.setEnabled(false);
                        continueOnRemoteErrorBox.setEnabled(false);
                        itemProcessDelayField.setEnabled(false);
                        remoteFileTimeoutField.setEnabled(false);
                        userAgentField.setEnabled(false);
                    } else {
                        batch.setItemProcessDelay(itemProcessDelayField.getText());
                        batch.setRemoteFileTimeout(remoteFileTimeoutField.getText());

                        console.append("\nBatch successfully loaded.\n\n");

                        transitionToLoaded();
                    }
                }
            }
        });

        writeSAFBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {

                if (actionStatus.equals(ActionStatus.VERIFIED)) {
                    lockThreadSensitiveControls();

                    // Action Status Field may have "cancelled" text, so reset it before writing begins.
                    actionStatusField.setText("Your batch has been verified!");
                    actionStatusField.setBackground(Color.green);

                    writeSAFBtn.setText("Writing..");
                    writeCancelBtn.setEnabled(true);

                    console.append("Parsing CSV file and writing output to DC XML files...\n");
                    statusIndicator.setText("Batch Status:\n Verified\n Written:\n 0 / " + batch.getItems().size());

                    currentWriter = new ImportDataWriter() {
                        @Override
                        public void done() {
                            currentWriter = null;
                            writeCancelBtn.setEnabled(false);
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
                                statusIndicator.setText("Batch Status:\n Verified\n Written:\n " + update.getProcessed() + " / " + update.getTotal());
                            }
                        }
                    };

                    batch.setAction("Writing");
                    currentWriter.setBatch(batch);
                    currentWriter.setConsole(console);
                    currentWriter.setFlags(flagPanel);
                    currentWriter.execute();
                } else if (actionStatus.equals(ActionStatus.LOADED)
                        || actionStatus.equals(ActionStatus.FAILED_VERIFICATION)) {
                    console.append("\nPlease verify the batch before writing.\n\n");
                } else if (actionStatus.equals(ActionStatus.NONE_LOADED)) {
                    console.append("\nNo loaded SAF data to write.\n\n");
                } else if (actionStatus.equals(ActionStatus.WRITTEN)) {
                    lockThreadSensitiveControls();
                    writeSAFBtn.setText("Cleaning..");
                    writeCancelBtn.setEnabled(true);

                    console.append("Deleting all folders with flagged errors...\n");
                    statusIndicator.setText("Batch Status:\n Written\n Cleaned:\n 0 / " + batch.getItems().size());

                    currentCleaner = new ImportDataCleaner() {
                        @Override
                        public void done() {
                            currentCleaner = null;
                            writeCancelBtn.setEnabled(false);
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
                                statusIndicator.setText("Batch Status:\n Written\n Cleaned:\n " + update.getProcessed() + " / " + update.getTotal());
                            }
                        }
                    };

                    batch.setAction("Cleaning");
                    currentCleaner.setBatch(batch);
                    currentCleaner.setConsole(console);
                    currentCleaner.setFlags(flagPanel);
                    currentCleaner.execute();
                } else if (actionStatus.equals(ActionStatus.CLEANED)) {
                    console.append("\nSAF data is already written.\n\n");
                }
            }
        });

        writeCancelBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentWriter != null) {
                    currentWriter.cancel(true);
                }

                if (currentCleaner != null) {
                    currentCleaner.cancel(true);
                }

                console.append("Cancelling write..\n");
            }
        });
    }

    private void createFlagTableTab() {
        JPanel buttonsContainer = new JPanel();
        buttonsContainer.add(flagsDownloadCsvBtn);
        buttonsContainer.add(flagsReportSelectedBtn);
        buttonsContainer.setMaximumSize(new Dimension(400, 200));

        flagTableTab.setLayout(new BoxLayout(flagTableTab, BoxLayout.PAGE_AXIS));
        flagTableTab.add(flagPanel);
        flagTableTab.add(buttonsContainer);

        tabs.addTab("Flags", flagTableTab);

        flagsDownloadCsvBtn.setEnabled(false);
        flagsReportSelectedBtn.setEnabled(false);

        flagsDownloadCsvBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (actionStatus.equals(ActionStatus.NONE_LOADED)) {
                    console.append("Unable to write to write CSV file, reason: SAF not loaded.\n");
                    return;
                }

                if (csvOutputFileName.isEmpty() || outputDirectoryName.isEmpty()) {
                    console.append("Unable to write to write CSV file, reason: SAF directory not defined.\n");
                    return;
                }

                if (flagPanel.getRowCount() == 0) {
                    console.append("Unable to write to write CSV file, reason: No flags to output.\n");
                    return;
                }

                String csvFilePath = outputDirectoryName + File.separator + csvOutputFileName;
                try {
                    BufferedWriter writer = Files.newBufferedWriter(Paths.get(csvFilePath));
                    flagPanel.exportToCSV(writer);
                    console.append("Wrote to CSV File: " + csvFilePath + ".\n");
                } catch (IOException e1) {
                    console.append("Unable to write CSV file " + csvFilePath + ", reason: " + e1.getMessage() + ".\n");
                }
            }
        });

        flagsReportSelectedBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (flagPanel.getRowCount() == 0) {
                    console.append("Flag list is empty.\n");
                    return;
                }

                Flag row = flagPanel.getSelected();
                if (row == null) {
                    console.append("No valid row is selected.\n");
                    return;
                }

                Problem problem = new Problem(true, row.getCell(FlagColumns.DESCRIPTION));
                console.append("\nFlag Name: " + row.getCell(FlagColumns.FLAG) + "\n");
                console.append("Flag Description: " + problem.toString() + "\n");

                if (!row.getCell(FlagColumns.AUTHORITY).isEmpty()) {
                    console.append("Flag Authority: " + row.getCell(FlagColumns.AUTHORITY) + "\n");
                }

                if (!row.getCell(FlagColumns.URL).isEmpty()) {
                    console.append("Flag URL: " + row.getCell(FlagColumns.URL) + "\n");
                }

                if (row.getCell(FlagColumns.COLUMN).isEmpty()) {
                    if (!row.getCell(FlagColumns.ROW).isEmpty()) {
                        console.append("Flag Row: " + row.getCell(FlagColumns.ROW) + "\n");
                    }
                } else if (row.getCell(FlagColumns.ROW).isEmpty()) {
                    console.append("Flag Column: " + row.getCell(FlagColumns.COLUMN) + "\n");
                } else {
                    console.append("Flag Column, Row: " + row.getCell(FlagColumns.COLUMN) + ", " + row.getCell(FlagColumns.ROW) + "\n");
                }

                if (!row.getCell(FlagColumns.ACTION).isEmpty()) {
                    console.append("Flag Action: " + row.getCell(FlagColumns.ACTION) + "\n");
                }
            }
        });
    }

    private void createLicenseTab() {

        licenseTab.setLayout(new BoxLayout(licenseTab, BoxLayout.Y_AXIS));

        addLicenseFilePanel.setLayout(new BoxLayout(addLicenseFilePanel, BoxLayout.Y_AXIS));

        addLicenseFilePanel.add(addLicenseCheckbox);

        JPanel licenseFilenameLine = new JPanel();
        licenseFilenameLine.add(licenseFilenameFieldLabel);
        licenseFilenameLine.add(licenseFilenameField);
        addLicenseFilePanel.add(licenseFilenameLine);

        JPanel licenseBundleNameLine = new JPanel();
        licenseBundleNameLine.add(licenseBundleNameFieldLabel);
        licenseBundleNameLine.add(licenseBundleNameField);
        addLicenseFilePanel.add(licenseBundleNameLine);

        licenseTextScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        addLicenseFilePanel.add(licenseTextScrollPane);

        JPanel restrictToGroupPanel = new JPanel();
        restrictToGroupPanel.add(restrictToGroupCheckbox);
        restrictToGroupPanel.add(restrictToGroupField);

        // TODO:
        // radio button complex for picking CC license
        // JPanel addCCLicensePanel = new JPanel();

        licenseTab.add(addLicenseFilePanel);
        licenseTab.add(restrictToGroupPanel);

        addLicenseCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!actionStatus.equals(ActionStatus.NONE_LOADED) && !actionStatus.equals(ActionStatus.WRITTEN) && !actionStatus.equals(ActionStatus.CLEANED)) {
                    if (!addLicenseCheckbox.isSelected()) {
                        // user clicked to disable the license
                        console.append("License disabled.\n");
                        licenseTextField.setEditable(true);
                        batch.unsetLicense();
                    } else if (addLicenseCheckbox.isSelected()) {
                        // user clicked to enable the license
                        console.append("License enabled.\n");
                        licenseTextField.setEditable(false);
                        batch.setLicense(licenseFilenameField.getText(), licenseBundleNameField.getText(), licenseTextField.getText());
                    }
                } else {
                    addLicenseCheckbox.setSelected(false);
                    console.append("No active batch on which to change a license.\n");
                }
            }
        });

        licenseTextField.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!licenseTextField.isEditable()) {
                    console.append("License affixed to items - disable license to edit the license text.\n");
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

        restrictToGroupCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!actionStatus.equals(ActionStatus.NONE_LOADED) && !actionStatus.equals(ActionStatus.WRITTEN) && !actionStatus.equals(ActionStatus.CLEANED)) {
                    if (!restrictToGroupCheckbox.isSelected()) {
                        // user clicked to disable the restriction
                        console.append("Group restriction disabled.\n");
                        restrictToGroupField.setEditable(true);
                        batch.restrictItemsToGroup(null);

                    } else if (restrictToGroupCheckbox.isSelected()) {
                        // user clicked to enable the restriction
                        console.append("Group restriction to \"" + restrictToGroupField.getText() + "\" enabled.\n");
                        restrictToGroupField.setEditable(false);
                        batch.restrictItemsToGroup(restrictToGroupField.getText());
                    }
                } else {
                    restrictToGroupCheckbox.setSelected(false);
                    console.append("No active batch on which to apply group restrictions.\n");
                }
            }
        });

        restrictToGroupField.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!restrictToGroupField.isEditable()) {
                    console.append("Restriction to group \"" + restrictToGroupField.getText() + "\" affixed to items - disable restriction to edit the group name.\n");
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

        tabs.addTab("License Settings", licenseTab);
    }

    private void createVerificationTab() {
        validationTab.setLayout(new BoxLayout(validationTab, BoxLayout.Y_AXIS));

        // verifierTbl = new JTable(new VerifierTableModel());

        Vector<String> columnNames = new Vector<String>();
        Vector<Vector<Object>> rowData = new Vector<Vector<Object>>();

        columnNames.add("Verifier");
        columnNames.add("Generates Errors");
        columnNames.add("Is Enabled");

        for (Entry<String, VerifierProperty> entry : verifierSettings.entrySet()) {
            VerifierProperty verifier = entry.getValue();
            // System.out.println("Adding verifier " + verifier.getClass().getSimpleName());
            Vector<Object> row = new Vector<Object>();

            row.add(verifier.prettyName());
            row.add(verifier.generatesError());
            row.add(verifier.isEnabled());

            verifierNamesMap.add(verifier.getClass().getName());

            rowData.add(row);
        }

        verifierTbl = new JTable(rowData, columnNames);
        verifierTbl.setPreferredScrollableViewportSize(new Dimension(400, 50));
        verifierTbl.setEnabled(false);

        verifierTbl.addMouseListener(new java.awt.event.MouseAdapter() {
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

                int row = verifierTbl.rowAtPoint(evt.getPoint());
                int column = verifierTbl.columnAtPoint(evt.getPoint());
                if (column == 2 && row >= 0 && row < verifierNamesMap.size()) {
                    String verifierName = verifierNamesMap.get(row);
                    if (verifierName != null) {
                        VerifierProperty verifier = verifierSettings.get(verifierName);
                        if (verifier != null) {
                            verifier.setEnabled(!verifier.isEnabled());
                            verifierTbl.setValueAt(verifier.isEnabled(), row, column);

                            unlockVerifyButtons();
                            console.append(verifier.prettyName() + " is now "
                                    + (verifier.isEnabled() ? "Enabled" : "Disabled") + ".\n");

                            // the verification process must be when status is LOADED.
                            if (actionStatus != ActionStatus.NONE_LOADED && actionStatus != ActionStatus.LOADED) {
                                transitionToLoaded();
                            }
                        }
                    }
                }
            }
        });

        JScrollPane verifierTblScrollPane = new JScrollPane(verifierTbl);
        verifierTbl.setFillsViewportHeight(true);

        validationTab.add(verifierTblScrollPane);

        verifyCancelBtn.setEnabled(false);
        JPanel verifyBatchBtnPanel = new JPanel();
        verifyBatchBtnPanel.add(verifyBatchBtn);
        verifyBatchBtnPanel.add(verifyCancelBtn);
        validationTab.add(verifyBatchBtnPanel);

        tabs.addTab("Batch Verification", validationTab);

        verifyBatchBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (actionStatus == ActionStatus.LOADED || actionStatus == ActionStatus.FAILED_VERIFICATION) {
                    lockVerifyButtons();
                    createVerifiers();
                    flagPanel.clear();
                    batch.clearFailedRows();
                    batch.clearIgnoredRows();
                    batchVerified = null;

                    for (Entry<String, VerifierBackground> entry : verifiers.entrySet()) {
                        VerifierBackground verifier = entry.getValue();
                        if (verifier.isEnabled() && verifier.isSwingWorker()) {
                            currentVerifiers.add(verifier);
                        }
                    }

                    if (currentVerifiers.size() == 0) {
                        console.append("No verifiers are enabled.\n");
                        unlockVerifyButtons();
                        transitionToVerifySuccess();
                    } else {
                        currentVerifier = 0;
                        currentVerifiers.get(currentVerifier).execute();
                    }
                } else {
                    console.append("Nothing new to verify.\n");
                }
            }
        });

        verifyCancelBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                console.append("Cancelling " + currentVerifiers.get(currentVerifier).prettyName() + "..\n");

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

    private void createVerifiers() {
        VerifierProperty settings = null;

        currentVerifier = -1;
        currentVerifiers.clear();

        settings = verifierSettings.get(RemoteFilesExistVerifierImpl.class.getName());

        RemoteFilesExistVerifierImpl remoteFileExistsVerifier = new RemoteFilesExistVerifierImpl(settings) {
            @Override
            public List<Problem> doInBackground() {
                statusIndicator.setText("Batch Status:\n Unverified\n Remote File Exists?\n 0 / " + batch.getItems().size());
                batch.setAction(prettyName());
                return verify(batch, console, flagPanel);
            }

            @Override
            public void done() {
                if (isCancelled()) {
                    doCancel();
                    return;
                }

                List<Verifier.Problem> problems = new ArrayList<Verifier.Problem>();
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
                for (Verifier.Problem problem : problems) {
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
                    statusIndicator.setText("Batch Status:\n Unverified\n Remote File Exists?\n " + update.getProcessed() + " / " + update.getTotal());
                }
            }
        };

        settings = verifierSettings.get(LocalFilesExistVerifierImpl.class.getName());

        LocalFilesExistVerifierImpl localFileExistsVerifier = new LocalFilesExistVerifierImpl(settings) {
            @Override
            public List<Problem> doInBackground() {
                statusIndicator.setText("Batch Status:\n Unverified\n Local File Exists?\n 0 / " + batch.getItems().size());
                batch.setAction(prettyName());
                return verify(batch, console, flagPanel);
            }

            @Override
            public void done() {
                if (isCancelled()) {
                    doCancel();
                    return;
                }

                List<Verifier.Problem> problems = new ArrayList<Verifier.Problem>();
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
                for (Verifier.Problem problem : problems) {
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
                    statusIndicator.setText("Batch Status:\n Unverified\n Local File Exists?\n " + update.getProcessed() + " / " + update.getTotal());
                }
            }
        };

        settings = verifierSettings.get(ValidSchemaNameVerifierImpl.class.getName());

        ValidSchemaNameVerifierImpl validSchemaVerifier = new ValidSchemaNameVerifierImpl(settings) {
            @Override
            public List<Problem> doInBackground() {
                statusIndicator.setText("Batch Status:\n Unverified\n Schema Name?\n 0 / " + batch.getItems().size());
                batch.setAction(prettyName());
                return verify(batch, console, flagPanel);
            }

            @Override
            public void done() {
                if (isCancelled()) {
                    doCancel();
                    return;
                }

                List<Verifier.Problem> problems = new ArrayList<Verifier.Problem>();
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
                for (Verifier.Problem problem : problems) {
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
                    statusIndicator.setText("Batch Status:\n Unverified\n Schema Name?\n " + update.getProcessed() + " / " + update.getTotal());
                }
            }
        };

        if (batch != null) {
            batch.clearFailedRows();
            batch.clearIgnoredRows();
        }

        verifiers.clear();
        verifiers.put(RemoteFilesExistVerifierImpl.class.getName(), remoteFileExistsVerifier);
        verifiers.put(LocalFilesExistVerifierImpl.class.getName(), localFileExistsVerifier);
        verifiers.put(ValidSchemaNameVerifierImpl.class.getName(), validSchemaVerifier);
    }

    private void createVerifierSettings() {
        VerifierProperty validSchemaVerifier = new ValidSchemaNameVerifierImpl();
        verifierSettings.put(ValidSchemaNameVerifierImpl.class.getName(), validSchemaVerifier);

        VerifierProperty localFileExistsVerifier = new LocalFilesExistVerifierImpl();
        verifierSettings.put(LocalFilesExistVerifierImpl.class.getName(), localFileExistsVerifier);

        VerifierProperty remoteFileExistsVerifier = new RemoteFilesExistVerifierImpl();
        verifierSettings.put(RemoteFilesExistVerifierImpl.class.getName(), remoteFileExistsVerifier);
    }

    private void initializeGUIState() {
        actionStatus = ActionStatus.NONE_LOADED;
        batch = null;

        // actionStatusfield
        actionStatusField.setForeground(Color.white);
        actionStatusField.setBackground(Color.blue);

        // loadBatchBtn
        loadBatchBtn.setText("Load specified batch now!");

        // verifyBatchBtn
        verifyBatchBtn.setEnabled(false);

        // writeSAFBtn
        writeSAFBtn.setText("No batch loaded");

        // statusIndicator
        statusIndicator.setForeground(Color.white);
        statusIndicator.setBackground(Color.blue);
        statusIndicator.setText("No batch loaded");

    }

    private void lockThreadSensitiveControls() {
        loadBatchBtn.setEnabled(false);
        chooseInputFileBtn.setEnabled(false);
        chooseSourceDirectoryBtn.setEnabled(false);
        chooseOutputDirectoryBtn.setEnabled(false);
        verifyCancelBtn.setEnabled(true);
        verifyBatchBtn.setEnabled(false);

        if (actionStatus == ActionStatus.VERIFIED) {
            writeSAFBtn.setEnabled(false);
        }

        if (actionStatus != ActionStatus.NONE_LOADED) {
            ignoreFilesBox.setEnabled(false);
            continueOnRemoteErrorBox.setEnabled(false);
            itemProcessDelayField.setEnabled(false);
            remoteFileTimeoutField.setEnabled(false);
            userAgentField.setEnabled(false);
        }
    }

    private void lockVerifyButtons() {
        verifyBatchBtn.setText("Verifying..");
        lockThreadSensitiveControls();
    }

    private void transitionToCleaned() {
        writeSAFBtn.setText("No Batch Loaded");
        writeSAFBtn.setEnabled(false);
        actionStatusField.setForeground(Color.black);
        actionStatusField.setBackground(Color.green);
        actionStatusField.setText("Batch SAF written & failures removed.");
        statusIndicator.setText("Batch status:\n Written & Cleaned");
        statusIndicator.setForeground(Color.black);
        statusIndicator.setBackground(Color.green);
        actionStatus = ActionStatus.CLEANED;
    }

    private void transitionToCleanedFailed() {
        actionStatusField.setForeground(Color.black);
        actionStatusField.setBackground(Color.red);
        actionStatusField.setText("Batch SAF written & clean failed.");
        statusIndicator.setText("Batch status:\n Written & Clean Failed");
        statusIndicator.setForeground(Color.black);
        statusIndicator.setBackground(Color.red);
    }

    private void transitionToLoaded() {
        actionStatusField.setText("Your batch has not been verified.");
        actionStatusField.setForeground(Color.black);
        actionStatusField.setBackground(Color.red);
        actionStatus = ActionStatus.LOADED;
        statusIndicator.setText("Batch Status:\n Unverified");
        statusIndicator.setForeground(Color.white);
        statusIndicator.setBackground(Color.blue);
        loadBatchBtn.setText("Reload batch as specified");
        addLicenseCheckbox.setSelected(false);
        writeSAFBtn.setText("No batch loaded");
        writeSAFBtn.setEnabled(false);
        verifyBatchBtn.setEnabled(true);
        flagsDownloadCsvBtn.setEnabled(true);
        flagsReportSelectedBtn.setEnabled(true);

        batch.setIgnoreFiles(ignoreFilesBox.isSelected());
        batch.setRemoteBitstreamErrorContinue(continueOnRemoteErrorBox.isSelected());
        batch.setItemProcessDelay(itemProcessDelayField.getText());
        batch.setUserAgent(userAgentField.getText());

        flagPanel.clear();

        ignoreFilesBox.setEnabled(true);
        continueOnRemoteErrorBox.setEnabled(true);
        itemProcessDelayField.setEnabled(true);
        remoteFileTimeoutField.setEnabled(true);
        userAgentField.setEnabled(true);

        batchContinue = false;
    }

    private void transitionToVerifyFailed() {
        console.append("Batch failed verification.\n");
        actionStatus = ActionStatus.FAILED_VERIFICATION;
        actionStatusField.setText("Your batch failed to verify.");
        writeSAFBtn.setText("No valid batch.");
        actionStatusField.setBackground(Color.red);
        statusIndicator.setText("Batch Status:\n Unverified");
    }

    private void transitionToVerifySuccess() {
        console.append("Batch verified.\n");

        verifyBatchBtn.setEnabled(false);

        actionStatus = ActionStatus.VERIFIED;
        actionStatusField.setText("Your batch has been verified!");
        actionStatusField.setBackground(Color.green);

        statusIndicator.setText("Batch Status:\nVerified");
        statusIndicator.setForeground(Color.black);
        statusIndicator.setBackground(Color.green);

        writeSAFBtn.setEnabled(true);
        writeSAFBtn.setText("Write SAF data now!");
    }

    private void transitionToVerifySuccessIgnoreErrors() {
        console.append("Batch failed verification.\n");

        verifyBatchBtn.setEnabled(false);

        actionStatus = ActionStatus.VERIFIED;
        actionStatusField.setText("Your batch failed to verify, continuing.");
        actionStatusField.setBackground(Color.orange);

        statusIndicator.setText("Batch Status:\nUnverified,\nContinuing Anyway");
        statusIndicator.setForeground(Color.black);
        statusIndicator.setBackground(Color.orange);

        writeSAFBtn.setEnabled(true);
        writeSAFBtn.setText("Write SAF data now!");
    }

    private void transitionToWritten() {

        statusIndicator.setText("Batch status:\n Written");
        statusIndicator.setForeground(Color.black);
        statusIndicator.setBackground(Color.white);

        actionStatusField.setText("Your batch is written.");
        actionStatusField.setBackground(Color.green);

        writeSAFBtn.setText("No Batch Loaded");
        writeSAFBtn.setEnabled(false);

        actionStatusField.setText("Batch SAF written.");

        loadBatchBtn.setText("Reload batch as specified");
        actionStatus = ActionStatus.WRITTEN;
    }

    private void transitionToWrittenFailed() {
        statusIndicator.setText("Batch Status:\n verified\n write failed");
        statusIndicator.setForeground(Color.black);
        statusIndicator.setBackground(Color.red);

        actionStatusField.setText("Your batch had write failures.");
        actionStatusField.setBackground(Color.orange);

        writeSAFBtn.setText("Clean SAF Data");
        writeSAFBtn.setEnabled(true);

        actionStatusField.setText("Batch SAF written, with failures.");

        loadBatchBtn.setText("Reload batch as specified");
        actionStatus = ActionStatus.WRITTEN;
    }

    private void transitionToWrittenIgnored() {
        statusIndicator.setText("Batch Status:\n written\n with ignored");
        statusIndicator.setForeground(Color.black);
        statusIndicator.setBackground(Color.orange);

        actionStatusField.setText("Your batch has ignored rows.");
        actionStatusField.setBackground(Color.orange);

        writeSAFBtn.setText("Clean SAF Data");
        writeSAFBtn.setEnabled(true);

        actionStatusField.setText("Batch SAF written, with ignored.");

        loadBatchBtn.setText("Reload batch as specified");
        actionStatus = ActionStatus.WRITTEN;
    }

    private void unlockThreadSensitiveControls() {
        verifyCancelBtn.setEnabled(false);

        if (actionStatus == ActionStatus.LOADED || actionStatus == ActionStatus.FAILED_VERIFICATION) {
            verifyBatchBtn.setEnabled(true);
        }

        loadBatchBtn.setEnabled(true);
        chooseInputFileBtn.setEnabled(true);
        chooseSourceDirectoryBtn.setEnabled(true);
        chooseOutputDirectoryBtn.setEnabled(true);

        if (actionStatus == ActionStatus.VERIFIED) {
            writeSAFBtn.setEnabled(true);
        } else if (actionStatus == ActionStatus.CLEANED) {
            writeSAFBtn.setEnabled(false);
        }

        if (actionStatus != ActionStatus.NONE_LOADED) {
            ignoreFilesBox.setEnabled(true);
            continueOnRemoteErrorBox.setEnabled(true);
            itemProcessDelayField.setEnabled(true);
            remoteFileTimeoutField.setEnabled(true);
            userAgentField.setEnabled(true);
        }
    }

    private void unlockVerifyButtons() {
        verifyBatchBtn.setText("Verify Batch");
        unlockThreadSensitiveControls();
    }
}
