package org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetcodemanagerv1;

import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.PEResponse;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.PuppetCodeManagerV1;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetcodemanagerv1.CodeManagerEnvironmentV1;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetcodemanagerv1.CodeManagerEnvironmentErrorV1;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetcodemanagerv1.CodeManagerError;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetcodemanagerv1.CodeManagerException;

public class CodeManagerDeploysV1 extends PuppetCodeManagerV1 {
  private URI uri = null;
  private CodeManagerDeploysRequest request = null;
  private ArrayList<CodeManagerEnvironmentV1> deployedEnvironments = null;
  Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").create();

  public CodeManagerDeploysV1() throws Exception {
    this.uri = getURI("/deploys");
    this.request = new CodeManagerDeploysRequest();
  }

  public void setEnvironments(ArrayList environments) {
    this.request.environments = environments;
  }

  public void setWait(Boolean wait) {
    this.request.wait = wait;
  }

  public ArrayList<CodeManagerEnvironmentV1> getDeployedEnvironments() {
    return this.deployedEnvironments;
  }

  public void execute() throws CodeManagerException, Exception {
    PEResponse response = send(this.uri, this.request);

    if (response.getResponseCode() != 200) {
      CodeManagerError error = gson.fromJson(response.getJSON(), CodeManagerError.class);
      throw new CodeManagerException(error.getKind(), error.getMessage(), error.getSubject());
    }

    Type listOfEnvironmentsType = new TypeToken<ArrayList<CodeManagerEnvironmentV1>>(){}.getType();
    this.deployedEnvironments = gson.fromJson(response.getJSON(), listOfEnvironmentsType);
  }

  public ArrayList<CodeManagerEnvironmentV1> getErrors() {
    ArrayList<CodeManagerEnvironmentV1> errors = new ArrayList();

    for (CodeManagerEnvironmentV1 environment : deployedEnvironments ) {
      if (environment.hasError()) {
        errors.add(environment);
      }
    }

    return errors;
  }

  class CodeManagerDeploysRequest {
    public ArrayList environments = null;
    public Boolean wait = null;
  }
}
