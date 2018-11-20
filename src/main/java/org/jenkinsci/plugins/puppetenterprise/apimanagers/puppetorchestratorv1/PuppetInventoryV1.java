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

public class PuppetInventoryV1 extends PuppetOrchestratorV1 {
  private URI uri = null;
  private PuppetInventoryRequest  request  = null;
  private PuppetInventoryResponse response = null;

  public PuppetInventoryV1() throws Exception {
    this.uri = getURI("/inventory");
    this.request = new PuppetInventoryRequest();
    this.response = new PuppetInventoryResponse();
  }

  public void setNodes(ArrayList<String> nodes) {
    this.request.nodes = nodes;
  }

  private Boolean isSuccessful(PEResponse peResponse) {
    Integer code = peResponse.getResponseCode();
    if (code == 400 || code == 404 || code == 401) {
      return false;
    }

    return true;
  }

  public ArrayList<PuppetInventoryItemV1> execute() throws PuppetOrchestratorException, Exception {
    Gson gson = new Gson();
    PEResponse peResponse = send(this.uri, request);

    if (isSuccessful(peResponse)) {
      response = gson.fromJson(peResponse.getJSON(), PuppetInventoryResponse.class);
    } else {
      PuppetInventoryError error = gson.fromJson(peResponse.getJSON(), PuppetInventoryError.class);
      throw new PuppetOrchestratorException(error.kind, error.msg, error.details);
    }

    return response.getItems();
  }

  public ArrayList<PuppetInventoryItemV1> getItems() {
    return response.getItems();
  }

  class PuppetInventoryRequest {
    public ArrayList<String> nodes = new ArrayList();
  }

  class PuppetInventoryResponse {
    private ArrayList<PuppetInventoryItemV1> items = null;

    private ArrayList<PuppetInventoryItemV1> getItems() {
      return this.items;
    }
  }

  class PuppetInventoryError {
    public String kind;
    public String msg;
    private LinkedTreeMap<String,Object> details;
  }
}
