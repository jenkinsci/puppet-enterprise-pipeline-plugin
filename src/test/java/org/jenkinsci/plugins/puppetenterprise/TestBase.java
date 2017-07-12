package org.jenkinsci.plugins.puppetenterprise;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import org.apache.commons.lang.StringUtils;

import static org.junit.Assert.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.ClassRule;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import org.jenkinsci.plugins.workflow.steps.CodeDeployStepTest;
import org.jenkinsci.plugins.workflow.steps.PuppetJobStepTest;
import org.jenkinsci.plugins.workflow.steps.HieraStepTest;
import org.jenkinsci.plugins.workflow.steps.QueryStepTest;
import org.jenkinsci.plugins.workflow.steps.PuppetNodeGroupStepTest;
import org.jenkinsci.plugins.puppetenterprise.api.HieraDataStoreTest;
import org.jenkinsci.plugins.puppetenterprise.TestUtils;

@RunWith(Suite.class)
@SuiteClasses({PuppetJobStepTest.class, CodeDeployStepTest.class,
  HieraStepTest.class, QueryStepTest.class, HieraDataStoreTest.class,
  PuppetEnterpriseManagementTest.class, PuppetNodeGroupStepTest.class
})
public class TestBase {

  private static WireMockServer mockPuppetServer;

  @BeforeClass
  public static void setUp() {
    if (mockPuppetServer == null) {
      mockPuppetServer = new WireMockServer(options()
        .dynamicPort()
        .httpsPort(8140)
        .keystorePath(TestUtils.getKeystorePath())
        .keystorePassword(TestUtils.getKeystorePassword()));
    }

    if (!mockPuppetServer.isRunning()) {
      mockPuppetServer.start();

      com.github.tomakehurst.wiremock.client.WireMock wireMock = new com.github.tomakehurst.wiremock.client.WireMock(mockPuppetServer.port());

      wireMock.register(get(urlEqualTo("/puppet-ca/v1/certificate/ca"))
          .willReturn(aResponse()
              .withStatus(200)
              .withBody(TestUtils.getCACertificateString())));
    }
  }

  @AfterClass
  public static void tearDown() {
    mockPuppetServer.shutdown();
  }
}
