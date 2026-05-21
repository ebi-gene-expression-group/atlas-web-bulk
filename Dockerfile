# Docker image for atlas-web-bulk, based on the same Tomcat image
# used by the Kubernetes Helm chart.
#
# The Jenkins pipeline assembles the WAR under webapps/<APP_NAME>.war
# (stage "Assemble WAR file" in Jenkinsfile). This image simply
# bakes that WAR into Tomcat; all runtime configuration (JDBC,
# Solr, Tomcat users/manager, etc.) remains managed by K8s
# ConfigMaps/Secrets and Helm values.

FROM tomcat:8-jdk11

# Logical application name used for the WAR filename and context path
ARG APP_NAME=gxa

# Application user/group matching the Kubernetes podSecurityContext
ARG APP_USER=docker
ARG APP_UID=2921
ARG APP_GID=1146

ENV USER=${APP_USER} UID=${APP_UID} GID=${APP_GID}

RUN groupadd -g "${APP_GID}" "${APP_USER}" \
 && useradd -u "${APP_UID}" -g "${APP_GID}" \
      -d /usr/local/tomcat -s /bin/bash "${APP_USER}" \
 && chown -R "${APP_UID}:${APP_GID}" /usr/local/tomcat

# Path to the WAR file inside the build context. By default we use
# the location produced by `./gradlew :app:war` in the existing pipeline
# and expect webapps/<APP_NAME>.war.
ARG WAR_FILE=webapps/${APP_NAME}.war

# Enable the Tomcat Manager webapp. The manager lives in webapps.dist/ in the
# base image; copying it here means the init container in the Helm chart is no
# longer needed to populate the tomcat-webapps PVC.
# tomcat-users.xml and manager/META-INF/context.xml continue to be mounted
# by the Helm chart at runtime.
RUN cp -r /usr/local/tomcat/webapps.dist/manager /usr/local/tomcat/webapps/manager \
 && chown -R "${APP_UID}:${APP_GID}" /usr/local/tomcat/webapps/manager

# Copy the application WAR into Tomcat using the /<APP_NAME> context path
COPY ${WAR_FILE} /usr/local/tomcat/webapps/${APP_NAME}.war

# Expose the default HTTP port. Debug and other ports are controlled
# at the Kubernetes level via Helm values (tomcat.debug.*).
EXPOSE 8080

# Run as the non-root application user by default. The Helm chart
# can still override securityContext if needed, but by default this
# image will use the same UID/GID as configured there.
USER ${APP_UID}:${APP_GID}

# Default command; can be overridden by the Helm chart when JPDA
# debugging is enabled.
CMD ["catalina.sh", "run"]
