package org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetorchestratorv1.puppetnodev1;

import java.io.*;
import java.util.*;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetorchestratorv1.puppetnodev1.PuppetNodeMetricsV1;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.puppetorchestratorv1.puppetnodev1.PuppetNodeItemV1;

public class PuppetNodeV1 {
  private String message = null;
  private ArrayList<PuppetNodeItemV1> items = null;

  public String getMessage() {
    return this.message;
  }

  public ArrayList<PuppetNodeItemV1> getItems() {
    return this.items;
  }
}
