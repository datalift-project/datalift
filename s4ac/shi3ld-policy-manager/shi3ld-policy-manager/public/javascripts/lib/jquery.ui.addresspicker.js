/*
 * jQuery UI addresspicker @VERSION
 *
 * Copyright 2010, AUTHORS.txt (http://jqueryui.com/about)
 * Dual licensed under the MIT or GPL Version 2 licenses.
 * http://jquery.org/license
 *
 * http://docs.jquery.com/UI/Progressbar
 *
 * Depends:
 *   jquery.ui.core.js
 *   jquery.ui.widget.js
 *   jquery.ui.autocomplete.js
 */
 
 
 //-------------------RADIUS MANAGEMENT (added to original lib) ----------------------
 /**
       * A distance widget that will display a circle that can be resized and will
       * provide the radius in km.
       *
       * @param {google.maps.Map} map The map to attach to.
       *
       * @constructor
       */
      function DistanceWidget(addressPicker) {
        this.set('map', addressPicker.gmap);
        this.set('position', addressPicker.gmap.getCenter());

        /*var marker = new google.maps.Marker({
          draggable: true,
          title: 'Move me!'
        });*/
		var marker = addressPicker.gmarker;
        // Bind the marker map property to the DistanceWidget map property
        marker.bindTo('map', this);

        // Bind the marker position property to the DistanceWidget position
        // property
        marker.bindTo('position', this);

        // Create a new radius widget
        this.gradius = new RadiusWidget();

        // Bind the radiusWidget map to the DistanceWidget map
        this.gradius.bindTo('map', this);

        // Bind the radiusWidget center to the DistanceWidget position
        this.gradius.bindTo('center', this, 'position');

        // Bind to the radiusWidgets' distance property
        this.bindTo('distance', this.gradius);

        // Bind to the radiusWidgets' bounds property
        this.bindTo('bounds', this.gradius);
      }
      DistanceWidget.prototype = new google.maps.MVCObject();


      /**
       * A radius widget that add a circle to a map and centers on a marker.
       *
       * @constructor
       */
      function RadiusWidget() {
        this.gcircle = new google.maps.Circle({
          strokeWeight: 2
        });

        // Set the distance property value, default to 50km.
        this.set('distance', 1);

        // Bind the RadiusWidget bounds property to the circle bounds property.
        this.bindTo('bounds', this.gcircle);

        // Bind the circle center to the RadiusWidget center property
        this.gcircle.bindTo('center', this);

        // Bind the circle map to the RadiusWidget map
        this.gcircle.bindTo('map', this);

        // Bind the circle radius property to the RadiusWidget radius property
        this.gcircle.bindTo('radius', this);
		
		this.gcircle.setVisible(false);

        // Add the sizer marker
        this.addSizer_();
      }
      RadiusWidget.prototype = new google.maps.MVCObject();


      /**
       * Update the radius when the distance has changed.
       */
      RadiusWidget.prototype.distance_changed = function() {
        this.set('radius', this.get('distance') * 1000);
      }


      /**
       * Add the sizer marker to the map.
       *
       * @private
       */
      RadiusWidget.prototype.addSizer_ = function() {
        this.gsizer = new google.maps.Marker({
          draggable: true,
          title: 'Drag me!',
		  raiseOnDrag: false,
		  icon: '../images/resize-off.png'
        });

        this.gsizer.bindTo('map', this);
        this.gsizer.bindTo('position', this, 'sizer_position');
		this.gsizer.setVisible(false);

        var me = this;
        google.maps.event.addListener(this.gsizer, 'drag', function() {
          // Set the circle distance (radius)
          me.setDistance_();
        });
      }


      /**
       * Update the center of the circle and position the sizer back on the line.
       *
       * Position is bound to the DistanceWidget so this is expected to change when
       * the position of the distance widget is changed.
       */
      RadiusWidget.prototype.center_changed = function() {
        var bounds = this.get('bounds');

        // Bounds might not always be set so check that it exists first.
        if (bounds) {
          var lng = bounds.getNorthEast().lng();

          // Put the sizer at center, right on the circle.
          var position = new google.maps.LatLng(this.get('center').lat(), lng);
          this.set('sizer_position', position);
        }
      }
	
	/**
	 * Finds the closest left or right of the circle to the position.
	 *
	 * @param {google.maps.LatLng} pos The position to check against.
	 * @return {google.maps.LatLng} The closest point to the circle.
	 * @private.
	 */
	RadiusWidget.prototype.getSnappedPosition_ = function(pos) {
	  var bounds = this.get('bounds');
	  var center = this.get('center');
	  var left = new google.maps.LatLng(center.lat(),
	      bounds.getSouthWest().lng());
	  var right = new google.maps.LatLng(center.lat(),
	      bounds.getNorthEast().lng());
	
	  var leftDist = this.distanceBetweenPoints_(pos, left);
	  var rightDist = this.distanceBetweenPoints_(pos, right);
	
	  if (leftDist < rightDist) {
	    return left;
	  } else {
	    return right;
	  }
	  };
	  
	  /**
	 * Update the center of the circle and position the sizer back on the line.
	 */
	RadiusWidget.prototype.active_changed = function() {
	  var strokeColor;
	  var icon;
	
	  if (this.get('active')) {
	    if (this.get('activeColor')) {
	      strokeColor = this.get('activeColor');
	    }
	
	    if (this.get('activeSizerIcon')) {
	      icon = this.get('activeSizerIcon');
	    }
	  } else {
	    strokeColor = this.get('color');
	
	    icon = this.get('sizerIcon');
	  }
	
	  if (strokeColor) {
	    this.set('strokeColor', strokeColor);
	  }
	
	  if (icon) {
	    this.set('icon', icon);
	  }
	};
	
	
	/**
	 * Set the distance of the circle based on the position of the sizer.
	 * @private
	 */
	RadiusWidget.prototype.setDistance_ = function() {
	  // As the sizer is being dragged, its position changes.  Because the
	  // RadiusWidget's sizer_position is bound to the sizer's position, it will
	  // change as well.
	  var pos = this.get('sizer_position');
	  var center = this.get('center');
	  var distance = this.distanceBetweenPoints_(center, pos);
	
	  if (this.get('maxDistance') && distance > this.get('maxDistance')) {
	    distance = this.get('maxDistance');
	  }
	
	  if (this.get('minDistance') && distance < this.get('minDistance')) {
	    distance = this.get('minDistance');
	  }
	
	  // Set the distance property for any objects that are bound to it
	  this.set('distance', distance);
	
	  var newPos = this.getSnappedPosition_(pos);
	  this.set('sizer_position', newPos);
	};
	
	
	/**
	 * Finds the closest left or right of the circle to the position.
	 *
	 * @param {google.maps.LatLng} pos The position to check against.
	 * @return {google.maps.LatLng} The closest point to the circle.
	 * @private.
	 */
	RadiusWidget.prototype.getSnappedPosition_ = function(pos) {
	  var bounds = this.get('bounds');
	  var center = this.get('center');
	  var left = new google.maps.LatLng(center.lat(),
	      bounds.getSouthWest().lng());
	  var right = new google.maps.LatLng(center.lat(),
	      bounds.getNorthEast().lng());
	
	  var leftDist = this.distanceBetweenPoints_(pos, left);
	  var rightDist = this.distanceBetweenPoints_(pos, right);
	
	  if (leftDist < rightDist) {
	    return left;
	  } else {
	    return right;
	  }
	
	};

      /**
       * Calculates the distance between two latlng points in km.
       * @see http://www.movable-type.co.uk/scripts/latlong.html
       *
       * @param {google.maps.LatLng} p1 The first lat lng point.
       * @param {google.maps.LatLng} p2 The second lat lng point.
       * @return {number} The distance between the two points in km.
       * @private
       */
      RadiusWidget.prototype.distanceBetweenPoints_ = function(p1, p2) {
        if (!p1 || !p2) {
          return 0;
        }

        var R = 6371; // Radius of the Earth in km
        var dLat = (p2.lat() - p1.lat()) * Math.PI / 180;
        var dLon = (p2.lng() - p1.lng()) * Math.PI / 180;
        var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
          Math.cos(p1.lat() * Math.PI / 180) * Math.cos(p2.lat() * Math.PI / 180) *
          Math.sin(dLon / 2) * Math.sin(dLon / 2);
        var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        var d = R * c;
        return d;
      };
	
	
	
//-------------------RADIUS MANAGEMENT END ------------------------------------------
 
 
(function( $, undefined ) {
  var centre;
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(function (position) {
    	centre = new google.maps.LatLng(position.coords.latitude, position.coords.longitude);
  	});
  } else {
    centre = new google.maps.LatLng(46, 2);
  }	
  $.widget( "ui.addresspicker", {
    
    options: {
        appendAddressString: "",
        draggableMarker: true,
        regionBias: null,
        updateCallback: null,
        reverseGeocode: true,//false,
        mapOptions: {
            zoom: 10, 
            center: centre, 
            scrollwheel: false,
            mapTypeId: google.maps.MapTypeId.ROADMAP
        },
        elements: {
            map: false,
            lat: false,
            lng: false,
            street_number: false,
            route: false,
            locality: false,
						administrative_area_level_2: false,
            administrative_area_level_1: false,
						country: false,
						postal_code: false,
            type: false

        }
    },
	
	//added to manage distanceWidget visibility
	//actually is used also to initialize address picker, radiusTxt, and gcircle
	showDistance: function () {
	  this.gmarker.setVisible(true);
	  this.radiusTxt.val(this.gdistance.gradius.gcircle.getRadius())
	  this.gdistance.gradius.gcircle.setVisible(true);
	  this.gdistance.gradius.gsizer.setVisible(true);
	  this._markerMoved();
	},
	
	hideDistance: function () {
	  this.gmarker.setVisible(false);
	  this.gdistance.gradius.gcircle.setVisible(false);
	  this.gdistance.gradius.gsizer.setVisible(false);
	},

    marker: function() {
      return this.gmarker;
    },
    
    map: function() {
      return this.gmap;
    },
	
	circle: function () {
		return this.gdistance.gradius.gcircle;
	},
	
    updatePosition: function() {
      this._updatePosition(this.gmarker.getPosition());
    },
    
    updateMap: function () {
    	google.maps.event.trigger(this.gmap,"resize");
    	this.gmap.setCenter(this.gmarker.getPosition());
	},
    
    reloadPosition: function() {
      this.gmarker.setVisible(true);
	  this.gdistance.gradius.gcircle.setVisible(true);
	  this.gdistance.gradius.gsizer.setVisible(true);
      this.gmarker.setPosition(new google.maps.LatLng(this.lat.val(), this.lng.val()));
      this.gmap.setCenter(this.gmarker.getPosition());
    },
    
    selected: function() {
      return this.selectedResult;
    },
    
    _create: function() {
      this.geocoder = new google.maps.Geocoder();
      this.element.autocomplete({
        source: $.proxy(this._geocode, this),  
        focus:  $.proxy(this._focusAddress, this),
        select: $.proxy(this._selectAddress, this)
      });
      
      this.lat      = $(this.options.elements.lat);
      this.lng      = $(this.options.elements.lng);
      this.street_number = $(this.options.elements.street_number);
      this.route = $(this.options.elements.route);
      this.locality = $(this.options.elements.locality);
			this.administrative_area_level_2 = $(this.options.elements.administrative_area_level_2);
			this.administrative_area_level_1 = $(this.options.elements.administrative_area_level_1);
      this.country  = $(this.options.elements.country);
			this.postal_code = $(this.options.elements.postal_code);
      this.type     = $(this.options.elements.type);
      if (this.options.elements.map) {
        this.mapElement = $(this.options.elements.map);
        this.radiusTxt = $(this.options.elements.radius)
        this._initMap();
      }
    },

    _initMap: function() {
      if (this.lat && this.lat.val()) {
        this.options.mapOptions.center = new google.maps.LatLng(this.lat.val(), this.lng.val());
      }
	  if (!this.options.mapOptions.center) {
        this.options.mapOptions.center = centre || new google.maps.LatLng(46, 2);
      }
      this.gmap = new google.maps.Map(this.mapElement[0], this.options.mapOptions);
      this.gmarker = new google.maps.Marker({
        position: this.options.mapOptions.center, 
        map:this.gmap, 
        draggable: this.options.draggableMarker});
      google.maps.event.addListener(this.gmarker, 'dragend', $.proxy(this._markerMoved, this));
      this.gmarker.setVisible(false);
	  
	  //radius add-on
	  this.gdistance = new DistanceWidget(this);
	  var gdist = this.gdistance = new DistanceWidget(this);
	  var that = this;
	  google.maps.event.addListener(gdist, 'distance_changed', function() {
  		displayInfo(gdist);
	  });
	  
	  //move the sizer when radius changed
	  var gradius = gdist.gradius;
	  google.maps.event.addListener(gradius, 'radius_changed', function() {
  		var bounds = this.get('bounds');

        // Bounds might not always be set so check that it exists first.
        if (bounds) {
          var lng = bounds.getNorthEast().lng();

          // Put the sizer at center, right on the circle.
          var position = new google.maps.LatLng(this.get('center').lat(), lng);
          this.set('sizer_position', position);
        }
	  });
	
	  function displayInfo(widget) {
	   //return distance in meters
	   var meters = widget.get('distance')* 1000;
	   that.radiusTxt.val(meters.toFixed(0)) ;
	  }
	  
	  this.radiusTxt.on('change', function() {
		var val = parseInt($(this).val(), 10);
		if (isNaN(val)) {
			$(this).val(gradius.gcircle.getRadius());
			return;
		}
		$(this).val(val);
		gradius.gcircle.setRadius(val);
		that.showDistance();
	  });
    },
	
	
    
    _updatePosition: function(location) {
      if (this.lat) {
        this.lat.val(location.lat());
      }
      if (this.lng) {
        this.lng.val(location.lng());
      }
    },

    _addressParts: {street_number: null, route: null, locality: null, 
                     administrative_area_level_2: null, administrative_area_level_1: null,
                     country: null, postal_code:null, type: null},

    _updateAddressParts: function(geocodeResult){

      parsedResult = this._parseGeocodeResult(geocodeResult);

      for (addressPart in this._addressParts){
        if (this[addressPart]){
          this[addressPart].val(parsedResult[addressPart]);
        }
      }
    }, 

    _updateAddressPartsViaReverseGeocode: function(location){
      var latLng = new google.maps.LatLng(location.lat(), location.lng());

      this.geocoder.geocode({'latLng': latLng}, $.proxy(function(results, status){
          if (status == google.maps.GeocoderStatus.OK)

            this._updateAddressParts(results[0]);
            this.element.val(results[0].formatted_address);
            this.selectedResult = results[0];

            if (this.options.updateCallback) {
              this.options.updateCallback(this.selectedResult, this._parseGeocodeResult(this.selectedResult));
            }
          }, this));
    },

    _parseGeocodeResult: function(geocodeResult){

      var parsed = {lat: geocodeResult.geometry.location.lat(),
        lng: geocodeResult.geometry.location.lng()};

      for (var addressPart in this._addressParts){
        parsed[addressPart] = this._findInfo(geocodeResult, addressPart);
      }

      parsed.type = geocodeResult.types[0];

      return parsed;
    },
    
    _markerMoved: function(options) {
      this._updatePosition(this.gmarker.getPosition());

      if (this.options.reverseGeocode){
        this._updateAddressPartsViaReverseGeocode(this.gmarker.getPosition());
      }
      
    },
    
    // Autocomplete source method: fill its suggests with google geocoder results
    _geocode: function(request, response) {
        var address = request.term, self = this;
        this.geocoder.geocode({
            'address': address + this.options.appendAddressString,
            'region': this.options.regionBias
        }, function(results, status) {
            if (status == google.maps.GeocoderStatus.OK) {
                for (var i = 0; i < results.length; i++) {
                    results[i].label =  results[i].formatted_address;
                };
            } 
            response(results);
        })
    },
    
    _findInfo: function(result, type) {
      for (var i = 0; i < result.address_components.length; i++) {
        var component = result.address_components[i];
        if (component.types.indexOf(type) !=-1) {
          return component.long_name;
        }
      }
      return false;
    },
    
    _focusAddress: function(event, ui) {
      var address = ui.item;
      if (!address) {
        return;
      }
      
      if (this.gmarker) {
        this.gmarker.setPosition(address.geometry.location);
        this.gmarker.setVisible(true);
        this.radiusTxt.val(this.gdistance.gradius.gcircle.getRadius())
		this.gdistance.gradius.gcircle.setVisible(true);
	    this.gdistance.gradius.gsizer.setVisible(true);

        this.gmap.fitBounds(address.geometry.viewport);
      }

      this._updatePosition(address.geometry.location);

      this._updateAddressParts(address);
      
    },
    
    _selectAddress: function(event, ui) {
      this.selectedResult = ui.item;
      if (this.options.updateCallback) {
        this.options.updateCallback(this.selectedResult, this._parseGeocodeResult(this.selectedResult));
      }
      //added to manage radius
      if (this.gmarker.getVisible() == false) {
      	this.showDistance();
      }
    }
  });

  $.extend( $.ui.addresspicker, {
    version: "@VERSION"
  });

  // make IE think it doesn't suck
  if(!Array.indexOf){
    Array.prototype.indexOf = function(obj){
      for(var i=0; i<this.length; i++){
        if(this[i]==obj){
          return i;
        }
      }
      return -1;
    }
  }

})( jQuery );
