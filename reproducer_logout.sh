#!/usr/bin/env bash
# check EAP_HOME, if it's set, we'll operate in that directory
if [ -z "$EAP_HOME" ] ; then
    # check if we've been unzipped in a directory with EAP
    # our current path should be $EAP_HOME/reproducer/.
    EAP_HOME=`dirname $PWD`
    if [ ! -f $EAP_HOME/bin/standalone.sh ] ; then
        echo "EAP_HOME has not been set and the reproducer has not been unzipped in EAP home directory either"
        exit 1
    fi
else
    echo "EAP_HOME is set to $EAP_HOME, well be operating in that directory"
fi

# compile webapps
echo "Compiling webapp A, this step requires maven"
mvn -pl webapp_a clean install
RETURN_VALUE=$?

if [ $RETURN_VALUE != 0 ] ; then
  echo "Webapp A compilation failed."
  kill $$
fi

echo "Compiling webapp B, this step requires maven"
mvn -pl webapp_b clean install
RETURN_VALUE=$?

if [ $RETURN_VALUE != 0 ] ; then
  echo "Webapp B compilation failed."
  kill $$
fi

# add user if it doesn't exist (silently does nothing if it does exist)
echo "Adding user"
$EAP_HOME/bin/add-user.sh -a -g User -p secret_Passw0rd -u user -s

# configure SSO (fails with "Duplicate resource" if already configured, but we don't care)
echo "Configuring SSO (If SSO is already configured, expect this step to fail wit \"Duplicate resource\")"
$EAP_HOME/bin/jboss-cli.sh --file=single-sign-on.batch

#Delete standalone deployments
rm -rf $EAP_HOME/standalone/deployments/*

# copy server base directory to $EAP_HOME/standalone2 (deletes and replaces it if it already exists)
if [ -d $EAP_HOME/standalone2 ] ; then
    echo "Deleting old server base directory for second server"
    rm -rf $EAP_HOME/standalone2
fi
echo "Copying server base directory"
cp -r $EAP_HOME/standalone $EAP_HOME/standalone2

#copy standalone deployment into deployments directory (overwrite if already there)
echo "Copying $PWD/webapp_a/target/webapp_a.war into $EAP_HOME/standalone/deployments/webapp_a.war"
cp webapp_a/target/webapp_a.war $EAP_HOME/standalone/deployments/webapp_a.war
#copy standalone2 deployment into deployments directory (overwrite if already there)
echo "Copying $PWD/webapp_b/target/webapp_b.war into $EAP_HOME/standalone2/deployments/webapp_b.war"
cp webapp_b/target/webapp_b.war $EAP_HOME/standalone2/deployments/webapp_b.war

# start EAP
echo "Starting EAP servers (timeout = 20 seconds)"
$EAP_HOME/bin/standalone.sh -c standalone-ha.xml -Djboss.node.name=node1 -Djboss.server.base.dir=$EAP_HOME/standalone -Djboss.socket.binding.port-offset=0 > /dev/null &
sleep 5
$EAP_HOME/bin/standalone.sh -c standalone-ha.xml -Djboss.node.name=node2 -Djboss.server.base.dir=$EAP_HOME/standalone2 -Djboss.socket.binding.port-offset=100 > /dev/null &
sleep 15

# run the test
echo "Running the test itself, this step requires maven"
mvn clean install -pl sso_logout_different_apps
RETURN_VALUE=$?



# stop EAP servers
#echo "Stopping EAP servers"
$EAP_HOME/bin/jboss-cli.sh --connect command=:shutdown
$EAP_HOME/bin/jboss-cli.sh --connect --controller=localhost:10090 command=:shutdown

# evaluate
if [ $RETURN_VALUE == 0 ] ; then
    echo "----------------------------------------"
    echo "----------       PASSED       ----------"
    echo "----------------------------------------"
else
    echo "----------------------------------------"
    echo "----------       FAILED       ----------"
    echo "----------------------------------------"
fi
