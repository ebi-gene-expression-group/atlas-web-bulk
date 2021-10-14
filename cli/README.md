# Expression Atlas CLI Bulk
A minimal Spring Boot wrapper to run (bulk) Expression Atlas tasks from the command line.

## Requirements
- Java 11
- Expression Atlas environment (PostgreSQL server; SolrCloud cluster; bioentity annotation and experiment files)

*IMPORTANT*: Review file paths in `cli/src/main/resources/configuration.properties`, Postgres host in
`cli/src/main/resources/jdbc.properties`, and SolrCloud/ZooKeeper hosts in `cli/src/main/resources/solr.properties`
before building and running the application.

## Usage
There are two main ways to run the application: as an executable JAR or via Gradle. The latter is recommended on
development environments and Java is preferred in production environments. Be aware that any changes made to the
properties file won’t take effect unless you rebuild the JAR file.

### Gradle
```bash
./gradlew :cli:bootRun --args="<task-name> <options>"
```

### Executable JAR
Build the JAR file:
```bash
./gradlew :cli:bootJar
```

Then run it with Java:
```bash
java -jar ./cli/build/libs/atlas-cli-bulk.jar <task-name> <options>
```

## Configuration
The following configuration variables can be set in their respective properties file or via the `-D` option. Changes in
the properties file will be automatically picked up if the application is run with Gradle. If you run it with Java
`-Doption=value` will override the setting in the compiled file.

### Spring Boot options: `application.properties`
- `server.port`

### Expression Atlas file options: `configuration.properties`
- `data.files.location`
- `experiment.files.location`

### Expression Atlas database options: `jdbc.properties`
- `jdbc.url`
- `jdbc.username`
- `jdbc.password`

### Expression Atlas Solr options: `solr.properties`
- `zk.host`
- `zk.port`
- `solr.host`
- `solr.port`

## Tasks
Run without any arguments to get a list of available tasks:
```
Usage: <main class> [COMMAND]
Commands:
  bulk-analytics-json  Write JSONL files for the bulk-analytics collection for
                         the
  bioentities-json     Write JSONL files for the bioentities collection
  bioentities-map      Write a bioentity-to-bioentity properties map to file;
                         the source of bioentity (i.e. gene) IDs can be either
                         expression experiment matrices specified by their
                         accessions or a single species from the bioentities
                         collection
```

Pass the name of a task to obtain a detailed description of available options:
```bash
$ java -Dserver.port=9001 -jar ./cli/build/libs/atlas-cli-bulk.jar bioentities-map
...
Missing required option: '--output=<outputFilePath>'
Usage: <main class> bioentities-map -o=<outputFilePath>
                                    (-e=<experimentAccessions>[,
                                    <experimentAccessions>...]
                                    [-e=<experimentAccessions>[,
                                    <experimentAccessions>...]]... |
                                    -s=<species>)
Write a bioentity-to-bioentity properties map to file; the source of bioentity
(i.e. gene) IDs can be either expression experiment matrices specified by their
accessions or a single species from the bioentities collection
  -e, --experiment=<experimentAccessions>[,<experimentAccessions>...]
                            one or more experiment accessions
  -o, --output=<outputFilePath>
                            path of output file
  -s, --species=<species>   species
```

### `bioentities-json`
Generate JSONL files from the Ensembl annotations, array designs and Reactome stable IDs of each species. The format of
these files is adjusted to 
[the `bioentities` Solr collection schema](https://github.com/ebi-gene-expression-group/index-bioentities). Annotation
files are searched in the following directories:
```
data.files.location
       ├── annotations
       ├── array_designs
       └── reactome
```

The path `data.files.location` is defined in the `configuration.properties` file. Ensure that it points at the correct
directory in the environment where the application runs.

This task effectively transforms each of the TSV files in the above directories to JSONL.

Be aware that currently there’s no way to filter the generated files and the process is executed for all species,
generating a considerable number of files. For every species we usually have one Reactome file, one Ensembl file, often
a miRNA file of identifiers and array designs for the most “popular” species. At the time of writing this task
reads and writes 302 files for 85 species.

#### Examples
Write all JSONL files to directory `/tmp`:
```bash
bioentities-json -o /tmp
```
---
### `bioentities-map`
Create a map of bioentity (i.e. gene) ID to bioentity properties extracted from the Solr `bioentities` collection and
serialise it to a file. The format is Java-native as specified by
[`Serializable`](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/io/Serializable.html) and
[`ObjectOutputStream`](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/io/ObjectOutputStream.html).

The source of bioentities can be one or more experiments (in which case all experiment matrices will be parsed to 
retrieve all IDs before querying Solr) or one species. In the latter case the `bioentities` collection will also be 
used to find all matching bioentity identifiers of the given species. It’s therefore a good idea to run the
`bioentities-json` task and index the generated documents in Solr before executing `bioentities-map`. 

#### Examples
Create a map of a single experiment:
```bash
bioentities-map -o ./e-mtab-2770.map.bin -e E-MTAB-2770
```
---
Create a map of two experiments (notice that many of the gene IDs are shared):
```bash
bioentities-map -o ./two-big-experiments.map.bin -e E-MTAB-2770,E-MTAB-5423
```
---
Create a mouse map suitable for any *Mus musculus* in `bulk-analytics-json`:
```bash
bioentities-map -o ./mus-musculus.map.bin -s Mus_musculus
```
---
### `bulk-analytics-json`
Generate JSONL files from bulk experiments. Each row in these files represents an expression data point with metadata
and gene annotations of the contrast/assay group and expressed gene, respectively. The format is compatible with
[the `bulk-analytics Solr collection schema](https://github.com/ebi-gene-expression-group/index-gxa/).

If a map file from `bioentities-map` is not passed as an argument, the gene annotations will be pulled from Solr on an
as-needed basis. This means that it’s a good idea to keep one map per species as it will speed up the file generation
process.

#### Examples
Generate two analytics JSONL files for experiments GTEx and PanCancer experiments with an on-the-fly-built
bioentity-to-bioentity properties map. The output files will be `/tmp/E-MTAB-2770.jsonl` and `/tmp/E-MTAB-5423.jsonl`:
```bash
bulk-analytics-json -o /tmp -e E-MTAB-2770,E-MTAB-5423
```
---

```bash
bulk-analytics-json -o /tmp -e E-MTAB-2770,E-MTAB-5423 -i homo-sapiens.map.bin
```
---

## Workflow examples
### Load [FANTOM5 experiments](https://www.ebi.ac.uk/gxa/experiments?experimentDescription=FANTOM5) to `bulk-analytics` collection
#### Remove old mouse annotations from Solr
The `bioentities` collection has no dedupe/signature processor, so this step is necessary to avoid stale and duplicated
data:
```bash
curl -X POST -H 'Content-Type: application/json' \
'http://localhost:8983/solr/bioentities-v1/update/json?commit=true' --data-binary \
'{
  "delete": {
    "query": "species:Mus_musculus"
  }
}'
```

#### Prepare app and working directories
```bash
./gradlew :cli:bootJar
mkdir ./bioentities ./bulk-analytics
```

#### Generate bioentity JSONL files
```bash
java -jar ./cli/build/libs/atlas-cli-bulk.jar \
bioentities-json -o ./bioentities
```

#### Index mouse annotations
```bash
for FILE in ./bioentities/mus_musculus.*
do
  INPUT_JSONL=$FILE SOLR_COLLECTION=bioentities SCHEMA_VERSION=1 ./bin/solr-jsonl-chunk-loader.sh
done
```

#### Create mouse bioentity properties map
```bash
java -jar ./cli/build/libs/atlas-cli-bulk.jar \
bioentities-map -o ./bioentities/mus-musculus.map.bin -s Mus_musculus
```

#### Generate analytics JSONL files
```bash
java -jar ./cli/build/libs/atlas-cli-bulk.jar \
bulk-analytics-json -o ./bulk-analytics -i ./bioentities/mus-musculus.map.bin -e E-MTAB-3578,E-MTAB-3579,E-MTAB-3358
```

#### Load analytics files in Solr
If only the expression values and/or gene annotations and metadata change, but not the combinations of gene ID and
assay group ID in the expression matrix with non-zero values,  it’s not necessary to remove the data set from the
`bulk-analytics` collection. The dedupe processor’s signature is calculated with the gene ID and assay group or 
contrast ID and the old documents will be overwritten. 
```bash
for EXP_ID in E-MTAB-3578 E-MTAB-3579 E-MTAB-3358
do
  INPUT_JSONL=./bulk-analytics/${EXP_ID}.jsonl SOLR_COLLECTION=bulk-analytics SCHEMA_VERSION=1 SOLR_PROCESSORS=dedupe ./bin/solr-jsonl-chunk-loader.sh
done
```

Now FANTOM5 experiments can be shown and searched in Expression Atlas.

## Why JSONL?
The [JSON Lines](https://jsonlines.org/) format is convenient for documents which contain large amount of elements
because it eliminates the need to parse them as an array, a process which requires seeking the closing bracket at the
end of the file. Also, it usually reads the whole array in memory and such an  approach would be impractical, since 
it’s commmon for the generated files to be several gigabytes in size and to consist of millions of Solr documents.
JSONL files can also be easily broken up in chunks with command line utilities such as `split`, which we use in order
to load   documents into Solr in blocks that can be easily consumed by the server nodes.


## Troubleshooting
### Application fails to start with the message “Web server failed to start. Port XXXX was already in use.”
Since `atlas-web-bulk` needs a `ServletContext` to build the application context, this is technically a web
application and Spring Boot’s embedded web server is started with the app. Make sure that no other web server is
running and listening to the port specified in the `application.properties` file.

## TODO
- Exclude `WebConfig` from the app context to delete the `webapp` directory. This will require changes in
  `atlas-web-bulk` because `AppConfig` turns on component scanning which in turn adds `WebConfig` to the context. Also,
  while Tomcat might not appear to be necessary to run the application, certain classes in `atlas-web-bulk` such as
  `StaticPageController` need a `ServletContext`. I think getting rid of these dependencies will prove to be difficult.
- Test Gradle task `bootBuildImage` and optionally add a `Dockerfile` to containerise the application.
  
## Final thoughts
Spring Boot isn’t the right tool for this job, but it’s a quick and effective solution. Consider Spring Batch if this
project is going to be maintained in the long term, but be aware of the first point mentioned in the *TODO* section 
above.
