package org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetdbv4;

import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.PEResponse;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.PuppetDBV4;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetdbv4.PuppetDBException;

public class PuppetDBQueryV4 extends PuppetDBV4 {
  private URI uri = null;
  private PuppetDBQueryRequest request = new PuppetDBQueryRequest();
  private ArrayList results = new ArrayList();
  //Note that dates are not parsed out of the returned JSON since we have no models
  // for GSON to know what should be parsed as a Date object.
  // TODO: Figure out a way to enable Date parsing with GSON without models
  Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").create();

  public PuppetDBQueryV4() throws Exception {
    this.uri = getURI("/query/v4");
    this.request = new PuppetDBQueryRequest();
  }

  public void setQuery(String query) {
    this.request.query = query;
  }

  public ArrayList getResults() {
    return this.results;
  }

  public Boolean isSuccessful(PEResponse response) {
    if (response.getResponseCode() == 400 || response.getResponseCode() == 500) {
      return false;
    }

    return true;
  }

  public void execute() throws PuppetDBException, Exception {
    PEResponse response = send(this.uri, this.request);

    if (response.getResponseCode() == 401 || response.getResponseCode() == 403) {
      PuppetDBRBACError error = gson.fromJson(response.getJSON(), PuppetDBRBACError.class);
      throw new PuppetDBException(error.getKind(), error.getMessage(), error.getSubject());
    }

    if (isSuccessful(response)) {
      this.results = gson.fromJson(response.getJSON(), ArrayList.class);
    } else {
      PuppetDBQueryError error = new PuppetDBQueryError(response.getJSON());
      throw new PuppetDBException(error.getKind(), error.getMessage(), error.getDetails());
    }
  }

  class PuppetDBRBACError {
    private String kind = null;
    private String msg = null;
    private LinkedTreeMap<String,Object> subject = null;

    public String getKind() {
      return this.kind;
    }

    public String getMessage() {
      return this.msg;
    }

    public LinkedTreeMap<String,Object> getSubject() {
      return this.subject;
    }
  }

  class PuppetDBQueryError {
    private String kind = null;
    private String message = null;
    private LinkedTreeMap<String,Object> details = null;

    public PuppetDBQueryError(String message) {
      this.kind = "puppetlabs.puppdb/malformed-query";
      this.message = message;
    }

    public String getKind() {
      return this.kind;
    }

    public String getMessage() {
      return this.message;
    }

    public LinkedTreeMap<String, Object> getDetails() {
      return this.details;
    }
  }

  class PuppetDBQueryRequest {
    public String query = null;
  }
}
