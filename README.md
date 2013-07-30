# JSON-LD Tree

This library will allow the generation of stable, predictable JSON-LD based on RDF using the RDF Result ontology.

# Getting started

To generate the JSON String, do the following:

```java
//Create JSON-LD
new RdfTreeGenerator().generateRdfTree(jenaModel).asJson()

//Create HTML structured in a similar way to JSON-LD
new RdfTreeGenerator().generateRdfTree(jenaModel).asHtml()
```

The RDF model must contain RDF Result ontology statements to indicate how the graph should be interpretted. This can come in three different forms.

Lists to be ordered by a predicate value:

```text
"@prefix result: <http://purl.org/ontology/rdf-result/>

result:this result:listItem <urn:a> 
result:this result:listItem <urn:b>
result:this result:listItem <urn:c> 
result:this result:orderByPredicate <urn:p> 

<urn:a> <urn:p> \"a\" . 
<urn:b> <urn:p> \"b\" . 
<urn:c> <urn:p> \"c\" .
```

Linked Lists:
"@prefix result: <http://purl.org/ontology/rdf-result/>

result:this result:next <urn:a> 
<urn:a> result:next <urn:b>
<urn:b> result:next <urn:c> 

<urn:a> <urn:p> \"a\" . 
<urn:b> <urn:p> \"b\" . 
<urn:c> <urn:p> \"c\" .
```

Single items:
"@prefix result: <http://purl.org/ontology/rdf-result/>

result:this result:item <urn:a> 

<urn:a> <urn:p> \"a\" . 
```