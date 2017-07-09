package org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetnodemanagerv1;

import java.io.*;
import java.util.*;
import java.net.*;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.annotations.SerializedName;


public class NodeGroupV1 {
  private String name = null;
  private String id = null;
  private String description = null;
  private String environment = null;
  private Boolean environment_trumps = null;
  private String parent = null;
  private LinkedTreeMap rule = null;
  private HashMap classes = null;
  private HashMap deleted = null;
  private HashMap variables = null;

  public String getName() {
    return this.name;
  }

  public String getId() {
    return this.id;
  }

  public String getDescription() {
    return this.description;
  }

  public String getEnvironment() {
    return this.environment;
  }

  public Boolean getEnvironmentTrumps() {
    return this.environment_trumps;
  }

  public String getParent() {
    return this.parent;
  }

  public LinkedTreeMap getRule() {
    return this.rule;
  }

  public HashMap getClasses() {
    return this.classes;
  }

  public HashMap getDeleted() {
    return this.deleted;
  }

  public HashMap getVariables() {
    return this.variables;
  }
}
