package org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetorchestratorv1;

import java.util.*;

public class PuppetInventoryItemV1 {
  private Boolean connected = null;
  private String name = null;
  private String broker = null;
  private Date timestamp = null;

  public Boolean getConnected() {
    return this.connected;
  }

  public String getName() {
    return this.name;
  }

  public String getBroker() {
    return this.broker;
  }

  public Date getTimestamp() {
    return this.timestamp;
  }
}
