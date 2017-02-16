<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title></title>

    <asset:link rel="icon" href="favicon.ico" type="image/x-ico" />
</head>
<body>

  <div id="feed-watcher" data-feedid="${params.id}">
    Topic ${params}
  </div>

  <asset:script>
    function capEvent(evt) {
      console.log("capEvent %o",evt);
    }
    console.log("Created cap event handler");
  </asset:script>
  <asset:javascript src="application.js"/>
  <asset:javascript src="events.js"/>
</body>
</html>
