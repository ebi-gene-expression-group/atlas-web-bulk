#!/usr/bin/env bash
lftp -e \
"mirror -vvv /pub/databases/microarray/data/atlas/bioentity_properties /atlas-data/bioentity_properties; exit" \
ftp.ebi.ac.uk

for EXP_ID in ${EXP_IDS}
do
  lftp -e \
  "mirror -vvv /pub/databases/microarray/data/atlas/experiments/${EXP_ID} /atlas-data/gxa/magetab/${EXP_ID}; exit" \
  ftp.ebi.ac.uk
done
