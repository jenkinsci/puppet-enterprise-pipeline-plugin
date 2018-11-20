package org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetorchestratorv1.puppetjobreportv1;

import java.io.*;
import java.util.*;
import java.util.Date;
import com.google.gson.internal.LinkedTreeMap;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetorchestratorv1.puppetjobreportv1.PuppetJobReportNodeEventV1;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetorchestratorv1.puppetjobreportv1.PuppetJobReportNodeMetricsV1;

public class PuppetJobReportNodeV1 implements Serializable {
  private String node = null;
  private String state = null;
  private Date timestamp = null;
  private ArrayList<PuppetJobReportNodeEventV1> events = new ArrayList();
  private String certname = null;
  private String transaction_uuid = null;
  private String environment = null;
  private String configuration_version = null;
  private PuppetJobReportNodeMetricsV1 metrics = null;

  public String getNode() {
    return this.node;
  }

  public String getState() {
    return this.state;
  }

  public Date getTimestamp() {
    return this.timestamp;
  }

  public String getConfigurationVersion() {
    return this.configuration_version;
  }

  public ArrayList<PuppetJobReportNodeEventV1> getEvents() {
    //For some reason the orchestrator return null
    // for the events value instead of an empty array
    if (this.events == null) {
      return new ArrayList();
    } else {
      return this.events;
    }
  }

  public String getCertname() {
    return this.certname;
  }

  public String getTransactionUuid() {
    return this.transaction_uuid;
  }

  public String getEnvironment() {
    return this.environment;
  }

  public PuppetJobReportNodeMetricsV1 getMetrics() {
    return this.metrics;
  }
}
