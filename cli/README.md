# Expression Atlas CLI Bulk
A minimal Spring Boot wrapper to run (bulk) Expression Atlas tasks from the command line.

## Requirements
- Java 11
- Expression Atlas environment (PostgreSQL server; SolrCloud cluster; bioentity annotation and experiment files)

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
Configuration variables are set with `-Dproperty=value` if you run the application via `java -jar ...`, or by adding
`-Pproperty=value` to the Gradle task (in the tables below: Java property name, and Gradle propery name, respectively).

**IMPORTANT**: At the very least you will need to set the environment variables described in the Default value columns
to run/compile the application with Gradle. However, notice that the `-D` arguments will override whatever was set at
compile time, so if you forget or your environment changes, you don’t need to recompile.

### Expression Atlas file options: `configuration.properties`
| Java property name          | Gradle property name      | Default value            |
|-----------------------------|---------------------------|--------------------------|
| `data.files.location`       | `dataFilesLocation`       | `${ATLAS_DATA_PATH}`     |
| `experiment.files.location` | `experimentFilesLocation` | `${ATLAS_DATA_PATH}/gxa` |

### Expression Atlas database options: `jdbc.properties`
| Java Property name | Gradle property name | Default value                                                       |
|--------------------|----------------------|---------------------------------------------------------------------|
| `jdbc.url`         | `jdbcUrl`            | `jdbc:postgresql://${ATLAS_POSTGRES_HOST}:5432/${ATLAS_POSTGRES_DB` |
| `jdbc.username`    | `jdbcUsername`       | `${ATLAS_POSTGRES_USER}`                                            |
| `jdbc.password`    | `jdbcPassword`       | `${ATLAS_POSTRES_PASSWORD}`                                         |

### Expression Atlas Solr options: `solr.properties`
| Java property name | Gradle property name | Default value        |
|--------------------|----------------------|----------------------|
| `zk.host`          | `zkHost`             | `${ATLAS_ZK_HOST}`   |
| `zk.port`          | `zkPort`             | `2181`               |
| `solr.host`        | `solrHost`           | `${ATLAS_SOLR_HOST}` |
| `solr.port`        | `solrPort`           | `8983`               |

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
$ java -jar ./cli/build/libs/atlas-cli-bulk.jar bioentities-map
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
to load documents into Solr in blocks that can be easily consumed by the server nodes.


## Troubleshooting
### Gradle isn’t respecting my configuration properties
Do not place configuration variables inside the `args` parameter.

**Wrong**:
```bash
./gradlew :cli:bootRun --args="-PdataFilesLocation=/atlas-data -PexperimentFilesLocation=/atas-data/gxa bioentities-json"
```

**Right**:
```bash
./gradlew :cli:bootRun -PdataFilesLocation=/atlas-data -PexperimentFilesLocation=/atas-data/gxa --args="bioentities-json"
```

### Application fails to start with the message “bean of type 'javax.servlet.ServletContext' could not be found”
If at any point you see the error messages below when running the application, it means that certain components/beans
from the atlas-web-bulk `app` subproject (i.e. the web application) are instantiated at run time. It fails because they
need a `ServletContext` (i.e. a web server) and Tomcat isn’t running:
```
...
Error starting ApplicationContext. To display the conditions report re-run your application with 'debug' enabled.
2021-10-19 21:57:44.022 ERROR 579874 --- [           main] o.s.b.d.LoggingFailureAnalysisReporter   : 

***************************
APPLICATION FAILED TO START
***************************

Description:

Parameter 0 of constructor in uk.ac.ebi.atlas.controllers.page.StaticPageController required a bean of type 'javax.servlet.ServletContext' that could not be found.


Action:

Consider defining a bean of type 'javax.servlet.ServletContext' in your configuration.
```

Classes `WebConfig` and `StaticPageController` are both being excluded at run time by activating the `cli` Spring 
profile. The project by default includes this setting in `application.properties`; if you encounter the error above, 
verify that the `spring_profiles_active` property is present in that file.

Look for the following line in the app logs:
```
2021-10-20 10:11:05.755  INFO 591952 --- [           main] u.a.e.a.c.ExpressionAtlasCliApplication  : The following profiles are active: cli
```

Remember that you can override this setting when running the JAR file like this:
```bash
java -Dspring.profiles.active=cli -jar ./cli/build/libs/atlas-cli-bulk.jar 
```

## TODO
- Test Gradle task `bootBuildImage` and optionally add a `Dockerfile` to containerise the application.
  
## Final thoughts
Spring Boot is a quick and effective solution to leverage our existing codebase. However, we should consider Spring 
Batch if this project is going to be maintained in the long term.
