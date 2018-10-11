package org.jenkinsci.plugins.workflow.steps;

import java.io.Serializable;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.plaincredentials.*;
import org.kohsuke.stapler.DataBoundSetter;
import hudson.Util;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;

public abstract class PuppetEnterpriseStep extends AbstractStepImpl implements Serializable {
  private String credentialsId;

  @DataBoundSetter public void setCredentialsId(String credentialsId) {
    this.credentialsId = Util.fixEmpty(credentialsId);
  }

  protected static StringCredentials lookupCredentials(@Nonnull String credentialId) {
    return CredentialsMatchers.firstOrNull(
      CredentialsProvider.lookupCredentials(StringCredentials.class, Jenkins.getInstance(), ACL.SYSTEM),
      CredentialsMatchers.withId(credentialId)
    );
  }

  public String getTokenID() {
    return this.credentialsId;
  }

  public String getToken() {
    return lookupCredentials(credentialsId).getSecret().getPlainText();
  }

  public String getCredentialsId() {
    return this.credentialsId;
  }
}
