JIRA OAuth Configuration
========================

Prerequisites
--------------
* RSA public/private key pair
  * Both Atlassian's web interface and Java libraries expect very specific key formats and key properties. The following commands can be used to generate a compatible key pair:
    * `openssl genrsa -out rsa-key.pem 1024`
    * `openssl rsa -in rsa-key.pem -pubout -out rsa-key.pub`
    * `openssl pkcs8 -topk8 -nocrypt -in rsa-key.pem -out rsa-key_pcks8`
* JIRA administrator privileges to add Application Links and allow/modify application permissions.

Configuring OAuth
-----------------
### Create JIRA Application Link (on JIRA):
* [**Gear icon**] | Administration -> (JIRA Administration) **Applications**
* (Integration) **Application links**
* **Create new link** with a URL, e.g. "https://utils.companyname.com/jenkins", ignore "No response" warning
  * Enter an **Application Name**
  * Select "Generic Application" in **Application Type**
  * **Continue**
* [**Pen icon**] | Edit newly created Application Link row
  * **Incoming Authentication**
    * Enter the **Consumer Key** produced by `java -jar jira-changelog-builder.jar --print-consumer-key`
    * Enter a **Consumer Name**
    * Enter **Public Key** (e.g. `rsa-key.pub` from Prerequisites)
    * **Save**
    
### Request token
* Run `java -jar jira-changelog-builder.jar -r <JIRA_URL> <OAUTH_PRIVATE_KEY>`, where `<JIRA_URL>` is your JIRA base URL and `<OAUTH_PRIVATE_KEY>` is your private key text (e.g. the text in `rsa-key_pcks8` from Pre-requisites. TODO: Read from file if needed).

**Example output**
```
REQUEST_TOKEN is pFAbX576LTIcTl14F9mydPzVqgwxwNtu
TOKEN_SECRET is 6peeSLcri8mf4wLk5xsQz5WtcM1qJemK
Go to and 'Allow' to obtain VERIFIER:
https://companyname.atlassian.net/plugins/servlet/oauth/authorize?oauth_token=pFAbX576LTIcTl14F9mydPzVqgwxwNtu
```

### Allow application access
* Visit URL from step above

**Example**
```
The application JIRAChangelogBuilderFivium would like to have read and write access to your data on companyname.atlassian.net. The application will be allowed to use your credentials to authenticate (FirstName LastName) as you in the future.

Allow Deny

By allowing this, you can use JIRA without storing your JIRA password in JIRAChangelogBuilderFivium.

Click here to view or revoke application permissions.
```
* **Allow** and store VERIFIER for next step.

**Example**
```
Access Approved

You have successfully authorized 'JIRAChangelogBuilderFivium'. Your verification code is 'MDNWNn'. You will need to enter this exact text when prompted. You should write this value down before closing the browser window.
```

### Get access token

* Run `java -jar jira-changelog-builder.jar -a <JIRA_URL> <OAUTH_PRIVATE_KEY> <REQUEST_TOKEN> <TOKEN_SECRET> <VERIFIER>` where `<REQUEST_TOKEN>`, `<TOKEN_SECRET>`, `<VERIFIER>` are values obtained from previous steps.

**Example output**
```
ACCESS_TOKEN is 35FcJ5Jil60NR9srtI6pdZz3MiYHz1qR
```

* Store the ACCESS_TOKEN for later use.
* No further JIRA configuration necessary.
* Use the same private key and access token, from steps above, when running `jira-changelogbuilder.jar`. 
