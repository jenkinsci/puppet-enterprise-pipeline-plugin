package org.jenkinsci.plugins.puppetenterprise.apimanagers;

import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;
import org.jenkinsci.plugins.puppetenterprise.models.PuppetEnterpriseConfig;
import org.jenkinsci.plugins.puppetenterprise.apimanagers.PERequest;

public abstract class PuppetNodeManagerV1 extends PERequest {
  private String getNodeManagerAddress() {
    return PuppetEnterpriseConfig.getPuppetMasterUrl();
  }

  private Integer getNodeManagerPort() {
    return 4433;
  }

  protected URI getURI(String endpoint) throws Exception {
    String uriString = "https://" + getNodeManagerAddress() + ":" + getNodeManagerPort() + "/classifier-api/v1" + endpoint;
    URI uri = null;

    try {
      uri = new URI(uriString);
    } catch(URISyntaxException e) {
      StringBuilder message = new StringBuilder();

      message.append("Bad Node Manager Service Configuration.\n");

      if (getNodeManagerAddress() == null || getNodeManagerAddress().isEmpty()) {
        message.append("The Puppet Enterprise master address has not been configured yet.\nConfigure the Puppet Enterprise page under Manage Jenkins.");
      }

      message.append("Service Address: " + getNodeManagerAddress() + "\n");
      message.append("Service Port: " + getNodeManagerPort() + "\n");
      message.append("Details: " + e.getMessage());

      throw new Exception(message.toString());
    }

    return uri;
  }
}
