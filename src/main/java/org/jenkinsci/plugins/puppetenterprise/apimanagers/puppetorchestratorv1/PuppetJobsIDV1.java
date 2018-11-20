package org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetorchestratorv1;

import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.PEResponse;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.PuppetOrchestratorV1;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetorchestratorv1.puppetjobreportv1.*;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetorchestratorv1.puppetnodev1.*;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetorchestratorv1.PuppetOrchestratorException;

public class PuppetJobsIDV1 extends PuppetOrchestratorV1 {
  private String endpoint = "/jobs/%s";
  private PuppetJobsIDResponse response = null;
  private String name = "";
  private String state = "";
  private ArrayList<PuppetNodeItemV1> nodes = new ArrayList();
  private ArrayList<PuppetJobReportNodeV1> report = new ArrayList();
  private Integer nodeCount = null;
  private String environment = "";
  Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").create();

  public PuppetJobsIDV1() {
    this.response = new PuppetJobsIDResponse();
  }

  public PuppetJobsIDV1(String name) {
    this.name = name;
    this.response = new PuppetJobsIDResponse();
  }

  public String getState(){
    return this.state;
  }

  public String getEnvironment(){
    return this.environment;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  public ArrayList<PuppetJobReportNodeV1> getReport() throws URISyntaxException, Exception {
    URI uri = response.getReportURL().toURI();
    PEResponse peResponse = send(uri);

    if (isSuccessful(peResponse)) {
      report = gson.fromJson(peResponse.getJSON(), PuppetJobReportV1.class).getReport();
    } else {
      PuppetJobsIDError error = gson.fromJson(peResponse.getJSON(), PuppetJobsIDError.class);
      throw new PuppetOrchestratorException(error.kind, error.msg, error.details);
    }

    return report;
  }

  public ArrayList<PuppetNodeItemV1> getNodes() throws URISyntaxException, Exception {
    URI uri = response.getNodesURL().toURI();
    PEResponse peResponse = send(uri);

    if (isSuccessful(peResponse)) {
      nodes = gson.fromJson(peResponse.getJSON(), PuppetNodeV1.class).getItems();
    } else {
      PuppetJobsIDError error = gson.fromJson(peResponse.getJSON(), PuppetJobsIDError.class);
      throw new PuppetOrchestratorException(error.kind, error.msg, error.details);
    }

    return nodes;
  }

  public Integer getNodeCount() {
    return this.nodeCount;
  }

  public void execute() throws PuppetOrchestratorException, Exception {
    URI fullURI = getURI(String.format(this.endpoint, this.name));
    PEResponse peResponse = send(fullURI);

    if (isSuccessful(peResponse)) {
      response = gson.fromJson(peResponse.getJSON(), PuppetJobsIDResponse.class);

      this.state = response.state;
      this.nodeCount = response.node_count;
    } else {
      PuppetJobsIDError error = gson.fromJson(peResponse.getJSON(), PuppetJobsIDError.class);
      throw new PuppetOrchestratorException(error.kind, error.msg, error.details);
    }
  }

  public Boolean isSuccessful(PEResponse response) {
    Integer code = response.getResponseCode();
    if (code == 400 || code == 404 || code == 401) {
      return false;
    }

    return true;
  }

  class PuppetJobsIDResponse {
    public ArrayList items = new ArrayList();
    public URL id = null;
    public String state = null;
    public String name = "";
    public PuppetJobsIDResponseOptions options = new PuppetJobsIDResponseOptions();
    public Integer node_count = null;
    public PuppetJobsIDResponseOwner owner = new PuppetJobsIDResponseOwner();
    public Date timestamp = null;
    public ArrayList<PuppetJobsIDResponseStatus> status = new ArrayList();
    private LinkedTreeMap<String,String> environment = new LinkedTreeMap();
    private LinkedTreeMap<String,URL> nodes = new LinkedTreeMap();
    private LinkedTreeMap<String,URL> report = new LinkedTreeMap();

    class PuppetJobsIDResponseOwner {
      public String email = null;
      public Boolean is_revoked = null;
      public Date last_login = null;
      public Boolean is_remote = null;
      public String login = null;
      public ArrayList<Integer> inhereted_role_ids = null;
      public ArrayList<String> group_ids = null;
      public Boolean is_superuser = null;
      public String id = null;
      public ArrayList<Integer> role_ids = null;
      public String display_name = null;
      public Boolean is_group = null;
    }

    class PuppetJobsIDResponseOptions {
      public Integer concurrency = null;
      public Boolean noop = null;
      public Boolean trace = null;
      public Boolean debug = null;
      public LinkedTreeMap<String, Object> scope = new LinkedTreeMap();
      public Boolean enforce_environment = null;
      public String environment = "";
      public Boolean evaltrace = null;
      public String target = null;
    }

    class PuppetJobsIDResponseStatus {
      public String state = "";
      public Date enter_time = null;
      public Date exit_time = null;
    }

    public PuppetJobsIDResponseStatus getLastStatus() {
      return status.get(status.size() - 1);
    }

    public URL getNodesURL() {
      return nodes.get("id");
    }

    public URL getReportURL() {
      return report.get("id");
    }
  }

  class PuppetJobsIDError {
    public String kind;
    public String msg;
    public LinkedTreeMap<String,Object> details;
  }

}
