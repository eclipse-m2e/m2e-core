echo "Not implemented yet"
exit 1

if [ -z "$releaseStream" ]; then
  echo "releaseStream must be set"
fi

# Copy Content
ssh genie.m2e@projects-storage.eclipse.org mkdir -p /home/data/httpd/download.eclipse.org/technology/m2e/milestones/${releaseStream}/
snapshotPath = $(ssh genie.m2e@projects-storage.eclipse.org ls -1d /home/data/httpd/download.eclipse.org/technology/m2e/lsp4e/snapshots/${releaseStream}.*)
version = $(basename $snapshotPath)
ssh genie.m2e@projects-storage.eclipse.org cp -r /home/data/httpd/download.eclipse.org/technology/m2e/snapshots/${version} /home/data/httpd/download.eclipse.org/technology/m2e/milestones/${releaseStream}/

#edit composite to add ${version}
timestamp = $(date +"%s"000)
ssh genie.wildwebdeveloper@projects-storage.eclipse.org \
cd /home/data/httpd/download.eclipse.org/technology/m2e/milestones/ &&
if [ -f compositeArtifacts.xml ]; then
sed -re 's/<children size="([[:digit:]]*)">/echo "<children size=\\\"$((\1+1))\\\">\
<child location=\\"'$version'\\"\/>"/e'\
	-re 's/<property name="p2.timestamp" value="[[:digit]]*">/<property name="p2.timestamp" value="'$timestamp'"/e'\
	-i compositeArtifacts.xml compositeContent.xml
else
echo '<?xml version="1.0" encoding="UTF-8" ?>
<?compositeMetadataRepository version="1.0.0"" ?>
<repository name="m2e '$releaseStream'.x milestones repository" type="org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository" version="1.0.0">
  <properties size="1">
    <property name="p2.timestamp" value="'$timestamp'" />
  </properties>
  <children size="1">
    <child location="'$version'" />
  </children>
</repository>' > compositeContent.xml &&
echo '<?xml version="1.0" encoding="UTF-8" ?>
<?compositeMetadataRepository version="1.0.0" ?>
<repository name="m2e '$releaseStream'.x milestones repository" type="org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository" version="1.0.0">
  <properties size="1">
    <property name="p2.timestamp" value="'$timestamp'" />
  </properties>
  <children size="1">
    <child location="'$version'" />
  </children>
</repository>' > compositeArtifacts.xml &&
echo 'version = 1
metadata.repository.factory.order = compositeContent.xml,\!
artifact.repository.factory.order = compositeArtifacts.xml,\!
' > p2.index
fi
#TODO tag ${version}