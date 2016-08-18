Pivotal Tile Build Process
==========================

Pivotal Cloud Foundry allows deployment of the Cloud Foundry ForgeRock Broker via a Pivotal tile package. The build process can be performed using a Docker container, or locally if the necessary dependencies are present.

**Note:**
All steps below are performed from inside the folder containing the **cloudfoundry-service-broker-openam** project.

### Building the Pivotal Tile with Docker
The Pivotal Tile can be built using a Docker container pre-configured with the necessary dependencies required for the build.
1. Build the Docker container
    `docker build -t cf-build:latest ./docker-build`
2. Run the Docker container
    `docker run --rm --volume `pwd`:/app cf-build:latest`

### Building the Pivotal Tile without Docker
If Docker is unavailable, the build process can be run locally, but will require the installation of the build dependencies.
1. If necessary install the dependencies:
`apt-get install curl git python python-pip ruby`
2. Install the BOSH CLI:
`gem install bosh_cli --no-ri --no-rdoc`
3. Run the build script
`./build-sh`

Both the Docker build and local build will place a forgerock-broker-{version}.pivotal file in the target directory of the **cloudfoundry-service-broker-openam** project.
