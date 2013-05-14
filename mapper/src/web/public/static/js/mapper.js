
/**
 * Utility functions
 */
function localName(s) {
  var i = s.lastIndexOf('#');
  if (i == -1) {
    i = s.lastIndexOf('/');
  }
  return (i != -1)? s.slice(i+1): s;
}

function buildDescription(desc) {
  if (desc) {
    if (desc.length > 65) {
      var i = desc.lastIndexOf(' ', 65);
      if (i < 48) i = 64;
      desc = desc.substring(0, i) + "..."
    }
  }
  return desc;
}

function compare(a, b) {
  return a.compareTo(b);
}

function indent(e) {
  return (e == undefined)? 0: (1 + indent(e.owner));
}

var propertyMap = [];

/**
 * The OwlOntology class
 */
function OwlOntology(uri, name, desc, classes, properties) {
  var self = this;

  self.uri = uri;
  self.name = name;
  self.desc = buildDescription(desc);

  self.classList = classes;
  self.classes = [];
  if (classes != undefined) {
    // Populate class-by-URI map.
    for (var i=0; i<classes.length; i++) {
      var c = classes[i];
      self.classes[c.uri] = c;
    }
  }
  self.propList = properties;
  self.properties = [];
  if (properties != undefined) {
    for (var i=0; i<properties.length; i++) {
      var p = properties[i];
      self.properties[p.uri] = p;
    }
  }

  self.toString = function() {
    return self.name + ' - ' + self.uri;
  };
  self.compareTo = function(o) {
    return (o == null)? 1:
           (self.name == o.name)? 0: (self.name < o.name)? -1: 1;
  };

  self.getClass = function(uri) {
    return self.classes[uri];
  };
  self.getProperty = function(uri) {
    return self.properties[uri];
  };

  self.concreteTypes = function(owlClass) {
    var results = [];
    var roots = (owlClass == undefined)? self.classes: owlClass.subclasses;
    self.leafClasses(roots, false, results);
    var types = [];
    for (var k in results) { types.push(results[k]); }
    return types;
  };
  self.unionTypes = function(owlClass) {
    var results = [];
    var src = owlClass.parents(true);
    src[owlClass.uri] = owlClass;       // In case owlClass has no parent.
    self.leafClasses(src, true, results);
    var types = [];
    for (var k in results) { types.push(results[k]); }
    return types;
  };
  self.leafClasses = function(classes, union, results) {
    var count = 0;
    for (var k in classes) {
      var c = classes[k];
      var l = results.length;
      var i = self.leafClasses(c.subclasses, union, results);
      if ((i == 0) && (c.union == union)) {
        results[c.uri] = c;
        i++;
      }
      count += i;
    }
    return count;
  };

  self.toJSON = function(key) {
    return { name: self.name, uri: self.uri };
  };

  // Initialization completion: resolve URIs into objects.
  for (var k in self.classes) {
    self.classes[k].resolveUris(self);
  }
  for (var k in self.properties) {
    self.properties[k].resolveUris(self);
  }
}

/**
 * The OwlClass class
 */
function OwlClass(uri, data) {
  var self = this;

  self.uri  = uri;
  self.name = localName(uri);
  self.desc = buildDescription(data.desc);
  self.parentUris = (data.parents)? data.parents: [];
  self.subclassUris = (data.subclasses)? data.subclasses: [];
  self.disjoints = (data.disjoints)? data.disjoints: [];
  self.propUris = (data.properties)? data.properties: [];
  self.union = (data.union == true);

  self.parentClasses = [];
  self.subclasses = [];
  self.props = [];

  self.toString = function() {
    var s = self.name;
    if (self.desc) {
      s += ' - ' + self.desc;
    }
    return s;
  };
  self.compareTo = function(o) {
    return (o == null)? 1:
           (self.name == o.name)? 0: (self.name < o.name)? -1: 1;
  };

  self.resolveUris = function(ontology) {
    if (ontology != undefined) {
      // Resolve parent classes
      for (var i=0; i<self.parentUris.length; i++) {
        var c = ontology.getClass(self.parentUris[i]);
        if (c) {
          self.parentClasses[c.uri] = c;
        }
      }
      // Resolve subclasses
      for (var i=0; i<self.subclassUris.length; i++) {
        var c = ontology.getClass(self.subclassUris[i]);
        if (c) {
          self.subclasses[c.uri] = c;
        }
      }
      // Resolve properties
      for (var i=0; i<self.propUris.length; i++) {
        var p = ontology.getProperty(self.propUris[i]);
        if (p) {
          self.props[p.uri] = p;
        }
      }
    }
  };

  self.parents = function(recurse, results) {
    if (recurse) {
      if (results == undefined) {
        results = [];
      }
      for (var k in self.parentClasses) {
        if (results[k] == null) {
          // Not already present. => Add class and its own parents.
          var c = self.parentClasses[k];
          results[k] = c;
          c.parents(recurse, results);
        }
      }
      return results;
    }
    else {
      return self.parentClasses;
    }
  };

  self.properties = function(recurse, results) {
    if (recurse) {
      if (results == undefined) {
        results = [];
      }
      for (var k in self.props) {
        results[k] = self.props[k];
      }
      for (var k in self.parentClasses) {
        self.parentClasses[k].properties(recurse, results);
      }
      return results;
    }
    else {
      return self.props;
    }
  };

  self.toJSON = function(key) {
    return self.uri;
  };
}

/**
 * The OwlProperty class
 */
function OwlProperty(uri, data) {
  var self = this;

  self.uri  = uri;
  self.name = localName(uri);
  self.type = data.type;
  self.desc = buildDescription(data.desc);
  // Ignore domains as properties are directly linked by domain classes.
  self.rangeUris = (data.ranges)? data.ranges: [];

  self.ranges = [];

  self.toString = function() {
    var s = self.name;
    if (self.desc) {
      s += ' - ' + self.desc;
    }
    return s;
  };
  self.compareTo = function(o) {
    return (o == null)? 1:
           (self.name == o.name)? 0: (self.name < o.name)? -1: 1;
  };

  self.resolveUris = function(ontology) {
    if (ontology != undefined) {
      // Resolve range (value) classes
      for (var i=0; i<self.rangeUris.length; i++) {
        var c = ontology.getClass(self.rangeUris[i]);
        if (c) {
          self.ranges[c.uri] = c;
        }
      }
    }
  };

  self.toJSON = function(key) {
    return self.uri;
  };
}

/**
 * The MappingDesc class: the result of the mapping
 */
function MappingDesc(owner, owlClass, property, value) {
  var self = this;

  self.owner = owner;
  self.types = ko.observableArray([]);
  if (owlClass) {
    self.types.push(owlClass);
  }
  self.predicate = property;
  self.value = value;
  self.children = ko.observableArray([]);

  self.title = (property)? property.name: '';
  if (owlClass) {
    if (self.title.length != 0) self.title += ': ';
    self.title += owlClass.name;
  };

  self.label = ko.computed(function() {
      var s = self.title;
      if (self.types().length > 1) {
        var t = [];
        for (var i=1; i<self.types().length; i++) {
          t.push(self.types()[i].name);
        }
        s += ' (' + t.join(', ') + ')';
      }
      if (self.value) {
        s += ' = ' + self.value;
      }
      return s;
    });

  self.indent = '' + (8 + (indent(self.owner) * 16)) + 'px';

  self.toJSON = function(key) {
    var js = {};
    if (self.predicate) {
      js.predicate = self.predicate;
    }
    if (self.types().length != 0) {
      js.types = self.types();
    }
    if (self.value) {
      var elts = self.value.split(/\+/);
      var v = '';
      for (var i=0; i<elts.length; i++) {
        if (i != 0) {
          v += '+';
        }
        // Trim value to try to resolve a property name."
        var uri = propertyMap[elts[i].replace(/^\s+|\s+$/, '')];
        if (uri) {
          v += uri;
        }
        else {
          v += elts[i];
        }
      }
      js.value = v;
    }
    if (self.children().length != 0) {
      js.children = self.children();
    }
    return js;
  };
}

/**
 * Class to represent a sub part of a script
 */
function SubScript(name, source, target) {
  var self = this;
  
  self.name = name;
  self.source = source;
  self.target = target;
  
  self.toText = function() {
    return name + "(source=<" + source + ">,target=<" + target + ">)";
  }
  
  self.getLabel = function(uri) {
    var j = Math.max(uri.lastIndexOf('#'), uri.lastIndexOf('/')) + 1;
    return uri.substring(j);
  }
  
  self.getType = function() {
    if(name == 'MOVE_CLASS')
      return 'Class';
    else
      return 'Property';
  }
  
  self.toHtml = function() {
    return "<div>" + self.getType() + "</div>"
              + "<div>Source : " + self.getLabel(source) + "</div>"
              + "<div>Target : " + self.getLabel(target) + "</div>";
  }
}

/**
 * Class to represent a data property / a class mapping
 */
function Mapping(type, baseUri, selectedSource, uri, name) {
  var self = this;
  
  self.type = type;
  self.baseUri = baseUri;
  self.selectedSource = selectedSource;
  
  self.uri = uri;
  self.name = name;
  self.mapping = ko.observable();
  self.currentMapping = ko.observable();
  
  self.currentState = ko.computed(function() {
    if(self.mapping())
      return "Mapped with " + self.currentMapping().vocabElementName();
    else
      return "No mapping";
  });
  
  self.script = ko.computed(function() {
    if(self.mapping())
      return self.mapping().toText();
    else
      return "";
  });
  
  self.html = ko.computed(function() {
    if(self.mapping()) 
      return self.mapping().toHtml();
    else
      return "";
  });
  
  self.selectMapping = function(m) {
    if (m == self.currentMapping())
      return;

    if (m) {
      self.currentMapping(m);
      self.addMapping(m.vocabElementUri());
    }
  }

  self.addMapping = function(targetUri) {
    var scriptType = "";
    if(self.type == 'PROPERTY')
      scriptType = 'MOVE_PREDICATE';
    else
      scriptType = 'MOVE_CLASS';
      
    self.mapping(new SubScript(scriptType, self.uri, targetUri));
  }
  
  self.removeMapping = function() {
    self.mapping(null);
  }
  
}

/**
 * Search results
 */
function VocabElement(data) {
  var self = this;
  
  self.vocabElementUri = ko.observable(data.uri);
  self.vocabElementName = ko.observable(data.uriPrefixed);
  self.vocabElementType = ko.observable(data.types);
  self.vocabElementVocabUri = ko.observable(data.vocabulary);
  self.vocabElementScore = ko.observable(data.score);
  
}

function Vocab(data) {
  var self = this;
  
  self.vocabUri = ko.observable(data.vocabulary);
  self.vocabPrefix = ko.observable(data.vocabularyPrefix);
  self.vocabNsp = ko.observable(data.vocabularyPrefix);
  self.vocabName = ko.observable(data.uriPrefixed);
  
}

function SearchResult(vocabElements, vocabs) {
  var self = this;
  
  self.vocabElements = ko.observableArray(vocabElements);
  self.vocabs = ko.observableArray(vocabs);
  
}

/**
 * Viewmodel for the ontology mapper
 */
function MappingViewModel(baseUri, projectUri, sources, ontologies) {
  var self = this;
  
  self.baseUri = baseUri;
  self.projectUri = projectUri;
  self.sources = sources;

  self.availableOntologies = ko.observableArray([]);
  for (var i=0; i<ontologies.length; i++) {
    self.availableOntologies.push(new OwlOntology(ontologies[i].uri, ontologies[i].title));
  }
  self.availableOntologies.sort();

  self.selectedSource = ko.observable();
  self.createNewSource = ko.observable(true);
  self.targetSrcName  = ko.observable("New source");
  self.targetSrcGraph = ko.observable(self.sources[0].uri + '-mapped');
  self.availableSrcProps = [];
  self.displaySourceSelection = ko.observable(true);
  self.isSourceSelected = ko.computed(function() {
    return self.selectedSource() != undefined;
  });
  
  self.multipleMapping = ko.observable(true);

  self.selectedOntology = ko.observable();
  self.currentOntology = ko.observable();

  self.mappings = ko.observableArray([]);
  self.currentMapping = ko.observable();

  self.prefixMapping = [];
  self.availableSrcProps = [];

  self.primaryTypes = ko.observable();
  self.selectedPrimaryType = ko.observable();
  self.secondaryTypes = ko.observable([]);
  self.selectedSecondaryType = ko.observable();
  self.availableProperties = ko.observable();
  self.selectedProperty = ko.observable();
  self.valueExpected = ko.observable(false);
  self.propertyValue = ko.observable();

  self.displayPreview = ko.observable(false);
  self.preview = ko.observable();
  self.queryComplete = ko.observable(false);
  
  self.multipleMapping.subscribe(function() {
    self.createNewSource(self.multipleMapping());
  });
  
  self.createNewSource.subscribe(function() {
    if( ! self.createNewSource())
      self.multipleMapping(false);
  });

  self.selectedOntology.subscribe(function() {
      self.currentOntology(null);
      self.primaryTypes(null);
      self.secondaryTypes([]);
      self.availableProperties(null);

      if (self.selectedOntology()) {
        $.ajax({
          url: self.baseUri + "/mapper/ontology?src=" + self.selectedOntology().uri,
          dataType: 'json',
          success: function(data) {
            self.currentOntology(new OwlOntology(data.uri, data.name, data.desc,
                $.map(data.classes,    function(elt, uri) { return new OwlClass(uri, elt); }),
                $.map(data.properties, function(elt, uri) { return new OwlProperty(uri, elt); })));
            var types = self.currentOntology().concreteTypes();
            if (types.length != 0) {
              types.sort();
              self.primaryTypes(types);
            }
          },
          error: function(jqXHR, textStatus, message) {
            alert(mapperErrorMessages.readOnlogyFailed + ':\n' + jqXHR.status + ' (' + jqXHR.statusText + ') ' + jqXHR.responseText);
          }
        });
      }
    });
  self.selectedProperty.subscribe(function() {
      self.selectProperty(self.selectedProperty());
    });
  self.selectedPrimaryType.subscribe(function() {
      self.valueExpected(false);

      var t = self.selectedPrimaryType();
      if (t) {
        var n = 0;
        for (var k in t.properties(true)) { n++; }
        if (n == 0) {
          self.valueExpected(true);
        }
      }
    });
  self.selectedSource.subscribe(function() {
      var url = self.baseUri + "/sparql?default-graph-uri=internal&query=SELECT DISTINCT ?p WHERE { graph <" + self.selectedSource().uri + "> { ?s ?p ?o . }}&max=25";
      $.ajax({
        url: url,
        dataType: 'json',
        success: function(data) {
          var rdf = new RegExp("http://www.w3.org/1999/02/22-rdf-syntax-ns#");
          var rdfs= new RegExp("http://www.w3.org/2000/01/rdf-schema#");
          var owl = new RegExp("http://www.w3.org/2002/07/owl#");
          var prefixMapping = [];
          self.availableSrcProps = [];
          for (var i=0; i<data.results.bindings.length; i++ ) {
            var p = data.results.bindings[i].p.value;
            // Substitute well-known prefixes.
            var name = p.replace(rdf, "rdf:").replace(rdfs, "rdfs:").replace(owl, "owl:");
            if (name == p) {
              // Not a well-known prefix. => Extract namespace
              var j = Math.max(p.lastIndexOf('#'), p.lastIndexOf('/')) + 1;
              var ns = p.substring(0, j);
              name = p.substring(j);
              // Resolve name conflicts.
              var k = prefixMapping.indexOf(ns);
              if (k == -1) {
                // Namespace not yet known.
                k = prefixMapping.push(ns) - 1;
              }
              if (k != 0) {
                name = 'ns' + k + ':' + name;
              }
              propertyMap[name] = p;
            }
            self.availableSrcProps.push(name);
          }
          if (self.availableSrcProps.length == 0) {
            alert(mapperErrorMessages.noRdfClassFound);
          }
        },
        error: function(jqXHR, textStatus, message) {
          alert(jqXHR.responseText);
        }
      });
    });

  self.addTypeMapping = function() {
    self.displaySourceSelection(false);
    var m = new MappingDesc(self.currentMapping(),
                            self.selectedPrimaryType());
    if (self.currentMapping()) {
      self.currentMapping().children.push(m);
    }
    self.mappings.push(m);
    self.selectMapping(m);
    self.updatePreview();
  }

  self.addSecondaryType = function() {
    var m = self.currentMapping();
    var t = self.selectedSecondaryType();
    var types = m.types();
    var present = false;
    for (var i=0; i<types.length; i++) {
      if (types[i] == t) present = true;
    }
    if (! present) {
      m.types.push(t);
    }
    self.selectMapping(m);
    self.updatePreview();
  }

  self.addPropertyMapping = function() {
    var p = self.selectedProperty();
    var c = null;
    if (p.type == 'ObjectProperty') {
      c = self.selectedPrimaryType();
    }
    var m = new MappingDesc(self.currentMapping(), c, p, self.propertyValue());
    if (self.currentMapping()) {
      var i = self.mappings.indexOf(self.currentMapping());
      if (i < self.mappings().length - 1) {
        self.mappings.splice(i+1, 0, m);
      }
      else {
        self.mappings.push(m);
      }
      self.currentMapping().children.push(m);
    }
    else {
      self.mappings.push(m);
    }
    if (c) {
      self.selectMapping(m);
    }
    self.updatePreview();
  }

  self.updatePreview = function() {
    if (self.mappings().length != 0) {
      var args = {
          sourceGraph: self.selectedSource().uri,
          ontology:    ko.toJSON(self.selectedOntology()),
          mapping:     ko.toJSON(self.mappings()[0])
        };
      $.post(self.baseUri + "/mapper/preview", args,
             function(data) {
               self.preview(data);
               self.queryComplete((data.search("WHERE") != -1));
             });
    }
    else {
      self.preview(null);
      self.queryComplete(false);
    }
  }

  $("#submit-button").click(function(event) {
    // Set form fields.
    $("#submit-srcGraph").val(self.selectedSource().uri);
    $("#submit-ontology").val(ko.toJSON(self.selectedOntology()));
    $("#submit-mapping").val(ko.toJSON(self.mappings()[0]));
    if (self.createNewSource()) {
      $("#submit-targetName").val(self.targetSrcName());
      $("#submit-targetGraph").val(self.targetSrcGraph());
    }
    // Submit form.
    $("#execute-mapping-form").submit();
  });

  self.selectMapping = function(m) {
    if (m == self.currentMapping()) return;

    self.primaryTypes(null);
    self.selectedPrimaryType(null);
    self.secondaryTypes([]);
    self.selectedSecondaryType(null);
    self.availableProperties(null);
    self.selectedProperty(null);
    self.valueExpected(false);
    self.propertyValue(null);

    if (m) {
      self.currentMapping(m);
      if (m.types().length != 0) {
        self.selectedPrimaryType(m.types()[0]);

        var types = self.currentOntology().unionTypes(m.types()[0]);
        for (var i=0; i<m.types().length; i++) {
          var n = types.indexOf(m.types()[i]);
          if (n >= 0) {
            types.splice(n, 1);
          }
        }
        types.sort();
        self.secondaryTypes((types.length != 0)? types: []);

        var results = m.types()[0].properties(true);
        var props = [];
        for (var k in results) { props.push(results[k]); }
        props.sort();
        self.availableProperties((props.length != 0)? props: null);
      }
      self.valueExpected(m.value != null);
      self.propertyValue(m.value);
      $("#property-value").typeahead({ source: self.availableSrcProps });
    }
  }

  self.selectProperty = function(p) {
    self.valueExpected(false);

    if (p) {
      if (p.type == 'ObjectProperty') {
        var types = [];
        if (p.rangeUris.length != 0) {
          for (var c in p.ranges) {
            types.push(p.ranges[c]);
          }
        }
        else {
          types = self.currentOntology().concreteTypes();
        }
        types.sort();
        self.primaryTypes(types);
        self.selectedPrimaryType((types.length == 1)? types[0]: null);
      }
      else {
        self.valueExpected(true);
        self.propertyValue('');
      }
    }
  }

  self.removeMapping = function(m) {
    var p = m.owner;
    for (var i=0; i<m.children.length; i++) {
      self.removeMapping(children[i]);
    }
    p.children.remove(m);
    // Remove mapping.
    self.mappings.remove(m);
    // Make parent mapping the selected one.
    self.selectMapping(p);
    self.updatePreview();
  }
  
  /* * * * * * * * * * *
   * Multiple ontology *
   * * * * * * * * * * */
  
  self.availableSrcClasses = ko.observableArray([]);
  self.propertiesToMap = ko.observableArray([]);
  self.classesToMap = ko.observableArray([]);
  self.currentPropertyToMap = ko.observable();
  self.currentClassToMap = ko.observable();
  self.searchQuery = ko.observable();
  self.searchResults = ko.observable();
  self.currentResultSelection = ko.observable();
  self.executing = ko.observable(false);
  self.executingError = ko.observable(false);
  
  self.classesToMap.subscribe(function() {
    if( ! self.currentClassToMap())
      self.currentClassToMap(self.classesToMap()[0]);
  });
  
  self.currentClassToMapName = ko.computed(function() {
    if(self.currentClassToMap())
      return self.currentClassToMap().name;
    else {
      if(self.availableSrcClasses)
        return self.availableSrcClasses[0];
      else
        return "";
    }
  });
  
  self.selectClassToMap = function(classToMap) {
    if(classToMap == self.currentClassToMap())
      return;
      
    if(classToMap) {
      self.currentClassToMap(classToMap);
      self.searchResults(null);
      self.currentResultSelection(null);
      self.searchQuery("");
      //todo ???
    }
  }
  
  self.currentPropToMapName = ko.computed(function() {
    if(self.currentPropertyToMap())
      return self.currentPropertyToMap().name;
    else {
      if(self.availableSrcProps)
        return self.availableSrcProps[0];
      else
        return "";
    }
  });
  
  self.selectPropertyToMap = function(propertyToMap) {
    if(propertyToMap == self.currentPropertyToMap())
      return;
    
    if(propertyToMap) {
      self.currentPropertyToMap(propertyToMap);
      self.searchResults(null);
      self.currentResultSelection(null);
      self.searchQuery("");
      //todo ???
    }
  };

  self.currentResultSelectionUri = ko.computed(function() {
    if(self.currentResultSelection()) {
      return self.currentResultSelection().vocabElementUri();
    }
    else
      return "";
  });
  
  self.submitSrc = function() {
    var url_type = self.baseUri + "/sparql?default-graph-uri=internal&query=SELECT DISTINCT ?o WHERE { graph <" + self.selectedSource().uri + "> { ?s a ?o . }}&max=25";
    var url_prop = self.baseUri + "/sparql?default-graph-uri=internal&query=SELECT DISTINCT ?p WHERE { graph <" + self.selectedSource().uri + "> { ?s ?p ?o . }}&max=25";
    
    $.ajax({
      url: url_type,
      dataType: 'json',
      success: function(data) {
        for(var i=0; i<data.results.bindings.length; i++) {
          var o = data.results.bindings[i].o.value;
          var j = Math.max(o.lastIndexOf('#'), o.lastIndexOf('/')) + 1;
          var ns = o.substring(0, j);
          var name = o.substring(j);
          self.availableSrcClasses.push(name);
          self.classesToMap.push(new Mapping('CLASS', self.baseUri, self.selectedSource, o, name));
        }
        
        if(self.classesToMap.length > 0)
          self.selectClassToMap(classesToMap[0]);
      }
    });
    
    $.ajax({
      url: url_prop,
      dataType: 'json',
      success: function(data) {
        var rdf = new RegExp("http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        var rdfs= new RegExp("http://www.w3.org/2000/01/rdf-schema#");
        var owl = new RegExp("http://www.w3.org/2002/07/owl#");
        var prefixMapping = [];
        for (var i=0; i<data.results.bindings.length; i++ ) {
          var p = data.results.bindings[i].p.value;
          // Substitute well-known prefixes.
          var name = p.replace(rdf, "rdf:").replace(rdfs, "rdfs:").replace(owl, "owl:");
          if (name == p) {
            // Not a well-known prefix. => Extract namespace
            var j = Math.max(p.lastIndexOf('#'), p.lastIndexOf('/')) + 1;
            var ns = p.substring(0, j);
            name = p.substring(j);
            // Resolve name conflicts.
            var k = prefixMapping.indexOf(ns);
            if (k == -1) {
              // Namespace not yet known.
              k = prefixMapping.push(ns) - 1;
            }
            if (k != 0) {
              name = 'ns' + k + ':' + name;
            }
            propertyMap[name] = p;
          }
          if(name != "rdf:type") {
            self.availableSrcProps.push(name);
            self.propertiesToMap.push(new Mapping('PROPERTY', self.baseUri, self.selectedSource, p, name));
          }
        }
        if (self.availableSrcProps.length == 0) {
          alert(mapperErrorMessages.noRdfClassFound);
        }
        self.displaySourceSelection(false);
        self.selectPropertyToMap(self.propertiesToMap()[0]);
      },
      error: function(jqXHR, textStatus, message) {
        alert(jqXHR.responseText);
      }
    });
  }
  
  self.searchLovClasses = function() {
    self.searchLov("http://www.w3.org/2000/01/rdf-schema%23Class");
  }
  
  self.searchLovProperties = function() {
    self.searchLov("http://www.w3.org/1999/02/22-rdf-syntax-ns%23Property");
  }
  
  self.searchLov = function(type) {
    var url = self.baseUri + "/lov/search?q=" + self.searchQuery() + "&type=" + type;
    $.ajax({
      url: url,
      dataType: 'json',
      success: function(data) {
        var elements = $.map(data.results, function(item) { return new VocabElement(item); });
        var vocabs = $.map(data.results, function(item) { return new Vocab(item); });
        self.currentPage(1);
        self.searchResults(new SearchResult(elements, vocabs));
      },
      error: function(jqXHR, textStatus, message) {
        alert(jqXHR.responseText);
      }
    });
  }
  
  self.selectResult = function(result) {
    
    self.currentResultSelection(result);
    
    if(self.selectedTab() == 'classes') {
      self.currentClassToMap().selectMapping(result);
    }
    if(self.selectedTab() == 'properties') {
      self.currentPropertyToMap().selectMapping(result);
    }
  }
  
  self.executeMappings = function() {
    
    self.executing(true);
    self.executingError(false);
    
    var script = "";
    
    for(var i=0 ; i < self.classesToMap().length ; ++i) {
      if(self.classesToMap()[i].script() != "")
        script += self.classesToMap()[i].script() + ";";
    }
    
    for(var i=0 ; i < self.propertiesToMap().length ; ++i) {
      if(self.propertiesToMap()[i].script() != "")
        script += self.propertiesToMap()[i].script() + ";";
    }
    
    script = script.substring(0, script.length - 1);
    
    var project = self.projectUri;
    var source = self.selectedSource().uri;
    var url = self.baseUri + "/mapper/execute?project=" + project + "&source=" + source;
    var data = { dest_title: self.targetSrcName(), dest_graph_uri: self.targetSrcGraph(),
      script: script };
    
    executingProgress(0);
    
    $.post(url, data)
    .done(function() {
      $("progress-bar").css("width", "100%");
      window.location.replace(self.projectUri);
    })
    .fail(function() {
      self.executingError(true);
    })
    .always(function() {
      self.executing(false);
    });
  }
  
  executingProgress = function(percent) {
    if(percent < 88) {
      ++percent;
      $("#progress-bar").css("width", percent + "%");
      setTimeout("executingProgress(" + percent + ")", 50);
    }
  }
  
  $("#search-input").keypress(function(event) {
     if(event.which == 13) {
      event.preventDefault();
      
      self.searchQuery($("#search-input").val());

      if(self.selectedTab() == 'classes') {
        self.searchLovClasses();
      }
      else {
        self.searchLovProperties();
      }
    }
    
  });
  
  /*
   * Nav tabs
   */
  self.selectedTab = ko.observable("classes");
  
  self.selectClasses = function() {
    self.selectedTab("classes");
    self.searchResults(null);
    self.searchQuery("");
  }
  
  self.selectProperties = function() {
    self.selectedTab("properties");
    self.searchResults(null);
    self.searchQuery("");
  }
  
  /*
   * Search grid results
   */
  self.pageSize = 10;
  self.currentPage = ko.observable(1);
  self.pageNumber = ko.computed(function() {
    if(self.searchResults()) {
      return Math.ceil(self.searchResults().vocabElements().length / self.pageSize);
    }
    return 0;
  });
  
  self.startIndex = ko.computed(function() {
    return Math.ceil((self.currentPage() - 1) * self.pageSize);
  });
  
  self.displayedResults = ko.computed(function() {
    if(self.searchResults()) {
      if(self.searchResults().vocabElements())
        return self.searchResults().vocabElements().slice(self.startIndex(), self.startIndex() + self.pageSize);
    }
    return [];

  });
  
  self.currentPageText = ko.computed(function() {
    if(self.pageNumber() > 0) {
      return self.currentPage() + " / " + self.pageNumber();
    }
    return "";
  });
  
  self.prevPage = function() {
    if(self.currentPage() > 1) {
      self.currentPage(self.currentPage() - 1);
    }
  }
  
  self.nextPage = function() {
    if(self.currentPage() < self.pageNumber()) {
      self.currentPage(self.currentPage() + 1);
    }
  }
  
};
