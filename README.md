# Expression Atlas

## Requirements
- Docker v19+
- Docker Compose v1.25+
- 100 GB of available storage (experiment files, PostgreSQL and Solr backup snapshots and Docker volumes)

Notice that PostgreSQL and Solr snapshots are [`bind` mounted](https://docs.docker.com/storage/bind-mounts/) in order
to move data back and forth from the containers. Actual files managed by either Solr or PostgreSQL are kept in volumes
which will be reused even if the containers are removed or brought down by Docker Compose. If you want to start afresh
delete the old volume (e.g. for Postgres `docker volume rm gxa-pgdata`) and re-run the necessary step to return to the
initial state.

## Code
Clone the repositories of both Atlas Web Core (common business logic for bulk Expression Atlas and Single Cell
Expression Atlas) and Single Cell Expression Atlas proper:
```bash
git clone --recurse-submodules https://github.com/ebi-gene-expression-group/atlas-web-core.git && \
git clone --recurse-submodules https://github.com/ebi-gene-expression-group/atlas-web-bulk.git
```

## Data
Choose a suitable location for the experiment files, database and Solr backup data. Set the path in the variable
`ATLAS_DATA_PATH`.

To download the data you can use `rsync` if you’re connected to the EBI network (over VPN or from campus):
```bash
ATLAS_DATA_PATH=/path/to/sc/atlas/data 
rsync -ravz ebi-cli:/nfs/ftp/pub/databases/microarray/data/atlas/test/gxa/* $ATLAS_DATA_PATH
```

Alternatively you can use `wget` and connect to EBI’s FTP server over HTTP:
```bash
wget -P $ATLAS_DATA_PATH -c --reject="index.html*" --recursive -np -nc -nH --cut-dirs=7 --random-wait --wait 1 -e robots=off http://ftp.ebi.ac.uk/pub/databases/microarray/data/atlas/test/gxa/
```

Notice that either way `ATLAS_DATA_PATH` will be created for you if the directory doesn’t exist.

## Bring up the environment
Besides `ATLAS_DATA_PATH` you need to set some variables for the Postgres container. Use the settings below and replace
`ATLAS_DATA_PATH` value to the directory you set up in the first step.

In the `atlas-web-single-cell/docker` directory run the following:
```bash
ATLAS_DATA_PATH=/path/to/bulk/atlas/data \
POSTGRES_HOST=gxa-postgres \
POSTGRES_DB=gxpgxadev \
POSTGRES_USER=atlasprd3 \
POSTGRES_PASSWORD=atlasprd3 \
docker-compose up
```

You can also set a Docker Compose *Run* configuration in IntelliJ IDEA with the environment variables from the command
above if you find that more convenient.

After bringing up the containers, you may want to inspect the logs to see that all services are running fine. The last
log should come from Tomcat, and it should be something similar to:
```
gxa-tomcat    | 18-Dec-2020 13:40:58.907 INFO [main] org.apache.catalina.startup.Catalina.start Server startup in 6705 ms
```

Now let’s populate both the Postgres database and the SolrCloud collections.

### Postgres
Run the  following command to restore Postgres data from the provided `pg-dump.bin` file:
```bash
docker exec -it gxa-postgres bash -c 'pg_restore -d $POSTGRES_DB -h localhost -p 5432 -U $POSTGRES_USER --clean /var/backups/postgresql/pg-dump.bin'
```

A few minutes later your Postgres database will be ready.

### SolrCloud
Use the provided `Dockerfile` to bootstrap SolrCloud:
```bash
docker build -t gxa-solrcloud-bootstrap .
docker run -i --rm --network gxa gxa-solrcloud-bootstrap
```

You will see many warnings or errors in Solr’s responses. That’s alright and to be expected, since the scripts that
create the config sets, collections and define the schemas will attempt first to remove them to start from a clean,
known state; however Solr will reply with an error if the collections can’t be deleted.

Again, this step will take a few minutes.

### Tomcat
Copy the Tomcat credentials file to the container. The `admin` role is used to access several admin endpoints in Single
Cell Expression Atlas (e.g. `/admin/experiments/help`). Tomcat’s `conf` directory is persisted as a volume so that we
need to do this only once:
```bash
docker cp tomcat-users.xml gxa-tomcat:/usr/local/tomcat/conf
```

Run the Gradle task `war` in the `atlas-web-bulk` directory:
```bash
cd atlas-web-bulk
./gradlew -PbuildProfile=docker war
```

You should now have the file `build/libs/gxa.war` which by default Tomcat’s naming conventions will be served at
`gxa`. Point your browser at `http://localhost:8080/gxa` and voilà!

Every time you re-run the `war` task the web app will be automatically re-deployed by Tomcat.

## Backing up your data
Eventually you’ll add new experiments to your development instance of GXA, or new, improved collections in Solr will
replace the old ones. In such cases you’ll want to get a snapshot of the data to share with the team. Below there are
instructions to do that.

### PostgreSQL
If at some point you wish to create a backup dump of the database run the command below:
```bash
docker exec -it gxa-postgres bash -c 'pg_dump -d $POSTGRES_DB -h localhost -p 5432 -U $POSTGRES_USER -f /var/backups/postgresql/pg-dump.bin -F c -n $POSTGRES_USER -t $POSTGRES_USER.* -T *flyway*'
```

### SolrCloud
```bash
for SOLR_COLLECTION in $SOLR_COLLECTIONS
do
  START_DATE_IN_SECS=`date +%s`
  curl "http://localhost:8983/solr/${SOLR_COLLECTION}/replication?command=backup&location=/var/backups/solr&name=${SOLR_COLLECTION}"

  # Pattern enclosed in (?<=) is zero-width look-behind and (?=) is zero-width look-ahead, we match everything in between
  COMPLETED_DATE=`curl -s "http://localhost:8983/solr/${SOLR_COLLECTION}/replication?command=details" | grep -oP '(?<="snapshotCompletedAt",").*(?=")'`
  COMPLETED_DATE_IN_SECS=`date +%s -d "${COMPLETED_DATE}"`

  # We wait until snapshotCompletedAt is later than the date we took before issuing the backup operation
  while [ ${COMPLETED_DATE_IN_SECS} -lt ${START_DATE_IN_SECS} ]
  do
    sleep 1s
    COMPLETED_DATE=`curl -s "http://localhost:8983/solr/${SOLR_COLLECTION}/replication?command=details" | grep -oP '(?<="snapshotCompletedAt",").*(?=")'`
    COMPLETED_DATE_IN_SECS=`date +%s -d "${COMPLETED_DATE}"`
  done
done
```

### Update test data
Remember to update the file and any new experiments added to the `filesystem` directory by syncing your
`ATLAS_DATA_PATH` with `/nfs/ftp/pub/databases/microarray/data/atlas/test/gxa`:
```bash
rsync -ravz $ATLAS_DATA_PATH/* ebi-cli:/nfs/ftp/pub/databases/microarray/data/atlas/test/gxa/
```

## Troubleshooting

### SolrCloud nodes shut down on macOS
Docker for macOS sets fairly strict resource limits for all Docker containers. If your containers require e.g. more
memory you need to increase the available amount in the Docker Dashboard. For bulk Expression Atlas, plase set Memory
to between 8-12 GB and disk image to 100 GB or more. Please see the screenshot below for reference:

![Screenshot-2021-02-18-at-18-27-40](https://user-images.githubusercontent.com/4425744/109644570-8ccee680-7b4d-11eb-9db0-7a29fb4d9e2b.png)

### The script that backs up Solr snapshot hangs

Ensure you have writing privileges for the directory bind at `/var/backups/solr`. You can check the status of your
backup operation with (set `SOLR_HOST` and `SOLR_COLLECTION` to the appropriate values):
```bash
docker exec -i ${SOLR_HOST} curl -s "http://localhost:8983/solr/${SOLR_COLLECTION}/replication?command=details"
```
### I’m not getting any suggestions in Epression Atlas
Read the important message after you run `gxa-solrlcoud-bootstrap`:
> PLEASE READ!
> Suggesters haven’t been built because it’s very likely to get a `java.net.SocketTimeoutException` due
> to the size of the bioentities collection. Raising the timeout in Jetty could mask other errors down
> the line, and ignoring the exception doesn’t guarantee the suggester to be fully built since it still
> takes a few extra minutes: the exception is thrown before the process has completed.
> The best option is to manually build and supervise this step.
> 
> On one terminal session run the following command (don’t worry if the request returns a 500 error):
>
> `docker exec -i gxa-solrcloud-1 curl 'http://localhost:8983/solr/bioentities-v1/suggest?suggest.build=true&suggest.dictionary=propertySuggester'`
> 
> On another terminal, monitor the size of the suggester directory size:
>
> `docker exec -it gxa-solrcloud-1 bash -c 'watch du -sc server/solr/bioentities-v1*/data/*'`
> 
> The suggester will be built when the propertySuggester directory size stabilises.
> Run the above procedure for each of your SolrCloud containers.
 
