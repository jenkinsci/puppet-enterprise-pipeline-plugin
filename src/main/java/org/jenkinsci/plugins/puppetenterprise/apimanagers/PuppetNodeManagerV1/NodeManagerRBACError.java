package org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetnodemanagerv1;

import com.google.gson.internal.LinkedTreeMap;

public class NodeManagerRBACError {
  private String kind = null;
  private String msg = null;
  private LinkedTreeMap<String,Object> details = null;

  public String getKind() {
    return this.kind;
  }

  public String getMessage() {
    return this.msg;
  }

  public LinkedTreeMap<String, Object> getDetails() {
    return this.details;
  }
}
