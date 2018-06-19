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
public class PuppetJobStepTest extends Assert {

  public static @DataPoints String[] PEVersions = {"2016.2","2016.4"};

  @ClassRule
  public static WireMockRule mockOrchestratorService = new WireMockRule(options()
    .dynamicPort()
    .httpsPort(8143)
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

  private void stubJobDeploySuccessful(String peVersion) {
    mockOrchestratorService.stubFor(post(urlEqualTo("/orchestrator/v1/command/deploy"))
        .withHeader("content-type", equalTo("application/json"))
        .withHeader("X-Authentication", equalTo("super_secret_token_string"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(TestUtils.getAPIResponseBody(peVersion, "/orchestrator/v1/command/deploy", "job_deploy.json"))));

    mockOrchestratorService.stubFor(get(urlEqualTo("/orchestrator/v1/jobs/711"))
        .withHeader("X-Authentication", equalTo("super_secret_token_string"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(TestUtils.getAPIResponseBody(peVersion, "/orchestrator/v1/jobs/711", "job_details.json"))));

    mockOrchestratorService.stubFor(get(urlEqualTo("/orchestrator/v1/jobs/711/report"))
        .withHeader("X-Authentication", equalTo("super_secret_token_string"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(TestUtils.getAPIResponseBody(peVersion, "/orchestrator/v1/jobs/711/", "job_report.json"))));

    mockOrchestratorService.stubFor(get(urlEqualTo("/orchestrator/v1/jobs/711/nodes"))
        .withHeader("X-Authentication", equalTo("super_secret_token_string"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(TestUtils.getAPIResponseBody(peVersion, "/orchestrator/v1/jobs/711/nodes", "job_node_results.json"))));
  }

  @Theory
  public void puppetJobSeparateCredentialsCallSuccessful(final String peVersion) throws Exception {

    stubJobDeploySuccessful(peVersion);

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        //Create a job where the credentials are defined separately
        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Job with Credentials Defined Separately Against " + peVersion);
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.credentials 'pe-test-token'\n" +
          "  puppet.job 'production'\n" +
          "}", true));
        story.j.assertBuildStatusSuccess(job.scheduleBuild2(0));

        verify(postRequestedFor(urlMatching("/orchestrator/v1/command/deploy"))
            .withRequestBody(equalToJson("{\"environment\": \"production\", \"noop\": false}"))
            .withHeader("Content-Type", matching("application/json"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));

        verify(getRequestedFor(urlMatching("/orchestrator/v1/jobs/711"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));

        verify(getRequestedFor(urlMatching("/orchestrator/v1/jobs/711/nodes"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));
      }
    });
  }

  @Theory
  public void puppetJobCredentialsInMethodSuccessful(final String peVersion) throws Exception {

    stubJobDeploySuccessful(peVersion);

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        //Create a job where the credentials are defined as part of the job method call
        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Job with Credentials Defined With Method Call Against " + peVersion);
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  jobId = puppet.job 'production', credentials: 'pe-test-token'\n" +
          "  echo \"Returned job id: ${jobId}\"" +
          "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();
        story.j.assertBuildStatusSuccess(result);
        story.j.assertLogContains("Successfully started Puppet job 711", result);
        story.j.assertLogContains("Puppet Job Name: 711", result);
        story.j.assertLogContains("State: finished", result);
        story.j.assertLogContains("Environment: production", result);
        story.j.assertLogContains("Nodes: 11", result);
        story.j.assertLogContains("Returned job id: 711", result);

        if (peVersion.equals("2016.2")) {
          //Previous to 2016.4, PE did not support corrective changes
          story.j.assertLogContains("Resource Events: 0 failed   0 changed   0 skipped", result);
        } else {
          story.j.assertLogContains("Resource Events: 0 failed   0 changed   0 corrective   0 skipped", result);
        }

        verify(postRequestedFor(urlMatching("/orchestrator/v1/command/deploy"))
            .withRequestBody(equalToJson("{\"environment\": \"production\", \"noop\" : false}"))
            .withHeader("X-Authentication", matching("super_secret_token_string"))
            .withHeader("Content-Type", matching("application/json")));

        verify(getRequestedFor(urlMatching("/orchestrator/v1/jobs/711"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));

        verify(getRequestedFor(urlMatching("/orchestrator/v1/jobs/711/report"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));

        verify(getRequestedFor(urlMatching("/orchestrator/v1/jobs/711/nodes"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));
      }
    });
  }

  @Theory
  public void puppetJobNonExistantNodeFails(final String peVersion) throws Exception {
    mockOrchestratorService.stubFor(post(urlEqualTo("/orchestrator/v1/command/deploy"))
        .withHeader("content-type", equalTo("application/json"))
        .withHeader("X-Authentication", equalTo("super_secret_token_string"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(TestUtils.getAPIResponseBody(peVersion, "/orchestrator/v1/command/deploy", "job_deploy.json"))));

    mockOrchestratorService.stubFor(get(urlEqualTo("/orchestrator/v1/jobs/711"))
        .withHeader("X-Authentication", equalTo("super_secret_token_string"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(TestUtils.getAPIResponseBody(peVersion, "/orchestrator/v1/jobs/711", "job_node_does_not_exist.json"))));

    mockOrchestratorService.stubFor(get(urlEqualTo("/orchestrator/v1/jobs/711/nodes"))
        .withHeader("X-Authentication", equalTo("super_secret_token_string"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(TestUtils.getAPIResponseBody(peVersion, "/orchestrator/v1/jobs/711/nodes", "nodes_does_not_exist.json"))));

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        //Create a job where the credentials are defined as part of the job method call
        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Job Using List of Nodes Where Node Does Not Exist Fails Against " + peVersion);
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.credentials 'pe-test-token' \n" +
          "  puppet.job 'production', nodes: ['doesnotexist'] \n" +
          "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();
        story.j.assertBuildStatus(Result.FAILURE, result);
        story.j.assertLogContains("Error running puppet on doesnotexist: doesnotexist is not connected to the PCP broker", result);

        verify(postRequestedFor(urlMatching("/orchestrator/v1/command/deploy"))
            .withRequestBody(equalToJson("{\"environment\": \"production\", \"noop\" : false}"))
            .withHeader("X-Authentication", matching("super_secret_token_string"))
            .withHeader("Content-Type", matching("application/json")));

        verify(getRequestedFor(urlMatching("/orchestrator/v1/jobs/711"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));

        verify(getRequestedFor(urlMatching("/orchestrator/v1/jobs/711/nodes"))
            .withHeader("X-Authentication", matching("super_secret_token_string")));
      }
    });
  }

  @Test
  public void puppetJobFailsOnNoSuchEnvironment() throws Exception {

    mockOrchestratorService.stubFor(post(urlEqualTo("/orchestrator/v1/command/deploy"))
        .withHeader("content-type", equalTo("application/json"))
        .willReturn(aResponse()
            .withStatus(404)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"kind\": \"puppetlabs.orchestrator/unknown-environment\",\"msg\": \"Unknown environment nosuchenv\",\"details\": {\"environment\": \"nosuchenv\" }}")));

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Job of Non-Existent Environment Fails");
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.job 'nosuchenv', credentials: 'pe-test-token'\n" +
          "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();
        story.j.assertBuildStatus(Result.FAILURE, result);
        story.j.assertLogContains("Kind:    puppetlabs.orchestrator/unknown-environment", result);
      }
    });
  }

  @Theory
  public void puppetJobFailsOnExpiredToken(final String peVersion) throws Exception {

    mockOrchestratorService.stubFor(post(urlEqualTo("/orchestrator/v1/command/deploy"))
        .withHeader("content-type", equalTo("application/json"))
        .willReturn(aResponse()
            .withStatus(401)
            .withHeader("Content-Type", "application/json")
            .withBody(TestUtils.getAPIResponseBody(peVersion, "/orchestrator/v1/command/deploy", "expired_token.json"))));


    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Job Fails on Expired Token Against " + peVersion);
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.job 'production', credentials: 'pe-test-token'\n" +
          "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();
        story.j.assertBuildStatus(Result.FAILURE, result);
        story.j.assertLogContains("Kind:    puppetlabs.rbac/token-expired", result);
      }
    });
  }

  @Test
  public void puppetJobParameters() throws Exception {

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Job Passes Application Parameter");
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.credentials 'pe-test-token'\n" +
          "  puppet.job 'production', application: 'MyApp'\n" +
          "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();

        verify(postRequestedFor(urlMatching("/orchestrator/v1/command/deploy"))
            .withRequestBody(equalToJson("{\"environment\": \"production\", \"scope\": {\"application\": \"MyApp\"}, \"noop\": false}"))
            .withHeader("X-Authentication", matching("super_secret_token_string"))
            .withHeader("Content-Type", matching("application/json")));
      }
    });

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Job Passes Target Parameter");
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.credentials 'pe-test-token'\n" +
          "  puppet.job 'production', target: 'MyApp'\n" +
          "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();

        verify(postRequestedFor(urlMatching("/orchestrator/v1/command/deploy"))
            .withRequestBody(equalToJson("{\"environment\": \"production\", \"target\": \"MyApp\", \"noop\": false}"))
            .withHeader("X-Authentication", matching("super_secret_token_string"))
            .withHeader("Content-Type", matching("application/json")));
      }
    });

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Job Passes Nodes Parameter");
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.credentials 'pe-test-token'\n" +
          "  puppet.job 'production', nodes: ['node1','node2']\n" +
          "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();

        verify(postRequestedFor(urlMatching("/orchestrator/v1/command/deploy"))
            .withRequestBody(equalToJson("{\"environment\": \"production\", \"scope\": {\"nodes\": [\"node1\",\n\"node2\"]}, \"noop\" : false}"))
            .withHeader("X-Authentication", matching("super_secret_token_string"))
            .withHeader("Content-Type", matching("application/json")));
      }
    });

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Job Passes Query Parameter");
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.credentials 'pe-test-token'\n" +
          "  puppet.job 'production', query: 'inventory[certname] {environment = \"production\"}'\n" +
          "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();

        verify(postRequestedFor(urlMatching("/orchestrator/v1/command/deploy"))
            .withRequestBody(equalToJson("{\"environment\": \"production\", \"scope\": {\"query\": \"inventory[certname] {environment = \\\"production\\\"}\"}, \"noop\": false}"))
            .withHeader("X-Authentication", matching("super_secret_token_string"))
            .withHeader("Content-Type", matching("application/json")));
      }
    });

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Job Passes Concurrency Parameter");
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.credentials 'pe-test-token'\n" +
          "  puppet.job 'production', concurrency: 40\n" +
          "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();

        verify(postRequestedFor(urlMatching("/orchestrator/v1/command/deploy"))
            .withRequestBody(equalToJson("{\"environment\": \"production\", \"concurrency\": 40, \"noop\": false}"))
            .withHeader("X-Authentication", matching("super_secret_token_string"))
            .withHeader("Content-Type", matching("application/json")));
      }
    });

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Job Passes Noop Parameter");
        job.setDefinition(new CpsFlowDefinition(
          "node { \n" +
          "  puppet.credentials 'pe-test-token'\n" +
          "  puppet.job 'production', noop: true\n" +
          "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();

        verify(postRequestedFor(urlMatching("/orchestrator/v1/command/deploy"))
            .withRequestBody(equalToJson("{\"environment\": \"production\", \"noop\": true}"))
            .withHeader("X-Authentication", matching("super_secret_token_string"))
            .withHeader("Content-Type", matching("application/json")));
      }
    });
  }

  @Theory
  public void puppetJobFailsOnNoNodesDefined(final String peVersion) throws Exception {
    //2016.2 did not support appliction parameter for orchestrator jobs
    assumeThat(peVersion, is(not("2016.2")));

    mockOrchestratorService.stubFor(post(urlEqualTo("/orchestrator/v1/command/deploy"))
    .withHeader("content-type", equalTo("application/json"))
    .willReturn(aResponse()
      .withStatus(400)
      .withHeader("Content-Type", "application/json")
      .withBody(TestUtils.getAPIResponseBody(peVersion, "/orchestrator/v1/command/deploy", "job_failure_no_nodes_defined.json"))));

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Job Fails on No Nodes Defined Against " + peVersion);
        job.setDefinition(new CpsFlowDefinition(
        "node { \n" +
        "  puppet.job 'production', nodes: [], credentials: 'pe-test-token'\n" +
        "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();
        story.j.assertBuildStatus(Result.FAILURE, result);
        story.j.assertLogContains("Kind:    puppetlabs.orchestrator/empty-target", result);
      }
    });
  }

  @Theory
  public void puppetJobFailsOnNoNodesMatchingQuery(final String peVersion) throws Exception {
    //2016.2 did not support PQL for orchestrator jobs
    assumeThat(peVersion, is(not("2016.2")));

    mockOrchestratorService.stubFor(post(urlEqualTo("/orchestrator/v1/command/deploy"))
    .withHeader("content-type", equalTo("application/json"))
    .willReturn(aResponse()
      .withStatus(400)
      .withHeader("Content-Type", "application/json")
      .withBody(TestUtils.getAPIResponseBody(peVersion, "/orchestrator/v1/command/deploy", "job_failure_no_nodes_match_query.json"))));

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Job Fails on No Nodes Matching Query Against " + peVersion);
        job.setDefinition(new CpsFlowDefinition(
        "node { \n" +
        "  puppet.job 'production', query: 'nodes { certname = \"doesnotexist\"}', credentials: 'pe-test-token'\n" +
        "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();
        story.j.assertBuildStatus(Result.FAILURE, result);
        story.j.assertLogContains("Kind:    puppetlabs.orchestrator/empty-target", result);
      }
    });
  }

  @Theory
  public void puppetJobFailsOnEmptyApplicationName(final String peVersion) throws Exception {
    //2016.2 did not support appliction parameter for orchestrator jobs
    assumeThat(peVersion, is(not("2016.2")));

    mockOrchestratorService.stubFor(post(urlEqualTo("/orchestrator/v1/command/deploy"))
    .withHeader("content-type", equalTo("application/json"))
    .willReturn(aResponse()
      .withStatus(404)
      .withHeader("Content-Type", "application/json")
      .withBody(TestUtils.getAPIResponseBody(peVersion, "/orchestrator/v1/command/deploy", "job_failure_no_application_defined.json"))));

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Job Fails on Empty Application Name Against " + peVersion);
        job.setDefinition(new CpsFlowDefinition(
        "node { \n" +
        "  puppet.credentials 'pe-test-token' \n" +
        "  puppet.job 'production', application: \"\"\n" +
        "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();
        story.j.assertBuildStatus(Result.FAILURE, result);
        story.j.assertLogContains("Kind:    puppetlabs.orchestrator/unknown-target", result);
      }
    });
  }

  @Theory
  public void puppetJobFailsOnEmptyQuery(final String peVersion) throws Exception {
    //2016.2 did not support PQL for orchestrator jobs
    assumeThat(peVersion, is(not("2016.2")));

    mockOrchestratorService.stubFor(post(urlEqualTo("/orchestrator/v1/command/deploy"))
    .withHeader("content-type", equalTo("application/json"))
    .willReturn(aResponse()
      .withStatus(400)
      .withHeader("Content-Type", "application/json")
      .withBody(TestUtils.getAPIResponseBody("2016.4", "/orchestrator/v1/command/deploy", "job_failure_empty_query.json"))));

    story.addStep(new Statement() {
      @Override
      public void evaluate() throws Throwable {

        WorkflowJob job = story.j.jenkins.createProject(WorkflowJob.class, "Puppet Job Fails on Empty Query");
        job.setDefinition(new CpsFlowDefinition(
        "node { \n" +
        "  puppet.credentials 'pe-test-token' \n" +
        "  puppet.job 'production', application: \"\"\n" +
        "}", true));
        WorkflowRun result = job.scheduleBuild2(0).get();
        story.j.assertBuildStatus(Result.FAILURE, result);
        story.j.assertLogContains("Kind:    puppetlabs.orchestrator/query-error", result);
      }
    });
  }
}
