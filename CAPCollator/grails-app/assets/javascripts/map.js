/* For map functions */
var map;
var ajaxRequest;
var plotlist;
var plotlayers=[];

function initOSM(map_element_id, alert_body) {
  // set up the map
  map = new L.Map(map_element_id);

  // create the tile layer with correct attribution
  var osmUrl='https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
  var osmAttrib='Map data © <a href="https://openstreetmap.org">OpenStreetMap</a> contributors';
  var osm = new L.TileLayer(osmUrl, {minZoom: 8, maxZoom: 12, attribution: osmAttrib});    

  // start the map in South-East England
  map.setView(new L.LatLng(51.3, 0.7),9);
  map.addLayer(osm);
}

function initMap(map_element_id, alert_body) {
  return initOSM(map_element_id, alert_body);
}

function initGoogleMap(map_element_id, alert_body) {

  var bounds = new google.maps.LatLngBounds();

  var map = new google.maps.Map(document.getElementById(map_element_id), {
    // center: {lat: -34.397, lng: 150.644},
    // zoom: 8
  });

  // An alert_info consists of one or more areas, each with a type and a geometry
  var list_of_info_elements = alert_body.info instanceof Array ? alert_body.info : [ alert_body.info ];

  list_of_info_elements.forEach( function(info_element) {

    var list_of_areas = info_element.area instanceof Array ? info_element.area : [ info_element.area ];
    list_of_areas.forEach( function(area_element) {

      console.log("process area %o",area_element);
      if ( area_element.cc_polys instanceof Array ) {
        // New style records, with a list of cc_polys
        area_element.cc_polys.forEach( function(poly) {
          var poly = toPoly(poly.type, poly.coordinates, bounds, poly.radius);
          if ( poly != null ) {
            console.log("Got poly, add to map");
            poly.setMap(map);
          }
        });
      }
      else {
        var poly = toPoly(area_element.cc_poly.type, area_element.cc_poly.coordinates, bounds, area_element.cc_poly.radius);

        if ( poly != null ) {
          console.log("Got poly, add to map");
          poly.setMap(map);
        }
      }
    })
  });

  map.fitBounds(bounds);

  return map;
}

function toPoly(geom_type, geom, bounds, rad) {

  console.log("toPoly(%o,%o,%o)",geom_type, geom, bounds);

  var result=null;

  if ( geom_type==='polygon') {
    var coords = [];

    geom[0].forEach( function(elem) {
      var lat=parseFloat(elem[1]);
      var lng=parseFloat(elem[0]);
      bounds.extend(new google.maps.LatLng(lat,lng));
      coords.push({lat:lat, lng:lng});
    });

    console.log("Create polygon with coords %o",coords);

    // Construct the polygon.
    result = new google.maps.Polygon({
      paths: coords,
      strokeColor: '#FF0000',
      strokeOpacity: 0.8,
      strokeWeight: 2,
      fillColor: '#FF0000',
      fillOpacity: 0.35
    });
  }
  else if ( geom_type==='circle' ) {
    var lat=parseFloat(geom[1]);
    var lng=parseFloat(geom[0]);
    var center = {lat: lat, lng: lng};
    var rad_km = parseInt(rad);
    console.log("Draw circle at %o %o %o",geom,rad,rad_km);

    bounds.extend(new google.maps.LatLng(lat,lng));
    var radius = 20;
    result = new google.maps.Circle({
               strokeColor: '#FF0000',
               strokeOpacity: 0.8,
               strokeWeight: 2,
               fillColor: '#FF0000',
               fillOpacity: 0.35,
               center: center,
               radius: (rad_km * 1000)
             });
    // result.getBounds will give the bounds for the circle.
    bounds.union(result.getBounds());
  }


  return result;
}
