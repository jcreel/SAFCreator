package edu.tamu.di.SAFCreator.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import edu.tamu.di.SAFCreator.model.CellDatumImpl;

public class Bitstream extends CellDatumImpl
{
	private static int TimeoutConnection = 5000;
	private static int TimeoutRead = 5000;

	private Bundle bundle;
	private URI source;
	private String relativePath;
	private File destination;
	private String readPolicyGroupName = null;

	public Bundle getBundle() {
		return bundle;
	}

	public void setBundle(Bundle bundle) {
		this.bundle = bundle;
	}

	public URI getSource() {
		return source;
	}

	public void setSource(URI source) {
		this.source = source;
	}

	public File getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = new File(destination);
	}

	public String getReadPolicyGroupName() {
		return readPolicyGroupName;
	}

	public void setReadPolicyGroupName(String readPolicyGroupName) {
		this.readPolicyGroupName = readPolicyGroupName;
	}
	
	public String getContentsManifestLine()
	{
		String line = getRelativePathForwardSlashes() + "\tbundle:" + bundle.getName().trim() + (readPolicyGroupName==null?"\n":"\tpermissions:-r "+readPolicyGroupName)+"\n"; 
		return line;
	}
	
	public void copyMe()
	{
		try {
			// Avoid writing to existing files, primarily to avoid potential network overhead of downloading remote files.
			if (destination.exists()) {
				return;
			}

			if (source.isAbsolute() && !source.getScheme().toString().equalsIgnoreCase("file")) {
				URL url = source.toURL();
				if (source.getScheme().toString().equalsIgnoreCase("ftp")) {
					FTPClient conn = new FTPClient();

					try {
						conn.connect(source.toURL().getHost());
						conn.setFileType(FTP.BINARY_FILE_TYPE);
						conn.enterLocalPassiveMode();
						conn.login("anonymous", "");

						String decodedUrl = URLDecoder.decode(source.toURL().getPath(), "ASCII");
						OutputStream output = new FileOutputStream(destination);
						conn.retrieveFile(decodedUrl, output);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					try {
						if (conn.isConnected()) {
							conn.disconnect();
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else {
					FileUtils.copyURLToFile(url, destination, TimeoutConnection, TimeoutRead);
				}
		    }
			else {
				File file = new File(source.getPath());
				FileUtils.copyFile(file, destination);
		    }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setRelativePath(String value) {
		relativePath = value;
		destination = new File(bundle.getItem().getSAFDirectory()+"/"+relativePath);
	}
	
	public String getRelativePath()
	{
		return relativePath;
	}
	
	public String getRelativePathForwardSlashes()
	{
		String relativePathForwardSlashes = relativePath.replace(File.separatorChar, '/');
		return relativePathForwardSlashes;
	}

}
