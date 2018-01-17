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

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import edu.tamu.di.SAFCreator.model.Batch;
import edu.tamu.di.SAFCreator.model.Bitstream;
import edu.tamu.di.SAFCreator.model.Bundle;
import edu.tamu.di.SAFCreator.model.Item;
import edu.tamu.di.SAFCreator.model.Verifier;

public class FilesExistVerifierImpl implements Verifier {
	private static int MaxRedirects = 20;
	private static int TimeoutConnection = 5000;
	private static int TimeoutRead = 5000;

	public List<Problem> verify(Batch batch) 
	{
		List<Problem> missingFiles = new ArrayList<Problem>();
		
		if( ! batch.getIgnoreFiles())
		{
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
									}
								} catch (IOException e) {
									Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumn(), generatesError(), "Connection problem for FTP file URL " + source.toString() + ", message: " + e.getMessage() + ".");
									missingFiles.add(missingFile);
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
								HttpURLConnection conn = null;

								try {
									int itemProcessDelay = batch.getItemProcessDelay();
									if (itemProcessDelay > 0) {
										TimeUnit.MILLISECONDS.sleep(itemProcessDelay);
									}

									conn = (HttpURLConnection) source.toURL().openConnection();
									conn.setRequestMethod("HEAD");
									conn.setInstanceFollowRedirects(true);
									conn.getInputStream().close();

									int response = conn.getResponseCode();

									if (response == HttpURLConnection.HTTP_MOVED_TEMP || response == HttpURLConnection.HTTP_MOVED_PERM || response == HttpURLConnection.HTTP_SEE_OTHER) {
										int totalRedirects = 0;
										HashSet<String> previousUrls = new HashSet<String>();
										previousUrls.add(source.toString());

										do {
											if (totalRedirects++ > MaxRedirects) {
												Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumn(), generatesError(), "Unable to validate file URL " + source.toString() + ", too many redirects.");
												missingFiles.add(missingFile);
												response = HttpURLConnection.HTTP_OK;
												break;
											}

											//String redirectCookies = conn.getHeaderField("Set-Cookie");
											String redirectTo = conn.getHeaderField("Location");
											URL redirectToUrl = new URL(redirectTo);
											if (previousUrls.contains(redirectTo)) {
												Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumn(), generatesError(), "Unable to validate file URL " + source.toString() + ", recursive redirection.");
												missingFiles.add(missingFile);
												response = HttpURLConnection.HTTP_OK;
												break;
											}
											previousUrls.add(redirectTo);

											String schema = redirectToUrl.getProtocol();
											if (schema.equalsIgnoreCase("http") || schema.equalsIgnoreCase("https") || schema.equalsIgnoreCase("ftp")) {
												conn.disconnect();
												conn = (HttpURLConnection) redirectToUrl.openConnection();
												conn.setRequestMethod("HEAD");
												conn.setInstanceFollowRedirects(true);
												//conn.setRequestProperty("Cookie", redirectCookies);
												conn.getInputStream().close();

												response = conn.getResponseCode();
												if (response == HttpURLConnection.HTTP_OK) {
													String redirectFrom = source.toURL().toString();
													Problem warning = new Problem(bitstream.getRow(), bitstream.getColumn(), false, "Redirecting " + redirectFrom + " to " + redirectToUrl.toString() + ".");
													missingFiles.add(warning);
													bitstream.setSource(redirectToUrl.toURI());
												}
											}
											else {
												String redirectFrom = source.toURL().toString();
												Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumn(), generatesError(), "Refusing to redirect from " + redirectFrom + " to " + redirectToUrl.toString() + " due to protocol.");
												missingFiles.add(missingFile);
											}
										} while (response == HttpURLConnection.HTTP_MOVED_TEMP || response == HttpURLConnection.HTTP_MOVED_PERM || response == HttpURLConnection.HTTP_SEE_OTHER);
									}

									if (response != HttpURLConnection.HTTP_OK) {
										Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumn(), generatesError(), "Unable to validate file URL " + source.toString() + ", HTTP Response Code: " + response + ".");
										missingFiles.add(missingFile);
									}
								} catch (MalformedURLException e) {
									Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumn(), generatesError(), "Source file URL " + source.toString() + " is invalid, reason: " + e.getMessage() + ".");
									missingFiles.add(missingFile);
								} catch (IOException e) {
									Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumn(), generatesError(), "Source file URL " + source.toString() + " had a connection error, message: " + e.getMessage() + ".");
									missingFiles.add(missingFile);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (URISyntaxException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} finally {
									if (conn != null) {
										conn.disconnect();
									}
								}
							}
						} else {
							File file = new File(bitstream.getSource().getPath());

							if(!file.exists())
							{
								Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumn(), generatesError(), "Source file " + file.getAbsolutePath() + " not found.");
								missingFiles.add(missingFile);
							}
						}
					}
				}
			}
		}
		
		return missingFiles;
	}

	public boolean generatesError() 
	{
		return true;
	}
	
	public String prettyName()
	{
		return "Content Files Exist Verifier";
	}

}
