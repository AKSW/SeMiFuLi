# SeMiFuLi: Semantically described mini function library

Library of small Java functions, semantically described with the [FNO ontology](https://w3id.org/function/spec). This enables the use e.g. in [RML mappings](https://www.w3.org/RML/).
The functions included deal with the following topics
* prefix substitution
* copies of some [grel functions](https://github.com/FnOio/grel-functions-java), adapted with null input handling
* timestamp conversion

## Usage
* The [RMLMapper](https://github.com/RMLio/rmlmapper-java) and [YARRRML](https://rml.io/yarrrml) supports usage of functions via FNO. You can add the functions on invocations of the `rmlmapper-java` by using the `-f` parameter of rmlmapper and adding the function library to the java classpath.
* For usage with [Function-Agent-Java](https://github.com/FnOio/function-agent-java) see tests.
* [xlsx2owl](https://github.com/AKSW/xlsx2owl) uses SeMiFuLi.

## History
* v0.2.1 (9.11.2023)
    * added readme, some cleanup
    * added String functions `contains` and `notContains`
    * major cleanup on `functions_xlsx2owl.ttl`, moved grel parameter definition copies to `grelTypes.ttl`, updated jar path, reordered entries
    * changes in testGetPrefixMapFilePath
    * updated dependency function-agent-java to v1.1.0
* 0.2 added more functions like timestamp conversion
* 0.0.1 initial version

## Authors
* Lars-Peter Meyer: lpmeyer@infai.org

## License
SeMiFuLi is licenced under the MIT licence.