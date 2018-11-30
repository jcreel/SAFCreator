package edu.tamu.di.SAFCreator.model.verify.impl;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLProtocolException;
import javax.swing.JTextArea;


import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.http.Header;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;

import edu.tamu.di.SAFCreator.model.Batch;
import edu.tamu.di.SAFCreator.model.Bitstream;
import edu.tamu.di.SAFCreator.model.Bundle;
import edu.tamu.di.SAFCreator.model.Flag;
import edu.tamu.di.SAFCreator.model.FlagPanel;
import edu.tamu.di.SAFCreator.model.Item;
import edu.tamu.di.SAFCreator.model.Problem;
import edu.tamu.di.SAFCreator.model.verify.VerifierBackground;
import edu.tamu.di.SAFCreator.model.verify.VerifierProperty;

public class RemoteFilesExistVerifierImpl extends VerifierBackground {
    private static int TimeoutRead = 20000;
    private static int MaxRedirects = 20;

    private FTPClient ftpConnection;
    private HttpHead httpHead;
    private HttpGet httpGet;
    private RequestConfig requestConfig;
    private CloseableHttpClient httpClient;
    private CloseableHttpResponse httpResponse;
    private int remoteFileTimeout;
    private int responseCode;


    public RemoteFilesExistVerifierImpl() {
        super();
        remoteFileTimeout = TimeoutRead;
        responseCode = 0;
    }

    public RemoteFilesExistVerifierImpl(VerifierProperty settings) {
        super(settings);
        remoteFileTimeout = TimeoutRead;
        responseCode = 0;
    }

    private void abortConnections() {
        if (ftpConnection != null && ftpConnection.isConnected()) {
            try {
                ftpConnection.abort();
            } catch (IOException e) {
                // error status of abort is not relevant here because this is generally a cancel operation or an exit operation.
            }
            ftpConnection = null;
        }

        if (httpResponse != null) {
            try {
                httpResponse.close();
            } catch (IOException e) {
             // error status of close is not relevant here because this is generally a cancel operation or an exit operation.
            }
            httpResponse = null;
        }

        if (httpHead != null) {
            httpHead.abort();
            httpHead = null;
        }

        if (httpGet != null) {
            httpGet.abort();
            httpGet = null;
        }

        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException e) {
                // error status of close is not relevant here because this is generally a cancel operation or an exit operation.
            }
            httpClient = null;
        }
    }

    @Override
    public void doCancel() {
        abortConnections();
    }

    @Override
    protected List<Problem> doInBackground() {
        return new ArrayList<Problem>();
    }

    @Override
    public boolean generatesError() {
        return true;
    }

    @Override
    public boolean isSwingWorker() {
        return true;
    }

    @Override
    public String prettyName() {
        return "Remote Content Files Exist Verifier";
    }

    @Override
    public List<Problem> verify(Batch batch) {
        return verify(batch, null, null);
    }

    @Override
    public List<Problem> verify(Batch batch, JTextArea console, FlagPanel flagPanel) {
        List<Problem> missingFiles = new ArrayList<Problem>();

        if (!batch.getIgnoreFiles()) {
            int totalItems = batch.getItems().size();
            int itemCount = 0;

            remoteFileTimeout = batch.getRemoteFileTimeout();
            requestConfig = RequestConfig.custom().setSocketTimeout(remoteFileTimeout).setConnectTimeout(remoteFileTimeout).build();

            for (Item item : batch.getItems()) {
                if (isCancelled()) {
                    abortConnections();
                    if (console != null) {
                        console.append("Cancelled " + prettyName() + ".\n");
                    }
                    return missingFiles;
                }

                for (Bundle bundle : item.getBundles()) {
                    if (isCancelled()) {
                        abortConnections();
                        if (console != null) {
                            console.append("Cancelled " + prettyName() + ".\n");
                        }
                        return missingFiles;
                    }

                    for (Bitstream bitstream : bundle.getBitstreams()) {
                        if (isCancelled()) {
                            abortConnections();
                            if (console != null) {
                                console.append("Cancelled " + prettyName() + ".\n");
                            }
                            return missingFiles;
                        }

                        URI source = bitstream.getSource();
                        if (!source.isAbsolute() || source.getScheme().toString().equalsIgnoreCase("file")) {
                            // ignore local files.
                            continue;
                        }

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
                                    Flag flag = new Flag(Flag.NOT_FOUND, "FTP file URL was not found.", batch.getAction().toString(), bitstream);
                                    batch.ignoreRow(bitstream.getRow());
                                    batch.failedRow(bitstream.getRow());
                                    Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), generatesError(), "FTP file URL was not found.", flag);
                                    missingFiles.add(missingFile);
                                    if (console != null) {
                                        console.append("\t" + missingFile.toString() + "\n");
                                    }
                                    if (flagPanel != null) {
                                        flagPanel.appendRow(flag);
                                    }
                                }
                            } catch (IOException e) {
                                Flag flag = new Flag(Flag.IO_FAILURE, "FTP file URL had a connection problem, message: " + e.getMessage(), batch.getAction().toString(), bitstream);
                                batch.ignoreRow(bitstream.getRow());
                                batch.failedRow(bitstream.getRow());
                                Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), generatesError(), "FTP file URL had a connection problem.", flag);
                                missingFiles.add(missingFile);
                                if (console != null) {
                                    console.append("\t" + missingFile.toString() + "\n");
                                }
                                if (flagPanel != null) {
                                    flagPanel.appendRow(flag);
                                }
                            } catch (InterruptedException e) {
                                if (isCancelled()) {
                                    abortConnections();
                                    if (console != null) {
                                        console.append("Cancelled " + prettyName() + ".\n");
                                    }
                                    return missingFiles;
                                }
                            }

                            try {
                                if (ftpConnection.isConnected()) {
                                    ftpConnection.disconnect();
                                }
                            } catch (IOException e) {
                                Problem warning = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), false, "Error when closing FTP connection, reason: " + e.getMessage() + ".");
                                missingFiles.add(warning);
                                if (console != null) {
                                    console.append("\t" + warning.toString() + "\n");
                                }
                            }

                            ftpConnection = null;
                        } else {
                            if (isCancelled()) {
                                abortConnections();
                                if (console != null) {
                                    console.append("Cancelled " + prettyName() + ".\n");
                                }
                                return missingFiles;
                            }

                            int itemProcessDelay = batch.getItemProcessDelay();
                            if (itemProcessDelay > 0) {
                                try {
                                    TimeUnit.MILLISECONDS.sleep(itemProcessDelay);
                                } catch (InterruptedException e) {
                                    if (isCancelled()) {
                                        abortConnections();
                                        if (console != null) {
                                            console.append("Cancelled " + prettyName() + ".\n");
                                        }
                                        return missingFiles;
                                    } else {
                                        Problem warning = new Problem(bitstream.getRow(), bitstream.getColumnLabel(),
                                                false, "Failed to sleep for " + itemProcessDelay
                                                        + " milliseconds, reason: " + e.getMessage() + ".");
                                        missingFiles.add(warning);
                                        if (console != null) {
                                            console.append("\t" + warning.toString() + "\n");
                                        }
                                    }
                                }
                            }

                            abortConnections();

                            String userAgent = batch.getUserAgent();
                            httpClient = createHttpClient(batch.getAllowSelfSigned());
                            httpHead = null;
                            httpGet = null;

                            try {
                                httpHead = new HttpHead(source.toURL().toString());
                                httpHead.setConfig(requestConfig);
                                if (userAgent != null) {
                                    httpHead.addHeader("User-Agent", userAgent);
                                }
                                httpResponse = httpClient.execute(httpHead);
                                processHttpResponseStatus(httpResponse);

                                Header redirectTo = httpResponse.getFirstHeader("Location");
                                httpHead.releaseConnection();
                                httpHead = null;

                                if (isCancelled()) {
                                    abortConnections();
                                    if (console != null) {
                                        console.append("Cancelled " + prettyName() + ".\n");
                                    }
                                    return missingFiles;
                                }

                                // some servers do no support HEAD requests, so attempt a GET request.
                                if (responseCode == HttpURLConnection.HTTP_BAD_METHOD) {
                                    httpGet = new HttpGet(source.toURL().toString());
                                    httpGet.setConfig(requestConfig);
                                    if (userAgent != null) {
                                        httpGet.addHeader("User-Agent", userAgent);
                                    }
                                    httpResponse = httpClient.execute(httpGet);
                                    processHttpResponseStatus(httpResponse);
                                    redirectTo = httpResponse.getFirstHeader("Location");
                                    httpGet.releaseConnection();
                                    httpGet = null;
                                }

                                if (responseCode == HttpURLConnection.HTTP_SEE_OTHER || responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                                    int totalRedirects = 0;
                                    HashSet<String> previousUrls = new HashSet<String>();
                                    previousUrls.add(source.toString());
                                    URL previousUrl = source.toURL();

                                    do {
                                        if (isCancelled()) {
                                            abortConnections();
                                            if (console != null) {
                                                console.append("Cancelled " + prettyName() + ".\n");
                                            }
                                            return missingFiles;
                                        }

                                        if (totalRedirects++ > MaxRedirects) {
                                            Flag flag = new Flag(Flag.REDIRECT_LIMIT, "HTTP URL redirected too many times, final redirect URL: " + previousUrl, batch.getAction().toString(), bitstream);
                                            batch.ignoreRow(bitstream.getRow());
                                            batch.failedRow(bitstream.getRow());
                                            Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), true, "HTTP URL redirected too many times.", flag);
                                            missingFiles.add(missingFile);
                                            if (console != null) {
                                                console.append("\t" + missingFile.toString() + "\n");
                                            }
                                            if (flagPanel != null) {
                                                flagPanel.appendRow(flag);
                                            }
                                            break;
                                        }

                                        if (redirectTo == null) {
                                            Flag flag = new Flag(Flag.REDIRECT_FAILURE, "HTTP URL redirected without a valid destination URL.", batch.getAction().toString(), bitstream);
                                            batch.ignoreRow(bitstream.getRow());
                                            batch.failedRow(bitstream.getRow());
                                            Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), true, "HTTP URL redirected without a valid destination URL.", flag);
                                            missingFiles.add(missingFile);
                                            if (console != null) {
                                                console.append("\t" + missingFile.toString() + "\n");
                                            }
                                            if (flagPanel != null) {
                                                flagPanel.appendRow(flag);
                                            }
                                            break;
                                        }

                                        String redirectToLocation = redirectTo.getValue();
                                        URI redirectToUri = null;
                                        try {
                                            redirectToUri = new URI(redirectToLocation);
                                        } catch (URISyntaxException e) {
                                            // attempt to correct an invalid URL, focus on ASCII space.
                                            redirectToLocation = redirectToLocation.replace(" ", "%20");
                                            try {
                                                redirectToUri = new URI(redirectToLocation);
                                            } catch (URISyntaxException e1) {
                                                Flag flag = new Flag(Flag.REDIRECT_FAILURE, "HTTP URL redirected to an invalid URL, reason: " + e.getMessage() + ".", batch.getAction().toString(), bitstream);
                                                batch.ignoreRow(bitstream.getRow());
                                                batch.failedRow(bitstream.getRow());
                                                Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), true, "HTTP URL redirected to an invalid URL.", flag);
                                                missingFiles.add(missingFile);
                                                if (console != null) {
                                                    console.append("\t" + missingFile.toString() + "\n");
                                                }
                                                if (flagPanel != null) {
                                                    flagPanel.appendRow(flag);
                                                }
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
                                                } else {
                                                    redirectToLocation = "http://" + redirectToLocation;
                                                }
                                            }
                                            try {
                                                redirectToUri = new URI(redirectToLocation);
                                            } catch (URISyntaxException e) {
                                                // attempt to correct an invalid URL, focus on ASCII space.
                                                redirectToLocation = redirectToLocation.replace(" ", "%20");
                                                try {
                                                    redirectToUri = new URI(redirectToLocation);
                                                } catch (URISyntaxException e1) {
                                                    Flag flag = new Flag(Flag.REDIRECT_FAILURE, "HTTP URL redirected to an invalid URL, reason: " + e.getMessage() + ".", batch.getAction().toString(), bitstream);
                                                    batch.ignoreRow(bitstream.getRow());
                                                    batch.failedRow(bitstream.getRow());
                                                    Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), true, "HTTP URL redirected to an invalid URL.", flag);
                                                    missingFiles.add(missingFile);
                                                    if (console != null) {
                                                        console.append("\t" + missingFile.toString() + "\n");
                                                    }
                                                    if (flagPanel != null) {
                                                        flagPanel.appendRow(flag);
                                                    }
                                                    break;
                                                }
                                            }
                                        }

                                        if (previousUrls.contains(redirectToLocation)) {
                                            Flag flag = new Flag(Flag.REDIRECT_LOOP, "HTTP URL has circular redirects, final redirect URL: " + redirectToLocation + ".", batch.getAction().toString(), bitstream);
                                            batch.ignoreRow(bitstream.getRow());
                                            batch.failedRow(bitstream.getRow());
                                            Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), true, "HTTP URL has circular redirects.", flag);
                                            missingFiles.add(missingFile);
                                            if (console != null) {
                                                console.append("\t" + missingFile.toString() + "\n");
                                            }
                                            if (flagPanel != null) {
                                                flagPanel.appendRow(flag);
                                            }
                                            break;
                                        }

                                        if (isCancelled()) {
                                            abortConnections();
                                            if (console != null) {
                                                console.append("Cancelled " + prettyName() + ".\n");
                                            }
                                            return missingFiles;
                                        }

                                        httpHead = new HttpHead(redirectToLocation);
                                        httpHead.setConfig(requestConfig);
                                        if (userAgent != null) {
                                            httpHead.addHeader("User-Agent", userAgent);
                                        }
                                        httpResponse = httpClient.execute(httpHead);
                                        processHttpResponseStatus(httpResponse);
                                        httpHead.releaseConnection();
                                        httpHead = null;
                                        previousUrl = redirectToUri.toURL();

                                        // some servers do no support HEAD requests, so attempt a GET request.
                                        if (responseCode == HttpURLConnection.HTTP_BAD_METHOD) {
                                            if (isCancelled()) {
                                                abortConnections();
                                                if (console != null) {
                                                    console.append("Cancelled " + prettyName() + ".\n");
                                                }
                                                return missingFiles;
                                            }

                                            httpGet = new HttpGet(redirectToUri.toURL().toString());
                                            httpGet.setConfig(requestConfig);
                                            if (userAgent != null) {
                                                httpGet.addHeader("User-Agent", userAgent);
                                            }
                                            httpResponse = httpClient.execute(httpGet);
                                            processHttpResponseStatus(httpResponse);
                                            redirectTo = httpResponse.getFirstHeader("Location");
                                            httpGet.releaseConnection();
                                            httpGet = null;
                                        } else {
                                            redirectTo = httpResponse.getFirstHeader("Location");
                                        }
                                    } while (responseCode == HttpURLConnection.HTTP_SEE_OTHER || responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP);
                                }

                                if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_SEE_OTHER && responseCode != HttpURLConnection.HTTP_MOVED_PERM && responseCode != HttpURLConnection.HTTP_MOVED_TEMP) {
                                    if (responseCode == 304 || responseCode == 509) {
                                        Flag flag = new Flag(Flag.SERVICE_REJECTED, "HTTP service was denied (may have a download/bandwidth limit), HTTP response code: " + responseCode + ".", batch.getAction().toString(), bitstream);
                                        batch.ignoreRow(bitstream.getRow());
                                        batch.failedRow(bitstream.getRow());
                                        Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), generatesError(), "HTTP service was denied, HTTP response code: " + responseCode + ".", flag);
                                        missingFiles.add(missingFile);
                                        if (console != null) {
                                            console.append("\t" + missingFile.toString() + "\n");
                                        }
                                        if (flagPanel != null) {
                                            flagPanel.appendRow(flag);
                                        }
                                    } else if (responseCode == 404) {
                                        Flag flag = new Flag(Flag.NOT_FOUND, "HTTP file was not found, HTTP response code: " + responseCode + ".", batch.getAction().toString(), bitstream);
                                        batch.ignoreRow(bitstream.getRow());
                                        batch.failedRow(bitstream.getRow());
                                        Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), generatesError(), "HTTP file was not found, HTTP response code: " + responseCode + ".", flag);
                                        missingFiles.add(missingFile);
                                        if (console != null) {
                                            console.append("\t" + missingFile.toString() + "\n");
                                        }
                                        if (flagPanel != null) {
                                            flagPanel.appendRow(flag);
                                        }
                                    } else if (responseCode == 403) {
                                        Flag flag = new Flag(Flag.ACCESS_DENIED, "HTTP file access was denied, HTTP response code: " + responseCode + ".", batch.getAction().toString(), bitstream);
                                        batch.ignoreRow(bitstream.getRow());
                                        batch.failedRow(bitstream.getRow());
                                        Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), generatesError(), "HTTP file access was denied, HTTP response code: " + responseCode + ".", flag);
                                        missingFiles.add(missingFile);
                                        if (console != null) {
                                            console.append("\t" + missingFile.toString() + "\n");
                                        }
                                        if (flagPanel != null) {
                                            flagPanel.appendRow(flag);
                                        }
                                    } else if (responseCode == 500) {
                                        Flag flag = new Flag(Flag.SERVICE_ERROR, "HTTP server had an internal error, HTTP response code: " + responseCode + ".", batch.getAction().toString(), bitstream);
                                        batch.ignoreRow(bitstream.getRow());
                                        batch.failedRow(bitstream.getRow());
                                        Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), generatesError(), "HTTP server had an internal error, HTTP response code: " + responseCode + ".", flag);
                                        missingFiles.add(missingFile);
                                        if (console != null) {
                                            console.append("\t" + missingFile.toString() + "\n");
                                        }
                                        if (flagPanel != null) {
                                            flagPanel.appendRow(flag);
                                        }
                                    } else {
                                        Flag flag = new Flag(Flag.HTTP_FAILURE, "HTTP failure, HTTP response code: " + responseCode + ".", batch.getAction().toString(), bitstream);
                                        batch.ignoreRow(bitstream.getRow());
                                        batch.failedRow(bitstream.getRow());
                                        Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), generatesError(), "HTTP failure, HTTP response code: " + responseCode + ".", flag);
                                        missingFiles.add(missingFile);
                                        if (console != null) {
                                            console.append("\t" + missingFile.toString() + "\n");
                                        }
                                        if (flagPanel != null) {
                                            flagPanel.appendRow(flag);
                                        }
                                    }
                                }
                            } catch (MalformedURLException e) {
                                String responseString = (responseCode > 0 ? ", HTTP response code: " + responseCode : "");
                                Flag flag = new Flag(Flag.INVALID_FORMAT, "HTTP URL is invalid" + responseString + ", reason: " + e.getMessage() + ".", batch.getAction().toString(), bitstream);
                                batch.ignoreRow(bitstream.getRow());
                                batch.failedRow(bitstream.getRow());
                                Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), generatesError(), "HTTP URL is invalid" + responseString + ".", flag);
                                missingFiles.add(missingFile);
                                if (console != null) {
                                    console.append("\t" + missingFile.toString() + "\n");
                                }
                                if (flagPanel != null) {
                                    flagPanel.appendRow(flag);
                                }
                            } catch (SSLProtocolException e) {
                                String responseString = (responseCode > 0 ? ", HTTP response code: " + responseCode : "");
                                Flag flag = new Flag(Flag.SSL_FAILURE, "HTTP URL had an SSL failure" + responseString + ", reason: " + e.getMessage() + ".", batch.getAction().toString(), bitstream);
                                batch.ignoreRow(bitstream.getRow());
                                batch.failedRow(bitstream.getRow());
                                Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), generatesError(), "HTTP URL had an SSL failure" + responseString + ".", flag);
                                missingFiles.add(missingFile);
                                if (console != null) {
                                    console.append("\t" + missingFile.toString() + "\n");
                                }
                                if (flagPanel != null) {
                                    flagPanel.appendRow(flag);
                                }
                            } catch (IOException e) {
                                String responseString = (responseCode > 0 ? ", HTTP response code: " + responseCode : "");
                                Flag flag = new Flag(Flag.IO_FAILURE, "HTTP URL had a connection error, reason: " + e.getMessage() + ".", batch.getAction().toString(), bitstream);
                                batch.ignoreRow(bitstream.getRow());
                                batch.failedRow(bitstream.getRow());
                                Problem missingFile = new Problem(bitstream.getRow(), bitstream.getColumnLabel(), generatesError(), "HTTP URL had a connection error" + responseString + ".", flag);
                                missingFiles.add(missingFile);
                                if (console != null) {
                                    console.append("\t" + missingFile.toString() + "\n");
                                }
                                if (flagPanel != null) {
                                    flagPanel.appendRow(flag);
                                }
                            } finally {
                                if (ftpConnection != null && ftpConnection.isConnected()) {
                                    try {
                                        ftpConnection.disconnect();
                                    } catch (IOException e) {
                                    }
                                    ftpConnection = null;
                                }

                                if (httpResponse != null) {
                                    try {
                                        httpResponse.close();
                                    } catch (IOException e) {
                                    }
                                    httpResponse = null;
                                }

                                if (httpHead != null) {
                                    httpHead.releaseConnection();
                                    httpHead = null;
                                }

                                if (httpGet != null) {
                                    httpGet.releaseConnection();
                                    httpGet = null;
                                }

                                if (httpClient != null) {
                                    try {
                                        httpClient.close();
                                    } catch (IOException e) {
                                    }
                                    httpClient = null;
                                }
                            }
                        }
                    }
                }

                if (isCancelled()) {
                    abortConnections();
                    if (console != null) {
                        console.append("Cancelled " + prettyName() + ".\n");
                    }
                    return missingFiles;
                }

                itemCount++;
                publish(new VerifierBackground.VerifierUpdates(itemCount, totalItems));
            }
        }

        if (isCancelled()) {
            abortConnections();
            if (console != null) {
                console.append("Cancelled " + prettyName() + ".\n");
            }
            return missingFiles;
        }

        return missingFiles;
    }

    private void processHttpResponseStatus(CloseableHttpResponse httpResponse) {
        if (httpResponse != null) {
            StatusLine statusLine = httpResponse.getStatusLine();
            if (statusLine != null) {
                responseCode = statusLine.getStatusCode();
            }
        }
    }

    private CloseableHttpClient createHttpClient(boolean allowSelfSigned) {
        CloseableHttpClient httpClient = null;

        if (allowSelfSigned) {
            try {
                SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
                SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
                httpClient = HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).build();
            } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
                e.printStackTrace();
            }
        }

        if (httpClient == null) {
            httpClient = HttpClients.createDefault();
        }

        return httpClient;
    }
}
