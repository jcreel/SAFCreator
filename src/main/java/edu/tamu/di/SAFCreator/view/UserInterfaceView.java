package edu.tamu.di.SAFCreator.view;

import java.awt.Color;
import java.awt.Dimension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;


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
import javax.swing.table.DefaultTableModel;

import edu.tamu.di.SAFCreator.model.FlagPanel;
import edu.tamu.di.SAFCreator.model.verify.VerifierProperty;
import edu.tamu.di.SAFCreator.model.verify.impl.LocalFilesExistVerifierImpl;
import edu.tamu.di.SAFCreator.model.verify.impl.RemoteFilesExistVerifierImpl;
import edu.tamu.di.SAFCreator.model.verify.impl.ValidSchemaNameVerifierImpl;


/**
 * A convenience JFRame user interface to be shared across the project.
 *
 * This is implemented with all the properties as public so that this class operates as a convenience structure for accessing the properties.
 */
public final class UserInterfaceView extends JFrame {

    private static final long serialVersionUID = 1L;

    // defaults
    public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:57.0) Gecko/20100101 Firefox/57.0";
    public static final int DEFAULT_PROCESS_DELAY = 400;
    public static final int DEFAULT_REMOTE_FILE_TIMEOUT = 10000;
    public static final String DEFAULT_CSV_OUTPUT_NAME = "SAF-Flags.csv";


    // tabbed views
    private final JPanel mainTab = new JPanel();
    private final JPanel licenseTab = new JPanel();
    private final JPanel validationTab = new JPanel();
    private final JPanel advancedSettingsTab = new JPanel();
    private final JPanel flagTableTab = new JPanel();

    private final JTabbedPane tabs = new JTabbedPane();


    // Components of the Batch Detail tab
    private final JButton chooseInputFileBtn = new JButton("Select metadata CSV file");
    private final JButton chooseSourceDirectoryBtn = new JButton("Select source files directory");
    private final JButton chooseOutputDirectoryBtn = new JButton("Select SAF output directory");

    private final JButton loadBatchBtn = new JButton("Load specified batch now!");
    private final JButton writeSAFBtn = new JButton("No batch loaded");
    private final JButton writeCancelBtn = new JButton("Cancel");

    private final JFileChooser inputFileChooser = new JFileChooser(".");
    private final JFileChooser sourceDirectoryChooser = new JFileChooser(".");
    private final JFileChooser outputDirectoryChooser = new JFileChooser(".");

    private final JTextField inputFileNameField = new JTextField("", 42);
    private final JTextField sourceDirectoryNameField = new JTextField("", 40);
    private final JTextField outputDirectoryNameField = new JTextField("", 40);
    private final JTextField actionStatusField = new JTextField("Please load a batch for processing.");


    // Components of the License tab
    private final JCheckBox addLicenseCheckbox = new JCheckBox("Add a license:");
    private final JCheckBox restrictToGroupCheckbox = new JCheckBox("Restrict read access to a group - Group name:");

    private final JLabel licenseFilenameFieldLabel = new JLabel("License bitstream filename:");
    private final JLabel licenseBundleNameFieldLabel = new JLabel("License bundle name:");

    private final JPanel addLicenseFilePanel = new JPanel();

    private final JTextArea licenseTextField = new JTextArea("This item is hereby licensed.", 6, 0);

    private final JTextField licenseFilenameField = new JTextField("license.txt", 39);
    private final JTextField licenseBundleNameField = new JTextField("LICENSE", 42);
    private final JTextField restrictToGroupField = new JTextField("member", 36);

    private final JScrollPane licenseTextScrollPane = new JScrollPane(licenseTextField);


    // Components of the Verify Batch tab
    private final JButton verifyBatchBtn = new JButton("Verify Batch");
    private final JButton verifyCancelBtn = new JButton("Cancel");

    private final JTable verifierTbl = new JTable();

    private final Map<String, VerifierProperty> verifierSettings = new HashMap<String, VerifierProperty>();
    private final List<String> verifierNamesMap = new ArrayList<String>();


    // Components of the Advanced Settings tab
    private final JCheckBox ignoreFilesBox = new JCheckBox("Omit bitstreams (content files) from generated SAF:");
    private final JCheckBox continueOnRemoteErrorBox = new JCheckBox("Allow writing even if remote bitstream verification flags an error:");

    private final JLabel itemProcessDelayLabel = new JLabel("Item Processing Delay (in milliseconds):");
    private final JLabel remoteFileTimeoutLabel = new JLabel("Remote File Timeout (in milliseconds):");
    private final JLabel userAgentLabel = new JLabel("User agent:");

    private final JTextField itemProcessDelayField = new JTextField("" + DEFAULT_PROCESS_DELAY, 10);
    private final JTextField remoteFileTimeoutField = new JTextField("" + DEFAULT_REMOTE_FILE_TIMEOUT, 10);
    private final JTextField userAgentField = new JTextField(DEFAULT_USER_AGENT, 48);


    // Components of the Flag List tab
    private final JButton flagsDownloadCsvBtn = new JButton("Generate CSV");
    private final JButton flagsReportSelectedBtn = new JButton("Display Selected Row");

    private final FlagPanel flagPanel = new FlagPanel();


    // Components shown under any tab
    private final JPanel statusPanel = new JPanel();

    private final JTextArea statusIndicator = new JTextArea("No batch loaded", 2, 10);
    private final JTextArea console = new JTextArea(20, 50);

    private final JScrollPane scrollPane = new JScrollPane(console);


    public UserInterfaceView() {
        createVerifierSettings();

        createBatchDetailsTab();

        createLicenseTab();

        createVerificationTab();

        createAdvancedSettingsTab();

        createFlagTableTab();

        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        // add the tabbed views
        getContentPane().add(tabs);

        // add the status info area present under all tabs
        console.setEditable(false);
        console.setLineWrap(true);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        statusIndicator.setBackground(Color.blue);
        statusIndicator.setForeground(Color.white);
        statusIndicator.setEditable(false);
        statusIndicator.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        scrollPane.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

        statusPanel.add(statusIndicator);
        statusPanel.add(scrollPane);
        getContentPane().add(statusPanel);

        initializeState();

        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("DSpace Simple Archive Format Creator");
    }

    public JTextField getActionStatusField() {
        return actionStatusField;
    }

    public JCheckBox getAddLicenseCheckbox() {
        return addLicenseCheckbox;
    }

    public JPanel getAddLicenseFilePanel() {
        return addLicenseFilePanel;
    }
    public JPanel getAdvancedSettingsTab() {
        return advancedSettingsTab;
    }

    public JButton getChooseInputFileBtn() {
        return chooseInputFileBtn;
    }
    public JButton getChooseOutputDirectoryBtn() {
        return chooseOutputDirectoryBtn;
    }

    public JButton getChooseSourceDirectoryBtn() {
        return chooseSourceDirectoryBtn;
    }

    public JTextArea getConsole() {
        return console;
    }

    public JCheckBox getContinueOnRemoteErrorBox() {
        return continueOnRemoteErrorBox;
    }

    public FlagPanel getFlagPanel() {
        return flagPanel;
    }

    public JButton getFlagsDownloadCsvBtn() {
        return flagsDownloadCsvBtn;
    }

    public JButton getFlagsReportSelectedBtn() {
        return flagsReportSelectedBtn;
    }

    public JPanel getFlagTableTab() {
        return flagTableTab;
    }

    public JCheckBox getIgnoreFilesBox() {
        return ignoreFilesBox;
    }

    public JFileChooser getInputFileChooser() {
        return inputFileChooser;
    }

    public JTextField getInputFileNameField() {
        return inputFileNameField;
    }

    public JTextField getItemProcessDelayField() {
        return itemProcessDelayField;
    }

    public JLabel getItemProcessDelayLabel() {
        return itemProcessDelayLabel;
    }

    public JTextField getLicenseBundleNameField() {
        return licenseBundleNameField;
    }

    public JLabel getLicenseBundleNameFieldLabel() {
        return licenseBundleNameFieldLabel;
    }

    public JTextField getLicenseFilenameField() {
        return licenseFilenameField;
    }

    public JLabel getLicenseFilenameFieldLabel() {
        return licenseFilenameFieldLabel;
    }

    public JPanel getLicenseTab() {
        return licenseTab;
    }

    public JTextArea getLicenseTextField() {
        return licenseTextField;
    }

    public JScrollPane getLicenseTextScrollPane() {
        return licenseTextScrollPane;
    }

    public JButton getLoadBatchBtn() {
        return loadBatchBtn;
    }

    public JPanel getMainTab() {
        return mainTab;
    }

    public JFileChooser getOutputDirectoryChooser() {
        return outputDirectoryChooser;
    }

    public JTextField getOutputDirectoryNameField() {
        return outputDirectoryNameField;
    }

    public JTextField getRemoteFileTimeoutField() {
        return remoteFileTimeoutField;
    }

    public JLabel getRemoteFileTimeoutLabel() {
        return remoteFileTimeoutLabel;
    }

    public JCheckBox getRestrictToGroupCheckbox() {
        return restrictToGroupCheckbox;
    }

    public JTextField getRestrictToGroupField() {
        return restrictToGroupField;
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    public JFileChooser getSourceDirectoryChooser() {
        return sourceDirectoryChooser;
    }

    public JTextField getSourceDirectoryNameField() {
        return sourceDirectoryNameField;
    }

    public JTextArea getStatusIndicator() {
        return statusIndicator;
    }

    public JPanel getStatusPanel() {
        return statusPanel;
    }

    public JTabbedPane getTabs() {
        return tabs;
    }

    public JTextField getUserAgentField() {
        return userAgentField;
    }

    public JLabel getUserAgentLabel() {
        return userAgentLabel;
    }

    public JPanel getValidationTab() {
        return validationTab;
    }

    public List<String> getVerifierNamesMap() {
        return verifierNamesMap;
    }

    public Map<String, VerifierProperty> getVerifierSettings() {
        return verifierSettings;
    }

    public JTable getVerifierTbl() {
        return verifierTbl;
    }

    public JButton getVerifyBatchBtn() {
        return verifyBatchBtn;
    }

    public JButton getVerifyCancelBtn() {
        return verifyCancelBtn;
    }

    public JButton getWriteCancelBtn() {
        return writeCancelBtn;
    }

    public JButton getWriteSAFBtn() {
        return writeSAFBtn;
    }

    private void createAdvancedSettingsTab() {
        advancedSettingsTab.setLayout(new BoxLayout(advancedSettingsTab, BoxLayout.Y_AXIS));
        advancedSettingsTab.add(ignoreFilesBox);
        advancedSettingsTab.add(continueOnRemoteErrorBox);

        tabs.addTab("Advanced Settings", advancedSettingsTab);

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
        remoteFileTimeoutField.setEnabled(false);
        userAgentField.setEnabled(false);
    }

    private void createBatchDetailsTab() {
        sourceDirectoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        outputDirectoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        inputFileNameField.setEditable(false);
        sourceDirectoryNameField.setEditable(false);
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

        tabs.addTab("License Settings", licenseTab);
    }

    private void createVerificationTab() {
        validationTab.setLayout(new BoxLayout(validationTab, BoxLayout.Y_AXIS));

        Vector<String> columnNames = new Vector<String>();
        Vector<Vector<Object>> rowData = new Vector<Vector<Object>>();

        columnNames.add("Verifier");
        columnNames.add("Generates Errors");
        columnNames.add("Is Enabled");

        for (Entry<String, VerifierProperty> entry : verifierSettings.entrySet()) {
            VerifierProperty verifier = entry.getValue();
            Vector<Object> row = new Vector<Object>();

            row.add(verifier.prettyName());
            row.add(verifier.generatesError());
            row.add(verifier.isEnabled());

            verifierNamesMap.add(verifier.getClass().getName());

            rowData.add(row);
        }

        verifierTbl.setModel(new DefaultTableModel(rowData, columnNames));
        verifierTbl.setPreferredScrollableViewportSize(new Dimension(400, 50));
        verifierTbl.setEnabled(false);

        JScrollPane verifierTblScrollPane = new JScrollPane(verifierTbl);
        verifierTbl.setFillsViewportHeight(true);
        verifierTbl.setAutoCreateRowSorter(true);

        validationTab.add(verifierTblScrollPane);

        verifyCancelBtn.setEnabled(false);
        JPanel verifyBatchBtnPanel = new JPanel();
        verifyBatchBtnPanel.add(verifyBatchBtn);
        verifyBatchBtnPanel.add(verifyCancelBtn);
        validationTab.add(verifyBatchBtnPanel);

        tabs.addTab("Batch Verification", validationTab);
    }

    private void createVerifierSettings() {
        VerifierProperty validSchemaVerifier = new ValidSchemaNameVerifierImpl();
        verifierSettings.put(ValidSchemaNameVerifierImpl.class.getName(), validSchemaVerifier);

        VerifierProperty localFileExistsVerifier = new LocalFilesExistVerifierImpl();
        verifierSettings.put(LocalFilesExistVerifierImpl.class.getName(), localFileExistsVerifier);

        VerifierProperty remoteFileExistsVerifier = new RemoteFilesExistVerifierImpl();
        verifierSettings.put(RemoteFilesExistVerifierImpl.class.getName(), remoteFileExistsVerifier);
    }

    private void initializeState() {
        actionStatusField.setForeground(Color.white);
        actionStatusField.setBackground(Color.blue);

        loadBatchBtn.setText("Load specified batch now!");
        verifyBatchBtn.setEnabled(false);
        writeSAFBtn.setText("No batch loaded");

        statusIndicator.setForeground(Color.white);
        statusIndicator.setBackground(Color.blue);
        statusIndicator.setText("No batch loaded");
    }
}
