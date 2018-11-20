package org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetorchestratorv1;

import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.PERequest;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.PEResponse;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.PuppetOrchestratorV1;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetorchestratorv1.PuppetOrchestratorException;

public class PuppetCommandDeployV1 extends PuppetOrchestratorV1 {
  private URI uri = null;
  private PuppetCommandDeployRequest  request  = null;
  private PuppetCommandDeployResponse response = null;

  public PuppetCommandDeployV1() throws Exception {
    this.uri = getURI("/command/deploy");
    this.request = new PuppetCommandDeployRequest();
    this.response = new PuppetCommandDeployResponse();
  }

  public void setScope(String application, ArrayList nodes, String query) {
    this.request.setScope(application, nodes, query);
  }

  public void setEnvironment(String environment) {
    this.request.environment = environment;
  }

  public void setConcurrency(Integer concurrency) {
    this.request.concurrency = concurrency;
  }

  public void setEnforceEnvironment(Boolean enforcement) {
    this.request.enforce_environment = enforcement;
  }

  public void setDebug(Boolean debug) {
    this.request.debug = debug;
  }

  public void setTrace(Boolean trace) {
    this.request.trace = trace;
  }

  public void setNoop(Boolean noop) {
    this.request.noop = noop;
  }

  public void setEvalTrace(Boolean evalTrace) {
    this.request.evaltrace = evalTrace;
  }

  public void setTarget(String target) {
    this.request.target = target;
  }

  private Boolean isSuccessful(PEResponse peResponse) {
    Integer code = peResponse.getResponseCode();
    if (code == 400 || code == 404 || code == 401) {
      return false;
    }

    return true;
  }

  public void execute() throws PuppetOrchestratorException, Exception {
    Gson gson = new Gson();
    PEResponse peResponse = send(this.uri, request);

    if (isSuccessful(peResponse)) {
      response = gson.fromJson(peResponse.getJSON(), PuppetCommandDeployResponse.class);
    } else {
      PuppetCommandDeployError error = gson.fromJson(peResponse.getJSON(), PuppetCommandDeployError.class);
      throw new PuppetOrchestratorException(error.kind, error.msg, error.details);
    }
  }

  public String getID() {
    return response.getID();
  }

  public String getName() {
    return response.getName();
  }

  class PuppetCommandDeployRequest {
    public String environment = null;
    public Boolean noop = null;
    public PuppetCommandDeployRequestScope scope = new PuppetCommandDeployRequestScope();
    public Integer concurrency = null;
    public Boolean enforce_environment = null;
    public Boolean debug = null;
    public Boolean trace = null;
    public Boolean evaltrace = null;
    public String target = null;

    class PuppetCommandDeployRequestScope {
      public String application = null;
      public ArrayList nodes = null;
      public String query = null;
      public Boolean whole_environment = null;

      public Boolean isEmpty() {
        return (this.application == null
          && this.nodes == null
          && this.query == null);
      }
    }

    public void setScope(String application, ArrayList nodes, String query) {
      this.scope.application = application;
      this.scope.nodes = nodes;
      this.scope.query = query;
    }
  }

  class PuppetCommandDeployResponse {
    class PuppetCommandDeployResponseJob {
      public String id = "";
      public String name = "";
    }

    private PuppetCommandDeployResponseJob job = new PuppetCommandDeployResponseJob();

    public String getID() {
      return job.id;
    }

    public String getName() {
      return job.name;
    }
  }

  class PuppetCommandDeployError {
    public String kind;
    public String msg;
    private LinkedTreeMap<String,Object> details;
  }
}
