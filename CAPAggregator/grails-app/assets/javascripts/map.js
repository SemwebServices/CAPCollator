/* For map functions */
var map;
var ajaxRequest;
var plotlist;
var plotlayers=[];

// A more general function for showing a map and fitting an arbitrary list of features
function initOSMMapWithGeoJsonFeatures(map_element_id, feature_list) {
  // set up the map
  map = new L.Map(map_element_id);
  map.setView(new L.LatLng(0, 0),9);

  console.log("initOSMMapWithFeatures %o", feature_list);

  // create the tile layer with correct attribution
  var osmUrl='https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
  var osmAttrib='Map data © <a href="https://openstreetmap.org">OpenStreetMap</a> contributors';
  var osm = new L.TileLayer(osmUrl, {minZoom: 2, maxZoom: 12, attribution: osmAttrib});
  var features = []

  // start the map in South-East England
  // map.setView(new L.LatLng(51.3, 0.7),9);
  map.addLayer(osm);

  if ( feature_list != null ) {
    feature_list.forEach( function(feature) {
      console.log("Feature: %o",feature);
      var coords = [];

       feature.forEach( function(elem) {
         var lat=parseFloat(elem[1]);
         var lng=parseFloat(elem[0]);
         coords.push({lat:lat, lng:lng});
       });

       console.log("Create polygon with coords %o",coords);

       // Construct the polygon.
       var poly = new L.Polygon( coords );

       if ( poly != null ) {
         poly.setStyle({fillColor: '#FF0000', color: '#FF0000', fillOpacity:0.35, opacity:0.8, weight:2});
         console.log("add poly %o",poly);
         map.addLayer(poly)
         features.push(poly)
       }
    });
  }

  if ( features.length > 0 ) {
    console.log("fitting: %o",features);
    var group = new L.featureGroup(features);
    map.fitBounds(group.getBounds());
  }

}

function initOSM(map_element_id, alert_body) {
  // set up the map
  map = new L.Map(map_element_id);
  map.setView(new L.LatLng(0, 0),9);

  // create the tile layer with correct attribution
  var osmUrl='https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
  var osmAttrib='Map data © <a href="https://openstreetmap.org">OpenStreetMap</a> contributors';
  var osm = new L.TileLayer(osmUrl, {minZoom: 2, maxZoom: 12, attribution: osmAttrib});    
  var features = []

  // start the map in South-East England
  // map.setView(new L.LatLng(51.3, 0.7),9);
  map.addLayer(osm);

  // An alert_info consists of one or more areas, each with a type and a geometry
  var list_of_info_elements = alert_body.info instanceof Array ? alert_body.info : [ alert_body.info ];

  list_of_info_elements.forEach( function(info_element) {

    var list_of_areas = info_element.area instanceof Array ? info_element.area : [ info_element.area ];
    list_of_areas.forEach( function(area_element) {
        console.log("process area %o",area_element);
        if ( area_element.cc_polys instanceof Array ) {
          // New style records, with a list of cc_polys
          area_element.cc_polys.forEach( function(poly) {
            var poly = toPoly(poly.type, poly.coordinates, poly.radius);
            if ( poly != null ) {
              console.log("Got poly, add to map");
              map.addLayer(poly)
              features.push(poly)
            }
          });
        }
        else {
          var poly = toPoly(poly.type, poly.coordinates, poly.radius);
  
          if ( poly != null ) {
            console.log("Got poly, add to map");
            map.addLayer(poly)
            features.push(poly)
          }
        }
    })
  });

  if ( features.length > 0 ) {
    var group = new L.featureGroup(features);
    map.fitBounds(group.getBounds());
  }

}

function initMap(map_element_id, alert_body) {
  return initOSM(map_element_id, alert_body);

}

function mapWithGeoJsonFeatures(map_element_id, feature_list) {
  return initOSMMapWithGeoJsonFeatures(map_element_id, feature_list);
}

function toPoly(geom_type, geom, rad) {

  // console.log("toPoly(%o,%o)",geom_type, geom);

  var result=null;

  if ( geom_type==='polygon') {
    var coords = [];

    geom[0].forEach( function(elem) {
      var lat=parseFloat(elem[1]);
      var lng=parseFloat(elem[0]);
      // bounds.extend(new google.maps.LatLng(lat,lng));
      coords.push({lat:lat, lng:lng});
    });

    // console.log("Create polygon with coords %o",coords);

    // Construct the polygon.
    result = new L.Polygon( coords );
  }
  else if ( geom_type==='circle' ) {
    var lat=parseFloat(geom[1]);
    var lng=parseFloat(geom[0]);
    var center = {lat: lat, lng: lng};
    var rad_km = parseInt(rad);
    if ( ( rad_km == null ) || ( rad_km == 0 ) )
      rad_km  1;

    // console.log("Draw circle at %o %o %o",geom,rad,rad_km);

    result = new L.Circle(center, rad_km)
  }
  else if ( geom_type==='point' ) {
    var lat=parseFloat(geom[1]);
    var lng=parseFloat(geom[0]);
    var center = {lat: lat, lng: lng};
    var rad_km = 1;
    result = new L.Circle(center, rad_km)
  }
  else {
    console.log("Unhandled geom_type: %s",geom_type);
  }

  if ( result != null ) {
    result.setStyle({fillColor: '#FF0000', color: '#FF0000', fillOpacity:0.35, opacity:0.8, weight:2});
  }


  return result;
}
