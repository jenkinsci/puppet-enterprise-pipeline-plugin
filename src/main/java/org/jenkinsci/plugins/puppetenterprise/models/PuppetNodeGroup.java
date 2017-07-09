package org.jenkinsci.plugins.puppetenterprise.models;

import java.io.*;
import java.util.*;
import java.lang.Exception;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.PuppetNodeManagerV1;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetnodemanagerv1.NodeGroupV1;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetnodemanagerv1.NodeManagerGroupsV1;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetnodemanagerv1.NodeManagerException;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.PERequest;
import com.google.gson.internal.LinkedTreeMap;

public class PuppetNodeGroup {
  private String name = null;
  private String id = null;
  private String description = null;
  private String environment = null;
  private Boolean environment_trumps = null;
  private String parent = null;
  private ArrayList<Object> rule = null;
  private HashMap classes = null;
  private HashMap variables = null;
  private PrintStream logger = null;
  private ArrayList<NodeGroupV1> groups = null;
  private String token = null;

  public PuppetNodeGroup() { }

  public void setName(String name) {
    this.name = name;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public void setId(String uuid) {
    this.id = id;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  public void setEnvironmentTrumps(Boolean environment_trumps) {
    this.environment_trumps = environment_trumps;
  }

  public void setParent(String parent) {
    this.parent = parent;
  }

  public void setRule(ArrayList<Object> rule) {
    this.rule = rule;
  }

  public void setClasses(HashMap classes) {
    this.classes = classes;
  }

  public void setVariables(HashMap variables) {
    this.variables = variables;
  }

  public void setLogger(PrintStream logger) {
    this.logger = logger;
  }

  public String getName() {
    return this.name;
  }

  public String getId() {
    return this.id;
  }

  public ArrayList<NodeGroupV1> getGroups() throws NodeManagerException, Exception {
    if (this.groups != null) {
      NodeManagerGroupsV1 groups = new NodeManagerGroupsV1();
      this.groups = groups.getAll();
    }

    return this.groups;
  }

  public void set() throws NodeManagerException, Exception {
    NodeManagerGroupsV1 groups = new NodeManagerGroupsV1();

    groups.setClasses(this.classes);
    groups.setVariables(this.variables);
    groups.setEnvironment(this.environment);
    groups.setEnvironmentTrumps(this.environment_trumps);
    groups.setName(this.name);
    groups.setDescription(this.description);
    groups.setParent(this.parent);
    groups.setToken(this.token);


    if (groupExists()) {
      if (this.rule != null) {
        throw new Exception("The existing node group named \"" + this.name + "\" cannot have its rule updated.");
      }

      groups.update();

    } else {
      groups.setRule(this.rule);
      groups.create();
    }
  }

  public void delete() throws NodeManagerException, Exception {
    NodeManagerGroupsV1 groups = new NodeManagerGroupsV1();

    if (groupExists()) {
      groups.delete();
    } else {
      throw new Exception("Group with name " + this.name + " cannot be found. Cannot delete");
    }
  }

  private String getGroupId(String name) throws NodeManagerException, Exception {
    for ( NodeGroupV1 group : this.getGroups() ) {
      if (group.getName() == this.getName()) {
        return group.getId();
      }
    }

    //Return null if we didn't find anything
    return null;

  }

  private Boolean groupExists() throws NodeManagerException, Exception {
    for ( NodeGroupV1 group : this.getGroups() ) {
      if (group.getName() == this.getName()) {
        id = group.getId();
        return false;
      }
    }

    return true;
  }
}
