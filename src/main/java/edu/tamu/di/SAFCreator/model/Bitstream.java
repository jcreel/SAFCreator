package edu.tamu.di.SAFCreator.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import edu.tamu.di.SAFCreator.model.CellDatumImpl;
import edu.tamu.di.SAFCreator.model.Verifier.Problem;

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

	public void copyMe(List<Problem> problems)
	{
		// Avoid writing to existing files, primarily to avoid potential network overhead of downloading remote files.
		if (destination.exists()) {
			return;
		}

		if (source.isAbsolute() && !source.getScheme().toString().equalsIgnoreCase("file")) {
			int itemProcessDelay = bundle.getItem().getBatch().getItemProcessDelay();
			if (itemProcessDelay > 0) {
				try
				{
					TimeUnit.MILLISECONDS.sleep(itemProcessDelay);
				} catch (InterruptedException e)
				{
					Problem problem = new Problem(getRow(), getColumn(), false, "Failed to sleep for " + itemProcessDelay + " milliseconds, reason: " + e.getMessage() + ".");
					problems.add(problem);
				}
			}

			try
			{
				URL url = source.toURL();
				if (source.getScheme().toString().equalsIgnoreCase("ftp")) {
					FTPClient conn = new FTPClient();

					try {
						conn.setConnectTimeout(TimeoutConnection);
						conn.setDataTimeout(TimeoutRead);
						conn.connect(source.toURL().getHost());
						conn.setFileType(FTP.BINARY_FILE_TYPE);
						conn.enterLocalPassiveMode();
						conn.login("anonymous", "");

						String decodedUrl = URLDecoder.decode(source.toURL().getPath(), "ASCII");
						OutputStream output = new FileOutputStream(destination);
						conn.retrieveFile(decodedUrl, output);
					} catch (IOException e) {
						Problem problem = new Problem(getRow(), getColumn(), true, "Source file URL " + source.toString() + " had a connection error, reason: " + e.getMessage() + ".");
						problems.add(problem);
					}

					try {
						if (conn.isConnected()) {
							conn.disconnect();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				else {
					String userAgent = bundle.getItem().getBatch().getUserAgent();
					HttpClient client = new HttpClient();
					GetMethod get = null;
					try
					{
						client.getHttpConnectionManager().getParams().setConnectionTimeout(TimeoutConnection);
						get = new GetMethod(url.toString());
						if (userAgent != null) {
							get.addRequestHeader("User-Agent", userAgent);
						}
						get.setFollowRedirects(true);
						client.executeMethod(get);

						// We could get the filename from response.getFirstHeader('Content-Disposition').getValue();
						// however, that would have to be sanitized, so just stick to simple pre-defined filenames until otherwise requested.
						InputStream input = get.getResponseBodyAsStream();
						FileUtils.copyToFile(input, destination);
						input.close();
					} catch (HttpException e)
					{
						Problem problem = new Problem(getRow(), getColumn(), true, "Source file URL " + source.toString() + " had an HTTP error, reason: " + e.getMessage() + ".");
						problems.add(problem);
					} catch (IOException e)
					{
						Problem problem = new Problem(getRow(), getColumn(), true, "Source file URL " + source.toString() + " had a connection error, reason: " + e.getMessage() + ".");
						problems.add(problem);
					} finally
					{
						if (get != null) {
							get.releaseConnection();
							client.getHttpConnectionManager().closeIdleConnections(TimeoutConnection);
						}
					}
				}
			} catch (MalformedURLException e)
			{
				Problem problem = new Problem(getRow(), getColumn(), true, "Source file URL " + source.toString() + " is invalid, reason: " + e.getMessage() + ".");
				problems.add(problem);
			}
	    }
		else {
			try
			{
				File file = new File(source.getPath());
				FileUtils.copyFile(file, destination);
			} catch (IOException e)
			{
				Problem problem = new Problem(getRow(), getColumn(), true, "Source file path " + source.toString() + " failed to copy, reason: " + e.getMessage() + ".");
				problems.add(problem);
			}
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
