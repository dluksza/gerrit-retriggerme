Configuration
=============

## Project configuration

Per project configuration of the @PLUGIN@ plugin is done in the
`project.config` file of the project. Missing values are inherited
from the parent projects. This means a global default configuration can
be done in the `project.config` file of the `All-Projects` root project.
Other projects can then override the configuration in their own
`project.config` file.

```
[plugin "@PLUGIN@"]
    jenkinsUrl = https://jenkins.foo.com/
    selfName = gerrit.foo.com
```

plugin.@PLUGIN@.jenkinsUrl
:	The URL of the Jenkins server. It must be end with a slash.

plugin.@PLUGIN@.selfName
:	The name of the Gerrit trigger configuration for this Gerrit instance
	on the Jenkins server.

Alternatively, global default configuration of the @PLUGIN@ plugin can
be stored in `gerrit.config` file in the site's `etc` directory. These
values are used if they are not specified in the project.

## Authentication

Authentication with the Jenkins server is configured globally in
`$site_path/etc/secure.config`. The file should contain a section for
each jenkins server which require authentication.

If no authentication information is provided, anonymous access is used.

```
[@PLUGIN@ "https://jenkins.foo.com/"]
    userName = foo.bar
    token = 1564716325471
```

@PLUGIN@.\<jenkinsUrl>.userName
:	the username to use to connect to Jenkins

@PLUGIN@.\<jenkinsUrl>.token
:	the api token of that user. The API token is available in a users'
	Jenkins personal configuration page: click your name on the top right
	corner on every page, then click "Configure" to see your API token.
	The API token can also changed from here.
