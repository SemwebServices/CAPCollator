<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title></title>

    <asset:link rel="icon" href="favicon.ico" type="image/x-ico" />
</head>
<body>

  String subscriptionUrl
  String filterType
  String filterGeometry

  <div class="container-fluid">
    <div class="row">
      <div class="container-fluid">
        <h1>${subscription.subscriptionId} / ${subscription.subscriptionName}</a></h1>

        <div class="panel panel-default">
          <div class="panel-heading">
            <h3 class="panel-title">Subscription Info</h3>
          </div>
          <div class="panel-body form-horizontal">
            <div class="form-group">
              <label class="col-sm-2 control-label">URL</label>
              <div class="col-sm-10"><p class="form-control-static">${subscription.subscriptionUrl}</p></div>
            </div>
            <div class="form-group">
              <label class="col-sm-2 control-label">Filter Type</label>
              <div class="col-sm-10"><p class="form-control-static">${subscription.filterType}</p></div>
            </div>
            <div class="form-group">
              <label class="col-sm-2 control-label">Filter Geometry</label>
              <div class="col-sm-10"><p class="form-control-static">${subscription.filterGeometry}</p></div>
            </div>
          </div>
        </div>

  <pre>
${latestAlerts}
  </pre>

      </div>
    </div>
    <div class="row">
      <div class="container-fluid">
        <table class="table table-bordered table-striped">
          <thead>
            <tr>
            </tr>
          </thead>
          <tbody>
            <g:each in="${latestAlerts.hits.hits}" var="alert" status="s">
              <tr>
                <td>
                  <div class="MapWithAlert" id="map_for_${s}"
                                            data-alert-id="${alert.getId()}" 
                                            data-alert-type="${alert.getSource().AlertBody.info.area.cc_poly.type}" 
                                            data-alert-geometry="${alert.getSource().AlertBody.info.area.cc_poly.coordinates}"></div>
                </td>
                <td>
                  ${alert.getSource().AlertBody.info.headline}
                </td>
                <td>
                  <pre>${alert}</pre>
                </td>
              </tr>
            </g:each>
          </tbody>
        </table>
      </div>
    </div>
  </div>

<asset:javascript src="map.js"/>

<asset:script type="text/javascript">
  if (typeof jQuery !== 'undefined') {
    (function($) {
      $('.MapWithAlert').each(function(i,obj) {
        initMap(obj.id, 
                $(obj).data("alert-type"),
                $(obj).data("alert-geometry"));
      });
    })(jQuery);
  }
</asset:script>

</body>
</html>
