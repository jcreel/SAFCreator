package edu.tamu.di.SAFCreator.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLProtocolException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.tika.detect.DefaultDetector;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

import edu.tamu.di.SAFCreator.enums.FlagColumns;

public class Bitstream extends CellDatumImpl {
    private static int TimeoutRead = 20000;
    private static int MaxRedirects = 20;

    private String action = "";
    private Bundle bundle;
    private URI source;
    private String relativePath;
    private File destination;
    private String readPolicyGroupName = null;
    private MimeType mimeType = null;


    public void copyMe(List<Problem> problems) {
        // Avoid writing to existing files, primarily to avoid potential network overhead of downloading remote files.
        if (destination.exists()) {
            return;
        }

        if (source.isAbsolute() && !source.getScheme().toString().equalsIgnoreCase("file")) {
            int itemProcessDelay = bundle.getItem().getBatch().getItemProcessDelay();
            if (itemProcessDelay > 0) {
                try {
                    TimeUnit.MILLISECONDS.sleep(itemProcessDelay);
                } catch (InterruptedException e) {
                    Problem problem = new Problem(getRow(), getColumnLabel(), false, "Failed to sleep for " + itemProcessDelay + " milliseconds, reason: " + e.getMessage() + ".");
                    problems.add(problem);
                }
            }

            int remoteFileTimeout = bundle.getItem().getBatch().getRemoteFileTimeout();

            try {
                URL url = source.toURL();
                if (source.getScheme().toString().equalsIgnoreCase("ftp")) {
                    FTPClient conn = new FTPClient();

                    try {
                        conn.setConnectTimeout(remoteFileTimeout);
                        conn.setDataTimeout(TimeoutRead);
                        conn.connect(source.toURL().getHost());
                        conn.setFileType(FTP.BINARY_FILE_TYPE);
                        conn.enterLocalPassiveMode();
                        conn.login("anonymous", "");

                        String decodedUrl = URLDecoder.decode(source.toURL().getPath(), "UTF-8");
                        OutputStream output = new FileOutputStream(destination);
                        conn.retrieveFile(decodedUrl, output);
                    } catch (IOException e) {
                        Flag flag = new Flag(Flag.IO_FAILURE, "FTP file URL had a connection problem, reason: " + e.getMessage() + ".", action, this);
                        Problem problem = new Problem(getRow(), getColumnLabel(), true, "FTP file URL had a connection problem.", flag);
                        problems.add(problem);
                    }

                    try {
                        if (conn.isConnected()) {
                            conn.disconnect();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    String userAgent = bundle.getItem().getBatch().getUserAgent();
                    CloseableHttpClient httpClient = createHttpClient(bundle.getItem().getBatch().getAllowSelfSigned());
                    CloseableHttpResponse httpResponse = null;
                    HttpGet httpGet = null;
                    RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(remoteFileTimeout).setConnectTimeout(remoteFileTimeout).build();
                    int responseCode = 0;

                    try {
                        httpGet = new HttpGet(url.toString());
                        httpGet.setConfig(requestConfig);
                        if (userAgent != null) {
                            httpGet.addHeader("User-Agent", userAgent);
                        }
                        httpResponse = httpClient.execute(httpGet);
                        responseCode = getResponseCode(httpResponse);

                        if (responseCode == java.net.HttpURLConnection.HTTP_SEE_OTHER || responseCode == java.net.HttpURLConnection.HTTP_MOVED_PERM || responseCode == java.net.HttpURLConnection.HTTP_MOVED_TEMP) {
                            int totalRedirects = 0;
                            HashSet<String> previousUrls = new HashSet<String>();
                            previousUrls.add(source.toString());
                            URL previousUrl = url;
                            Header redirectTo = httpResponse.getFirstHeader("Location");

                            do {
                                if (totalRedirects++ > MaxRedirects) {
                                    Flag flag = new Flag(Flag.REDIRECT_LIMIT, "HTTP URL redirected too many times, final redirect URL: " + previousUrl, action, this);
                                    Problem problem = new Problem(getRow(), getColumnLabel(), true, "HTTP URL redirected too many times.", flag);
                                    problems.add(problem);
                                    break;
                                }

                                if (redirectTo == null) {
                                    Flag flag = new Flag(Flag.REDIRECT_FAILURE, "HTTP URL redirected without a valid destination URL.", action, this);
                                    Problem problem = new Problem(getRow(), getColumnLabel(), true, "HTTP URL redirected without a valid destination URL.", flag);
                                    problems.add(problem);
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
                                        Flag flag = new Flag(Flag.REDIRECT_FAILURE, "HTTP URL redirected to an invalid URL, reason: " + e.getMessage() + ".", action, this);
                                        Problem problem = new Problem(getRow(), getColumnLabel(), true, "HTTP URL redirected to an invalid URL.", flag);
                                        problems.add(problem);
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
                                            Flag flag = new Flag(Flag.REDIRECT_FAILURE, "HTTP URL redirected to an invalid URL, reason: " + e.getMessage() + ".", action, this);
                                            Problem problem = new Problem(getRow(), getColumnLabel(), true, "HTTP URL redirected to an invalid URL.", flag);
                                            problems.add(problem);
                                            break;
                                        }
                                    }
                                }

                                if (previousUrls.contains(redirectToLocation)) {
                                    Flag flag = new Flag(Flag.REDIRECT_LOOP, "HTTP URL has circular redirects, final redirect URL: " + redirectToLocation + ".", action, this);
                                    Problem problem = new Problem(getRow(), getColumnLabel(), true, "HTTP URL has circular redirects.", flag);
                                    problems.add(problem);
                                    break;
                                }

                                httpGet.releaseConnection();
                                httpGet = new HttpGet(redirectToLocation);
                                if (userAgent != null) {
                                    httpGet.addHeader("User-Agent", userAgent);
                                }
                                httpResponse = httpClient.execute(httpGet);
                                responseCode = getResponseCode(httpResponse);
                                previousUrl = redirectToUri.toURL();
                                redirectTo = httpResponse.getFirstHeader("Location");
                            } while (responseCode == java.net.HttpURLConnection.HTTP_SEE_OTHER || responseCode == java.net.HttpURLConnection.HTTP_MOVED_PERM || responseCode == java.net.HttpURLConnection.HTTP_MOVED_TEMP);
                        }

                        if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                            HttpEntity httpEntity = httpResponse.getEntity();
                            InputStream input = null;

                            try {
                                input = httpEntity.getContent();
                                FileUtils.copyToFile(input, destination);
                            } catch (IOException e) {
                                throw e;
                            }
                            finally {
                                if (input != null) {
                                    input.close();
                                }
                            }

                            Header contentTypeHeader = httpResponse.getFirstHeader("Content-Type");
                            String contentTypeHeaderValue = contentTypeHeader == null ? "" : contentTypeHeader.getValue();
                            httpGet.releaseConnection();
                            httpGet = null;

                            String[] contentTypeHeaderParts = contentTypeHeaderValue.split("[;]");
                            String contentType = "";
                            if (contentTypeHeaderParts.length > 0) {
                                contentType = contentTypeHeaderParts[0];
                            }

                            // require a mimeType default.
                            if (mimeType == null) {
                                try {
                                    mimeType = MimeTypes.getDefaultMimeTypes().forName("application/pdf");
                                } catch (MimeTypeException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }

                            Problem problem = null;
                            MimeType originalMimeType = mimeType;
                            if (contentType.equalsIgnoreCase("text/html") || contentType.equalsIgnoreCase("text/plain")) {
                                Flag flag = new Flag(Flag.INVALID_MIME, "HTTP URL did not return the expected file, reason: an HTML page or plain text page was returned.", action, this);
                                problem = new Problem(getRow(), getColumnLabel(), true, "HTTP URL did not return the expected file.", flag);
                                problems.add(problem);
                            }
                            else if (contentType.equalsIgnoreCase("application/octet-stream")) {
                                Flag flag = determineMimeType(destination);
                                if (flag != null) {
                                    problem = new Problem(getRow(), getColumnLabel(), true, flag.getCell(FlagColumns.DESCRIPTION), flag);
                                    problems.add(problem);
                                } else if (mimeType == null || mimeType.toString().isEmpty()) {
                                    flag = new Flag(Flag.INVALID_MIME, "HTTP URL may not be a valid file, reason: unable to determine mime-type.", action, this);
                                    problem = new Problem(getRow(), getColumnLabel(), false, "HTTP URL may not be a valid file.", flag);
                                    problems.add(problem);
                                    mimeType = originalMimeType;
                                }
                            } else if (contentType.equalsIgnoreCase("application/pdf")) {
                                Flag flag = determineMimeType(destination);
                                if (flag != null) {
                                    problem = new Problem(getRow(), getColumnLabel(), true, flag.getCell(FlagColumns.DESCRIPTION), flag);
                                    problems.add(problem);
                                } else if (!contentType.equalsIgnoreCase(mimeType.toString())) {
                                    flag = new Flag(Flag.INVALID_MIME, "HTTP URL may not be a valid PDF, reason: server designated a mimetype of " + contentType + ", detected mimetype is " + mimeType + ".", action, this);
                                    problem = new Problem(getRow(), getColumnLabel(), false, "HTTP URL may not be a valid PDF.", flag);
                                    problems.add(problem);
                                    mimeType = originalMimeType;
                                }
                            } else if (contentType.equalsIgnoreCase("image/png") || contentType.equalsIgnoreCase("image/jpg") || contentType.equalsIgnoreCase("image/jpeg") || contentType.equalsIgnoreCase("image/gif")) {
                                Flag flag = determineMimeType(destination);
                                if (flag != null) {
                                    problem = new Problem(getRow(), getColumnLabel(), true, flag.getCell(FlagColumns.DESCRIPTION), flag);
                                    problems.add(problem);
                                } else if (!contentType.equalsIgnoreCase(mimeType.toString())) {
                                    flag = new Flag(Flag.INVALID_MIME, "HTTP URL may not be a valid image, reason: server designated a mimetype of " + contentType + ", detected mimetype is " + mimeType + ".", action, this);
                                    problem = new Problem(getRow(), getColumnLabel(), false, "HTTP URL may not be a valid image.", flag);
                                    problems.add(problem);
                                    mimeType = originalMimeType;
                                }
                            }

                            // rename destination file on mime type change.
                            if (problem == null) {
                                Flag flag = renameFileUsingMimeType(destination, originalMimeType);
                                if (flag != null) {
                                    problem = new Problem(getRow(), getColumnLabel(), true, flag.getCell(FlagColumns.DESCRIPTION), flag);
                                    problems.add(problem);
                                }
                            }
                            else {
                                if (problem.isError() && destination.exists()) {
                                    destination.delete();
                                }
                            }
                        } else if (responseCode != java.net.HttpURLConnection.HTTP_SEE_OTHER && responseCode != java.net.HttpURLConnection.HTTP_MOVED_PERM && responseCode != java.net.HttpURLConnection.HTTP_MOVED_TEMP) {
                            if (responseCode == 304 || responseCode == 509) {
                                Flag flag = new Flag(Flag.SERVICE_REJECTED, "HTTP service was denied (may have a download/bandwidth limit), HTTP response code: " + responseCode + ".", action, this);
                                Problem problem = new Problem(getRow(), getColumnLabel(), true, "HTTP service was denied, HTTP response code: " + responseCode + ".", flag);
                                problems.add(problem);
                            } else if (responseCode == 404) {
                                Flag flag = new Flag(Flag.NOT_FOUND, "HTTP file was not found, HTTP response code: " + responseCode + ".", action, this);
                                Problem problem = new Problem(getRow(), getColumnLabel(), true, "HTTP file was not found, HTTP response code: " + responseCode + ".", flag);
                                problems.add(problem);
                            } else if (responseCode == 403) {
                                Flag flag = new Flag(Flag.ACCESS_DENIED, "HTTP file access was denied, HTTP response code: " + responseCode + ".", action, this);
                                Problem problem = new Problem(getRow(), getColumnLabel(), true,
                                        "HTTP file access was denied, HTTP response code: " + responseCode + ".", flag);
                                problems.add(problem);
                            } else if (responseCode == 500) {
                                Flag flag = new Flag(Flag.SERVICE_ERROR, "HTTP server had an internal error, HTTP response code: " + responseCode + ".", action, this);
                                Problem problem = new Problem(getRow(), getColumnLabel(), true, "HTTP server had an internal error, HTTP response code: " + responseCode + ".", flag);
                                problems.add(problem);
                            } else {
                                Flag flag = new Flag(Flag.HTTP_FAILURE, "HTTP failure, HTTP response code: " + responseCode + ".", action, this);
                                Problem problem = new Problem(getRow(), getColumnLabel(), true, "HTTP failure, HTTP response code: " + responseCode + ".", flag);
                                problems.add(problem);
                            }
                        }
                    } catch (SSLProtocolException e) {
                        String responseString = (responseCode > 0 ? ", HTTP response code: " + responseCode : "");
                        Flag flag = new Flag(Flag.HTTP_FAILURE, "HTTP URL had an SSL failure" + responseString + ", reason: " + e.getMessage() + ".", action, this);
                        Problem problem = new Problem(getRow(), getColumnLabel(), true, "HTTP URL had an SSL failure" + responseString + ".", flag);
                        problems.add(problem);
                    } catch (SocketException e) {
                        String responseString = (responseCode > 0 ? ", HTTP response code: " + responseCode : "");
                        Flag flag = new Flag(Flag.SOCKET_ERROR, "HTTP URL had a socket error" + responseString + ", reason: " + e.getMessage() + ".", action, this);
                        Problem problem = new Problem(getRow(), getColumnLabel(), true, "HTTP URL had a socket error" + responseString + ".", flag);
                        problems.add(problem);
                    } catch (IOException e) {
                        String responseString = (responseCode > 0 ? ", HTTP response code: " + responseCode : "");
                        Flag flag = new Flag(Flag.IO_FAILURE, "HTTP URL had a connection error" + responseString + ", reason: " + e.getMessage() + ".", action, this);
                        Problem problem = new Problem(getRow(), getColumnLabel(), true, "HTTP URL had a connection error" + responseString + ".", flag);
                        problems.add(problem);
                    } finally {
                        if (httpResponse != null) {
                            try {
                                httpResponse.close();
                            } catch (IOException e) {
                            }
                            httpResponse = null;
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
            } catch (MalformedURLException e) {
                Flag flag = new Flag(Flag.INVALID_FORMAT, "HTTP URL is invalid, reason: " + e.getMessage() + ".", action, this);
                Problem problem = new Problem(getRow(), getColumnLabel(), true, "HTTP URL is invalid.", flag);
                problems.add(problem);
            }
        } else {
            try {
                File file = new File(source.getPath());
                FileUtils.copyFile(file, destination);
            } catch (IOException e) {
                Flag flag = new Flag(Flag.IO_FAILURE, "Source file path failed to copy, reason" + e.getMessage() + ".", "local", source.toString(), getColumnLabel(), "" + getRow(), action);
                Problem problem = new Problem(getRow(), getColumnLabel(), true, "Source file path failed to copy.", flag);
                problems.add(problem);
            }
        }
    }

    /**
     * Determine the mime-type of the file.
     *
     * This is intended to be used to identify or confirm the validity of a particular file. The this.mimeType will be updated on successful detection.
     *
     * @param destination
     *            The file to validate.
     *
     * @return A Flag is returned on error, null is returned otherwise.
     */
    private Flag determineMimeType(File destination) {
        FileInputStream fileStream = null;
        TikaInputStream tikaStream = null;

        try {
            fileStream = new FileInputStream(destination);
            tikaStream = TikaInputStream.get(fileStream);
            Metadata metadata = new Metadata();
            Detector detector = new DefaultDetector(MimeTypes.getDefaultMimeTypes());

            MediaType mediaType = detector.detect(tikaStream, metadata);
            setMimeType(mediaType.toString());
        } catch (MimeTypeException e) {
            return new Flag(Flag.INVALID_MIME, "Unable to determine mime type of file, reason: " + e.getMessage() + ".", action, this);
        } catch (IOException e) {
            return new Flag(Flag.IO_FAILURE, "File read failed, reason: " + e.getMessage() + ".", action, this);
        } finally {
            try {
                if (tikaStream != null) {
                    tikaStream.close();
                }
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }

        return null;
    }

    public String getAction() {
        return action;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public String getContentsManifestLine() {
        String line = getRelativePathForwardSlashes() + "\tbundle:" + bundle.getName().trim()
                + (readPolicyGroupName == null ? "\n" : "\tpermissions:-r " + readPolicyGroupName) + "\n";
        return line;
    }

    public File getDestination() {
        return destination;
    }

    public MimeType getMimeType() {
        return mimeType;
    }

    public String getReadPolicyGroupName() {
        return readPolicyGroupName;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getRelativePathForwardSlashes() {
        String relativePathForwardSlashes = relativePath.replace(File.separatorChar, '/');
        return relativePathForwardSlashes;
    }

    public URI getSource() {
        return source;
    }

    /**
     * Rename the file extension for convenience.
     *
     * Only a small subset of known mime-types are used.
     *
     * @param destination
     * @param originalMimeType
     *
     * @return A Flag is returned on error, null is returned otherwise.
     */
    private Flag renameFileUsingMimeType(File destination, MimeType originalMimeType) {
        String newName = "";
        String oldName = destination.getName();

        if (originalMimeType.compareTo(mimeType) == 0) {
            return null;
        }

        newName = oldName.replaceAll("\\" + originalMimeType.getExtension() + "$", mimeType.getExtension());
        if (mimeType.getExtension().equalsIgnoreCase(".png") || mimeType.getExtension().equalsIgnoreCase(".jpg") || mimeType.getExtension().equalsIgnoreCase(".gif")) {
            newName = newName.replaceAll("^document-", "image-");
        }

        if (!newName.isEmpty()) {
            try {
                File renamed = new File(bundle.getItem().getSAFDirectory() + "/" + newName);

                if (renamed.exists()) {
                    return new Flag(Flag.FILE_ERROR, "File rename failed, reason: the file " + relativePath + " already exists.", action, this);
                }

                if (!destination.renameTo(renamed)) {
                    return new Flag(Flag.FILE_ERROR, "File rename failed, reason: failed to rename " + oldName + " to " + relativePath + ".", action, this);
                }

                relativePath = newName;
            } catch (SecurityException e) {
                return new Flag(Flag.FILE_ERROR, "File rename failed, reason: " + e.getMessage() + ".", action, this);
            }
        }

        return null;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public void setDestination(String destination) {
        this.destination = new File(destination);
    }

    public void setMimeType(String mimeType) throws MimeTypeException {
        this.mimeType = MimeTypes.getDefaultMimeTypes().forName(mimeType);
    }

    public void setReadPolicyGroupName(String readPolicyGroupName) {
        this.readPolicyGroupName = readPolicyGroupName;
    }

    public void setRelativePath(String value) {
        relativePath = value;
        destination = new File(bundle.getItem().getSAFDirectory() + "/" + relativePath);
    }

    public void setSource(URI source) {
        this.source = source;
    }

    private int getResponseCode(CloseableHttpResponse httpResponse) {
        int responseCode = 0;

        if (httpResponse != null) {
            StatusLine statusLine = httpResponse.getStatusLine();
            if (statusLine != null) {
                responseCode = statusLine.getStatusCode();
            }
        }

        return responseCode;
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
