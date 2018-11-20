package org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetcodemanagerv1;

import com.google.gson.internal.LinkedTreeMap;

public class CodeManagerException extends Exception {
  private String kind = "";
  private String message = "";
  private LinkedTreeMap<String, String> subject = null;

  public CodeManagerException(String kind, String message, LinkedTreeMap<String, String> subject) {
    this.kind = kind;
    this.message = message;
    this.subject = subject;
  }

  public String getKind() {
    return this.kind;
  }

  public String getMessage() {
    return this.message;
  }

  public LinkedTreeMap<String, String> getSubject() {
    if (this.subject == null) {
      return new LinkedTreeMap();
    }

    return this.subject;
  }
}
