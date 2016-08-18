#!/bin/bash

export ARTIFACT_URI=https://maven.forgerock.org/repo/releases/org/forgerock/cloudfoundry-service-broker-openam/
export WORKSPACE_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
export BUILD_DIR=/tmp/pivotal
export VERSION=`curl --silent $ARTIFACT_URI | cut -d\" -f2 | grep '[0-9].[0-9].[0-9]' | cut -d/ -f1 | sort -r | head -n1`

if [ $? -eq 0 ]; then
  echo Building Tile for CloudFoundry ForgeRock Broker $VERSION
 
  cp -R $WORKSPACE_DIR/pivotal $BUILD_DIR
  cd $BUILD_DIR

  echo Cloning tile-generator from GitHub
  git clone -q https://github.com/cf-platform-eng/tile-generator.git $BUILD_DIR/tile-generator

  echo Installing tile-generator dependencies
  pip install -r ./tile-generator/requirements.txt

  echo Filtering tile.yml ready for build
  sed -e 's/\${version}/'"$VERSION"'/' tile-template.yml > tile.yml

  echo Copying Broker artifact from Artifactory
  curl --silent $ARTIFACT_URI$VERSION/cloudfoundry-service-broker-openam-$VERSION.war -o $BUILD_DIR/resources/forgerock-broker-$VERSION.zip

  $BUILD_DIR/tile-generator/bin/tile build $VERSION

  if [ ! -d "$WORKSPACE_DIR/target" ]; then
    mkdir $WORKSPACE_DIR/target
  fi

  cp $BUILD_DIR/product/forgerock-broker*.pivotal $WORKSPACE_DIR/target
  chmod -R go+w $WORKSPACE_DIR/target
  cd $WORKSPACE_DIR

  echo Tidying up
  rm -rf $BUILD_DIR
else
  echo "Unable to determine latest version of the CloudFoundry Service Broker from maven.forgerock.org"
fi
