<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>CAP Collator - Setup</title>

    <asset:link rel="icon" href="favicon.ico" type="image/x-ico" />
</head>
<body>
  <div class="container-fluid">
    <div class="row">
      <div class="container-fluid">
        <h1>CAP Collator Initial System Setup</a></h1>

        <div class="panel panel-default">
          <div class="panel-heading">
            <h3 class="panel-title">System User</h3>
          </div>
          <div class="panel-body form-horizontal container">
            <div class="row">

              <g:form controller="setup" action="index" method="POST">
                <div class="col-md-6">
                  <div class="form-group">
                    <label class="col-sm-3 control-label">Username</label>
                    <div class="col-sm-7">
                      <input type="text" name="adminUsername" />
                    </div>
                  </div>

                  <div class="form-group">
                    <label class="col-sm-3 control-label">Password</label>
                    <div class="col-sm-7">
                      <input type="text" name="adminPassword" />
                    </div>
                  </div>

                  <div class="form-group">
                    <label class="col-sm-3 control-label">Admin Email</label>
                    <div class="col-sm-7">
                      <input type="text" name="adminEmail" />
                    </div>
                  </div>

                  <div class="form-group">
                    <label class="col-sm-3 control-label"></label>
                    <div class="col-sm-7">
                      <button type="submit">Submit</button>
                    </div>
                  </div>
                </div>
              </g:form>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</body>
</html>
