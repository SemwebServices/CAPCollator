<!DOCTYPE html>
<html lang="en">
<head>
	  <title>Filtered Alert Hub: Most Recent CAP Alerts</title>
    <asset:link rel="icon" href="favicon.ico" type="image/x-ico" />
	<style type="text/css">
		/* map height defines the size of the div element that contains the map. */
		#map {
		  height: 100%;
		}
		html, body {
		  font-family: Arial, sans-serif;
		  background: #FFFFFF;
		  height: 100%;
		  margin: 0;
		  padding: 0;
		}
		div.legend {
		  font-family: Arial, sans-serif;
    	  background: rgba(255, 255, 255, 0.4);
		}
	</style>
	<!-- include Leaflet CSS file -->
	<link rel="stylesheet" href="https://unpkg.com/leaflet@1.0.2/dist/leaflet.css" />
	<!--[if lte IE 8]><link rel="stylesheet" href="leaflet/leaflet.ie.css" /><![endif]-->
	<!-- include Leaflet JavaScript file -->
	<script src="https://unpkg.com/leaflet@1.0.2/dist/leaflet.js"></script>
	<!-- include Leaflet plug-in PIP (Point in Polygon) JavaScript file -->
	<script src="https://unpkg.com/leaflet-pip@latest/leaflet-pip.js"></script>
		<!-- include JQuery javascript file -->
	<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.1.1/jquery.min.js"></script>
	<!--[if lte IE 9]>
	<script type='text/javascript' src='//cdnjs.cloudflare.com/ajax/libs/jquery-ajaxtransport-xdomainrequest/1.0.0/jquery.xdomainrequest.min.js'></script>
	<![endif]-->
    <!-- Automatically provides/replaces `Promise` if missing or broken. -->
    <script src="https://cdn.jsdelivr.net/npm/es6-promise@4/dist/es6-promise.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/es6-promise@4/dist/es6-promise.auto.js"></script> 
	<script language="javascript">
//  Note: This code written with dependencies on JQuery, Leaflet, and
//        on @mapbox/leaflet-pip (that is: point in polygon for Leaflet)
var subscriptionId = "unfiltered";
//var subscriptionId = "official-public";
var subscriptionUrl = "https://alert-feeds.s3.amazonaws.com/"+subscriptionId+"/rss.xml";
var subscriptionName = "";
var feedItemsLimit = 0;
var mapCenterLat = 0;
var mapCenterLng = 0;
var mapZoom = 2;
var capNs = "";
var countAlerts = 0;
var getAlertErrors = 0;
var fileRefs = {
	"alerts": []
	};
var map;
//  onEachFeature does setStyle for display
var alerts = L.geoJson(false, {
	onEachFeature: onEachFeature
	});
	
function controller() {
  // This function is called from HTML body onload event.
  // On completeion, all alerts are shown and map is ready 
  // for user interaction via an onClick event. The function
  // uses four Promises that must be executed in order. An 
  // error in any Promise causes fall through to the end.

  var getSubscriptionParms = function() {
    return new Promise(function(resolve, reject){
      $.ajax({
        type: "GET",
        aysnc: false,
        url: "https://alert-hub-subscriptions.s3.amazonaws.com/json",
        dataType: "json",
        success: function(json) {
          for(var i =0; i < json.subscriptions.length; i++) {
            if (!(subscriptionId == json.subscriptions[i].subscription.subscriptionId)) { 
              continue; 
			};
            // found the matching subscriptionId
            subscriptionName = json.subscriptions[i].subscription.subscriptionName;
            feedItemsLimit = json.subscriptions[i].subscription.feedItemsLimit;
            var polygonCoordinates = L.polygon(json.subscriptions[i].subscription.areaFilter.polygonCoordinates);
            subscriptionBounds = polygonCoordinates.getBounds();
            // Note: In this case, Leaflet function getBounds() returns lat and lng reversed, like Json
			var boxSouth = subscriptionBounds.getSouthWest().lng; // not lat!
            var boxWest = subscriptionBounds.getSouthWest().lat;  // not lng!
            var boxNorth = subscriptionBounds.getNorthEast().lng; // not lat!
            var boxEast = subscriptionBounds.getNorthEast().lat;  // not lng!
            mapCenterLat = boxSouth + (boxNorth - boxSouth)/2;
            mapCenterLng = boxWest + (boxEast - boxWest)/2;
            setMapZoom(boxSouth,boxWest,boxNorth,boxEast);
            break;
          };  // end of subscriptions for loop
          if ("" == subscriptionName) {
            reject("Error: subscriptionId ["+subscriptionId+"] not found. Please contact progammer.");
          } else {
            resolve("sucess");
          }
        },   // end of success function
        error: function(xhr, status, error) {
          reject("Error: "+xhr.responseText+" on attempt to get Alert Hub subscriptions file."+
               "\nIf error presists after retrying, please contact progammer.");
        }
      });  // end of ajax function GET json}
    }); // end of Promise function
  }; // end of getSubscriptionParms
  
  var initMap = function() {
    return new Promise(function(resolve, reject){
      map = new L.Map('map');
      map.setView(new L.LatLng(mapCenterLat,mapCenterLng), mapZoom); 
      // The tile layer definition has s, z, x, and y in it. These are replaced
      // with actual values whenever Leaflet needs to fetch a tile.
      L.tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors',
            maxZoom: 20,
            maxNativeZoom: 18 // tiles will be interpolated/autoscaled at zoom beyond maxNativeZoom
      }).addTo(map);
      alerts.on('click', handleClick);
      alerts.addTo(map);
      var legend = L.control({position: 'topright'});
      legend.onAdd = function (map) {
        var div = L.DomUtil.create('div', 'legend');
        div.innerHTML += '<p><b>'+subscriptionName+'</b><br>' +
                         '&nbsp;&nbsp;red - high priority alerts<br>' +
                         '&nbsp;&nbsp;yellow - all other alerts<br>' +
                         '<i>Displaying up to '+feedItemsLimit+' alerts</i></p>';
        return div;
      };
      legend.addTo(map);
      map.attributionControl.setPrefix(''); // Don't show the 'Powered by Leaflet' text.
      resolve("sucess");
    }); // end of Promise function
  }; // end of initMap
  	
  var compileAlerts = function() {
    return new Promise(function(resolve, reject){
      $.ajax({
        type: "GET",
        url: subscriptionUrl,
        dataType: "xml",
        success: function(rssXml) {
          // Note: RSS version 2 does not use a declared namespace for its
          //  elements. Therefore, its elements cannot have a namespace prefix.
          $(rssXml).find('item').each(function(){
            var title = $(this).find('title').text();
            var link = $(this).find('link').text();
            var pubDate = $(this).find('pubDate').text();
            var alertRef = {
              'title': title,
              'link': link,
              'pubDate': pubDate
            }
            fileRefs.alerts.push(alertRef);
            countAlerts = countAlerts + 1;
            if (countAlerts > feedItemsLimit) {
              return false;
            }
          }); // end of items each loop
          resolve("sucess");
        },  // end of success function
        error: function(xhr, status, error) {
          reject("Error: "+xhr.responseText+" on attempt to get subscription URL: "+subscriptionUrl);
        }
      });  // end of ajax function GET xml
    }); // end of Promise function
  }; // end of compileAlerts

  var showAllAlerts = function() {
    return new Promise(function(resolve, reject){
      // Note: alerts processed in reverse order, i.e., most recent last
      for (var i = countAlerts - 1; i > -1; i--) {
        var thisAlertTitle = fileRefs.alerts[i].title;
        var thisAlertLink = fileRefs.alerts[i].link;
        var thisAlertPubDate = fileRefs.alerts[i].pubDate;
        showThisAlert(thisAlertTitle, thisAlertLink, thisAlertPubDate);
      } // end of fileRefs for loop
      if (getAlertErrors > 0) {
      	// show the message, but missing alerts is not a fatal error
        alert("Not able to display "+getAlertErrors+" of the alerts.")
      }
      resolve("sucess");
    }); // end of Promise function
  }; // end of showAllAlerts

  getSubscriptionParms()
    .then(initMap)
    .then(compileAlerts)
    .then(showAllAlerts)
    .catch(function(error) { // error set by reject(error) in any Promise
       alert(error);
	});
}

function showThisAlert(title, link, pubDate) {
	// Called from showAllAlerts() to handle a CAP alert instance
	$.ajax({
      type: "GET",
      url: link,
      dataType: "xml",
      success: function(alertXml) {
      capNs = nsPrefix(alertXml, "urn:oasis:names:tc:emergency:cap");
      var identifier = $(alertXml).find(capNs+"identifier").text();
      var sent = $(alertXml).find(capNs+"sent").text();
      $(alertXml).find(capNs+"info").each(function(){  // alert/info may be multiple
        var thisInfo = $(this);
        var headlineOrEvent = thisInfo.find(capNs+"headline").text();
        if (headlineOrEvent == "") {
          headlineOrEvent = thisInfo.find(capNs+"event").text();
        }
        var areaDesc = thisInfo.find(capNs+"areaDesc").text()
        var urgency = thisInfo.find(capNs+"urgency").text();
        var severity = thisInfo.find(capNs+"severity").text();
        var certainty = thisInfo.find(capNs+"certainty").text();
        var priority = "lower";
        if ((urgency == "Immediate" || urgency == "Expected") &&
            (severity == "Extreme" || severity == "Severe") &&
            (certainty == "Observed" || certainty == "Likely")) {
              priority = "highest";
        }
        thisInfo.find(capNs+"area").each(function (){
          var thisArea = $(this);
          // process all circles and polygons in this area
          // process all polygons - alert/info/area/polygon may be multiple
          thisArea.find(capNs+"polygon").each(function(){
            var capPolygon = $(this).text().trim();
            var geojsonPolygon = constructPolygon(capPolygon, link,
                headlineOrEvent, areaDesc, sent, priority);
            if (geojsonPolygon !== null) {
              alerts.addData(geojsonPolygon);
            }
          });	// end of this alert/info/area/polygon function
          // process all circles - alert/info/area/circle may be multiple
          thisArea.find(capNs+"circle").each(function(){
            var capCircle = $(this).text().trim();
            var geojsonPolygonCircle = constructCircle(capCircle, link,
                headlineOrEvent, areaDesc, sent, priority);
            if (geojsonPolygonCircle !== null) {
              alerts.addData(geojsonPolygonCircle);
            }
          });	// end of this alert/info/area/circle function
        });    // end of this alert/info/area function
       });   // end of this alert/info function
      },  //  end of success function
      error: function (jqXHR, exception) {
        var msg = '';
	    if (jqXHR.status === 0) {
	      msg = 'Status shows as not connected. Verify network and permissions.';
	    } else if (jqXHR.status == 404) {
	      msg = 'Requested page not found. [404]';
	    } else if (jqXHR.status == 500) {
	      msg = 'Internal Server Error [500].';
	    } else if (exception === 'parsererror') {
	      msg = 'Requested JSON parse failed.';
	    } else if (exception === 'timeout') {
	      msg = 'Time out error.';
	    } else if (exception === 'abort') {
	      msg = 'Ajax request aborted.';
	    } else {
	      msg = 'Uncaught Error.\n' + jqXHR.responseText;
	    }
        alert('Missing alert: '+headline+'\n for: '+areaDesc+'\n'+msg);
	      return false;
      }  //  end of error function
	});  //  end of ajax function GET json
}

function constructPolygon(capPolygon, link, headlineOrEvent, areaDesc, sent, priority) {
	// Called from function showThisAlert(), to handle
	// processing of a CAP alert/info/area/polygon instance
	var polygonCoords = extractVertices(capPolygon);
	if (polygonCoords.length < 1) { return null; } // silent fail-- errors found in capPolygon
	var popupContent = " sent: "+sent+"<br/>"+
	                   "<a href='"+link+"'>"+headlineOrEvent+"</a>";
	var geojsonFeature = {
	    "type": "Feature",
	    "name": headlineOrEvent,
	    "properties": {
	    	"priority": priority,
	        "popupContent": popupContent
	    },
	    "geometry": {
	        "type": "Polygon",
	        "coordinates": [ polygonCoords ]
	    }
	};
	return geojsonFeature;
}

function extractVertices(capPolygon) {
	// Called from function constructPolygon() to convert a CAP
	// circle to a geoJSON polygon (needed for Point in Polygon).
	var polygonCoords = [];
	// example capPolygon: "46.201,6.070 46.203,6.154 46.245,6.155 46.252,6.127 46.226,6.083 46.201,6.070"
	var capVertices = capPolygon.split(" ");
	for(var j = 0; j < capVertices.length; j++) {
		var latLngVertex = capVertices[j].trim().split(",");
		if (isNaN(latLngVertex[0]) || isNaN(latLngVertex[1])) {
			//alert("Error: Not numeric polygonCoordinates, link:\n"+link);
			polygonCoords = [];
			return polygonCoords;
		}
		var lat = parseFloat(latLngVertex[0]);
		var lng = parseFloat(latLngVertex[1]);
		if ( lat < -90 || lat > 90 || lng < -180 || lng > 180) {
			//alert("Error: Out of range polygon coordinates, link:\n"+link);
			polygonCoords = [];
			return polygonCoords;
		}
		polygonCoords.push([lng, lat]);
	}
	return polygonCoords;
}

function constructCircle(capCircle, link, headlineOrEvent, areaDesc, sent, priority) {
	// Called from function constructPolygon(), to handle
	// processing of CAP alert/info/area/circle instance.
	var centerRadius = capCircle.split(" ");
	var centerLatLng = centerRadius[0].trim().split(",");
	if (isNaN(centerRadius[1]) ||
	    isNaN(centerLatLng[0]) ||
		isNaN(centerLatLng[1])) {
		//alert("Error: Not numeric circle center or radius, link:\n"+link);
		return null;
	}
	var lat = parseFloat(centerLatLng[0]);
	var lng = parseFloat(centerLatLng[1]);
	var radius  = parseFloat(centerRadius[1]);
	if (radius == 0) {  // circle with radius zero is a point,
		radius = 1;       // so: coerce to one kilometer circle
	}
	var polygonCoords = polygonFromCircle (lng, lat, radius);
	if (polygonCoords.length < 1) { return null; } // silent fail-- errors in circle center or radius
	var popupContent = " sent: "+sent+"<br/>"+
	                   "<a href='"+link+"'>"+headlineOrEvent+"</a>";
	var geojsonFeature = {
	    "type": "Feature",
	    "name": headlineOrEvent,
	    "properties": {
	    	"priority": priority,
	        "popupContent": popupContent
	    },
	    "geometry": {
	        "type": "Polygon",
	        "coordinates": [ polygonCoords ]
	    }
	};
	return geojsonFeature;
}

function polygonFromCircle(lng, lat, radius) {
	// Called from function constructCircle() to convert a circle to
	// a many-sided geoJSON polygon (needed for Point in Polygon).
	var points = 20;          // more points makes smoother circle
	var d2r = Math.PI / 180;  // degrees to radians
	var r2d = 180 / Math.PI;  // radians to degrees
	var earthRadius = 6371;   // 6,371 is radius of the earth in kilometers
	// find the radius in lat/lon
	var rlat = (radius / earthRadius) * r2d;
	var rlng = rlat / Math.cos(lat * d2r);
	var polygonCoords = new Array();
	for (var i=0; i < points+1; i++) { // one extra to close the circle
		var theta = Math.PI * (i / (points/2));
		ex = lng + (rlng * Math.cos(theta)); // center a + radius x * cos(theta)
		ey = lat + (rlat * Math.sin(theta)); // center b + radius y * sin(theta)
		polygonCoords.push([ex, ey]);
	}
	return polygonCoords;
}

function nsPrefix(alertXml, namespaceUri) {
	// Called from function showThisAlert() to support XML namepsaces. It
	// parses the given xmlString to isolate text between the literal 'xmlns'
	// and the given namespaceUri. The nsPrefix will be extracted from that
	// text, after removing any white space, colon, quote, and equals sign.
	// If the remaining text is empty, there is no prefix (return empty string).
	var xmlString = (new XMLSerializer()).serializeToString(alertXml);
	var namespaceUriPosition = xmlString.search(namespaceUri);
	if (namespaceUriPosition < 0) { return ""; } // namepsaceUri not fouond
	var textBeforeNsUri = xmlString.substring(0, namespaceUriPosition);
	var xmlnsPosition = textBeforeNsUri.search('xmlns');
	if (xmlnsPosition < 0) { return ""; } // xmlns not found before namepsaceUri
	var textContainsPrefix = textBeforeNsUri.substring(xmlnsPosition+5);
	// next: remove any white space, colon, quote, or equals sign
	var nsPrefix = textContainsPrefix.replace(/\s|\:|\'|"|\=/gm,"");
	if (nsPrefix.length > 0) { // what remains is the namepsace prefix (e.g., 'cap')
		return nsPrefix+"\\:"; // in jquery, the colon must be escaped
	}
	return "";
}

function onEachFeature(feature, layer) {
	// Called when an alerts feature is to be added, changed, or deleted;
    // to setStyle if the feature has a property named priority
    if (feature.properties && feature.properties.priority) {
		if (layer.feature.properties.priority === "highest") {
			// This feature is highest priority; color the area red.
			layer.setStyle({ color: '#FF0000', opacity: 1,
				fillColor: '#FF9999', fillOpacity: 0.3});
		} else {
			// This feature not highest priority; color area yellow.
			layer.setStyle({ color: '#FFFF00', opacity: 1,
				fillColor: '#FFFF99', fillOpacity: 0.3});
		}
    }
}

function handleClick(e) {
	// Called when a click event occurs on the map, to generate popup.
	// Looks through each layer in order and acts if the clicked point,
    // e.latlng, is a match for the layer (which here is one feature).
    var html = "";
    var match = leafletPip.pointInLayer(e.latlng, alerts, false);
    if (match.length) {
		if (match.length == 1) {
			html += match[0].feature.properties.popupContent;
		} else {
	    	html += match.length + " alerts at this location.<br/>";
	     	for (var i = match.length-1; i > -1; i--) {
	     		var feedNum = match.length - i;
	            html += "["+ feedNum +"] " +
					match[i].feature.properties.popupContent+"<br/>";
			}
		}
	}
    if (html) {
        map.openPopup(html, e.latlng);
    }
    // no popup if there was no match
}

function setMapZoom(boxSouth,boxWest,boxNorth,boxEast) {
  var spanLat = boxNorth - boxSouth;
  var spanLng = boxEast - boxWest;
  var spanDegrees;
  if(spanLat > spanLng) {
    spanDegrees = spanLat;
  } else {
    spanDegrees = spanLng;
  }
  switch (true) {
    case (spanDegrees < .05): 
	  mapZoom = 13;
	  break;
    case (spanDegrees < .1): 
	  mapZoom = 12;
	  break;
    case (spanDegrees < .2): 
	  mapZoom = 11;
	  break;
    case (spanDegrees < .5): 
	  mapZoom = 10;
	  break;
    case (spanDegrees < 1): 
	  mapZoom = 9;
	  break;
    case (spanDegrees < 3): 
	  mapZoom = 8;
	  break;
    case (spanDegrees < 5): 
	  mapZoom = 7;
	  break;
    case (spanDegrees < 8): 
	  mapZoom = 6;
	  break;
    case (spanDegrees < 15): 
      mapZoom = 5;
	  break;
    case (spanDegrees < 30): 
      mapZoom = 4;
	  break;
    case (spanDegrees < 60): 
      mapZoom = 3;
	  break;
    default:
      mapZoom = 2;
  }
}
	</script>
</head>
<body onLoad="javascript:controller();">
	<div id="map"></div>
</body>
</html>
