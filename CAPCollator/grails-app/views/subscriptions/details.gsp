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
              <div class="col-sm-10"><p class="form-control-static" id="sub-filter-geom" data-sub-geom="${subscription.filterGeometry}" >${subscription.filterGeometry}</p></div>
            </div>
            <div class="form-group">
              <label class="col-sm-2 control-label">Current Alert Count</label>
              <div class="col-sm-10"><p class="form-control-static">${latestAlerts.hits.totalHits} (${latestAlerts.hits.hits.size()} shown)</p></div>
            </div>
          </div>
        </div>

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
                                            data-alert-body="${alert.getSource().AlertBody as grails.converters.JSON}"></div>
                </td>
                <td>
                  <g:set var="alsrc" value="${alert.getSource()}"/>
                  <h3>${alsrc.AlertBody.info.headline}</h3>

                  <div class="form-horizontal">
                    <div class="form-group"> <label class="col-sm-2 control-label">Alert Identifier</label> <div class="col-sm-10"><p class="form-control-static">${alsrc.AlertBody.identifier}</p></div> </div>
                    <div class="form-group"> <label class="col-sm-2 control-label">Alert Sender</label> <div class="col-sm-10"><p class="form-control-static">${alsrc.AlertBody.sender}</p></div> </div>
                    <div class="form-group"> <label class="col-sm-2 control-label">Alert Sent</label> <div class="col-sm-10"><p class="form-control-static">${alsrc.AlertBody.sent}</p></div> </div>
                    <div class="form-group"> <label class="col-sm-2 control-label">Source</label> <div class="col-sm-10"><p class="form-control-static">${alsrc.AlertMetadata.SourceUrl}</p></div> </div>
                    <div class="form-group"> <label class="col-sm-2 control-label">Matched Subscriptions</label> <div class="col-sm-10">
                     <ul><g:each in="${alsrc.AlertMetadata.MatchedSubscriptions}" var="ms"><li>${ms}</li></g:each></ul>
                      </div> 
                    </div>
                  </div>

                </td>
              </tr>
            </g:each>
          </tbody>
        </table>
      </div>
    </div>
  </div>

<asset:script type="text/javascript">
  if (typeof jQuery !== 'undefined') {
    (function($) {

      let sub_geom = $('#sub-filter-geom').data("sub-geom");
      let sub_poly = [];

      console.log("Sub poly is %o",sub_geom);

      $('.MapWithAlert').each(function(i,obj) {
        initMap(obj.id, $(obj).data("alert-body"));
      });
    })(jQuery);
  }
</asset:script>

</body>
</html>
