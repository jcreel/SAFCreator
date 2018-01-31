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
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import edu.tamu.di.SAFCreator.model.Batch;
import edu.tamu.di.SAFCreator.model.Flag;
import edu.tamu.di.SAFCreator.model.Flag.Columns;
import edu.tamu.di.SAFCreator.model.Verifier.Problem;
import edu.tamu.di.SAFCreator.model.FlagPanel;
import edu.tamu.di.SAFCreator.model.Verifier;
import edu.tamu.di.SAFCreator.model.VerifierBackground;
import edu.tamu.di.SAFCreator.verify.FilesExistVerifierImpl;
import edu.tamu.di.SAFCreator.verify.ValidSchemaNameVerifierImpl;


public class ImporterGUI extends JFrame 
{
	private Batch batch;

	private List<VerifierBackground> verifiers = new ArrayList<VerifierBackground>();

	private enum ActionStatus {NONE_LOADED, LOADED, FAILED_VERIFICATION, VERIFIED, WRITTEN};
	private ActionStatus actionStatus = ActionStatus.NONE_LOADED;

	private enum FieldChangeStatus {NO_CHANGES, CHANGES};
	private FieldChangeStatus itemProcessDelayFieldChangeStatus = FieldChangeStatus.NO_CHANGES;
	private FieldChangeStatus userAgentFieldChangeStatus = FieldChangeStatus.NO_CHANGES;

	private static final long serialVersionUID = 1L;
	private static final String defaultUserAgent = "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:57.0) Gecko/20100101 Firefox/57.0";
	private static final int defaultProcessDelay = 400;
	
	//tabbed views
	private final JTabbedPane tabs = new JTabbedPane();
	private final JPanel mainTab = new JPanel();
	private final JPanel licenseTab = new JPanel();
	private final JPanel validationTab = new JPanel();
	private final JPanel advancedSettingsTab = new JPanel();
	private final JPanel flagTableTab = new JPanel();
	
	//Components of the Batch Detail tab
	private final JButton chooseInputFileBtn = new JButton("Select metadata CSV file");
	private final JButton chooseSourceDirectoryBtn = new JButton("Select source files directory");
	private final JButton chooseOutputDirectoryBtn = new JButton("Select SAF output directory");
	private final JFileChooser inputFileChooser = new JFileChooser(".");
	private final JFileChooser sourceDirectoryChooser = new JFileChooser(".");
	private final JFileChooser outputDirectoryChooser = new JFileChooser(".");;
	private final JTextField inputFileNameField = new JTextField("", 42);
	private final JTextField sourceDirectoryNameField = new JTextField("", 40);
	private final JTextField outputDirectoryNameField = new JTextField("", 40);
	private final JTextField actionStatusField = new JTextField("Please load a batch for processing.");
	private final JButton loadBatchBtn = new JButton("Load specified batch now!");
	private final JButton writeSAFBtn = new JButton("No batch loaded");
	private final JButton writeCancelBtn = new JButton("Cancel");
	
	//Components of the License tab
	private final JPanel addLicenseFilePanel = new JPanel();
	private final JCheckBox addLicenseCheckbox = new JCheckBox("Add a license:");
	private final JLabel licenseFilenameFieldLabel = new JLabel("License bitstream filename:");
	private final JTextField licenseFilenameField = new JTextField("license.txt", 39);
	private final JLabel licenseBundleNameFieldLabel = new JLabel("License bundle name:");
	private final JTextField licenseBundleNameField = new JTextField("LICENSE", 42);
	//private final JLabel licenseTextFieldLabel = new JLabel("License text:");
	private final JTextArea licenseTextField = new JTextArea("This item is hereby licensed.", 6, 0);
	private final JScrollPane licenseTextScrollPane = new JScrollPane(licenseTextField);
	private final JCheckBox restrictToGroupCheckbox = new JCheckBox("Restrict read access to a group - Group name:");
	private final JTextField restrictToGroupField = new JTextField("member", 36);
	
	
	//Components of the Verify Batch tab
	private final JButton verifyBatchBtn = new JButton("Verify Batch");
	private final JButton verifyCancelBtn = new JButton("Cancel");
	private JTable verifierTbl = null;
	
	//Components of the Advanced Settings tab
	private final JCheckBox ignoreFilesBox = new JCheckBox("Omit bitstreams (content files) from generated SAF:");
	private final JLabel itemProcessDelayLabel = new JLabel("Item Processing Delay (in milliseconds):");
	private final JTextField itemProcessDelayField= new JTextField(String.valueOf(defaultProcessDelay), 10);
	private final JLabel userAgentLabel = new JLabel("User agent:");
	private final JTextField userAgentField = new JTextField(defaultUserAgent, 48);
	private final JCheckBox continueOnRemoteErrorBox = new JCheckBox("Allow writing even if remote bitstream verification flags an error:");

	//Components of the Flag List tab
	private final FlagPanel flagPanel = new FlagPanel();
	private final JButton flagsDownloadCsvBtn = new JButton("Generate CSV");
	private final JButton flagsReportSelectedBtn = new JButton("Display Selected Row");
	private static final String csvOutputFileName = "SAF-Flags.csv";
	
	
	//Components shown under any tab
	private final JPanel statusPanel = new JPanel();
	private final JTextArea statusIndicator = new JTextArea("No batch loaded", 2, 10);
	private final JTextArea console = new JTextArea(20, 50);
	private final JScrollPane scrollPane;
	
	private String metadataInputFileName;
	private static String sourceDirectoryName;
	private static String outputDirectoryName;
	
	private final ImportDataProcessor processor;


	// swing background process handling
	private ImportDataWriter currentWriter = null;
	private VerifierBackground currentVerifier = null;
	private static Boolean batchVerified = null;
	private boolean batchContinue = false;

	
	public ImporterGUI(final ImportDataProcessor processor)
	{	
		this.processor=processor;

		createBatchDetailsTab();
		
		createLicenseTab();
		
		createVerificationTab();
		
		createAdvancedSettingsTab();
		
		createFlagTableTab();

		this.setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
		
//		JMenuBar bar = new JMenuBar();
//		bar.add(new JMenu("menu"));
//		this.setJMenuBar(bar);
		
		//add the tabbed views
		getContentPane().add(tabs);
		
		
		//add the status info area present under all tabs
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
		
		
		this.pack();
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setTitle("DSpace Simple Archive Format Creator");
	
	}


	private void createVerificationTab() 
	{
		validationTab.setLayout(new BoxLayout(validationTab, BoxLayout.Y_AXIS));		
	
		//verifierTbl = new JTable(new VerifierTableModel());
		
		Vector<String>columnNames = new Vector<String>();
		Vector<Vector<Object>>rowData = new Vector<Vector<Object>>();
		
		columnNames.add("Verifier");
		columnNames.add("Generates Errors");
		
		for(Verifier verifier : verifiers)
		{
			//System.out.println("Adding verifier  " + verifier.getClass().getSimpleName());
			Vector<Object> row = new Vector<Object>();
			
			row.add(verifier.prettyName());
			row.add(verifier.generatesError());
			
			rowData.add(row);
		}
		
		
		verifierTbl = new JTable(rowData, columnNames);
		verifierTbl.setPreferredScrollableViewportSize(new Dimension(400, 50));
		verifierTbl.setEnabled(false);
		
		
		JScrollPane verifierTblScrollPane = new JScrollPane(verifierTbl);
		verifierTbl.setFillsViewportHeight(true);
		
		validationTab.add(verifierTblScrollPane);
		
		verifyCancelBtn.setEnabled(false);
		JPanel verifyBatchBtnPanel = new JPanel();
		verifyBatchBtnPanel.add(verifyBatchBtn);
		verifyBatchBtnPanel.add(verifyCancelBtn);
		validationTab.add(verifyBatchBtnPanel);
		
		
	
		tabs.addTab("Batch Verification", validationTab);
		
		verifyBatchBtn.addActionListener
		(
			new ActionListener()
			{

				public void actionPerformed(ActionEvent e) 
				{
					if(actionStatus == ActionStatus.LOADED)
					{
						lockVerifyButtons();
						currentVerifier = null;
						batch.clearIgnoredRows();

						for(VerifierBackground verifier : verifiers)
						{
							if (!verifier.isSwingWorker()) {
								// TODO: consider handling non-background verifiers by creating a custom worker and then appending as a background verifier.
								continue;
							}

							currentVerifier = verifier;
							break;
						}

						if (currentVerifier == null) {
							// TODO report error
							unlockVerifyButtons();
						}
						else {
							currentVerifier.execute();
						}
					}
					else
					{
						console.append("Nothing new to verify.\n");
					}
				}
			}
		);

		verifyCancelBtn.addActionListener
		(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					if (currentVerifier == null) {
						return;
					}

					currentVerifier.cancel(false);
				}
			}
		);
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
		
		//TODO:
		//radio button complex for picking CC license
		//JPanel addCCLicensePanel = new JPanel();
		
		
		licenseTab.add(addLicenseFilePanel);
		licenseTab.add(restrictToGroupPanel);
		
		addLicenseCheckbox.addActionListener
		(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e) 
				{
					if(!actionStatus.equals(ActionStatus.NONE_LOADED) && !actionStatus.equals(ActionStatus.WRITTEN))
					{
						if(!addLicenseCheckbox.isSelected())
						{
							//user clicked to disable the license
							console.append("License disabled.\n");
							licenseTextField.setEditable(true);
							batch.unsetLicense();
						}
						else if(addLicenseCheckbox.isSelected())
						{
							//user clicked to enable the license
							console.append("License enabled.\n");
							licenseTextField.setEditable(false);
							batch.setLicense(licenseFilenameField.getText(), licenseBundleNameField.getText(), licenseTextField.getText());
						}
					}
					else
					{
						addLicenseCheckbox.setSelected(false);
						console.append("No active batch on which to change a license.\n");
					}
				}
			}
		);
		
		licenseTextField.addMouseListener
		(
			new MouseListener()
			{
				public void mouseClicked(MouseEvent e) 
				{
					if(!licenseTextField.isEditable())
					{
						console.append("License affixed to items - disable license to edit the license text.\n");
					}
				}

				public void mousePressed(MouseEvent e) {}

				public void mouseReleased(MouseEvent e) {}

				public void mouseEntered(MouseEvent e) {}

				public void mouseExited(MouseEvent e) {}
			}
		);
		
		
		restrictToGroupCheckbox.addActionListener
		(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e) 
				{
					if(!actionStatus.equals(ActionStatus.NONE_LOADED) && !actionStatus.equals(ActionStatus.WRITTEN))
					{
						if(!restrictToGroupCheckbox.isSelected())
						{
							//user clicked to disable the restriction
							console.append("Group restriction disabled.\n");
							restrictToGroupField.setEditable(true);
							batch.restrictItemsToGroup(null);
							
						}
						else if(restrictToGroupCheckbox.isSelected())
						{
							//user clicked to enable the restriction
							console.append("Group restriction to \"" + restrictToGroupField.getText() + "\" enabled.\n");
							restrictToGroupField.setEditable(false);
							batch.restrictItemsToGroup(restrictToGroupField.getText());
						}
					}
					else
					{
						restrictToGroupCheckbox.setSelected(false);
						console.append("No active batch on which to apply group restrictions.\n");
					}
				}
			}
		);
		
		restrictToGroupField.addMouseListener
		(
			new MouseListener()
			{
				public void mouseClicked(MouseEvent e) 
				{
					if(!restrictToGroupField.isEditable())
					{
						console.append("Restriction to group \"" + restrictToGroupField.getText() + "\" affixed to items - disable restriction to edit the group name.\n");
					}
				}

				public void mousePressed(MouseEvent e) {}

				public void mouseReleased(MouseEvent e) {}

				public void mouseEntered(MouseEvent e) {}

				public void mouseExited(MouseEvent e) {}
			}
		);
		
		
		tabs.addTab("License Settings", licenseTab);
		
		
	}


	private void createVerifiers() {
		VerifierBackground fileExistsVerifier = new FilesExistVerifierImpl() {
			@Override
			public List<Problem> doInBackground() {
				statusIndicator.setText("Batch Status:\n Unverified\n File Exists?\n 0 / " + batch.getItems().size());
				return verify(batch, console, flagPanel);
			}

			@Override
			public void done() {
				if (isCancelled()) {
					cancelVerifyCleanup();
					return;
				}

				List<Verifier.Problem> problems = new ArrayList<Verifier.Problem>();
				if (batchVerified == null) {
					batchVerified = true;
				}

				try
				{
					problems = get();
				} catch (InterruptedException | ExecutionException e)
				{
					batchVerified = false;
					e.printStackTrace();
				}

				batchContinue = batch.getRemoteBitstreamErrorContinue();
				for(Verifier.Problem problem : problems)
				{
					if(problem.isError()) {
						if (problem.isFlagged()) {
							batchVerified = false;
							if (batch.hasIgnoredRows()) {
								continue;
							}
							else {
								break;
							}
						}
						else {
							batchContinue = false;
							batchVerified = false;
							break;
						}
					}
				}

				if (getNextVerifier() == null) {
					if(batchVerified)
					{
						transitionToVerifySuccess();
					}
					else
					{
						if (batchContinue) {
							transitionToVerifySuccessIgnoreErrors();
						}
						else {
							transitionToVerifyFailed();
						}
					}

					unlockVerifyButtons();
					return;
				}

				currentVerifier = super.getNextVerifier();
				if (currentVerifier != null) {
					currentVerifier.execute();
				}
			}

			@Override
			protected void process(List<VerifierBackground.VerifierUpdates> updates) {
				if (updates.size() == 0) {
					return;
				}

				VerifierBackground.VerifierUpdates update = updates.get(updates.size() - 1);
				if (update != null && update.getTotal() > 0) {
					statusIndicator.setText("Batch Status:\n Unverified\n File Exists?\n " + update.getProcessed() + " / " + update.getTotal());
				}
			}
		};

		VerifierBackground validSchemaVerifier = new ValidSchemaNameVerifierImpl() {
			@Override
			public List<Problem> doInBackground() {
				statusIndicator.setText("Batch Status:\n Unverified\n Schema Name?\n 0 / " + batch.getItems().size());
				return verify(batch, console, flagPanel);
			}

			@Override
			public void done() {
				if (isCancelled()) {
					cancelVerifyCleanup();
					return;
				}

				List<Verifier.Problem> problems = new ArrayList<Verifier.Problem>();
				if (batchVerified == null) {
					batchVerified = true;
				}

				try
				{
					problems = get();
				} catch (InterruptedException | ExecutionException e)
				{
					batchVerified = false;
					e.printStackTrace();
				}

				batchContinue = batch.getRemoteBitstreamErrorContinue();
				for(Verifier.Problem problem : problems)
				{
					if(problem.isError()) {
						if (problem.isFlagged()) {
							batchVerified = false;
							if (batch.hasIgnoredRows()) {
								continue;
							}
							else {
								break;
							}
						}
						else {
							batchContinue = false;
							batchVerified = false;
							break;
						}
					}
				}

				if (getNextVerifier() == null) {
					if(batchVerified)
					{
						transitionToVerifySuccess();
					}
					else
					{
						if (batchContinue) {
							transitionToVerifySuccessIgnoreErrors();
						}
						else {
							transitionToVerifyFailed();
						}
					}

					unlockVerifyButtons();
					return;
				}

				currentVerifier = super.getNextVerifier();
				if (currentVerifier != null) {
					currentVerifier.execute();
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
			batch.clearIgnoredRows();
		}
		verifiers.clear();
		verifiers.add(fileExistsVerifier);

		fileExistsVerifier.setNextVerifier(validSchemaVerifier);
		verifiers.add(validSchemaVerifier);
	}

	private void createBatchDetailsTab() 
	{
		sourceDirectoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		outputDirectoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		metadataInputFileName = "";
		sourceDirectoryName = "";
		outputDirectoryName = "";
//		metadataInputFileName = "/Users/jcreel/Development/SAF/testo.csv";
//		sourceDirectoryName = "/Users/jcreel/Development/SAF/testfiles";
//		outputDirectoryName = "/Users/jcreel/Development/SAF/test-output";
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
		
		
		chooseInputFileBtn.addActionListener
		( 
			new ActionListener()
			{
			    public void actionPerformed( final ActionEvent e )
			    {   
			        if( inputFileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION )
			        {
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
							userAgentField.setEnabled(false);

							writeSAFBtn.setEnabled(false);
							writeSAFBtn.setText("No batch loaded");

							actionStatus = ActionStatus.NONE_LOADED;
						}
			        }
			    }
			}
		);
		
		
		chooseSourceDirectoryBtn.addActionListener
		(
			new ActionListener()
			{
			    public void actionPerformed( final ActionEvent e )
			    {
			    	if( sourceDirectoryChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION )
			    	{
			    		console.append("You have selected this source directory: " + sourceDirectoryChooser.getSelectedFile().getName() + "\n");
			    		sourceDirectoryName = sourceDirectoryChooser.getSelectedFile().getAbsolutePath();
			    		sourceDirectoryNameField.setText(sourceDirectoryName);			    		
			    	}
			    }
			}
		);
		
		
		chooseOutputDirectoryBtn.addActionListener
		(
			new ActionListener()
			{
			    public void actionPerformed( final ActionEvent e )
			    {
			    	if( outputDirectoryChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION )
			    	{
			    		console.append("You have selected this target directory: " + outputDirectoryChooser.getSelectedFile().getName() + "\n");
			    		outputDirectoryName = outputDirectoryChooser.getSelectedFile().getAbsolutePath();
			    		outputDirectoryNameField.setText(outputDirectoryName);			    		
			    	}
			    }
			}
		);
		
		
		loadBatchBtn.addActionListener
		(
			new ActionListener()
			{
				public void actionPerformed( final ActionEvent e )
				{
					if(actionStatus.equals(ActionStatus.NONE_LOADED) || 
					   actionStatus.equals(ActionStatus.LOADED) || 
					   actionStatus.equals(ActionStatus.WRITTEN) ||
					   actionStatus.equals(ActionStatus.VERIFIED) ||
					   actionStatus.equals(ActionStatus.FAILED_VERIFICATION))
					{
						// verifiers must be re-created if they are already processed (or canceled).
						createVerifiers();

						//Attempt to load the batch and set status to LOADED if successful, NONE_LOADED if failing
						console.append("\nLoading batch for " + metadataInputFileName + "...\n");
						batch = processor.loadBatch(metadataInputFileName, sourceDirectoryName, outputDirectoryName, console);
						if(batch == null)
						{
							console.append("\nFAILED TO READ BATCH.\n\n");
							actionStatus = ActionStatus.NONE_LOADED;
							
							statusIndicator.setText("\nNo batch loaded.\n\n");
							statusIndicator.setForeground(Color.white);
							statusIndicator.setBackground(Color.blue);
							
							statusIndicator.setText("\nPlease load a batch for processing.\n\n");
							actionStatusField.setForeground(Color.white);
							actionStatusField.setBackground(Color.blue);

							ignoreFilesBox.setEnabled(false);
							continueOnRemoteErrorBox.setEnabled(false);
							itemProcessDelayField.setEnabled(false);
							userAgentField.setEnabled(false);

							return;
						}

						console.append("\nBatch successfully loaded.\n\n");

						transitionToLoaded();
					}										
				}
			}
		);		
						
		
		writeSAFBtn.addActionListener
		(
			new ActionListener()
			{
				public void actionPerformed( final ActionEvent e )
				{
	
					if(actionStatus.equals(ActionStatus.VERIFIED))
					{
						lockThreadSensitiveControls();
						writeSAFBtn.setText("Writing..");
						writeCancelBtn.setEnabled(true);

						console.append("Parsing CSV file and writing output to DC XML files...\n");
						statusIndicator.setText("Batch Status:\n Verified\n Written:\n 0 / " + batch.getItems().size());

						currentWriter = new ImportDataWriter() {
							@Override
							public void done() {
								writeSAFBtn.setText("Write SAF data now!");
								currentWriter = null;
								writeCancelBtn.setEnabled(false);
								unlockThreadSensitiveControls();

								if (isCancelled()) {
									return;
								}

								try
								{
									if (get()) {
										transitionToWritten();
									}
									else {
										transitionToWrittenFailed();
									}
								} catch (InterruptedException | ExecutionException e)
								{
									e.printStackTrace();
								}
							}

							@Override
							protected void process(List<ImportDataWriter.WriterUpdates> updates) {
								if (updates.size() == 0) {
									return;
								}

								ImportDataWriter.WriterUpdates update = updates.get(updates.size() - 1);
								if (update != null && update.getTotal() > 0) {
									statusIndicator.setText("Batch Status:\n Verified\n Written:\n " + update.getProcessed() + " / " + update.getTotal());
								}
							}
						};

						currentWriter.setBatch(batch);
						currentWriter.setConsole(console);
						currentWriter.setFlags(flagPanel);
						currentWriter.execute();
					}
					else if(actionStatus.equals(ActionStatus.LOADED) || actionStatus.equals(ActionStatus.FAILED_VERIFICATION))
					{
						console.append("\nPlease verify the batch before writing.\n\n");
					}
					else if(actionStatus.equals(ActionStatus.WRITTEN) || actionStatus.equals(ActionStatus.NONE_LOADED))
					{
						//nothing to do
						console.append("\nNo new SAF data to write.\n\n");
					}
				}
			}
		);

		writeCancelBtn.addActionListener
		(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					if (currentWriter == null) {
						return;
					}

					currentWriter.cancel(false);
				}
			}
		);
	}
	
	private void createAdvancedSettingsTab()
	{
		advancedSettingsTab.setLayout(new BoxLayout(advancedSettingsTab, BoxLayout.Y_AXIS));
		advancedSettingsTab.add(ignoreFilesBox);
		advancedSettingsTab.add(continueOnRemoteErrorBox);

		tabs.addTab("Advanced Settings", advancedSettingsTab);
		
		ignoreFilesBox.addMouseListener
		(
			new MouseListener()
			{
				public void mouseClicked(MouseEvent e) 
				{
					if (!ignoreFilesBox.isEnabled()) {
						// checkbox mouse clicks still trigger even when set to disabled.
						return;
					}

					if(ignoreFilesBox.isSelected())
					{
						console.append("Bitstream (content) files will be ommitted from generated SAF.\n");
						batch.setIgnoreFiles(true);
					}
					else
					{
						console.append("Bitstream (content) files will be included in generated SAF.\n");
						batch.setIgnoreFiles(false);
					}
				}

				public void mousePressed(MouseEvent e) {}

				public void mouseReleased(MouseEvent e) {}

				public void mouseEntered(MouseEvent e) {}

				public void mouseExited(MouseEvent e) {}
			}
		);

		continueOnRemoteErrorBox.addMouseListener
		(
			new MouseListener()
			{
				public void mouseClicked(MouseEvent e)
				{
					if (!continueOnRemoteErrorBox.isEnabled()) {
						// checkbox mouse clicks still trigger even when set to disabled.
						return;
					}

					if(continueOnRemoteErrorBox.isSelected())
					{
						console.append("Allowing write even if the remote bitstream verification flags an error.\n");
						batch.setRemoteBitstreamErrorContinue(true);
					}
					else
					{
						console.append("Denying write if the remote bitstream verification flags an error.\n");
						batch.setRemoteBitstreamErrorContinue(false);
					}
				}

				public void mousePressed(MouseEvent e) {}

				public void mouseReleased(MouseEvent e) {}

				public void mouseEntered(MouseEvent e) {}

				public void mouseExited(MouseEvent e) {}
			}
		);

		JPanel itemProcessDelayPanel = new JPanel();
		itemProcessDelayPanel.add(itemProcessDelayLabel);
		itemProcessDelayPanel.add(itemProcessDelayField);
		advancedSettingsTab.add(itemProcessDelayPanel);

		JPanel userAgentPanel = new JPanel();
		userAgentPanel.add(userAgentLabel);
		userAgentPanel.add(userAgentField);
		advancedSettingsTab.add(userAgentPanel);

		// initialize as disabled so that it will only be enable once a batch is assigned.
		ignoreFilesBox.setEnabled(false);
		continueOnRemoteErrorBox.setEnabled(false);
		itemProcessDelayField.setEnabled(false);
		itemProcessDelayField.getDocument().addDocumentListener
		(
			new DocumentListener()
			{
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
			}
		);

		itemProcessDelayField.addFocusListener(
			new FocusListener() {

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
						}
						else {
							batch.setItemProcessDelay(delay);
							console.append("The Item Process Delay is now set to " + delay + " milliseconds.\n");
						}

						itemProcessDelayFieldChangeStatus = FieldChangeStatus.NO_CHANGES;
					}
				}
			}
		);

		userAgentField.setEnabled(false);
		userAgentField.getDocument().addDocumentListener
		(
			new DocumentListener()
			{
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
			}
		);

		userAgentField.addFocusListener(
			new FocusListener() {

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
			}
		);
	}

	private void createFlagTableTab()
	{
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

		flagsDownloadCsvBtn.addActionListener
		(
			new ActionListener()
			{
			    public void actionPerformed(final ActionEvent e)
			    {
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
					try
					{
						BufferedWriter writer = Files.newBufferedWriter(Paths.get(csvFilePath));
						flagPanel.exportToCSV(writer);
						console.append("Wrote to CSV File: " + csvFilePath + ".\n");
					} catch (IOException e1)
					{
						console.append("Unable to write CSV file " + csvFilePath + ", reason: " + e1.getMessage() + ".\n");
					}
			    }
			}
		);

		flagsReportSelectedBtn.addActionListener
		(
			new ActionListener()
			{
			    public void actionPerformed(final ActionEvent e)
			    {
					if (flagPanel.getRowCount() == 0) {
						console.append("Flag list is empty.\n");
						return;
					}

					Flag row = flagPanel.getSelected();
					if (row == null) {
						console.append("No valid row is selected.\n");
						return;
					}

					Problem problem = new Problem(true, row.getCell(Columns.DESCRIPTION));
					console.append("\nFlag Name: " + row.getCell(Columns.FLAG) + "\n");
					console.append("Flag Description: " + problem.toString() + "\n");

					if (!row.getCell(Columns.AUTHORITY).isEmpty() ) {
						console.append("Flag Authority: " + row.getCell(Columns.AUTHORITY) + "\n");
					}

					if (!row.getCell(Columns.URL).isEmpty()) {
						console.append("Flag URL: " + row.getCell(Columns.URL) + "\n");
					}

					if (row.getCell(Columns.COLUMN).isEmpty()) {
						if (!row.getCell(Columns.ROW).isEmpty()) {
							console.append("Flag Row: " + row.getCell(Columns.ROW) + "\n");
						}
					}
					else if (row.getCell(Columns.ROW).isEmpty()) {
						console.append("Flag Column: " + row.getCell(Columns.COLUMN) + "\n");
					}
					else {
						console.append("Flag Column, Row: " + row.getCell(Columns.COLUMN) + ", " + row.getCell(Columns.ROW) + "\n");
					}
			    }
			}
		);
	}

	private void initializeGUIState()
	{
		actionStatus = ActionStatus.NONE_LOADED;
		batch=null;
		
		//actionStatusfield
		actionStatusField.setForeground(Color.white);
		actionStatusField.setBackground(Color.blue);
		
		//loadBatchBtn
		loadBatchBtn.setText("Load specified batch now!");
		
		//writeSAFBtn
		writeSAFBtn.setText("No batch loaded");
		
		//statusIndicator
		statusIndicator.setForeground(Color.white);
		statusIndicator.setBackground(Color.blue);
		statusIndicator.setText("No batch loaded");
		
	}
	
	private void transitionToLoaded()
	{
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
		userAgentField.setEnabled(true);

		batchContinue = false;
	}
	

	private void transitionToVerifySuccess()
	{
		console.append("Batch verified.\n");

		actionStatus = ActionStatus.VERIFIED;
		actionStatusField.setText("Your batch has been verified!");
		actionStatusField.setBackground(Color.green);

		statusIndicator.setText("Batch Status:\nVerified");
		statusIndicator.setForeground(Color.black);
		statusIndicator.setBackground(Color.green);

		writeSAFBtn.setEnabled(true);
		writeSAFBtn.setText("Write SAF data now!");
	}
	

	private void transitionToVerifySuccessIgnoreErrors()
	{
		console.append("Batch failed verification.\n");

		actionStatus = ActionStatus.VERIFIED;
		actionStatusField.setText("Your batch failed to verify, continuing.");
		actionStatusField.setBackground(Color.orange);

		statusIndicator.setText("Batch Status:\nUnverified,\nContinuing Anyway");
		statusIndicator.setForeground(Color.black);
		statusIndicator.setBackground(Color.orange);

		writeSAFBtn.setEnabled(true);
		writeSAFBtn.setText("Write SAF data now!");
	}

	private void transitionToVerifyFailed()
	{
		console.append("Batch failed verification.\n");
		actionStatus = ActionStatus.FAILED_VERIFICATION;
		actionStatusField.setText("Your batch failed to verify.");
		writeSAFBtn.setText("No valid batch.");
		actionStatusField.setBackground(Color.red);
		statusIndicator.setText("Batch Status:\n Unverified");
	}

	private void transitionToWritten()
	{

		loadBatchBtn.setText("Reload batch as specified");
		writeSAFBtn.setEnabled(false);
		actionStatusField.setForeground(Color.black);
		actionStatusField.setBackground(Color.white);
		actionStatusField.setText("Batch SAF written.");
		statusIndicator.setText("Batch status:\n Written");
		statusIndicator.setBackground(Color.white);
		statusIndicator.setForeground(Color.black);
		actionStatus = ActionStatus.WRITTEN;
	}

	private void transitionToWrittenFailed()
	{
		statusIndicator.setBackground(Color.red);
		statusIndicator.setText("Batch Status:\n verified\n write failed");
	}
	
	private void lockVerifyButtons() {
		verifyBatchBtn.setText("Verifying..");
		lockThreadSensitiveControls();
	}

	private void unlockVerifyButtons() {
		verifyBatchBtn.setText("Verify Batch");
		unlockThreadSensitiveControls();
	}

	private void cancelVerifyCleanup() {
		currentVerifier = null;
		unlockVerifyButtons();
		console.append("Validation process has been cancelled.\n");
		statusIndicator.setText("Batch Status:\n Unverified");

		// verifiers must be re-created after canceling.
		createVerifiers();
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
			userAgentField.setEnabled(false);
		}
	}

	private void unlockThreadSensitiveControls() {
		verifyCancelBtn.setEnabled(false);
		verifyBatchBtn.setEnabled(true);
		loadBatchBtn.setEnabled(true);
		chooseInputFileBtn.setEnabled(true);
		chooseSourceDirectoryBtn.setEnabled(true);
		chooseOutputDirectoryBtn.setEnabled(true);

		if (actionStatus == ActionStatus.VERIFIED) {
			writeSAFBtn.setEnabled(true);
		}

		if (actionStatus != ActionStatus.NONE_LOADED) {
			ignoreFilesBox.setEnabled(true);
			continueOnRemoteErrorBox.setEnabled(true);
			itemProcessDelayField.setEnabled(true);
			userAgentField.setEnabled(true);
		}
	}
}
