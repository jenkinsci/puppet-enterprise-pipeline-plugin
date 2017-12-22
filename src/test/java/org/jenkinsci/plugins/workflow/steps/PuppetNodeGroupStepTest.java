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

  @Theory
  public void puppetNodeGroupWithCredentialsCallSuccessful(final String peVersion) throws Exception {

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
          "  puppet.nodeGroup credentials: 'pe-test-token', name: 'Test Group', description: 'created by Jenkins', parent: 'All Nodes', rule: ['=', ['trusted', 'extensions', 'pp_application'], 'app']\n" +
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

  @Theory
  public void puppetNodeGroupCreateNodeGroupIfNotExists(final String peVersion) throws Exception {

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

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Node Group Creates Node Group If It Does Not Exist against " + peVersion);
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.nodeGroup credentials: 'pe-test-token', name: 'Non-Existent Group', parent: 'All Nodes' \n" +
          "}", true));
        story.j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        verify(getRequestedFor(urlMatching("/classifier-api/v1/groups"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));

        verify(postRequestedFor(urlMatching("/classifier-api/v1/groups"))
            .withRequestBody(equalToJson("{\"environment\":\"production\",\"name\":\"Non-Existent Group\",\"parent\":\"00000000-0000-4000-8000-000000000000\",\"rule\": [ ], \"classes\": { }, \"variables\": { } }"))
            .withHeader("Content-Type", matching("application/json"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));
      }
    });
  }

  @Theory
  public void puppetNodeGroupUpdateExistingGroup(final String peVersion) throws Exception {

    mockNodeManagerService.stubFor(get(urlEqualTo("/classifier-api/v1/groups"))
        .withHeader("X-Authentication", equalTo("super_secret_token_string"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(TestUtils.getAPIResponseBody(peVersion, "/node-manager", "groups.json"))));

    mockNodeManagerService.stubFor(put(urlEqualTo("/classifier-api/v1/groups/6324536f-3df4-4eab-8a7c-ae00be078d0d"))
        .withHeader("X-Authentication", equalTo("super_secret_token_string"))
        .willReturn(aResponse()
            .withStatus(201)));

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Environment Parameter Is Updated Against " + peVersion);
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.nodeGroup credentials: 'pe-test-token', name: 'PE ActiveMQ Broker', environment: 'qa' \n" +
          "}", true));
        story.j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        verify(putRequestedFor(urlMatching("/classifier-api/v1/groups/6324536f-3df4-4eab-8a7c-ae00be078d0d"))
            .withRequestBody(equalToJson("{\"environment\":\"qa\",\"environment_trumps\": false, \"name\":\"PE ActiveMQ Broker\",\"parent\":\"5a585133-0680-4143-a7af-422731408027\",\"rule\": [ \"or\", [\"=\", \"name\", \"master.inf.puppet.vm\"]], \"classes\": { \"puppet_enterprise::profile::amq::broker\": {}}, \"variables\": { }, \"id\": \"6324536f-3df4-4eab-8a7c-ae00be078d0d\" }"))
            .withHeader("Content-Type", matching("application/json"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));
      }
    });

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Environment Trumps Parameter Is Updated Against " + peVersion);
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.nodeGroup credentials: 'pe-test-token', name: 'PE ActiveMQ Broker', environment_trumps: true \n" +
          "}", true));
        story.j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        verify(putRequestedFor(urlMatching("/classifier-api/v1/groups/6324536f-3df4-4eab-8a7c-ae00be078d0d"))
            .withRequestBody(equalToJson("{\"environment\":\"production\",\"environment_trumps\": true, \"name\":\"PE ActiveMQ Broker\",\"parent\":\"66844661-b59c-466e-826a-096c955268bc\",\"rule\": [ \"or\", [\"=\", \"name\", \"master.inf.puppet.vm\"]], \"classes\": { \"puppet_enterprise::profile::amq::broker\": {}}, \"variables\": { }, \"id\": \"6324536f-3df4-4eab-8a7c-ae00be078d0d\" }"))
            .withHeader("Content-Type", matching("application/json"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));
      }
    });

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Parent Parameter Is Updated Against " + peVersion);
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.nodeGroup credentials: 'pe-test-token', name: 'PE ActiveMQ Broker', parent: \"66844661-b59c-466e-826a-096c955268bc\"\n" +
          "}", true));
        story.j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        verify(putRequestedFor(urlMatching("/classifier-api/v1/groups/6324536f-3df4-4eab-8a7c-ae00be078d0d"))
            .withRequestBody(equalToJson("{\"environment\":\"production\",\"environment_trumps\": false, \"name\":\"PE ActiveMQ Broker\",\"parent\":\"66844661-b59c-466e-826a-096c955268bc\",\"rule\": [ \"or\", [\"=\", \"name\", \"master.inf.puppet.vm\"]], \"classes\": { \"puppet_enterprise::profile::amq::broker\": {}}, \"variables\": { }, \"id\": \"6324536f-3df4-4eab-8a7c-ae00be078d0d\" }"))
            .withHeader("Content-Type", matching("application/json"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));
      }
    });

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Rule Parameter Is Updated Against " + peVersion);
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.nodeGroup credentials: 'pe-test-token', name: 'PE ActiveMQ Broker', rule: [ \"=\", \"name\", \"newnode\"]\n" +
          "}", true));
        story.j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        verify(putRequestedFor(urlMatching("/classifier-api/v1/groups/6324536f-3df4-4eab-8a7c-ae00be078d0d"))
            .withRequestBody(equalToJson("{\"environment\":\"production\",\"environment_trumps\": false, \"name\":\"PE ActiveMQ Broker\",\"parent\":\"66844661-b59c-466e-826a-096c955268bc\",\"rule\": [ \"=\", \"name\", \"newnode\"], \"classes\": { \"puppet_enterprise::profile::amq::broker\": {}}, \"variables\": { }, \"id\": \"6324536f-3df4-4eab-8a7c-ae00be078d0d\" }"))
            .withHeader("Content-Type", matching("application/json"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));
      }
    });

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Classes Parameter Is Merged Against " + peVersion);
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.nodeGroup credentials: 'pe-test-token', name: 'PE ActiveMQ Broker', mergeClasses: true, classes: [ \"newclass\": [\"parameterkey\": \"parametervalue\"]]\n" +
          "}", true));
        story.j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        verify(putRequestedFor(urlMatching("/classifier-api/v1/groups/6324536f-3df4-4eab-8a7c-ae00be078d0d"))
            .withRequestBody(equalToJson("{\"environment\":\"production\",\"environment_trumps\": false, \"name\":\"PE ActiveMQ Broker\",\"parent\":\"66844661-b59c-466e-826a-096c955268bc\",\"rule\": [ \"=\", \"name\", \"newnode\"], \"classes\": { \"puppet_enterprise::profile::amq::broker\": {}, \"newclass\": { \"parameterkey\": \"parametervalue\"}}, \"variables\": { }, \"id\": \"6324536f-3df4-4eab-8a7c-ae00be078d0d\" }"))
            .withHeader("Content-Type", matching("application/json"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));
      }
    });

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Classes Parameter Is Updated Against " + peVersion);
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.nodeGroup credentials: 'pe-test-token', name: 'PE ActiveMQ Broker', classes: [ \"newclass\": [\"parameterkey\": \"parametervalue\"]]\n" +
          "}", true));
        story.j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        verify(putRequestedFor(urlMatching("/classifier-api/v1/groups/6324536f-3df4-4eab-8a7c-ae00be078d0d"))
            .withRequestBody(equalToJson("{\"environment\":\"production\",\"environment_trumps\": false, \"name\":\"PE ActiveMQ Broker\",\"parent\":\"66844661-b59c-466e-826a-096c955268bc\",\"rule\": [ \"=\", \"name\", \"newnode\"], \"classes\": { \"newclass\": { \"parameterkey\": \"parametervalue\"}}, \"variables\": { }, \"id\": \"6324536f-3df4-4eab-8a7c-ae00be078d0d\" }"))
            .withHeader("Content-Type", matching("application/json"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));
      }
    });

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Variables Parameter Is Merged Against " + peVersion);
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.nodeGroup credentials: 'pe-test-token', name: 'Artifactory', mergeVariables: true, variables: \"variables\": [ \"newvariable\": \"newvalue\"]\n" +
          "}", true));
        story.j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        verify(putRequestedFor(urlMatching("/classifier-api/v1/groups/6324536f-3df4-4eab-8a7c-ae00be078d0d"))
            .withRequestBody(equalToJson("{\"environment\":\"production\",\"environment_trumps\": false, \"name\":\"Artifactory\",\"parent\":\"66844661-b59c-466e-826a-096c955268bc\",\"rule\": [ \"=\", \"name\", \"newnode\"], \"classes\": { \"newclass\": { \"parameterkey\": \"parametervalue\"}}, \"variables\": { \"my-app\", \"value\", \"newvariable\": \"newvalue\" }, \"id\": \"6324536f-3df4-4eab-8a7c-ae00be078d0d\" }"))
            .withHeader("Content-Type", matching("application/json"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));
      }
    });

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Variables Parameter Is Replaced Against " + peVersion);
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.nodeGroup credentials: 'pe-test-token', name: 'PE ActiveMQ Broker', variables: \"variables\": [ \"newvariable\": \"newvalue\"]\n" +
          "}", true));
        story.j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        verify(putRequestedFor(urlMatching("/classifier-api/v1/groups/6324536f-3df4-4eab-8a7c-ae00be078d0d"))
            .withRequestBody(equalToJson("{\"environment\":\"production\",\"environment_trumps\": false, \"name\":\"PE ActiveMQ Broker\",\"parent\":\"66844661-b59c-466e-826a-096c955268bc\",\"rule\": [ \"=\", \"name\", \"newnode\"], \"classes\": { \"newclass\": { \"parameterkey\": \"parametervalue\"}}, \"variables\": { \"newvariable\": \"newvalue\" }, \"id\": \"6324536f-3df4-4eab-8a7c-ae00be078d0d\" }"))
            .withHeader("Content-Type", matching("application/json"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));
      }
    });
  }

  @Theory
  public void puppetNodeDeleteGroupThatExistsSucceeds(final String peVersion) throws Exception {

    mockNodeManagerService.stubFor(get(urlEqualTo("/classifier-api/v1/groups/6324536f-3df4-4eab-8a7c-ae00be078d0d"))
        .withHeader("X-Authentication", equalTo("super_secret_token_string"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(TestUtils.getAPIResponseBody(peVersion, "/node-manager", "groups.json"))));

    mockNodeManagerService.stubFor(delete(urlEqualTo("/classifier-api/v1/groups/6324536f-3df4-4eab-8a7c-ae00be078d0d"))
        .withHeader("X-Authentication", equalTo("super_secret_token_string"))
        .willReturn(aResponse()
            .withStatus(204)));

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Node Group Delete Node Group If It Exists Against " + peVersion);
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.nodeGroup credentials: 'pe-test-token', name: 'PE ActiveMQ Broker', delete: true \n" +
          "}", true));
        story.j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        verify(deleteRequestedFor(urlMatching("/classifier-api/v1/groups/6324536f-3df4-4eab-8a7c-ae00be078d0d"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));
      }
    });
  }

  @Theory
  public void puppetNodeDeleteGroupThatDoesNotExistFails(final String peVersion) throws Exception {

    mockNodeManagerService.stubFor(get(urlEqualTo("/classifier-api/v1/groups/6324536f-3df4-4eab-8a7c-ae00be078d0d"))
        .withHeader("X-Authentication", equalTo("super_secret_token_string"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(TestUtils.getAPIResponseBody(peVersion, "/node-manager", "groups.json"))));

    mockNodeManagerService.stubFor(delete(urlEqualTo("/classifier-api/v1/groups/6324536f-3df4-4eab-8a7c-ae00be078d0d"))
        .withHeader("X-Authentication", equalTo("super_secret_token_string"))
        .willReturn(aResponse()
            .withStatus(404)));

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Node Group Delete Node Group If It Does Not Exist Against " + peVersion);
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.nodeGroup credentials: 'pe-test-token', name: 'PE ActiveMQ Broker', delete: true \n" +
          "}", true));
        story.j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        verify(deleteRequestedFor(urlMatching("/classifier-api/v1/groups/6324536f-3df4-4eab-8a7c-ae00be078d0d"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));
      }
    });
  }
}
