package org.jenkinsci.plugins.workflow.steps;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.ClassRule;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runners.model.Statement;
import org.junit.experimental.theories.*;
import org.junit.runner.RunWith;

import static org.junit.Assume.*;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;

import jenkins.model.Jenkins;
import hudson.model.Result;
import hudson.model.FreeStyleBuild;
import hudson.util.Secret;
import hudson.ExtensionList;
import hudson.security.ACL;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.lang.StringBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;

import org.jenkinsci.plugins.puppetenterprise.models.PuppetEnterpriseConfig;
import org.jenkinsci.plugins.puppetenterprise.TestUtils;

@RunWith(Theories.class)
public class PuppetNodeGroupStepTest extends Assert {

  public static @DataPoints String[] PEVersions = {"2016.2","2016.4","2017.2"};

  @ClassRule
  public static WireMockRule mockNodeManagerService = new WireMockRule(options()
    .dynamicPort()
    .httpsPort(4433)
    .keystorePath(TestUtils.getKeystorePath())
    .keystorePassword(TestUtils.getKeystorePassword()));

  @ClassRule
  public static BuildWatcher buildWatcher = new BuildWatcher();

  @Rule
  public RestartableJenkinsRule story = new RestartableJenkinsRule();

  @Before
  public void setup() {
    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          PuppetEnterpriseConfig.setPuppetMasterUrl("localhost");
        }
        catch(java.io.IOException e) {e.printStackTrace();}
        catch(java.security.NoSuchAlgorithmException e) {e.printStackTrace();}
        catch(java.security.KeyStoreException e) {e.printStackTrace();}
        catch(java.security.KeyManagementException e) {e.printStackTrace();}

        StringCredentialsImpl credential = new StringCredentialsImpl(CredentialsScope.GLOBAL, "pe-test-token", "PE test token", Secret.fromString("super_secret_token_string"));
        CredentialsStore store = CredentialsProvider.lookupStores(story.j.jenkins).iterator().next();
        store.addCredentials(Domain.global(), credential);
      }
    });
  }

  @Theory
  public void puppetNodeGroupSeparateCredentialsCallSuccessful(final String peVersion) throws Exception {

    mockNodeManagerService.stubFor(get(urlEqualTo("/classifier-api/v1/groups"))
        .withHeader("X-Authentication", equalTo("super_secret_token_string"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(TestUtils.getAPIResponseBody(peVersion, "/node-manager", "groups.json"))));

    mockNodeManagerService.stubFor(post(urlEqualTo("/classifier-api/v1/groups"))
        .withHeader("X-Authentication", equalTo("super_secret_token_string"))
        .willReturn(aResponse()
            .withStatus(200)));

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        //Create a job where the credentials are defined separately
        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Node Group with Credentials Defined Separately Against " + peVersion);
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.credentials 'pe-test-token'\n" +
          "  puppet.nodeGroup name: 'Test Group', description: 'created by Jenkins', parent: 'All Nodes', rule: ['=', ['trusted', 'extensions', 'pp_application'], 'app']\n" +
          "}", true));
        story.j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        verify(getRequestedFor(urlMatching("/classifier-api/v1/groups"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));

        verify(postRequestedFor(urlMatching("/classifier-api/v1/groups"))
            .withRequestBody(equalToJson("{\"environment\":\"production\",\"name\":\"Test Group\",\"description\":\"created by Jenkins\",\"parent\":\"00000000-0000-4000-8000-000000000000\",\"rule\": [ \"=\", [ \"trusted\", \"extensions\", \"pp_application\" ], \"app\" ], \"classes\": { }, \"variables\": { } }"))
            .withHeader("Content-Type", matching("application/json"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));
      }
    });
  }

}
