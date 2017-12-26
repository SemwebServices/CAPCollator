<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title></title>

    <asset:link rel="icon" href="favicon.ico" type="image/x-ico" />
</head>
<body>

  <div class="container-fluid">
    <div class="row">
      <div class="container-fluid">
        Alert detail...
        <div id="mapdiv" style="width:100%; height:100%;"></div>
      </div>
    </div>
  </div>

  <div id="cfg" data-api="${createLink(controller:'api', action:'nearby')}"/>

<asset:script type="text/javascript">
  if (typeof jQuery !== 'undefined') {
    (function($) {
      console.log("start1");
      // Get current locaton
      if(!!navigator.geolocation) {

        console.log("start2");
    
        var map;
    
        var mapOptions = {
            zoom: 15,
            mapTypeId: google.maps.MapTypeId.ROADMAP
        };
        
        map = new google.maps.Map(document.getElementById('mapdiv'), mapOptions);
    
        console.log("About to call getCurrentPosition");
        navigator.geolocation.getCurrentPosition(function(position) {

            var geolocate = new google.maps.LatLng(position.coords.latitude, position.coords.longitude);
            var api_base_url = $('#cfg').data('api');

            console.log("Geolocated %o, api:%s",position,api_base_url);

            $.ajax({
              contentType: "application/json",
              url: api_base_url,
              dataType: "json",
              data: {"lat":position.coords.latitude , "lon":position.coords.longitude},
              success: function( nearby_alerts,status,jqxhr ) {
                console.log( "got response %o",nearby_alerts );
              },
              error: function(data){
                console.log("Problem fecthing data %o",data);
              },
              type: "GET"
            });
            
            // var infowindow = new google.maps.InfoWindow({
            //     map: map,
            //     position: geolocate,
            //     content:
            //         '<h1>Location pinned from HTML5 Geolocation!</h1>' +
            //         '<h2>Latitude: ' + position.coords.latitude + '</h2>' +
            //         '<h2>Longitude: ' + position.coords.longitude + '</h2>'
            // });
            
            // map.setCenter(geolocate);
            
        },
        function(err) {
          console.log("problem %o",err);
        },
        {timeout:10000,enableHighAccuracy:true});

        console.log("all done");
        
      } else {
        document.getElementById('mapdiv').innerHTML = 'No Geolocation Support.';
      }

    })(jQuery);
  }
</asset:script>

</body>
</html>
