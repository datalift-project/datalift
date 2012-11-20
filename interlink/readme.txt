Title: Silk Interlink Readme 
Author: Thibaud Colas 
Date: 07/10/12

# Silk Interconnection Readme #

## tl;dr ##

This module is an interface for [Silk]("http://www4.wiwiss.fu-berlin.de/bizer/silk/").  
Version 1.0

## What is Silk Interlink ##

The Interlink module launches the Silk link generation framework using custom configuration files.  The configuration files are either directly uploaded by the user or generated using a web form.
When launching Silk, new interlinkings are created according to critera in the given configuration file.

## Future development ##

- Server-side form field validation with AJAX
- Source filtering so that they can't be equal
- Filter values to prevent errors
- Form field precompletion via GET
- Prefix management
- Use localStorage to store field values
- Use sliders / tickers to increment threshold and weight
- Use input = number, input = range, placeholder with polyfills

## External resources ##

* [Silk documentation]("http://www4.wiwiss.fu-berlin.de/bizer/silk/")
* [Interconnection module interface design and development]("http://thibweb.github.com/linked-lifted/")
