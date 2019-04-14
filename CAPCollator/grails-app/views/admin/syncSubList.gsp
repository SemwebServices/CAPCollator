<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>CAP Collator - Subscription File Import</title>
 
    
    <g:if test="${status?.running}">
      <meta http-equiv="refresh" content="10">
    </g:if>

    <asset:link rel="icon" href="favicon.ico" type="image/x-ico" />
</head>
<body>
  <h1>CAP Subscriptions Import <g:if test="${status.running}">(RUNNING NOW)</g:if></h1>

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

  <h3>Recent Import Jobs</h3>
  <table class="table table-striped table-bordered">
    <thead>
      <tr>
        <th>Job Start Time</th>
        <th>Job Status</th>
        <th>Job End Time</th>
        <th>Job Url</th>
        <th>Filesize</th>
        <th>Processed</th>
        <th>Created</th>
        <th>Updated</th>
        <th>Errors</th>
        <th>Avg Processing Time</th>
      </tr>
    </thead>
    <tbody>
      <g:each in="${status.progress}" var="sp">
        <tr>
          <td><g:formatDate format="yyyy-MM-dd HH:mm:ss" date="${sp.startTime}"/> </td>
          <td>${sp.status}</td>
          <td><g:formatDate format="yyyy-MM-dd HH:mm:ss" date="${sp.endTime}"/></td>
          <td>${sp.url}</td>
          <td>${sp.numEntriesInFile}</td>
          <td>${sp.numProcessed}</td>
          <td>${sp.numCreated}</td>
          <td>${sp.numUpdated}</td>
          <td>${sp.numErrors}</td>
          <td>${sp.average_processing_time}</td>
        </tr>
      </g:each>
    </tbody>
  </table>

</body>
</html>
