<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>CAP Aggregator - Setup</title>

    <asset:link rel="icon" href="favicon.ico" type="image/x-ico" />
</head>
<body>
  <div class="container-fluid">
    <div class="row">
      <div class="container-fluid">
        <h1>CAP Aggregator Initial System Setup</a></h1>

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
                    <div class="col-sm-5">
                      <input type="text" name="adminUsername" />
                    </div>
                    <small id="adminUsernameHelp" class="form-text text-muted  col-sm-4">The new administrative username.</small>
                  </div>

                  <div class="form-group">
                    <label class="col-sm-3 control-label">Password</label>
                    <div class="col-sm-5">
                      <input type="password" name="adminPassword" />
                    </div>
                    <small id="adminPasswordHelp" class="form-text text-muted  col-sm-4">The new administrative password.</small>
                  </div>

                  <div class="form-group">
                    <label class="col-sm-3 control-label">Admin Email</label>
                    <div class="col-sm-5">
                      <input type="text" name="adminEmail" />
                    </div>
                  </div>

                  <div class="form-group">
                    <label class="col-sm-3 control-label">Feed title prefix</label>
                    <div class="col-sm-5">
                      <input type="text" name="feedTitlePrefix" />
                    </div>
                  </div>

                  <div class="form-group">
                    <label class="col-sm-3 control-label">Feed title postfix</label>
                    <div class="col-sm-5">
                      <input type="text" name="feedTitlePostfix" />
                    </div>
                  </div>

                  <div class="form-group">
                    <label class="col-sm-3 control-label">Feed entry prefix</label>
                    <div class="col-sm-5">
                      <input type="text" name="feedEntryPrefix" />
                    </div>
                  </div>

                  <div class="form-group">
                    <label class="col-sm-3 control-label">Feed entry postfix</label>
                    <div class="col-sm-5">
                      <input type="text" name="feedEntryPostfix" />
                    </div>
                  </div>

                  <div class="form-group">
                    <label class="col-sm-3 control-label">AWS Bucket Name</label>
                    <div class="col-sm-5">
                      <input type="text" name="awsBucketName" value="${grailsApplication.config.defaultAwsBucketName}" />
                    </div>
                  </div>

                  <div class="form-group">
                    <label class="col-sm-3 control-label">Default Base URL (Feeds)</label>
                    <div class="col-sm-5">
                      <input id="staticFeedsBaseUrl" type="text" name="staticFeedsBaseUrl" />
                    </div>
                    <small id="staticFeedsDirHelp" class="form-text text-muted col-sm-4">Default feed URL (No trailing slash).</small>
                  </div>

                  <div class="form-group">
                    <label class="col-sm-3 control-label">Static Feeds Directory</label>
                    <div class="col-sm-5">
                      <input id="staticFeedsDir" type="text" name="staticFeedsDir" value="${grailsApplication.config.staticFeedsDir?:'/var/www/html/cap'}" />
                    </div>
                    <small id="staticFeedsDirHelp" class="form-text text-muted col-sm-4">The filesystem directory where static feeds will be written. 
                    In production, probably /var/www/html/cap
                    </small>
                  </div>

                  <div class="form-group">
                    <label class="col-sm-3 control-label">Static Feed List Size</label>
                    <div class="col-sm-5">
                      <input type="text" name="staticFeedListSize" value="${grailsApplication.config.staticFeedListSize}" />
                    </div>
                  </div>

                  <div class="form-group">
                    <label class="col-sm-3 control-label">Static Feed Xslt</label>
                    <div class="col-sm-5">
                      <input type="text" name="alertXslt" value="https://cap-alerts.s3.amazonaws.com/rss-style.xsl" />
                    </div>
                  </div>

                  <div class="form-group">
                    <label class="col-sm-3 control-label"></label>
                    <div class="col-sm-5">
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
  <script type="text/javascript">
    document.getElementById('staticFeedsBaseUrl').value = window.location.href;
  </script>
</body>
</html>
