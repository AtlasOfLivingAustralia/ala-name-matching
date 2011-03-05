#!/bin/sh
# Amazon EC2 setup

# new key pair for ap-southeast-1
ec2-add-keypair apac_keypair --region ap-southeast-1 > id-apac-keypair
chmod 600 id-apac-keypair
ssh-add id-apac-keypair
ec2-run-instances ami-32423c60 -k apac_keypair --region ap-southeast-1

# ssh in
ssh -i id-apac-keypair ubuntu@ec2-122-248-212-55.ap-southeast-1.compute.amazonaws.com

# setup SUN Java
wget http://cds.sun.com/is-bin/INTERSHOP.enfinity/WFS/CDS-CDS_Developer-Site/en_US/-/USD/VerifyItem-Start/jdk-6u24-linux-x64.bin?BundledLineItemUUID=gm.J_hCx4zsAAAEuJTsWt4Bg&OrderID=aoyJ_hCxndcAAAEuFTsWt4Bg&ProductID=oSKJ_hCwOlYAAAEtBcoADqmS&FileName=/jdk-6u24-linux-x64.bin
mv jdk-6u24-linux-x64.bin\?BundledLineItemUUID\=gm.J_hCx4zsAAAEuJTsWt4Bg jdk-sun
chmod 777 jdk-sun
./jdk-sun

# setup cassandra
wget http://apache.mirror.aussiehq.net.au//cassandra/0.6.12/apache-cassandra-0.6.12-bin.tar.gz
gzip -d apache*
tar xvf apache*
ln -s apache-cassandra-0.6.12 cassandra

# cassandra config
cd /home/ubuntu/cassandra/conf/
rm storage-conf.xml
wget http://ala-portal.googlecode.com/svn/trunk/biocache-store/conf/storage-conf.xml
sed -e "s/\/var\/log\/cassandra/\/data\/cassandra/g" log4j.properties > log4j.properties.1
rm log4j.properties
mv log4j.properties.1 log4j.properties

# setup environment variables
echo "export JAVA_HOME=/home/ubuntu/jdk1.6.0_24" > bashbit1
echo "export CASSANDRA_HOME=/home/ubuntu/cassandra" > bashbit2
echo "export PATH=\$PATH:\$JAVA_HOME/bin:\$CASSANDRA_HOME/bin" > bashbit3
echo "alias l=\"pwd && ls -la\"" > bashbit4
cat bashbit1 bashbit2 bashbit3 bashbit4 > .bash_profile
rm bashbit*
source .bash_profile

# directory setup
sudo mkdir -p /mnt/data
sudo ln -s  /mnt/data /data
sudo chown -R ubuntu /data/
sudo mkdir -p /data/cassandra/data
sudo chown -R ubuntu /data/cassandra
sudo mkdir -p /data/cassandra

# get the data
cd /data/cassandra/data/
wget http://bie.ala.org.au/repo/bie.tgz
wget http://bie.ala.org.au/repo/occ.tgz
tar zxvf bie.tgz
tar zxvf occ.tgz
cd /data
wget http://bie.ala.org.au/repo/lucene.tgz
tar zxvf lucene.tgz

# get biocache store jar
cd
wget http://maven.ala.org.au/repository/au/org/ala/biocache-store/1.0-SNAPSHOT/biocache-store-1.0-SNAPSHOT-assembly.jar
jar xf biocache-store-1.0-SNAPSHOT-assembly.jar lib lib


# run stuff
/home/ubuntu/cassandra/bin/cassandra -p /data/cassandra-pidfile
cd
java -Xmx4g -Xms4g -classpath .:biocache-store-1.0-SNAPSHOT-assembly.jar au.org.ala.util.ProcessRecords