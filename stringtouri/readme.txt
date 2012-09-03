Title: StringToURI Readme
Author: Thibaud Colas
Date: 15/08/12

# StringToURI Readme #

## tl;dr ##

This module is an interface for [StringToURI]("http://stringtouri.assembla.me/").  
Version 0.9

## What is StringToURI ##

StringToURI is a simple interconnection module which converts Strings to URIs _(hence the name)_.
StringToURI takes two predicates and matches their objects' values. When the values are equal, the object (a string value) of the triple to be modified is replaced by the subject of the reference triple (a URI).

------------------------------------------------------------------------------

### An abstract example ###

Let's define two triples : S, P, O and SS, PP, OO.

* SPO is a triple to update, where O is a string value.
* SSPPOO is our reference triple where OO is also a string.

If OO and O are found equal, the first triple will become S, P, SS and the second one will remain the same.

------------------------------------------------------------------------------

### A real life fictional example ###

In a given dataset, an event takes place in a city.

	<givenDataset:AwesomeMusicFestival> . <givenDataset:takesPlaceIn> . "Acapulco"
	
It'd be better the AwesomeMusicFestival was directly linked to the entity Acapulco, not only to a string describing it.
Let's find a reference dataset which contains the entity Acapulco.

	<referenceDataset:Acapulco> . <rdf:type> . <referenceDataset:City>
	<referenceDataset:Acapulco> . <referenceDataset:name> "Acapulco"
	...
	
We can compare the two predicates `<givenDataset:takesPlaceIn>` and `<referenceDataset:name>` to obtain :

	<givenDataset:AwesomeMusicFestival> . <givenDataset:takesPlaceIn> . <referenceDataset:Acapulco>
	
## Future development ##

- Server-side form field validation with AJAX
- Source filtering so that they can't be equal
- Filter classes to keep only those of the chosen source
- Filter predicates to keep only those of the chosen class
- Form field precompletion via GET
- Prefix management
- Use http://jqueryui.com/demos/autocomplete/#combobox
- Add a help page on the Datalift wiki ?
- Add an example to work with ?
- Add unit tests ?

## External resources ##

* StringToURI is made with the Sesame API and repositories.
* [StringToURI documentation]("http://stringtouri.assembla.me/")
* [StringToURI interface design and development]("http://thibweb.github.com/linked-lifted/")
