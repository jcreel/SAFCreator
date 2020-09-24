package edu.tamu.di.SAFCreator.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import edu.tamu.di.SAFCreator.enums.BatchStatus;

public class Batch {
    private String name;
    private BatchStatus status;
    private File inputFilesDir;
    private File outputSAFDir;
    private List<Item> items = new ArrayList<Item>();
    private License license;
    private List<ColumnLabel> labels = new ArrayList<ColumnLabel>();
    private Boolean ignoreFiles = false;
    private Boolean processUri = false;
    private Boolean flattenDirectories = true;
    private Boolean allowSelfSigned = true;
    private Boolean remoteBitstreamErrorContinue = false;
    private int itemProcessDelay = 0;
    private int remoteFileTimeout = 10000;
    private String userAgent = null;
    private List<Integer> ignoreRows = new ArrayList<Integer>();
    private List<Integer> failedRows = new ArrayList<Integer>();
    private String action = "";

    /**
     * Add a new item to the list of items contained within this batch.
     *
     */
    public void addItem(Item item) {
        items.add(item);
    }

    /**
     * Removes all failed row values.
     */
    public void clearFailedRows() {
        failedRows.clear();
    }

    /**
     * Removes all assigned ignore row values.
     */
    public void clearIgnoredRows() {
        ignoreRows.clear();
    }

    /**
     * Flag a specific batch row as failed.
     *
     * @param row the number of the row to ignore.
     */
    public void failedRow(Integer row) {
        failedRows.add(row);
    }

    /**
     * @return The action associated with this batch.
     */
    public String getAction() {
        return action;
    }

    /**
     * @return Whether or not self-signed certificates are always allowed.
     */
    public Boolean getAllowSelfSigned() {
        return allowSelfSigned;
    }

    /**
     * @return Whether or not to ignore files.
     */
    public Boolean getIgnoreFiles() {
        return ignoreFiles;
    }

    /**
     * @return The base directory from where to begin looking for all associated file content for this batch.
     */
    public File getinputFilesDir() {
        return inputFilesDir;
    }

    /**
     * @return The item process delay time (in milliseconds).
     */
    public int getItemProcessDelay() {
        return itemProcessDelay;
    }

    /**
     * @return A list of all items associated with this batch.
     */
    public List<Item> getItems() {
        return items;
    }

    public List<ColumnLabel> getLabels() {
        return labels;
    }

    public License getLicense() {
        return license;
    }

    /**
     * @return The user supplied name of this batch
     */
    public String getName() {
        return name;
    }

    public File getOutputSAFDir() {
        return outputSAFDir;
    }

    public Boolean getProcessUri() {
        return processUri;
    }

    /**
     * @return The remote bitstream error continue status. When true to enable ignoring errors. When false to stop on errors.
     */
    public Boolean getRemoteBitstreamErrorContinue() {
        return remoteBitstreamErrorContinue;
    }

    /**
     * @return The remote file timeout time (in milliseconds).
     */
    public int getRemoteFileTimeout() {
        return remoteFileTimeout;
    }

    /**
     * @return The current evaluation status of the batch.
     */
    public BatchStatus getStatus() {
        return status;
    }

    /**
     * @return The item process delay time (in milliseconds).
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Reports whether or not any rows are flagged as failed.
     *
     * @return true if any row is assigned to be ignored, false otherwise.
     */
    public boolean hasFailedRows() {
        return !failedRows.isEmpty();
    }

    /**
     * Reports whether or not any rows are assigned to be ignored.
     *
     * @return true if any row is assigned to be ignored, false otherwise. When remoteBitstreamErrorContinue is false, this always returns false.
     */
    public boolean hasIgnoredRows() {
        if (!remoteBitstreamErrorContinue) {
            return false;
        }
        return !ignoreRows.isEmpty();
    }

    /**
     * Ignore a specific batch row.
     *
     * A particular row number for the items list can be ignored. This is intended to be used when remoteBitstreamErrorContinue is true. If any single column is invalid, the entire row is to be ignored.
     *
     * @param row the number of the row to ignore.
     */
    public void ignoreRow(Integer row) {
        ignoreRows.add(row);
    }

    /**
     * Check to see if a row is flagged as failed.
     *
     * @param row the number of the row that has failed.
     *
     * @return true if failed, false otherwise.
     */
    public boolean isFailedRow(Integer row) {
        return failedRows.contains(row);
    }

    /**
     * Check to see if a row is flagged to be ignored.
     *
     * @param row the number of the row that may be ignored.
     *
     * @return true if ignored, false otherwise. When remoteBitstreamErrorContinue is false, this always returns false.
     */
    public boolean isIgnoredRow(Integer row) {
        if (!remoteBitstreamErrorContinue) {
            return false;
        }
        return ignoreRows.contains(row);
    }

    public void restrictItemsToGroup(String groupName) {
        for (Item item : items) {
            for (Bundle bundle : item.getBundles()) {
                for (Bitstream bitstream : bundle.getBitstreams()) {
                    bitstream.setReadPolicyGroupName(groupName);
                }
            }
        }

    }

    /**
     * Set the action status associated with this batch.
     *
     * @param action The new action name.
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Set the whether or not to always allow self-signed SSL certificates.
     *
     * @param allowSelfSigned
     */
    public void setAllowSelfSigned(Boolean allowSelfSigned) {
        this.allowSelfSigned = allowSelfSigned;
    }

    /**
     * Set the whether or not to ignore files.
     *
     * @param ignoreFiles
     */
    public void setIgnoreFiles(Boolean ignoreFiles) {
        this.ignoreFiles = ignoreFiles;
    }

    /**
     * Set the base directory from where to begin searching for any associated files.
     *
     * @param directory The new base directory.
     */
    public void setinputFilesDir(File directory) {
        inputFilesDir = directory;
    }

    public void setinputFilesDir(String directoryName) {
        inputFilesDir = new File(directoryName);
    }

    /**
     * Set the item process delay time (in milliseconds).
     *
     * @param itemProcessDelay The process delay time.
     */
    public void setItemProcessDelay(int itemProcessDelay) {
        this.itemProcessDelay = itemProcessDelay;
    }

    /**
     * Set the item process delay time (in milliseconds).
     *
     * @param itemProcessDelay The process delay time.
     */
    public void setItemProcessDelay(String itemProcessDelay) {
        this.itemProcessDelay = Integer.parseInt(itemProcessDelay);
    }

    public void setLabels(List<ColumnLabel> labels) {
        this.labels = labels;
    }

    public void setLicense(String filename, String bundleName, String licenseText) {
        license = new License();
        license.setFilename(filename);
        license.setBundleName(bundleName);
        license.setLicenseText(licenseText);
    }

    /**
     * Set the user supplied name of this batch.
     *
     * @param name The new name.
     */
    public void setName(String name) {
        this.name = name;
    }

    public void setOutputSAFDir(File outputSAFDir) {
        this.outputSAFDir = outputSAFDir;
    }

    public void setOutputSAFDir(String outputSAFDirName) {
        outputSAFDir = new File(outputSAFDirName);
    }

    public void setProcessUri(Boolean processUri) {
        this.processUri = processUri;
    }

    /**
     * Set the remote bitstream error continue status.
     *
     * @param remoteBitstreamErrorContinue Set to true to enable ignoring errors, false to stop on errors.
     */
    public void setRemoteBitstreamErrorContinue(Boolean remoteBitstreamErrorContinue) {
        this.remoteBitstreamErrorContinue = remoteBitstreamErrorContinue;
    }

    /**
     * Set the remote file timeout time (in milliseconds).
     *
     * @param itemProcessDelay The process delay time.
     */
    public void setRemoteFileTimeout(int remoteFileTimeout) {
        this.remoteFileTimeout = remoteFileTimeout;
    }

    /**
     * Set the remote file timeout time (in milliseconds).
     *
     * @param itemProcessDelay The process delay time.
     */
    public void setRemoteFileTimeout(String remoteFileTimeout) {
        this.remoteFileTimeout = Integer.parseInt(remoteFileTimeout);
    }

    /**
     * Set the evaluation status of the batch.
     */
    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    /**
     * Set the user agent string.
     *
     * @param itemProcessDelay The process delay time.
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void unsetLicense() {
        license = null;
    }

    public void setFlattenDirectories(boolean b) {
        flattenDirectories = b;

    }

    public Boolean getFlattenDirectories() {
        return flattenDirectories;
    }

    public void setFlattenDirectories(Boolean flattenDirectories) {
        this.flattenDirectories = flattenDirectories;
    }
}
