package org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetnodemanagerv1;

import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.reflect.Type;
import java.lang.Exception;
import com.google.gson.reflect.TypeToken;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.PEResponse;
import org.jenkinsci.plugins.puppetenterprise.models.PuppetNodeGroup;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.PuppetNodeManagerV1;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetnodemanagerv1.NodeManagerException;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetnodemanagerv1.NodeGroupV1;

public class NodeManagerGroupsV1 extends PuppetNodeManagerV1 {
  private NodeManagerGroupsRequest request = null;
  private NodeGroupV1 response = null;
  private String id = null;
  Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").create();

  public void setId(String id) {
    this.id = id;
  }

  public void setClasses(HashMap classes) {
    request.classes = classes;
  }

  public void setVariables(HashMap variables) {
    request.variables = variables;
  }

  public void setEnvironment(String environment) {
    request.environment = environment;
  }

  public void setEnvironmentTrumps(Boolean environmentTrumps) {
    request.environment_trumps = environmentTrumps;
  }

  public void setName(String name) {
    request.name = name;
  }

  public void setDescription(String description) {
    request.description = description;
  }

  public void setParent(String parent) {
    request.parent = parent;
  }

  public void setRule(ArrayList rule) {
    request.rule = rule;
  }

  public NodeManagerGroupsV1() throws Exception {
    this.request = new NodeManagerGroupsRequest();
  }

  public ArrayList<NodeGroupV1> getAll() throws NodeManagerException, Exception {
    URI uri = getURI("/groups");
    PEResponse peResponse = send(uri, "GET");
    ArrayList<NodeGroupV1> groups = null;

    if (isSuccessful(peResponse)) {
      Type listOfGroupsType = new TypeToken<ArrayList<NodeGroupV1>>(){}.getType();
      groups = gson.fromJson(peResponse.getJSON(), listOfGroupsType);
    } else {
      throwError(peResponse);
    }

    return groups;
  }

  public void update() throws NodeManagerException, Exception {
    URI uri = getURI("/groups/" + this.id);
    PEResponse peResponse = send(uri, this.request, "PUT");

    if (isSuccessful(peResponse)) {
      response = gson.fromJson(peResponse.getJSON(), NodeGroupV1.class);
    } else {
      throwError(peResponse);
    }
  }

  public void create() throws NodeManagerException, Exception {
    URI uri = getURI("/groups");
    PEResponse peResponse = send(uri, this.request, "POST");

    if (isSuccessful(peResponse)) {
      response = gson.fromJson(peResponse.getJSON(), NodeGroupV1.class);
    } else {
      throwError(peResponse);
    }
  }

  public void delete() throws NodeManagerException, Exception {
    URI uri = getURI("/groups/" + this.id);
    PEResponse peResponse = send(uri, this.request, "DELETE");

    if (isSuccessful(peResponse)) {
      response = gson.fromJson(peResponse.getJSON(), NodeGroupV1.class);
    } else {
      throwError(peResponse);
    }
  }

  private void throwError(PEResponse response) throws NodeManagerException, Exception {
    NodeManagerRBACError error = gson.fromJson(response.getJSON(), NodeManagerRBACError.class);
    throw new NodeManagerException(error.getKind(), error.getMessage(), error.getDetails());
  }

  private Boolean isSuccessful(PEResponse response) {
    if (response.getResponseCode() == 401 || response.getResponseCode() == 403) {
      return false;
    } else {
      return true;
    }
  }

  class NodeManagerGroupsRequest {
    public String name = null;
    public String environment = null;
    public Boolean environment_trumps = null;
    public String description = null;
    public String parent = null;
    public ArrayList rule = null;
    public HashMap classes = null;
    public HashMap variables = null;
  }
}
