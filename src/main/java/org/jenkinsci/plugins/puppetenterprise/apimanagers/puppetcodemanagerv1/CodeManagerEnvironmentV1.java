package org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetcodemanagerv1;

import java.io.*;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.annotations.SerializedName;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetcodemanagerv1.CodeManagerEnvironmentV1;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetcodemanagerv1.CodeManagerEnvironmentErrorV1;

public class CodeManagerEnvironmentV1 {
  @SerializedName("deploy-signature")
  private String deploySignature = null;

  @SerializedName("file-sync")
  private LinkedTreeMap<String,String> fileSync = null;

  private String environment = null;
  private Integer id = null;
  private String status = null;
  private CodeManagerEnvironmentErrorV1 error = null;

  public String getName() {
    return this.environment;
  }

  public String getStatus() {
    return this.status;
  }

  public Integer getID() {
    return this.id;
  }

  public Boolean hasError() {
    return (this.error != null);
  }

  public Boolean isSuccessful() {
    return (this.error == null);
  }

  public CodeManagerEnvironmentErrorV1 getError() {
    return this.error;
  }
}
