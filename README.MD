Cloud Foundry Broker
====================

The Cloud Foundry broker allows applications running inside Cloud Foundry to obtain OAuth2 credentials for authentication from an externally hosted OpenAM instance. The broker can be hosted inside or outside of Cloud Foundry as the end user requires.

Prerequisites
-------------

Installation and configuration of the ForgeRock Cloud Foundry Broker requires an installation of the Cloud Foundry CLI tools, which are available from [https://github.com/cloudfoundry/cli](https://github.com/cloudfoundry/cli)

Configuring OpenAM for use with the broker
------------------------------------------

It is advised to create specific user credentials to allow the Cloud Foundry Broker to create and delete OAuth2 clients. 

1. Log in to OpenAM.
2. Navigate to the realm in which the broker will create/delete OAuth2 clients.
3. Create a new user with the required username and password.
4. Create a group for the broker user.
5. Select the Privilege tab and add "Read and write access to all configured Agents" privilege to the group.

Deploying the Cloud Foundry Broker to Pivotal Cloud Foundry
-----------------------------------------------------------

Pivotal Cloud Foundry allows the deployment of applications and brokers via a tile package rather than using the command line. This simplifies the deployment process, removing a majority of the command line operations.

1. Log in to Pivotal Cloud Foundry OpsMgr.
2. Click on "Import a Product" on the bottom left of the screen.
3. Navigate to and upload the ForgeRock broker Pivotal package.
4. The "Pivotal Cloud Foundry ForgeRock Broker" tile will be displayed on the Installation Dashboard.
5. Click on the "Pivotal Cloud Foundry ForgeRock Broker" tile.
6. Fill in the details as necessary under the OpenAM section. These comprise of:
	a. Location - the URL of the OpenAM instance (http://host:port/path/openam)
	b. Username - the username of the account to use to create the OAuth2 clients
	c. Password - the password of the account to use to create the OAuth2 clients
	d. Realm - the realm in which to create the OAuth2 clients (specify "/" to use the root realm)
6. Click "Save" and return to the installation dashboard.
7. Click "Apply Changes" to install the "Pivotal Cloud Foundry ForgeRock Broker".
8. At the command line create the service
	`cf create-service openam-oauth2 shared {servicename}`
9. Bind applications as necessary
	`cf bind-service {application} {servicename}`

Removing the Cloud Foundry Broker from Pivotal Cloud Foundry
------------------------------------------------------------

Uninstalling the "Pivotal Cloud Foundry ForgeRock Broker" tile in the OpsMgr does not unbind applications from the broker or remove the OAuth2 clients from OpenAM. Unbinding must be performed before uninstallation.

1. Unbind the application from the service broker
	`cf unbind-service {application} {servicename}`
2. Log in to Pivotal Cloud Foundry OpsMgr.
3. Click on the trashcan icon on the bottom right of the "Pivotal Cloud Foundry ForgeRock Broker" tile.
4. Acknowledge the uninstallation message and click "Apply Changes" to remove the "Pivotal Cloud Foundry ForgeRock Broker".

Deploying the Cloud Foundry Broker into Cloud Foundry
-----------------------------------------------------

The broker can also be deployed into non-Pivotal Cloud Foundry installations, although this requires more involved use of the command line.

1. Unzip the Cloud Foundry Broker WAR file  
	`unzip cloudfoundry-service-broker-openam-{version}.war -d ~/cf-broker`
2. Navigate to the folder containing the extracted WAR file  
	`cd ~/cf-broker`
3. Push the application to Cloud Foundry  
	`cf push forgerockbroker-{version}`
4. Set the required environment variables for the broker  
	`cf set-env forgerockbroker-{version} OPENAM_BASE_URI {location}  
	cf set-env forgerockbroker-{version} OPENAM_USERNAME {username}  
	cf set-env forgerockbroker-{version} OPENAM_PASSWORD {password}  
	cf set-env forgerockbroker-{version} OPENAM_REALM {realm}`
5. Restage the application so that changes to the environment variables are applied  
	`cf restage forgerockbroker-{version}`
6. Find the url for the application
	`cf app forgerockbroker-{version}`
7. Create the service broker. The username and password specified in this command should be different from those used in OpenAM - these are for internal use in Cloud Foundry.  
	`cf create-service-broker forgerockbroker {cf-username} {cf-password} {url}`
8. After the service broker has been created, you must grant access to its service plans.  
	`cf enable-service-access openam-oauth2`
9. Create the service  
	`cf create-service openam-oauth2 shared {servicename}`
10. Bind applications as necessary  
	`cf bind-service {application-to-bind} {servicename}`

Removing the Cloud Foundry Broker from Cloud Foundry
----------------------------------------------------

To remove the Cloud Foundry Broker from Cloud Foundry the bindings, service, broker and application must all be removed in order.

1. Unbind the application(s) from the service broker  
	`cf unbind-service {bound-application} {servicename}`
2. Delete the service  
	`cf delete-service {servicename}`
3. Delete the broker  
	`cf delete-service-broker forgerockbroker`
4. Delete the application  
	`cf delete forgerockbroker-{version}`