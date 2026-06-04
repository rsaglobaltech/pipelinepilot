# Demo Jenkins with the PipelinePilot companion plugin preinstalled.
# Build context = jenkins-plugin/  (so we can COPY target/pipelinepilot.hpi)
FROM jenkins/jenkins:2.504.3-lts-jdk17

# Docker CLI so pipelines that use docker.build / sh 'docker ...' work against the
# host daemon (mount /var/run/docker.sock in docker-compose.yml).
USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends docker.io curl \
    && rm -rf /var/lib/apt/lists/*
USER jenkins

# Skip the setup wizard for a one-command demo.
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"
# JCasC location for the demo config (admin user + seed job).
# Keep it OUTSIDE /var/jenkins_home so the named volume does not shadow it.
ENV CASC_JENKINS_CONFIG="/usr/share/jenkins/ref/casc.yaml"

# Runtime plugin dependencies of the companion.
COPY docker/plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli -f /usr/share/jenkins/ref/plugins.txt

# The companion plugin itself.
COPY target/pipelinepilot.hpi /usr/share/jenkins/ref/plugins/pipelinepilot.jpi

# Demo config (admin/admin, a sample pipeline job).
COPY docker/casc.yaml /usr/share/jenkins/ref/casc.yaml
