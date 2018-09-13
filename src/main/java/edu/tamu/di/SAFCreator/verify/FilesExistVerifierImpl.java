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

import javax.net.ssl.SSLProtocolException;
import javax.swing.JTextArea;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import edu.tamu.di.SAFCreator.model.Batch;
import edu.tamu.di.SAFCreator.model.Bitstream;
import edu.tamu.di.SAFCreator.model.Bundle;
import edu.tamu.di.SAFCreator.model.Flag;
import edu.tamu.di.SAFCreator.model.FlagPanel;
import edu.tamu.di.SAFCreator.model.Item;
import edu.tamu.di.SAFCreator.model.VerifierBackground;
import edu.tamu.di.SAFCreator.model.VerifierProperty;

public class FilesExistVerifierImpl extends VerifierBackground {
	private static int TimeoutRead = 20000;
	private static int MaxRedirects = 20;

	private FTPClient ftpConnection;
	private HeadMethod httpHead;
	private GetMethod httpGet;
	private DefaultHttpMethodRetryHandler retryHandler;

	public FilesExistVerifierImpl() {
		super();
		ftpConnection = null;
		retryHandler = new DefaultHttpMethodRetryHandler(1, false);
	}

	public FilesExistVerifierImpl(VerifierProperty settings) {
		super(settings);
		ftpConnection = null;
		retryHandler = new DefaultHttpMethodRetryHandler(1, false);
	}

	@Override
	public void doCancel() {
		if (ftpConnection != null) {
			try {
				ftpConnection.abort();
			} catch (IOException e) {
				e.printStackTrace();
			}
			ftpConnection = null;
		}

		if (httpHead != null) {
			httpHead.abort();
			httpHead = null;
		}

		if (httpGet != null) {
			httpGet.abort();
			httpGet = null;
		}
	}

	@Override
	public List<Problem> verify(Batch batch)
	{
		return verify(batch, null, null);
	}

	@Override
	public List<Problem> verify(Batch batch, JTextArea console, FlagPanel flagPanel)
	{
		List<Problem> missingFiles = new ArrayList<Problem>();

		if( ! batch.getIgnoreFiles())
		{
			int totalItems = batch.getItems().size();
			int itemCount = 0;
			int remoteFileTimeout = batch.getRemoteFileTimeout();

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
								ftpConnection = new FTPClient();

								try {
									int itemProcessDelay = batch.getItemProcessDelay();
									if (itemProcessDelay > 0) {
										TimeUnit.MILLISECONDS.sleep(itemProcessDelay);
									}

									ftpConnection.setConnectTimeout(remoteFileTimeout);
									ftpConnection.setDataTimeout(TimeoutRead);
									ftpConnection.connect(source.toURL().getHost());
									ftpConnection.enterLocalPassiveMode();
									ftpConnection.login("anonymous", "");

									String decodedUrl = URLDecoder.decode(source.toURL().getPath(), "ASCII");
									FTPFile[] files = ftpConnection.listFiles(decodedUrl);

									if (files.length == 0) {
										Flag flag = new Flag(Flag.NOT_FOUND, "FTP file URL was not found.", source.getAuthority(), source.toString(), bitstream.getColumnLabel(), "" + bitstream.getRow());
										batch.ignoreRow(bitstream.getRow());
										batch.failedRow(bitstream.getRow());
										Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), generatesError(), "FTP file URL was not found.", flag);
										missingFiles.add(missingFile);
										if (console != null) console.append("\t" + missingFile.toString()+"\n");
										if (flagPanel != null) flagPanel.appendRow(flag);
									}
								} catch (IOException e) {
									Flag flag = new Flag(Flag.IO_FAILURE, "FTP file URL had a connection problem, message: " + e.getMessage(), source.getAuthority(), source.toString(), bitstream.getColumnLabel(), "" + bitstream.getRow());
									batch.ignoreRow(bitstream.getRow());
									batch.failedRow(bitstream.getRow());
									Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), generatesError(), "FTP file URL had a connection problem.", flag);
									missingFiles.add(missingFile);
									if (console != null) console.append("\t" + missingFile.toString()+"\n");
									if (flagPanel != null) flagPanel.appendRow(flag);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

								try {
									if (ftpConnection.isConnected()) {
										ftpConnection.disconnect();
									}
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

								ftpConnection = null;
							}
							else {
								int itemProcessDelay = batch.getItemProcessDelay();
								if (itemProcessDelay > 0) {
									try
									{
										TimeUnit.MILLISECONDS.sleep(itemProcessDelay);
									} catch (InterruptedException e)
									{
										Problem warning = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), false, "Failed to sleep for " + itemProcessDelay + " milliseconds, reason: " + e.getMessage() + ".");
										missingFiles.add(warning);
										if (console != null) console.append("\t" + warning.toString()+"\n");
									}
								}

								String userAgent = batch.getUserAgent();
								HttpClient client = new HttpClient();
								httpHead = null;
								httpGet = null;
								int response = 0;

								//client.getParams().setParameter(HttpMethodParams.HEAD_BODY_CHECK_TIMEOUT, remoteFileTimeout);
								client.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, remoteFileTimeout);
								client.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, retryHandler);

								try
								{
									httpHead = new HeadMethod(source.toURL().toString());
									if (userAgent != null) {
										httpHead.addRequestHeader("User-Agent", userAgent);
									}
									httpHead.setFollowRedirects(true);
									response = client.executeMethod(httpHead);
									Header redirectTo = httpHead.getResponseHeader("Location");
									httpHead.releaseConnection();
									httpHead = null;

									// some servers do no support HEAD requests, so attempt a GET request.
									if (response == HttpURLConnection.HTTP_BAD_METHOD) {
										httpGet = new GetMethod(source.toURL().toString());
										if (userAgent != null) {
											httpGet.addRequestHeader("User-Agent", userAgent);
										}
										httpGet.setFollowRedirects(true);
										response = client.executeMethod(httpGet);
										redirectTo = httpGet.getResponseHeader("Location");
										httpGet.releaseConnection();
										httpGet = null;
									}

									if (response == HttpURLConnection.HTTP_SEE_OTHER || response == HttpURLConnection.HTTP_MOVED_PERM || response == HttpURLConnection.HTTP_MOVED_TEMP) {
										int totalRedirects = 0;
										HashSet<String> previousUrls = new HashSet<String>();
										previousUrls.add(source.toString());
										URL previousUrl = source.toURL();

										do {
											if (totalRedirects++ > MaxRedirects) {
												Flag flag = new Flag(Flag.REDIRECT_LIMIT, "HTTP URL redirected too many times, final redirect URL: " + previousUrl, source.getAuthority(), source.toString(), bitstream.getColumnLabel(), "" + bitstream.getRow());
												batch.ignoreRow(bitstream.getRow());
												batch.failedRow(bitstream.getRow());
												Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), true, "HTTP URL redirected too many times.", flag);
												missingFiles.add(missingFile);
												if (console != null) console.append("\t" + missingFile.toString()+"\n");
												if (flagPanel != null) flagPanel.appendRow(flag);
												break;
											}

											if (redirectTo == null) {
												Flag flag = new Flag(Flag.REDIRECT_FAILURE, "HTTP URL redirected without a valid destination URL.", source.getAuthority(), source.toString(), bitstream.getColumnLabel(), "" + bitstream.getRow());
												batch.ignoreRow(bitstream.getRow());
												batch.failedRow(bitstream.getRow());
												Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), true, "HTTP URL redirected without a valid destination URL.", flag);
												missingFiles.add(missingFile);
												if (console != null) console.append("\t" + missingFile.toString()+"\n");
												if (flagPanel != null) flagPanel.appendRow(flag);
												break;
											}

											String redirectToLocation = redirectTo.getValue();
											URI redirectToUri = null;
											try {
												redirectToUri = new URI(redirectToLocation);
											}
											catch (URISyntaxException e)
											{
												// attempt to correct an invalid URL, focus on ASCII space.
												redirectToLocation = redirectToLocation.replace(" ", "%20");
												try {
													redirectToUri = new URI(redirectToLocation);
												}
												catch (URISyntaxException e1)
												{
													Flag flag = new Flag(Flag.REDIRECT_FAILURE, "HTTP URL redirected to an invalid URL, reason: " + e.getMessage() + ".", source.getAuthority(), source.toString(), bitstream.getColumnLabel(), "" + bitstream.getRow());
													batch.ignoreRow(bitstream.getRow());
													batch.failedRow(bitstream.getRow());
													Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), true, "HTTP URL redirected to an invalid URL.", flag);
													missingFiles.add(missingFile);
													if (console != null) console.append("\t" + missingFile.toString()+"\n");
													if (flagPanel != null) flagPanel.appendRow(flag);
													break;
												}
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
													// attempt to correct an invalid URL, focus on ASCII space.
													redirectToLocation = redirectToLocation.replace(" ", "%20");
													try {
														redirectToUri = new URI(redirectToLocation);
													}
													catch (URISyntaxException e1)
													{
														Flag flag = new Flag(Flag.REDIRECT_FAILURE, "HTTP URL redirected to an invalid URL, reason: " + e.getMessage() + ".", source.getAuthority(), source.toString(), bitstream.getColumnLabel(), "" + bitstream.getRow());
														batch.ignoreRow(bitstream.getRow());
														batch.failedRow(bitstream.getRow());
														Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), true, "HTTP URL redirected to an invalid URL.", flag);
														missingFiles.add(missingFile);
														if (console != null) console.append("\t" + missingFile.toString()+"\n");
														if (flagPanel != null) flagPanel.appendRow(flag);
														break;
													}
												}
											}

											if (previousUrls.contains(redirectToLocation)) {
												Flag flag = new Flag(Flag.REDIRECT_LOOP, "HTTP URL has circular redirects, final redirect URL: " + redirectToLocation + ".", source.getAuthority(), source.toString(), bitstream.getColumnLabel(), "" + bitstream.getRow());
												batch.ignoreRow(bitstream.getRow());
												batch.failedRow(bitstream.getRow());
												Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), true, "HTTP URL has circular redirects.", flag);
												missingFiles.add(missingFile);
												if (console != null) console.append("\t" + missingFile.toString()+"\n");
												if (flagPanel != null) flagPanel.appendRow(flag);
												break;
											}

											httpHead = new HeadMethod(redirectToLocation);
											httpHead.setFollowRedirects(true);
											if (userAgent != null) {
												httpHead.addRequestHeader("User-Agent", userAgent);
											}
											response = client.executeMethod(httpHead);
											httpHead.releaseConnection();
											previousUrl = redirectToUri.toURL();

											// some servers do no support HEAD requests, so attempt a GET request.
											if (response == HttpURLConnection.HTTP_BAD_METHOD) {
												httpHead.releaseConnection();
												httpHead = null;

												httpGet = new GetMethod(redirectToUri.toURL().toString());
												if (userAgent != null) {
													httpGet.addRequestHeader("User-Agent", userAgent);
												}
												httpGet.setFollowRedirects(true);
												response = client.executeMethod(httpGet);
												redirectTo = httpGet.getResponseHeader("Location");
												httpGet.releaseConnection();
												httpGet = null;
											}
											else {
												redirectTo = httpHead.getResponseHeader("Location");
												httpHead.releaseConnection();
												httpHead = null;
											}
										} while (response == HttpURLConnection.HTTP_SEE_OTHER || response == HttpURLConnection.HTTP_MOVED_PERM || response == HttpURLConnection.HTTP_MOVED_TEMP);
									}

									if (response != HttpURLConnection.HTTP_OK && response != HttpURLConnection.HTTP_SEE_OTHER && response != HttpURLConnection.HTTP_MOVED_PERM && response != HttpURLConnection.HTTP_MOVED_TEMP) {
										if (response == 304 || response == 509) {
											Flag flag = new Flag(Flag.SERVICE_REJECTED, "HTTP service was denied (may have a download/bandwidth limit), HTTP response code: " + response + ".", source.getAuthority(), source.toString(), bitstream.getColumnLabel(), "" + bitstream.getRow());
											batch.ignoreRow(bitstream.getRow());
											batch.failedRow(bitstream.getRow());
											Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), generatesError(), "HTTP service was denied, HTTP response code: " + response + ".", flag);
											missingFiles.add(missingFile);
											if (console != null) console.append("\t" + missingFile.toString()+"\n");
											if (flagPanel != null) flagPanel.appendRow(flag);
										}
										else if (response == 404) {
											Flag flag = new Flag(Flag.NOT_FOUND, "HTTP file was not found, HTTP response code: " + response + ".", source.getAuthority(), source.toString(), bitstream.getColumnLabel(), "" + bitstream.getRow());
											batch.ignoreRow(bitstream.getRow());
											batch.failedRow(bitstream.getRow());
											Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), generatesError(), "HTTP file was not found, HTTP response code: " + response + ".", flag);
											missingFiles.add(missingFile);
											if (console != null) console.append("\t" + missingFile.toString()+"\n");
											if (flagPanel != null) flagPanel.appendRow(flag);
										}
										else if (response == 403) {
											Flag flag = new Flag(Flag.ACCESS_DENIED, "HTTP file access was denied, HTTP response code: " + response + ".", source.getAuthority(), source.toString(), bitstream.getColumnLabel(), "" + bitstream.getRow());
											batch.ignoreRow(bitstream.getRow());
											batch.failedRow(bitstream.getRow());
											Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), generatesError(), "HTTP file access was denied, HTTP response code: " + response + ".", flag);
											missingFiles.add(missingFile);
											if (console != null) console.append("\t" + missingFile.toString()+"\n");
											if (flagPanel != null) flagPanel.appendRow(flag);
										}
										else if (response == 500) {
											Flag flag = new Flag(Flag.SERVICE_ERROR, "HTTP server had an internal error, HTTP response code: " + response + ".", source.getAuthority(), source.toString(), bitstream.getColumnLabel(), "" + bitstream.getRow());
											batch.ignoreRow(bitstream.getRow());
											batch.failedRow(bitstream.getRow());
											Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), generatesError(), "HTTP server had an internal error, HTTP response code: " + response + ".", flag);
											missingFiles.add(missingFile);
											if (console != null) console.append("\t" + missingFile.toString()+"\n");
											if (flagPanel != null) flagPanel.appendRow(flag);
										}
										else {
											Flag flag = new Flag(Flag.HTTP_FAILURE, "HTTP failure, HTTP response code: " + response + ".", source.getAuthority(), source.toString(), bitstream.getColumnLabel(), "" + bitstream.getRow());
											batch.ignoreRow(bitstream.getRow());
											batch.failedRow(bitstream.getRow());
											Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), generatesError(), "HTTP failure, HTTP response code: " + response + ".", flag);
											missingFiles.add(missingFile);
											if (console != null) console.append("\t" + missingFile.toString()+"\n");
											if (flagPanel != null) flagPanel.appendRow(flag);
										}
									}
								} catch (MalformedURLException e)
								{
									String responseString = (response > 0 ? ", HTTP response code: " + response : "");
									Flag flag = new Flag(Flag.INVALID_FORMAT, "HTTP URL is invalid" + responseString + ", reason: " + e.getMessage() + ".", source.getAuthority(), source.toString(), bitstream.getColumnLabel(), "" + bitstream.getRow());
									batch.ignoreRow(bitstream.getRow());
									batch.failedRow(bitstream.getRow());
									Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), generatesError(), "HTTP URL is invalid" + responseString + ".", flag);
									missingFiles.add(missingFile);
									if (console != null) console.append("\t" + missingFile.toString()+"\n");
									if (flagPanel != null) flagPanel.appendRow(flag);
								} catch (SSLProtocolException e)
								{
									String responseString = (response > 0 ? ", HTTP response code: " + response : "");
									Flag flag = new Flag(Flag.SSL_FAILURE, "HTTP URL had an SSL failure" + responseString + ", reason: " + e.getMessage() + ".", source.getAuthority(), source.toString(), bitstream.getColumnLabel(), "" + bitstream.getRow());
									batch.ignoreRow(bitstream.getRow());
									batch.failedRow(bitstream.getRow());
									Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), generatesError(), "HTTP URL had an SSL failure" + responseString + ".", flag);
									missingFiles.add(missingFile);
									if (console != null) console.append("\t" + missingFile.toString()+"\n");
									if (flagPanel != null) flagPanel.appendRow(flag);
								} catch (IOException e)
								{
									String responseString = (response > 0 ? ", HTTP response code: " + response : "");
									Flag flag = new Flag(Flag.IO_FAILURE, "HTTP URL had a connection error, reason: " + e.getMessage() + ".", source.getAuthority(), source.toString(), bitstream.getColumnLabel(), "" + bitstream.getRow());
									batch.ignoreRow(bitstream.getRow());
									batch.failedRow(bitstream.getRow());
									Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), generatesError(), "HTTP URL had a connection error" + responseString + ".", flag);
									missingFiles.add(missingFile);
									if (console != null) console.append("\t" + missingFile.toString()+"\n");
									if (flagPanel != null) flagPanel.appendRow(flag);
								} finally
								{
									if (httpHead != null) {
										httpHead.releaseConnection();
										httpHead = null;
									}
									if (httpGet != null) {
										httpGet.releaseConnection();
										httpGet = null;
									}
									if (client != null) {
										client.getHttpConnectionManager().closeIdleConnections(remoteFileTimeout);
									}
								}
							}
						} else {
							File file = new File(bitstream.getSource().getPath());

							if(!file.exists())
							{
								Flag flag = new Flag(Flag.NOT_FOUND, "source file path was not found.", "local", file.getAbsolutePath(), bitstream.getColumnLabel(), "" + bitstream.getRow());
								Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), generatesError(), "Source file path not found.", flag);
								missingFiles.add(missingFile);
								if (console != null) console.append("\t" + missingFile.toString()+"\n");
								if (flagPanel != null) flagPanel.appendRow(flag);
							}
						}

						if (isCancelled()) {
							return missingFiles;
						}
					}

					if (isCancelled()) {
						return missingFiles;
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
