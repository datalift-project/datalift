function VocabSummary(vocabulary) {
	var self = this;
	
	self.vocabulary = vocabulary.vocabulary;
	self.count = 1;
	self.score = vocabulary.score;
	self.predicates = [];
	
	self.addPredicate = function(name, uri) {
		if ( ! self.containsPredicate(uri) ) {
			++self.count;
			self.predicates.push({
				'name': name,
				'uri': uri
			});
		}
	}
	
	self.removePredicate = function(predicateToRemove) {
		self.predicates.splice(self.predicates.indexOf(predicateToRemove), 1);
	}
	
	self.containsPredicate = function(uri) {
		for ( var i = 1 ; i < self.predicates.length ; ++i ) {
			if ( self.predicates[i].uri == uri ) {
				return true;
			}
		}
		return false;
	}

}

function VocabList() {
	var self = this;

	self.vocabularies = [];
	
	self.indexOfVocabulary = function(vocabularyUri) {
		for ( var i = 0 ; i < self.vocabularies.length ; ++i ) {
			if ( self.vocabularies[i].vocabulary == vocabularyUri ) return i;
		}
		return -1;
	}
	
	self.addVocabulary = function(lovResult) {
//		if (lovResult.hasOwnProperty("results")) {
//			console.log("lovResult has no results property in VocabList.addVocabulary" + JSON.stringify(lovResult));
//			return;
//		}
		for ( var i = 0 ; i < lovResult["results"].length ; ++i ) {
			var index = self.indexOfVocabulary(lovResult["results"][i].vocabulary);
			
			// See if the vocabulary is already in our list
			if ( index != -1 ) {
				self.vocabularies[index].score += lovResult["results"][i].score;
				self.vocabularies[index].addPredicate(lovResult.sourceName, lovResult.sourceUri);
			}
			else {
				var vocabSummary = new VocabSummary(lovResult["results"][i]);
				vocabSummary.addPredicate(lovResult.sourceName, lovResult.sourceUri);
				self.vocabularies.push(vocabSummary);
			}
		}
	}
}

function SearchOntologyProperties() {
	var self = this;
	
	self.ontology = {};
	
	self.search = function(keywords, ontology) {
		var keywordsList = keywords.split(" ");
		for ( var i = 0 ; i < keywordsList.length ; ++i ) {
		}
	}
	
	self.searchKeyword = function(keyword, ontology) {
		
	}
	
	self.rank = function() {
		
	}
	
}
