echo "Not implemented yet"
exit 1

if [ -z "$releaseStream" ]; then
  echo "releaseStream must be set"
fi

lastMilestone = $(ssh genie.m2e@projects-storage.eclipse.org "cd /home/data/httpd/download.eclipse.org/technology/m2e/milestones/$releaseStream/ && ls -1") | sort -r | head -n 1
# Copy content
ssh genie.m2e@projects-storage.eclipse.org mkdir -p /home/data/httpd/download.eclipse.org/technology/m2e/releases/$releaseStream
ssh genie.m2e@projects-storage.eclipse.org cp -r /home/data/httpd/download.eclipse.org/technology/m2e/milestone/$releaseStream/$lastMilestone /home/data/httpd/download.eclipse.org/technology/m2e/releases/$releaseStream

# Create composite files
ssh genie.wildwebdeveloper@projects-storage.eclipse.org \
cd /home/data/httpd/download.eclipse.org/technology/m2e/milestones/ &&
echo '<?xml version="1.0" encoding="UTF-8" ?>
<?compositeMetadataRepository version="1.0.0"" ?>
<repository name="m2e '$releaseStream'.x milestones repository" type="org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository" version="1.0.0">
  <properties size="1">
    <property name="p2.timestamp" value="'$timestamp'" />
  </properties>
  <children size="1">
    <child location="'$lastMilestone'" />
  </children>
</repository>' > compositeContent.xml &&
echo '<?xml version="1.0" encoding="UTF-8" ?>
<?compositeMetadataRepository version="1.0.0" ?>
<repository name="m2e '$releaseStream'.x milestones repository" type="org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository" version="1.0.0">
  <properties size="1">
    <property name="p2.timestamp" value="'$timestamp'" />
  </properties>
  <children size="1">
    <child location="'$lastMilestone'" />
  </children>
</repository>' > compositeArtifacts.xml &&
echo 'version = 1
metadata.repository.factory.order = compositeContent.xml,\!
artifact.repository.factory.order = compositeArtifacts.xml,\!
' > p2.index

# TODO tag