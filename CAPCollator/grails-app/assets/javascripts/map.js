/* For map functions */

function initMap(map_element_id, geom_type, geom) {

  // console.log("initMap %o %o %o",map_element_id,geom_type,geom);

  var bounds = new google.maps.LatLngBounds();

  var map = new google.maps.Map(document.getElementById(map_element_id), {
    // center: {lat: -34.397, lng: 150.644},
    // zoom: 8
  });

  var poly = toPoly(geom_type, geom, bounds);
  poly.setMap(map);

  map.fitBounds(bounds);

  return map;
}

function toPoly(geom_type, geom, bounds) {

  var result=null;

  if ( geom_type==='polygon') {
    var coords = [];

    geom[0].forEach( function(elem) {
      var lat=elem[1];
      var lng=elem[0];
      bounds.extend(new google.maps.LatLng(lat,lng));
      coords.push({lat:lat, lng:lng});
    });

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
