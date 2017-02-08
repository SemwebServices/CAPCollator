<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title></title>

    <asset:link rel="icon" href="favicon.ico" type="image/x-ico" />
</head>
<body>
  Sync Subs List

  <g:form action="syncSubList" method="POST">
    <div class="form-group">
      <label for="pollInterval"class="col-sm-2 control-label">URL Of Subscription List</label>
      <div class="col-sm-10">
        <input type="text" name="subUrl" class="form-control" id="subUrl" value="https://s3-eu-west-1.amazonaws.com/alert-hub-subscriptions/json"/>
      </div>
    </div>

    <div class="form-group">
      <div class="col-sm-2">
      </div>
      <div class="col-sm-10">
        <button id="Sync">Sync</button>
      </div>
    </div>

  </g:form>
</body>
</html>
