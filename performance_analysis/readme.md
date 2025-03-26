# Performance Analysis for GXA Bulk Backend

## Data source
Production logs from 27/2/2025

## pre processing

1. copy logs to logs directory
2. quick and dirty cleanup using shell. to be added to python.

```bash
 find logs -name \*02-27\* | xargs -n1 -I{} grep 'time taken' {} | awk '{print $7 " " $11}' | grep -v health | grep -v ' 0\.[0-5]' | grep -v search | awk -F/ '{print $3 " " $0}' > ./gxa_bulk_over_.5sec.txt
 find logs -name \*02-27\* | xargs -n1 -I{} grep 'time taken' {} | awk '{print $7 " " $11}' | grep -v health | grep -v ' 0\.[0-5]' | grep 'search?' | awk '{ print $1 " " $2 " " $0 }' | sed 's/search?/search /' | awk '{ print $1 " " $4 " " $3 " " $2 }' >> ./gxa_bulk_over_.5sec.txt
 ```
 
## installation and running

1. Create a venv

    ```bash
    python -mvenv .venv
    . .venvn/bin/activate
    ```

2. start jupyter
