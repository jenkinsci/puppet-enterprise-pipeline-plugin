### Master Build Status:  [![Build Status](https://ci.jenkins.io/buildStatus/icon?job=Plugins/puppet-enterprise-pipeline-plugin/master)](https://ci.jenkins.io/job/Plugins/job/puppet-enterprise-pipeline-plugin/job/master/)

### Table of contents

1. [Introduction](#introduction)
2. [Configuration](#configuration)
    * [Puppet Master Address](#puppet-master-address)
    * [Access Token Credentials](#access-token-credentials)
    * [Hiera](#hiera)
      * [Hiera 3](#hiera-3)
      * [Hiera 5](#hiera-5)
      * [Hiera HTTP Authentication](#hiera-http-authentication)
      * [Hiera Data Store Permissions](#hiera-data-store-permissions)
      * [Caution](#caution)
3. [Pipeline Steps](#pipeline-steps)
    * [puppet.credentials](#puppetcredentials)
    * [puppet.query](#puppetquery)
    * [puppet.codeDeploy](#puppetcodedeploy)
    * [puppet.job](#puppetjob)
    * [puppet.hiera](#puppethiera)
    * [puppet.waitForNodes](#puppetwaitfornodes)
4. [Compatibility](#compatibility)
  

## Introduction

This plugin adds Jenkins Pipeline steps for Puppet Enterprise. The provided
steps make it easy to interface with Puppet Enterprise services such as the
Code Manager, PuppetDB, Node Manager, and orchestrator service.

A Jenkins Pipeline project can use the provided step methods to deploy Puppet code to
Puppet Enterprise servers, create Puppet orchestrator jobs, query the infrastructure
using [Puppet Query Language (PQL)](https://docs.puppet.com/puppetdb/latest/api/query/v4/pql.html)

Puppet Enterprise RBAC access tokens are used to authenticate with the Puppet
Enterprise APIs, so Puppet itself doesn't have to be installed or configured on the Jenkins
server.

**Example**
```
node {
    puppet.credentials 'pe-access-token'
    puppet.codeDeploy 'production'
    puppet.job 'production', application: 'App[instance]', noop: true, concurrency: 10
}
```


## Configuration

### Puppet Master Address

Go to Jenkins > Manage Jenkins > Puppet Enterprise.  Fill out the DNS address
of the Puppet Enterprise Server.  Note, if the Puppet agent is installed on the
Jenkins server, it will be used to configure the Puppet Enterprise Server
address.

The Puppet Enterprise Server CA certificate is automatically pulled from the
Puppet Server's CA API. External CA's are not currently supported.

### Access Token Credentials

This plugin uses the [Plain Credentials plugin](https://wiki.jenkins-ci.org/display/JENKINS/Plain+Credentials+Plugin) to store access tokens.  

First, create a new RBAC access token in Puppet Enterprise. Follow the instructions for [generating a token for use by a service](https://docs.puppet.com/pe/latest/rbac_token_auth.html#generating-a-token-for-use-by-a-service).

Second, create new credentials in Jenkins. For **Kind** specify **Secret
text**.  Set the **Secret** value to the token printed out by the first step.
It is recommended to give the token an easy-to-identiy **ID** by clicking on
the **Advanced** button. That will make it easier to identify which token is
being used from the Jenkins Pipeline scripts.

### Hiera

This plugin provides a Hiera key/value store that Jenkins Pipeline jobs can 
use to set key/value pairs that Hiera can do lookups on. Key/value pairs are 
set using the provided [puppet.hiera](#pipeline-steps-puppet.hiera) method. 
Pairs are assigned to "scopes" which are arbitrary.

##### Hiera 3

For Hiera 3 (included in Puppet Enterprise up until Puppet Enterprise 2017.1),
this plugin relies on the [hiera-http](https://github.com/crayfishx/hiera-http) backend 
to perform key lookups.

Below is an example hiera.yaml configuration. To learn more about configuring Hiera in 
Puppet Enterprise, go to the [docs page](https://docs.puppet.com/hiera/3.3/configuring.html)

```
:backends:
  - http

:http:
  :host: jenkins.example.com
  :port: 8080
  :output: json
  :use_auth: true
  :auth_user: <user>
  :auth_pass: <pass>
  :cache_timeout: 10
  :failure: graceful
  :paths:
    - /hiera/lookup?scope=%{clientcert}&key=%{key}
    - /hiera/lookup?scope=%{environment}&key=%{key}
```

##### Hiera 5

** **comming soon** **

#### Hiera HTTP authentication

If Jenkins' Global Security is configured to allow unauthenticated read-only
access, it's unnecessary to configure the hiera.yaml to use HTTP authentication. 
Otherwise, create a user in Jenkins that has Overall/Read permissions and use that user's 
credentials for the hiera.yaml configuration.

#### Hiera Data Store permissions

If Jenkins' Global Security is configured to use matrix authorization, any user
with the Hiera/View permission is allowed to view the Hiera Data Store page and
any user with the Hiera/Delete permission can delete scopes and keys.  

Note, these permissions have no effect on the ability to lookup specific Hiera
keys using the /hiera/lookup endpoint.

#### Caution

The Hiera key/value pairs are stored in an XML file on the
Jenkins server. There is no audit history of the data and 
therefor no way to replicate past values. Also, if the file 
is lost due to disk failure, for example, the current values 
are lost until the necessary pipelines are run again to reset 
the key/value pairs.

Only use the Hiera Data Store if you trust your
Jenkins server backups and don't care about audit 
history for the Hiera key/value pairs set in Jenkins jobs.

## Pipeline Steps

### puppet.credentials

The `puppet.credentials` method sets the Puppet Enterprise RBAC token to be
used for all other Puppet pipeline step methods. It is only available in 
[Scripted Pipelines](https://jenkins.io/doc/book/pipeline/syntax/#scripted-pipeline), not [Declarative Pipelines](https://jenkins.io/doc/book/pipeline/syntax/#declarative-pipeline).

**Scripted Pipeline invocation**: puppet.credentials 'jenkins-credential-id'

**Example**

```
  puppet.credentials 'pe-access-token'
```

### puppet.query

The `puppet.query` method queries PuppetDB using the [Puppet Query Language (PQL)](https://docs.puppet.com/puppetdb/5.0/api/query/v4/pql.html).

This method returns an ArrayList object that can be stored in a variable and iterated on.

* **Scripted Pipeline invocation**: puppet.query('query', ...parameters...)
* **Declarative Pipeline invocation**: puppetQuery('query', ...parameters...)

**Parameters**

* extract - The key to extract from each item that matches the query. Query result items that do not have the key are discarded. Sub-hash keys can be matched using dot syntax (see examples below). String.
* credentials - The Jenkins credentials storing the PE RBAC token. String. Required if:
  * puppet.credentials not used in a Scripted Pipeline
  * the pipeline is a Declarative Pipeline. For declarative pipelines, use "credentialsId".

**Example**

```
  puppet.query 'nodes { catalog_environment = "staging" }', credentials: 'pe-access-token'
  results = puppet.query 'inventory { trusted.extensions.pp_role = "MyApp" }'

  //The following gets production nodes with failed report, extracts just their
  // certnames, then runs Puppet on them.
  results = puppet.query 'nodes { latest_report_status = "failed" and catalog_environment = "production"}', extract: 'certname'
  puppet.job 'production', nodes: results


  //The following gets all networking facts and extracts the ip value for the eth0 interfaces.
  //If a host does not have a eth0 interface, it is discarded.
  results =  puppet.query 'facts { name = "networking"  }', extract: 'value.interfaces.eth0.ip'
  // results = ["192.168.0.130", "10.0.0.152", "10.0.0.13"]
```

### puppet.codeDeploy

The `puppet.codeDeploy` method tells Puppet Enterprise to deploy new Puppet code,
Hiera data, and modules to a specified Puppet environment. To lean more about
code management in Puppet Enterprise, go here: [https://docs.puppet.com/pe/latest/code_mgr.html]

* **Scripted Pipeline invocation**: puppet.codeDeploy('environment', ...parameters...)
* **Declarative Pipeline invocation**: puppetCodeDeploy('environment', ...parameters...)

**Parameters**

* credentials - The Jenkins credentials storing the PE RBAC token. String. Required if:
  * puppet.credentials not used in a Scripted Pipeline
  * the pipeline is a Declarative Pipeline

**Example**

```
  puppet.codeDeploy 'production', credentials: 'pe-access-token'
  puppet.codeDeploy 'staging'
```

### puppet.job

The `puppet.job` step method creates [Puppet orcehstrator](https://docs.puppet.com/pe/latest/orchestrator_intro.html) jobs,
waits for them to finish, and reports on changes that took place, if any.

* **Scripted Pipeline invocation**: puppet.job('environment', ...parameters...)
* **Declarative Pipeline invocation**: puppetJob('environment', ...parameters...)

**Parameters**

* concurrency - Level of maximum concurrency when issuing Puppet runs. Defaults to unlimited. Integer.
* noop - Whether to run Puppet in noop mode. Defaults to false. Boolean
* reports - The type of reports you'd like to be printed to Jenkins console. See options in **reports** section below. Defaults to nodeSummary. Array of Strings.
* credentials - The Jenkins credentials storing the PE RBAC token. String. Required if:
  * puppet.credentials not used in a Scripted Pipeline
  * the pipeline is a Declarative Pipeline. For declarative pipelines, use "credentialsId".

**Puppet Enterprise 2016.2 - 2016.3 Parameters**

The following parameters should be used with Puppet Enterprise 2016.2 - 2016.3 for definining the job's run target.
Note, the target parameter will work with Puppet Enterprise 2016.4+ but has been deprecated.

* target - Target in environment to deploy to. Can be app, app instance, or app component. Defaults to entire environment. String

**Puppet Enterprise 2016.4+ Parameters**

The following parameters should be used with Puppet Enterprise 2016.4+ for definining the job's run scope.

* nodes - An array of nodes to run Puppet on.
* application - The name of the application to deploy to. Can be all instances or a specific instance. e.g 'MyApp' or 'MyApp[instance-1]'. String.
* query - The PQL query to determine the list of nodes to run Puppet on. String.

**Reports**

The following report types are available to be printed to the Jenkins console for each job report.
Use the `report` parameter. Multiple reports can be selected by using an array.

- nodeSummary:  Default. Shows a summary of the resource events count with a link to the Puppet Enterprise report.
- nodeChanges:  A list of every resource event per node.
- resourceChanges: A list of every resource event and each node that experienced the change event.

**Example**

```
  puppet.job 'staging'
  puppet.job 'production', concurrency: 10, noop: true
  puppet.job 'production', concurrency: 10, noop: true, credentials: 'pe-access-token'
  puppet.job 'production', nodes: ['node1.example.com','node2.example.com']
  puppet.job 'production', application: Rgbank
  puppet.job 'production', application: Rgbank[phase-1]
  puppet.job 'production', query: 'inventory { certname ~ "substring" and environment = "production" }'
  puppet.job 'production', reports: ['resourceChanges', 'nodeChanges']
```

**Reports**

The following report types are available to be printed to the Jenkins console for each job report.
Use the `report` parameter. Multiple reports can be selected by using an array.

- nodeSummary:  Default. Shows a summary of the resource events count with a link to the Puppet Enterprise report.
- nodeChanges:  A list of every resource event per node.
- resourceChanges: A list of every resource event and each node that experienced the change event.


### puppet.nodeGroup

This pipeline step creates, modifies, and deletes node groups in Puppet Enterprise's Node Manager.

**Procedural pipeline invocation**: puppet.nodeGroup
**Declarative piepline invocation**: puppetNodeGroup

**Parameters**

* name - The name of the node group. Required. String
* description - The description of the node group. String
* environment - The environment of the node group. String
* rule - The rules that nodes must match to automatically be added to the node group. See the [node manager rules grammer](https://docs.puppet.com/pe/latest/nc_groups.html#rule-condition-grammar) for syntax.
* parent - The name of the node group's parent group. Required if the node group doesn't exist already. String.
* environmentTrumps - Whether the environment for the group should be the environment member nodes should have their catalog compiled for. Boolean
* classes - The classes that should be assigned to the node group. Hash
* variables - The global variables that should be assigned to the node group. Hash
* delete - Wether or not to delete the group. It will error if the group does not exist. Boolean
* credentials - The Jenkins credentials storing the PE RBAC token. Required if puppet.credentials not used. String

```
  puppet.nodeGroup name: "App Group", parent: "All Nodes", description: "Made by Jenkins", classes: ["class_name": ["parameterkey":"parametervalue"]], rule: ["=", ["trusted","extensions","pp_application"], "app"]
  puppet.nodeGroup name: "App Group", classes: ["class_name": ["parameterkey":"NEWvalue"]]
  puppet.nodeGroup name: "App Group", delete: true
```


### puppet.hiera

* **Scripted Pipeline invocation**: puppet.hiera
* **Declarative Pipeline invocation**: puppetHiera

**Parameters**

* path - The path (scope) of the data lookup from Hiera. Usually this will be an environment name. Required. String
* key - The name of the key that Hiera will lookup. Required. String
* value - The value of the key to be returned to Hiera's lookup call. Required. Can be string, array, or hash

**Example**

```
  puppet.hiera scope: 'staging', key: 'app-build-version', value: 'master'
  puppet.hiera scope: 'production', key: 'app-build-version', value: '8f3ea2'
  puppet.hiera scope: 'dc1-us-example', key: 'list-example', value: ['a,'b','c']
  puppet.hiera scope: 'host.example.com', key: 'hash-example', value: ['a':1, 'bool':false, 'c': 'string']
```

### puppet.waitForNodes

**NOTE**: This step requires Puppet Enterprise 2016.5+

This pipeline step takes a list of nodes and waits up to 30 minutes for them to join the Puppet Enterprise orchestrator (PXP broker).
This is useful for dynamically provisioning VMs in the pipeline and waiting for them to be ready before kicking off a Puppet orchestrator job.

* **Scripted pipeline invocation**: puppet.waitForNodes
* **Declarative pipeline invocation**: puppetWaitForNodes

**Parameters**

* credentials - The Jenkins credentials storing the PE RBAC token. String. Required if:
  * puppet.credentials not used in a Scripted Pipeline
  * the pipeline is a Declarative Pipeline. For declarative pipelines, use "credentialsId".

**Example**

```
  puppet.waitForNodes(['artifactory.inf.puppet.vm','database-production.pdx.puppet.vm'])
  puppet.waitForNodes(['artifactory.inf.puppet.vm','database-production.pdx.puppet.vm'], credentials: 'access-token')
```


## Compatibility

This plugin is compatible with Puppet Enterprise 2016.2+ and Jenkins 1.642.3+

Some step parameters are only available on newer versions of Puppet Enterprise.
Those parameters are labeled as such.

This plugin has not been tested with Jenkins Declarative Pipelines.
