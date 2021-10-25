# BulkXpath-Executor
Extract data based on output of XpathGenerator, or merge data and XML fragments..

## Usage

java -jar BulkXpathExecutor.jar -p pathsfile [ -r datafile ]* [ -m ] [ -M ] [ -f ] [ -t ] [ -o outputfile ] [ -e errorfile ] [ -x extension ] [ -X extension ] [ documentfile | - ]

### Parameters
| Parameter | Required? | Description |
| --------- | --------- | ----------- |
| -p paths file | mandatory | tab separated file containing pairs of identifiers and xpaths. Comments start with #. Associates an identifier with an xpath |
| -r  datafile | optional | (0..n) tab separated file containing pairs of identifiers and data values to be assigned to those identifiers.  Comments start with #. Associates an identifier with a value to be applied in output file. |
| -m | optional | set in memory output Outputs are written to lists of string (for using this jar as a library) |
| -M | optional | set in memory error Errors are written to lists of string (for using this jar as a library)|
| -f | optional | prepend filename to error |
| -t | optional | include a timestamp with the error |
| -o output file | optional |  path to file to which modified xml file is to be output |
| -e error file | optional | path to file to which modification errors are to be output |
| -x extension | optional | file extension to be appended to output files. |
| -X extension | optional | file extension to be appended to error files |
| document file \| - | optional | 1 or more paths to well formed xml input files or stdin |
