/* For map functions */

function initMap(map_element_id, alert_body) {

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

      var poly = toPoly(area_element.cc_poly.type, area_element.cc_poly.coordinates, bounds);

      if ( poly != null ) {
        console.log("Got poly, add to map");
        poly.setMap(map);
      }
    })
  });


  map.fitBounds(bounds);

  return map;
}

function toPoly(geom_type, geom, bounds) {

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

  return result;
}
