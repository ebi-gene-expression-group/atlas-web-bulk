# Expression Atlas

## Prepare your development environment

### TL;DR
```bash
./docker/prepare-dev-environment/gradle-cache/run.sh -r -l gradle-cache.log && \
./docker/prepare-dev-environment/volumes/run.sh -r -l volumes.log && \
./docker/prepare-dev-environment/postgres/run.sh -r -l pg.log && \
./docker/prepare-dev-environment/solr/run.sh -r -l solr.log
```

### Requirements
- Docker v20+ with the [Compose plugin](https://docs.docker.com/compose/install/)
- 100 GB of available storage for the following Docker volumes:
  - Experiment files
  - Bioentity properties (i.e. gene annotations)
  - PostgreSQL
  - SolrCloud and ZooKeeper
  - Tomcat configuration files

Files written by Solr, PostgreSQL and Tomcat are kept in volumes which will be reused even if the containers are
removed (e.g. when running `docker-compose down`).  If you want to start afresh delete the old volume(s) (e.g. for
Postgres `docker volume rm gxa-pgdata`) and re-run the necessary script to return to the
initial state. You can find the volume names used by each service in the `volumes` section of its Docker Compose YAML
file.

The full list of volumes is:
- `gxa_atlas-data-bioentity-properties`
- `gxa_atlas-data-gxa`
- `gxa_atlas-data-gxa-expdesign`
- `gxa_gradle-ro-dep-cache`
- `gxa_gradle-wrapper-dists`
- `gxa_pgdata`
- `gxa_solrcloud-1-data`
- `gxa_solrcloud-2-data`
- `gxa_zk-1-data`
- `gxa_zk-1-datalog`
- `gxa_zk-2-data`
- `gxa_zk-2-datalog`
- `gxa_zk-3-data`
- `gxa_zk-3-datalog`
- `gxa_tomcat-conf`
- `gxa_webapp-properties`

### Code
Clone the repository of Bulk Expression Atlas with submodules:
```bash
git clone --recurse-submodules https://github.com/ebi-gene-expression-group/atlas-web-bulk.git
```
If you have already cloned the project ensure it’s up-to-date:
```bash
  git pull
  git submodule update --remote
```

### Create a Gradle read-only dependency cache
To speed up builds and tests it is strongly encouraged to create a Docker volume to back a [Gradle read-only dependency
cache](https://docs.gradle.org/current/userguide/dependency_resolution.html#sub:ephemeral-ci-cache).
```bash
./docker/prepare-dev-environment/gradle-cache/run.sh -r -l gradle-cache.log
```

### Prepare volumes
In order to run integration tests and a development instance of Bulk Expression Atlas you will need a few Docker
volumes first. They will be populated with data that will be indexed in Solr and Postgres. Bulk Expression Atlas
needs all three of: file bundles in the volumes, Solr collections and Postgres data. This step takes care of the first
requirement:
```bash
./docker/prepare-dev-environment/volumes/run.sh -r -l volumes.log
```

You can get detailed information about which volumes are created if you run the script with the `-h` flag.

This script, unless it’s run with the `-r` flag, can be interrupted without losing any data. The container mirrors
directories via FTP, and can resume after cancellation. It can be re-run to update the data in the volumes should the
contents of the source directories change. This is especially useful when experiments are re-analysed/re-annotated,
or the bioentity properties directory is updated after a release of  Ensembl, WormBase ParaSite, Reactome, Gene
Ontoloy, Plant Ontology or InterPro.

### How to add a private experiment bundle

Private experiments are not available to download from our FTP site. You can download them from the `codon-cluster` by using the following steps:

1. These steps should be done before the `PostGreSQL` and `Solr` steps. 
2. After logged in to the `codon-cluster` check if the experiment bundle can be found under this path:
```/nfs/production/irene/ma/experiments/```.
2. If it is there, then go to the folder on your local computer where you would like to download the bundle.
3. Download it by this command:
```scp -r codon-login:/nfs/production/irene/ma/experiments/<EXPERIMENT_ACCESSION_ID> .```.
4. Create a temp container with mounting the already existing data volume for our local experiments: 
````docker container create --name expVol -v gxa_atlas-data-exp:/atlas-data/exp ubuntu:jammy````
5. Copy the file bundles of the downloaded private experiment into the volume: ```docker cp <EXPERIMENT_ACCESSION_ID> expVol:/atlas-data/exp/magetab/```
6. Add `<EXPERIMENT_ACCESSION_ID>` into the `PRIVATE_EXP_IDS` variable. It is in to `test-data.env` file under the `docker/prepare-dev-environment` folder. If it is not there, then please create it.
7. The experiment accession IDs in that variable should be separated by SPACE.



### PostGreSQL

To create our PostGreSQL database and run the schema migrations up to the latest version please execute this script:
```bash
./docker/prepare-dev-environment/postgres/run.sh -r -l pg.log
```

### Solr
To create the collections, their schemas and populate them, please run the following script.

```bash
./docker/prepare-dev-environment/solr/run.sh -r -l solr.log
```

Run the script with the `-h` flag for more details.

You may want to speed up the process by raising the value of the environment variable `NUM_DOCS_PER_BATCH` (L81 of the
`run.sh` script). On [a fairly powerful laptop at the time of
writing](https://www.lenovo.com/gb/en/p/laptops/thinkpad/thinkpadx1/x1-extreme-gen-2/22tp2txx1e2) 20,000 has been
found to be a reliable number via painstaking trail and error, but your mileage may vary. Ensure that there are no
errors in the script logs, or update your test data by add the necessary species names and experiment accessions in the `test-dev.env` file and rebuild the development
environment. Some tests may fail due to incomplete annotations; `grep` for `DistributedUpdatesAsyncException` in
particular, which signals a problem storing the document batch, which in turn stops processing the current file. If
found, try again with a lower value for `NUM_DOCS_PER_BATCH`.

### Update test data
Add or change the necessary species names and experiment accessions in the `test-data.env` file and rebuild the
development environment.

## Testing

### TL;DR
```bash
./execute-all-tests.sh      
./execute-single-test.sh TEST_NAME 
./debug-single-test.sh TEST_NAME
./stop-and-remove-containers.sh
```

### Execute all tests
The `gxa-gradle` service in `docker/docker-compose-gradle.yml` executes all tests and writes reports to
`atlas-web-core/build` and `app/build` in the host machine. It requires the SolrCloud service described earlier, and a
Postgres container with the following differences compared to the development service, `gxa-postgres`: it doesn’t use
named volumes to ensure the database is clean before running any tests, and its name (as well as the dependency
declared in `docker-compose-gradle.yml`) has been changed to `gxa-postgres-test`. We don’t want to use
`gxa-postgres` by mistake and wipe the tables from the dev instance when cleaning fixtures... such an unfortunate
accident is known to have happened.

The job is split in the following six phases:
1. Clean build directory
2. Compile test classes
3. Run unit tests
4. Run integration tests
5. Run end-to-end tests
6. Generate JaCoCo reports


You will eventually see these log messages:
```
gxa-gradle         | BUILD SUCCESSFUL in 2s
gxa-gradle         | 3 actionable tasks: 1 executed, 2 up-to-date
gxa-gradle exited with code 0
```

Press `Ctrl+C` to stop the container and clean any leftovers:
```bash
docker-compose \
--env-file ./docker/dev.env \
-f ./docker/docker-compose-gradle.yml \
-f ./docker/docker-compose-postgres-test.yml \
-f ./docker/docker-compose-solrcloud.yml \
down
```

Or run `./stop-and-remove-containers.sh`.

You will find very convenient to use the script `execute-all-tests.sh`.
```bash
./execute-all-tests.sh
```

The script uses `docker-compose run`, and control returns to your shell once the tasks have finished, but you’ll need
to clean up the service containers anyway.

### Execute a single test
Many times you will find yourself working in a specific test case or class. Running all tests in such cases is
impractical. In such situations you can use
[Gradle’s continuous build execution](https://blog.gradle.org/introducing-continuous-build). See the example below for
e.g. `GenePageControllerIT.java`:
```bash
docker-compose \
--env-file ./docker/dev.env \
-f ./docker/docker-compose-gradle.yml \
-f ./docker/docker-compose-postgres-test.yml \
-f ./docker/docker-compose-solrcloud.yml \
run --rm --service-ports \
gxa-gradle bash -c '
./gradlew :app:clean &&
./gradlew \
-PdataFilesLocation=/atlas-data \
-PexperimentFilesLocation=/atlas-data/gxa \
-PjdbcUrl=jdbc:postgresql://${POSTGRES_HOST}:5432/${POSTGRES_DB} \
-PjdbcUsername=${POSTGRES_USER} \
-PjdbcPassword=${POSTGRES_PASSWORD} \
-PzkHosts=${SOLR_CLOUD_ZK_CONTAINER_1_NAME}:2181,${SOLR_CLOUD_ZK_CONTAINER_2_NAME}:2181,${SOLR_CLOUD_ZK_CONTAINER_3_NAME}:2181 \
-PsolrHosts=http://${SOLR_CLOUD_CONTAINER_1_NAME}:8983/solr,http://${SOLR_CLOUD_CONTAINER_2_NAME}:8983/solr \
app:testClasses &&
./gradlew --continuous :app:test --tests GenePageControllerIT
'
```

After running the test Gradle stays idle and waits for any changes in the code. When it detects that the files in your
project have been updated it will recompile them and run the specified test again. Notice that you can specify multiple
test files after `--tests` (by name or with wildcards).

Again, a convenience script can be used:
```bash
./execute-single-test.sh TEST_NAME
```

### Debug tests
If you want to use a debugger, add the option `-PremoteDebug` to the command of the test. For instance:
```bash
./gradlew -PremoteDebug :app:test --tests GenePageControllerIT
```

Be aware that Gradle won’t execute the tests until you attach a remote debugger to port 5005. It will notify you when
it’s ready with the following message:
```
> Task :app:test
Listening for transport dt_socket at address: 5005
<===========--> 90% EXECUTING [5s]
> IDLE
> IDLE
> IDLE
> IDLE
> IDLE
> IDLE
> IDLE
> :app:test > 0 tests completed
> IDLE
> IDLE
> IDLE
> IDLE
```

You can combine `--continuous` with `-PremoteDebug`, but the debugger will be disconnected at the end of the test. You
will need to start and attach the remote debugger every time Gradle compiles and runs the specified test.

To attach a remote debugger to your gradle test you can add following configuration in your IntelliJ:

[![RMoIhF.md.png](https://iili.io/RMoIhF.md.png)](https://freeimage.host/i/RMoIhF)


The script `debug-single-test.sh` is a shortcut for this task. It takes the same arguments as executing a single test.

```bash
./debug-single-test.sh TEST_NAME
```

## Run web application

Please check this first in the troubleshooting session: [Known Build Issue](#known-build-issue)

The web application is compiled in two stages:
1. Front end JavaScript packages are transpiled into “bundles” with [Webpack](https://webpack.js.org/)
2. Bundles and back end Java code are built as a WAR file

Lastly, Tomcat deploys the WAR file according to `app/src/main/webapp/META-INF/context.xml`; other Java EE web servers
might work but no testing has been carried out in this regard.

For the first step you can run the following script:
```bash
 ./compile-front-end-packages.sh -iu
```

The second step is simply:
```bash
./gradlew :app:war
```

The script `build-and-deploy-webapp.sh` puts it altogether and will eventually launch a Tomcat container with a running
dev instance of Single Cell Expression Atlas. The script before launching the web application can build only the back-end or front-end component or both.

Here is the usage of this script:

- -n Use this flag if you would not like to do any build, just execute the application.
- -f Use this flag if you would like to build the front-end javascript packages.
- -b Use this flag if you would like to build the back-end of the web application.
- -h Displaying the help file of this script.

If you don't give any flags, or you add both then the script is going to build both front and back-end part of the web application.

## Troubleshooting

### <a name="known-build-issue"></a>Known Build Issue

This current version of our developer env has a bug when we build and execute the application
with the `build-and-deploy-webapp.sh` script.
We have a ticket to fix this in our backlog: [Update bulk with the latest webpack and its dependencies](https://github.com/ebi-gene-expression-group/atlas-web-bulk/issues/176)

You can build a working WAR with the following steps:
1. Manually build the UI:
```bash
./compile-front-end-packages.sh -iu
```

2. Manually build the backend:
```bash
./gradlew :app:war
```

3. Use this script to start up the web app on your local environment:
```bash
./build-and-deploy-webapp.sh -n
```

### SolrCloud nodes shut down on macOS
Docker for macOS sets fairly strict resource limits for all Docker containers. If your containers require e.g. more
memory you need to increase the available amount in the Docker Dashboard. For bulk Expression Atlas, please set Memory
to between 8-12 GB and disk image to 100 GB or more. Please see the screenshot below for reference:

![Screenshot-2021-02-18-at-18-27-40](https://user-images.githubusercontent.com/4425744/109644570-8ccee680-7b4d-11eb-9db0-7a29fb4d9e2b.png)