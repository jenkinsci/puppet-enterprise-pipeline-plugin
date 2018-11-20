package org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetorchestratorv1.puppetjobreportv1;

import java.io.*;
import java.util.*;
import java.util.Date;
import com.google.gson.internal.LinkedTreeMap;

public class PuppetJobReportNodeMetricsV1 implements Serializable {
  private PuppetJobReportNodeMetricsResources resources = null;
  private LinkedTreeMap<String,Float> time = null;
  private PuppetJobReportNodeMetricsChanges changes = null;
  private PuppetJobReportNodeMetricsEvents events = null;

  public LinkedTreeMap<String,Float> getTime() {
    return this.time;
  }

  class PuppetJobReportNodeMetricsResources implements Serializable {
    public Integer total = null;
    public Integer out_of_sync = null;
    public Integer corrective_change = null;
    public Integer failed = null;
    public Integer scheduled = null;
    public Integer restarted = null;
    public Integer failed_to_restart = null;
    public Integer changed = null;
    public Integer skipped = null;
  }

  class PuppetJobReportNodeMetricsChanges implements Serializable {
    public Integer total = null;
  }

  class PuppetJobReportNodeMetricsEvents implements Serializable {
    public Integer failure = null;
    public Integer success = null;
    public Integer total = null;
  }
}
