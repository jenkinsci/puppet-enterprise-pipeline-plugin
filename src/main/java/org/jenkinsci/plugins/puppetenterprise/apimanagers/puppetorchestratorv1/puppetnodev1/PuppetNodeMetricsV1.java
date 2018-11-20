package org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetorchestratorv1.puppetnodev1;

public class PuppetNodeMetricsV1 {
  private Integer total = null;
  private Integer failed = null;
  private Integer changed = null;
  private Integer corrective_change = null;
  private Integer skipped = null;
  private Integer restarted = null;
  private Integer scheduled = null;
  private Integer out_of_sync = null;
  private Integer failed_to_restart = null;
  private String  message = null;

  public Integer getTotal() {
    return this.total;
  }

  public String getMessage() {
    return this.message;
  }

  public Integer getFailed() {
    return this.failed;
  }

  public Integer getChanged() {
    return this.changed;
  }

  public Integer getCorrectiveChanged() {
    return this.corrective_change;
  }

  public Integer getSkipped() {
    return this.skipped;
  }

  public Integer getRestarted() {
    return this.restarted;
  }

  public Integer getScheduled() {
    return this.scheduled;
  }

  public Integer getOutOfSync() {
    return this.out_of_sync;
  }

  public Integer getFailedToRestart() {
    return this.failed_to_restart;
  }
}
