package org.jenkinsci.plugins.workflow.steps;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.*;
import com.google.inject.Inject;
import hudson.Extension;
import hudson.Util;
import hudson.util.ListBoxModel;
import hudson.security.ACL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.apache.commons.lang.StringUtils;
import hudson.model.Run;
import hudson.model.Item;
import hudson.model.TaskListener;
import java.net.*;
import jenkins.model.Jenkins;
import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jenkinsci.plugins.plaincredentials.*;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.AncestorInPath;
import java.io.Serializable;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.google.gson.internal.LinkedTreeMap;

import org.jenkinsci.plugins.puppetenterprise.PuppetEnterpriseManagement;
import org.jenkinsci.plugins.puppetenterprise.models.PuppetJob;
import org.jenkinsci.plugins.puppetenterprise.models.UnknownPuppetJobReportType;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetorchestratorv1.PuppetOrchestratorException;
import org.jenkinsci.plugins.puppetenterprise.models.PEException;

public final class PuppetJobStep extends PuppetEnterpriseStep implements Serializable {

  private String target = null;
  private ArrayList nodes = null;
  private String application = null;
  private String query = null;
  private Integer concurrency = null;
  private Boolean noop = false;
  private String environment = null;
  private String credentialsId = "";
  private ArrayList<String> reports = null;

  @DataBoundSetter private void setTarget(String target) {
    this.target = Util.fixEmpty(target);
  }

  @DataBoundSetter private void setConcurrency(Integer concurrency) {
    this.concurrency = concurrency;
  }

  @DataBoundSetter private void setNoop(Boolean noop) {
    this.noop = noop;
  }

  @DataBoundSetter private void setEnvironment(String environment) {
    this.environment = environment;
  }

  @DataBoundSetter private void setQuery(String query) {
    this.query = query;
  }

  @DataBoundSetter private void setReports(ArrayList reports) {
    this.reports = reports;
  }

  @DataBoundSetter private void setNodes(ArrayList nodes) {
    this.nodes = nodes;
  }

  @DataBoundSetter private void setApplication(String application) {
    this.application = application;
  }

  public String getQuery() {
    return this.query;
  }

  public ArrayList getNodes() {
    return this.nodes;
  }

  public String getApplication() {
    return this.application;
  }

  public String getTarget() {
    return this.target;
  }

  public Integer getConcurrency() {
    return this.concurrency;
  }

  public String getEnvironment() {
    return this.environment;
  }

  public Boolean getNoop() {
    return this.noop;
  }

  public ArrayList<String> getReports() {
    return this.reports;
  }

  @DataBoundConstructor public PuppetJobStep() { }

  public static class PuppetJobStepExecution extends AbstractSynchronousStepExecution<String> {

    @Inject private transient PuppetJobStep step;
    @StepContextParameter private transient Run<?, ?> run;
    @StepContextParameter private transient TaskListener listener;

    @SuppressFBWarnings(
      value = "DLS_DEAD_LOCAL_STORE",
      justification = "Findbugs is wrong. The variable is not a dead store."
    )
    @Override protected String run() throws Exception {
      PuppetJob job = new PuppetJob();
      job.setConcurrency(step.getConcurrency());
      job.setNoop(step.getNoop());
      job.setEnvironment(step.getEnvironment());
      job.setToken(step.getToken());
      job.setLogger(listener.getLogger());

      // Target is still supported to support older versions of PE.
      // 2016.4 installs of PE should use the scope parameter when
      // creating orchestrator jobs.
      if (step.getTarget() != null && !step.getTarget().isEmpty()) {
        job.setTarget(step.getTarget());
      } else {
        job.setScope(step.getApplication(), step.getNodes(), step.getQuery());
      }

      try {
        String summary = "";

        job.run();

        summary = "Puppet job " + job.getName() + " " + job.getState() + "\n";

        try {
          listener.getLogger().println(job.generateReport(step.getReports()));
        } catch(UnknownPuppetJobReportType e) {
          throw new Exception(e.getMessage());
        } catch(Exception e) {
          StringBuilder bug = new StringBuilder();
          bug.append("You found a bug! The Puppet Enterprise plugin received something ");
          bug.append("in a job report it wasn't expecting. Please file a ticket here: ");
          bug.append("https://issues.jenkins-ci.org/browse/JENKINS-42899?jql=project%20%3D%20JENKINS%20AND%20component%20%3D%20'puppet-enterprise-pipeline-plugin'\n\n");
          bug.append("Include the following information:\n");
          bug.append("Exception Type: " + e.getClass().getSimpleName() + "\n");
          bug.append("Exception Message: " + e.getMessage() + "\n");

          throw new Exception(bug.toString());
        }

        if (job.failed() || job.stopped()) {
          throw new Exception(summary);
        }
      } catch(PuppetOrchestratorException e) {
        StringBuilder message = new StringBuilder();
        message.append("Puppet Orchestrator Job Error\n");
        message.append("Kind:    " + e.getKind() + "\n");
        message.append("Message: " + e.getMessage() + "\n");

        if (e.getDetails() != null) {
          message.append("Details: " + e.getDetails().toString() + "\n");
        }

        throw new PEException(message.toString(), listener);
      }

      return job.getName();
    }

    private static final long serialVersionUID = 1L;
  }

  @Extension public static final class DescriptorImpl extends AbstractStepDescriptorImpl {
    public DescriptorImpl() {
      super(PuppetJobStepExecution.class);
    }

    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item context, @QueryParameter String source) {
      if (context == null || !context.hasPermission(Item.CONFIGURE)) {
        return new ListBoxModel();
      }
      return new StandardListBoxModel().withEmptySelection().withAll(
      CredentialsProvider.lookupCredentials(StringCredentials.class, context, ACL.SYSTEM, URIRequirementBuilder.fromUri(source).build()));
    }

    @Override public String getFunctionName() {
      return "puppetJob";
    }

    @Override public String getDisplayName() {
      return "Create Puppet Orchestrator Job";
    }
  }
}
