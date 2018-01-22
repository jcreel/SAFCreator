package edu.tamu.di.SAFCreator.verify;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.JTextArea;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import edu.tamu.di.SAFCreator.model.Batch;
import edu.tamu.di.SAFCreator.model.Bitstream;
import edu.tamu.di.SAFCreator.model.Bundle;
import edu.tamu.di.SAFCreator.model.Item;
import edu.tamu.di.SAFCreator.model.VerifierBackground;

public class FilesExistVerifierImpl extends VerifierBackground {
	private static int TimeoutConnection = 5000;
	private static int TimeoutRead = 5000;
	private static int MaxRedirects = 20;

	@Override
	public List<Problem> verify(Batch batch) 
	{
		return verify(batch, null);
	}

	@Override
	public List<Problem> verify(Batch batch, JTextArea console)
	{
		List<Problem> missingFiles = new ArrayList<Problem>();
		
		if( ! batch.getIgnoreFiles())
		{
			int totalItems = batch.getItems().size();
			int itemCount = 0;
			for(Item item : batch.getItems())
			{
				for(Bundle bundle : item.getBundles())
				{
					for(Bitstream bitstream : bundle.getBitstreams())
					{
						URI source = bitstream.getSource();
						if (source.isAbsolute() && !source.getScheme().toString().equalsIgnoreCase("file"))
						{
							if (source.getScheme().toString().equalsIgnoreCase("ftp")) {
								FTPClient conn = new FTPClient();

								try {
									int itemProcessDelay = batch.getItemProcessDelay();
									if (itemProcessDelay > 0) {
										TimeUnit.MILLISECONDS.sleep(itemProcessDelay);
									}

									conn.setConnectTimeout(TimeoutConnection);
									conn.setDataTimeout(TimeoutRead);
									conn.connect(source.toURL().getHost());
									conn.enterLocalPassiveMode();
									conn.login("anonymous", "");

									String decodedUrl = URLDecoder.decode(source.toURL().getPath(), "ASCII");
									FTPFile[] files = conn.listFiles(decodedUrl);

									if (files.length == 0) {
										Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumn(), generatesError(), "FTP file URL " + source.toString() + " was not found.");
										missingFiles.add(missingFile);
										if (console != null) console.append(missingFile.toString()+"\n");
									}
								} catch (IOException e) {
									Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumn(), generatesError(), "Connection problem for FTP file URL " + source.toString() + ", message: " + e.getMessage() + ".");
									missingFiles.add(missingFile);
									if (console != null) console.append(missingFile.toString()+"\n");
								} catch (InterruptedException e) {
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
								int itemProcessDelay = batch.getItemProcessDelay();
								if (itemProcessDelay > 0) {
									try
									{
										TimeUnit.MILLISECONDS.sleep(itemProcessDelay);
									} catch (InterruptedException e)
									{
										Problem warning = new Problem(bitstream.getRow(), bitstream.getColumn(), false, "Failed to sleep for " + itemProcessDelay + " milliseconds, reason: " + e.getMessage() + ".");
										missingFiles.add(warning);
										if (console != null) console.append(warning.toString()+"\n");
									}
								}

								String userAgent = batch.getUserAgent();
								HttpClient client = new HttpClient();
								HeadMethod head = null;
								try
								{
									head = new HeadMethod(source.toURL().toString());
									if (userAgent != null) {
										head.addRequestHeader("User-Agent", userAgent);
									}
									head.setFollowRedirects(true);
									int response = client.executeMethod(head);

									if (response == HttpURLConnection.HTTP_SEE_OTHER || response == HttpURLConnection.HTTP_MOVED_PERM || response == HttpURLConnection.HTTP_MOVED_TEMP) {
										int totalRedirects = 0;
										HashSet<String> previousUrls = new HashSet<String>();
										previousUrls.add(source.toString());
										URL previousUrl = source.toURL();

										do {
											if (totalRedirects++ > MaxRedirects) {
												Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumn(), true, "HTTP URL " + source.toString() + " redirected too many times.");
												missingFiles.add(missingFile);
												if (console != null) console.append(missingFile.toString()+"\n");
												break;
											}

											Header redirectTo = head.getResponseHeader("Location");
											if (redirectTo == null) {
												Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumn(), true, "HTTP URL " + source.toString() + " was redirect without a destination URL.");
												missingFiles.add(missingFile);
												if (console != null) console.append(missingFile.toString()+"\n");
												break;
											}

											String redirectToLocation = redirectTo.getValue();
											URI redirectToUri = null;
											try {
												redirectToUri = new URI(redirectToLocation);
											}
											catch (URISyntaxException e)
											{
												Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumn(), true, "HTTP URL " + source.toString() + " redirected to invalid URL " + head.getResponseHeader("Location").getValue() + ", reason: " + e.getMessage() + ".");
												missingFiles.add(missingFile);
												if (console != null) console.append(missingFile.toString()+"\n");
												break;
											}

											String authority = redirectToUri.getAuthority();
											String scheme = redirectToUri.getScheme();
											if (authority == null || authority.isEmpty()) {
												if (!redirectToLocation.startsWith("/")) {
													redirectToLocation = "/" + redirectToLocation;
												}
												redirectToLocation = previousUrl.getAuthority() + redirectToLocation;
												if (scheme == null || scheme.isEmpty()) {
													if (redirectToLocation.startsWith("//")) {
													redirectToLocation = "http:" + redirectToLocation;
													}
													else {
														redirectToLocation = "http://" + redirectToLocation;
													}
												}
												try {
													redirectToUri = new URI(redirectToLocation);
												}
												catch (URISyntaxException e)
												{
													Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumn(), true, "HTTP URL " + source.toString() + " redirected to invalid URL " + redirectToLocation + ", reason: " + e.getMessage() + ".");
													missingFiles.add(missingFile);
													if (console != null) console.append(missingFile.toString()+"\n");
													break;
												}
											}

											if (previousUrls.contains(redirectToLocation)) {
												Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumn(), true, "HTTP URL " + source.toString() + " has circular redirects.");
												missingFiles.add(missingFile);
												if (console != null) console.append(missingFile.toString()+"\n");
												break;
											}

											head.releaseConnection();
											head = new HeadMethod(redirectToLocation);
											head.setFollowRedirects(true);
											if (userAgent != null) {
												head.addRequestHeader("User-Agent", userAgent);
											}
											response = client.executeMethod(head);
											previousUrl = redirectToUri.toURL();
										} while (response == HttpURLConnection.HTTP_SEE_OTHER || response == HttpURLConnection.HTTP_MOVED_PERM || response == HttpURLConnection.HTTP_MOVED_TEMP);
									}

									if (response != HttpURLConnection.HTTP_OK && response != HttpURLConnection.HTTP_SEE_OTHER && response != HttpURLConnection.HTTP_MOVED_PERM && response != HttpURLConnection.HTTP_MOVED_TEMP) {
										Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumn(), generatesError(), "Unable to validate HTTP URL " + source.toString() + ", HTTP Response Code: " + response + ".");
										missingFiles.add(missingFile);
										if (console != null) console.append(missingFile.toString()+"\n");
									}
								} catch (MalformedURLException e)
								{
									Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumn(), generatesError(), "HTTP URL " + source.toString() + " is invalid, reason: " + e.getMessage() + ".");
									missingFiles.add(missingFile);
									if (console != null) console.append(missingFile.toString()+"\n");
								} catch (IOException e)
								{
									Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumn(), generatesError(), "HTTP URL " + source.toString() + " had a connection error, message: " + e.getMessage() + ".");
									missingFiles.add(missingFile);
									if (console != null) console.append(missingFile.toString()+"\n");
								} finally
								{
									if (head != null) {
										head.releaseConnection();
									}
								}
							}
						} else {
							File file = new File(bitstream.getSource().getPath());

							if(!file.exists())
							{
								Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumn(), generatesError(), "Source file path " + file.getAbsolutePath() + " not found.");
								missingFiles.add(missingFile);
								if (console != null) console.append(missingFile.toString()+"\n");
							}
						}
					}
				}

				if (isCancelled()) {
					return missingFiles;
				}

				itemCount++;
				publish(new VerifierBackground.VerifierUpdates(itemCount, totalItems));
			}
		}
		
		return missingFiles;
	}

	@Override
	public boolean generatesError() 
	{
		return true;
	}
	
	@Override
	public String prettyName()
	{
		return "Content Files Exist Verifier";
	}

	@Override
	protected List<Problem> doInBackground()
	{
		return new ArrayList<Problem>();
	}

	@Override
	public boolean isSwingWorker()
	{
		return true;
	}
}
