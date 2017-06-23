package com.switchtrue.jira.changelog;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.JiraRestClientFactory;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;

@Deprecated
public class JiraAPIBasicAuth extends JiraAPI {
	
	private final String username_, password_; 
	
	public JiraAPIBasicAuth(String username, String password, String URL, String jql, String descriptionField, String fixVersionRestrictMode, String fixVersionRestrictTerm) {
		super(URL, jql, descriptionField, fixVersionRestrictMode, fixVersionRestrictTerm);
		username_ = username;
		password_ = password;
	}
	
	@Override
	public JiraRestClient getRestClient() {
		JiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
        final JiraRestClient restClient = factory.createWithBasicHttpAuthentication(jiraServerURI_, username_, password_);
        return restClient;
	}
	
}
