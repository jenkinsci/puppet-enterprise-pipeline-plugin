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
  private String environment = "production";
  private Boolean environment_trumps = null;
  private String parent = null;
  private ArrayList rule = null;
  private HashMap classes = new HashMap();
  private HashMap variables = new HashMap();
  private Boolean mergeClasses = null;
  private Boolean mergeVariables = null;
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

  public void setRule(ArrayList rule) {
    this.rule = rule;
  }

  public void setClasses(HashMap classes) {
    this.classes = classes;
  }

  public void setVariables(HashMap variables) {
    this.variables = variables;
  }

  public void setMergeClasses(Boolean mergeClasses) {
    this.mergeClasses = mergeClasses;
  }

  public void setMergeVariables(Boolean mergeVariables) {
    this.mergeVariables = mergeVariables;
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
    NodeManagerGroupsV1 groups = new NodeManagerGroupsV1();
    groups.setToken(this.token);
    this.groups = groups.getAll();

    return this.groups;
  }

  public void set() throws NodeManagerException, Exception {
    NodeManagerGroupsV1 groups = new NodeManagerGroupsV1();

    groups.setClasses(this.classes);
    groups.setDescription(this.description);
    groups.setVariables(this.variables);
    groups.setEnvironment(this.environment);
    groups.setEnvironmentTrumps(this.environment_trumps);
    groups.setName(this.name);
    groups.setToken(this.token);

    if (this.parent != null) {
      groups.setParent(getGroupParentId(this.parent));
    }

    if (groupExists()) {
      NodeGroupV1 group = getGroup(this.name);

      if (this.description != null) {
        groups.setDescription(this.description);
      } else {
        groups.setDescription(group.getDescription());
      }

      if (this.environment != null) {
        groups.setEnvironment(this.environment);
      } else {
        groups.setEnvironment(group.getEnvironment());
      }

      if (this.environment_trumps != null) {
        groups.setEnvironmentTrumps(this.environment_trumps);
      } else {
        groups.setEnvironmentTrumps(group.getEnvironmentTrumps());
      }

      if (this.rule != null) {
        groups.setRule(this.rule);
      } else {
        groups.setRule(group.getRule());
      }

      if (this.classes != null) {
        if (this.mergeClasses) {
          HashMap mergedClasses = new HashMap();
          mergedClasses.putAll(group.getClasses());
          mergedClasses.putAll(this.classes);
          groups.setClasses(mergedClasses);
        } else {
          groups.setVariables(this.variables);
        }
      } else {
        groups.setClasses(group.getClasses());
      }

      if (this.variables != null) {
        if (this.mergeVariables) {
          HashMap mergedVariables = new HashMap();
          mergedVariables.putAll(group.getVariables());
          mergedVariables.putAll(this.variables);
          groups.setVariables(mergedVariables);
        } else {
          groups.setVariables(this.variables);
        }
      } else {
        groups.setVariables(group.getVariables());
      }

      groups.setId(group.getId());
      groups.update();

    } else {
      if (this.rule != null) {
        groups.setRule(this.rule);
      }

      if (this.description != null) {
        groups.setDescription(this.description);
      }

      if (this.classes != null) {
        groups.setClasses(this.classes);
      }

      if (this.variables != null) {
        groups.setVariables(this.variables);
      }

      if (this.environment != null) {
        groups.setEnvironment(this.environment);
      }

      if (this.environment_trumps != null) {
        groups.setEnvironmentTrumps(this.environment_trumps);
      }

      groups.create();
    }
  }

  public void delete() throws NodeManagerException, Exception {
    NodeManagerGroupsV1 groups = new NodeManagerGroupsV1();

    groups.setName(this.name);
    groups.setId(getGroupId(this.name));
    groups.setToken(this.token);

    if (groupExists()) {
      groups.delete();
    } else {
      this.logger.println("Node group " + this.name + " was not found. Skipping delete.");
    }
  }

  private NodeGroupV1 getGroup(String groupName) throws NodeManagerException, Exception {
    NodeGroupV1 nodeGroup = new NodeGroupV1();

    for ( NodeGroupV1 group : this.getGroups() ) {
      if (group.getName().equals(groupName)) {
        nodeGroup = group;
      }
    }

    return nodeGroup;
  }

  private String getGroupParentId(String groupName) throws NodeManagerException, Exception {
    return getGroup(groupName).getParent();
  }

  private String getGroupId(String name) throws NodeManagerException, Exception {
    for ( NodeGroupV1 group : this.getGroups() ) {
      if (group.getName().equals(name)) {
        return group.getId();
      }
    }

    //Return null if we didn't find anything
    return null;
  }

  private Boolean groupExists() throws NodeManagerException, Exception {
    for ( NodeGroupV1 group : this.getGroups() ) {
      if (group.getName().equals(this.getName())) {
        id = group.getId();
        return true;
      }
    }

    return false;
  }
}
