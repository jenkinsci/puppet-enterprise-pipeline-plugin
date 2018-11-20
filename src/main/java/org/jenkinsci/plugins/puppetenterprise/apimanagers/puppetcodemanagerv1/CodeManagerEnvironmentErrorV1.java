package org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetcodemanagerv1;

import java.io.*;
import com.google.gson.internal.LinkedTreeMap;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetcodemanagerv1.CodeManagerEnvironmentV1;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetcodemanagerv1.CodeManagerEnvironmentErrorV1;

public class CodeManagerEnvironmentErrorV1 {
  private String kind = null;
  private String msg = null;
  private LinkedTreeMap<String, Object> details = null;

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
