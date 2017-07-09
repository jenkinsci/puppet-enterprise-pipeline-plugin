package org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetnodemanagerv1;

import com.google.gson.internal.LinkedTreeMap;

public class NodeManagerException extends Exception {
  private String kind = "";
  private String message = "";
  private LinkedTreeMap<String, Object> details = null;

  public NodeManagerException(String kind, String message, LinkedTreeMap<String, Object> details) {
    this.kind = kind;
    this.message = message;
    this.details = details;
  }

  public String getKind() {
    return this.kind;
  }

  public String getMessage() {
    return this.message;
  }

  public LinkedTreeMap<String, Object> getDetails() {
    if (this.details == null) {
      return new LinkedTreeMap();
    }

    return this.details;
  }
}
