package org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetcodemanagerv1;

import com.google.gson.internal.LinkedTreeMap;

public class CodeManagerError {
  private String kind = null;
  private String msg = null;
  private LinkedTreeMap<String,String> subject = null;

  public String getKind() {
    return this.kind;
  }

  public String getMessage() {
    return this.msg;
  }

  public LinkedTreeMap<String,String> getSubject() {
    return this.subject;
  }
}
