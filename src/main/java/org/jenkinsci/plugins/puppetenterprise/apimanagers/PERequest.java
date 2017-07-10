package org.jenkinsci.plugins.puppetenterprise.apimanagers;

import java.io.*;
import java.util.*;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.lang.InterruptedException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.*;
import javax.net.ssl.*;
import java.net.*;
import java.lang.reflect.Type;
import com.google.inject.Inject;
import com.google.gson.reflect.TypeToken;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.JsonSyntaxException;
import com.google.gson.Gson;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jenkinsci.plugins.plaincredentials.*;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.*;
import org.apache.http.client.entity.*;
import org.apache.http.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.conn.ssl.*;
import org.apache.commons.io.IOUtils;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.annotation.Nonnull;

import org.jenkinsci.plugins.puppetenterprise.apimanagers.PEResponse;
import org.jenkinsci.plugins.puppetenterprise.models.PuppetEnterpriseConfig;
import org.jenkinsci.plugins.puppetenterprise.models.PEException;

public abstract class PERequest {
  private static final Logger logger = Logger.getLogger(PERequest.class.getName());
  private String token = null;
  private Integer port = null;
  private String method = "GET";
  private Object body = new Object();
  private String endpoint = "";

  public String getToken() {
    return this.token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  private KeyStore getTrustStore(final InputStream pathToPemFile) throws IOException, KeyStoreException,
  NoSuchAlgorithmException, CertificateException {
    final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
    ks.load(null);

    // load all certs
    for (java.security.cert.Certificate cert : CertificateFactory.getInstance("X509")
    .generateCertificates(pathToPemFile)) {
      final X509Certificate crt = (X509Certificate) cert;

      try {
        final String alias = crt.getSubjectX500Principal().getName();
        ks.setCertificateEntry(alias, crt);
      } catch (KeyStoreException exp) {
        System.out.println(exp.getMessage());
      }
    }

    return ks;
  }

  private CloseableHttpClient createHttpClient() throws IOException {
    java.security.cert.Certificate ca;
    SSLConnectionSocketFactory sslsf = null;

    try {
      CertificateFactory cf = CertificateFactory.getInstance("X.509");

      String caString = PuppetEnterpriseConfig.getPuppetMasterCACertificate();
      InputStream caStream = new ByteArrayInputStream(caString.getBytes(StandardCharsets.UTF_8));
      ca = cf.generateCertificate(caStream);

      // Create a KeyStore containing our trusted CAs
      String keyStoreType = KeyStore.getDefaultType();
      KeyStore keyStore = KeyStore.getInstance(keyStoreType);
      keyStore.load(null, null);
      keyStore.setCertificateEntry("ca", ca);

      SSLContext sslcontext = SSLContexts.custom().loadTrustMaterial(keyStore).build();
      sslsf = new SSLConnectionSocketFactory(sslcontext,
                SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);

    } catch(CertificateException e) { logger.log(Level.SEVERE, e.getMessage()); }
      catch(KeyStoreException e) { logger.log(Level.SEVERE, e.getMessage()); }
      catch(NoSuchAlgorithmException e) { logger.log(Level.SEVERE, e.getMessage()); }
      catch(KeyManagementException e) { logger.log(Level.SEVERE, e.getMessage()); }

    if (sslsf == null) { System.out.println("sslsf is null"); }

    return HttpClients.custom().setSSLSocketFactory(sslsf).build();
  }

  public final PEResponse send(URI uri, Object body, String method) throws Exception {
    this.method = method;
    this.body = body;
    return this.send(uri);
  }

  public final PEResponse send(URI uri, Object body) throws Exception {
    this.method = "POST";
    this.body = body;
    return this.send(uri);
  }

  public final PEResponse send(URI uri, String method) throws Exception {
    this.method = method;
    return this.send(uri);
  }

  public final PEResponse send(URI uri) throws Exception {
    Gson gson = new Gson();
    String responseString = "";
    Object responseBody = null;
    String accessToken = getToken();
    PEResponse peResponse = null;
    HttpClient httpClient = null;

    try {
      httpClient = createHttpClient();
    } catch(IOException e) {
      logger.log(Level.SEVERE, e.getMessage());
      throw new Exception(e.getMessage());
    }

    try {
      HttpResponse response = null;

      if (method.equals("DELETE")) {
        HttpDelete request = new HttpDelete(uri.toString());
        request.addHeader("X-Authentication", accessToken);
        response = httpClient.execute(request);
      }

      if (method.equals("PUT")) {
        HttpPut request = new HttpPut(uri.toString());

        if (body != null) {
          request.addHeader("content-type", "application/json");
          request.addHeader("X-Authentication", accessToken);
          StringEntity requestJson = new StringEntity(gson.toJson(this.body));
          request.setEntity(requestJson);
        }
        response = httpClient.execute(request);
      }

      if (method.equals("POST")) {
        HttpPost request = new HttpPost(uri.toString());

        if (body != null) {
          request.addHeader("content-type", "application/json");
          request.addHeader("X-Authentication", accessToken);
          StringEntity requestJson = new StringEntity(gson.toJson(this.body));
          request.setEntity(requestJson);
        }
        response = httpClient.execute(request);
      }

      if (method.equals("GET")) {
        HttpGet request = new HttpGet(uri.toString());
        request.addHeader("X-Authentication", accessToken);
        response = httpClient.execute(request);
      }

      String json = "";
      if (response.getEntity() != null) {
        json = IOUtils.toString(response.getEntity().getContent());
      }

      Integer responseCode = response.getStatusLine().getStatusCode();

      peResponse = new PEResponse(new Object(), responseCode, json);
    } catch(IOException e) {
      throw new PEException(e.getMessage());
    }

    return peResponse;
  }
}
