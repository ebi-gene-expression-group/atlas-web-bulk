FROM tomcat:8-jdk11

# Using the NFS virtual user
ENV USER=fg_atlas
ENV UID=2921
ENV GID=1146
# Creating the user and group
RUN addgroup --gid "$GID" "$USER"
RUN adduser  --disabled-password \
  --gecos "" \
  --home "$(pwd)" \
  --ingroup "$USER" \
  --no-create-home \
  --uid "$UID" \
  "$USER"

# Making the virtual user the owner of the tomcat installation
RUN chown -R ${UID}:${GID} /usr/local/tomcat
# Switching to the virtual user
USER ${UID}:${GID}
# Making the manager App accessible
COPY ./tomcat/manager/context.xml /usr/local/tomcat/webapps.dist/manager/META-INF/context.xml