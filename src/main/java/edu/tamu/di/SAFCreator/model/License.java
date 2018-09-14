package edu.tamu.di.SAFCreator.model;

import java.io.File;
import java.io.IOException;

import edu.tamu.di.SAFCreator.Util;

public class License {
    private String filename;
    private String bundleName;
    private String licenseText;

    public String getBundleName() {
        return bundleName;
    }

    public String getContentsManifestLine() {

        return filename + "\tBUNDLE:" + bundleName + "\n";
    }

    public String getFilename() {
        return filename;
    }

    public String getLicenseText() {
        return licenseText;
    }

    public void setBundleName(String bundleName) {
        this.bundleName = bundleName;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setLicenseText(String licenseText) {
        this.licenseText = licenseText;
    }

    public void writeToItem(Item item) {
        File licenseFile = new File(item.getSAFDirectory() + "/" + filename);

        try {
            if (!licenseFile.exists()) {

                licenseFile.createNewFile();
            }

            Util.setFileContents(licenseFile, licenseText);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
