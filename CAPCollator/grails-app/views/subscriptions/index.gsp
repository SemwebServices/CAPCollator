<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title></title>

    <asset:link rel="icon" href="favicon.ico" type="image/x-ico" />
</head>
<body>
  Subscriptions

  <div class="container-fluid">
    <div class="row">
      <div class="container-fluid" style="vertical-align: middle; text-align:center;">

        <h1>Registered Feeds</h1>

        <g:form controller="subscriptions" action="index" method="get" class="container">
          <div class="input-group">
              <input type="text" name="q" class="form-control " placeholder="Text input" value="${params.q}">
              <span class="input-group-btn">
                  <button type="submit" class="btn btn-search">Search</button>
              </span>
          </div>
        </g:form>

        <div class="pagination">
          <g:paginate controller="subscriptions" action="index" total="${totalSubscriptions}" next="Next" prev="Previous" omitNext="false" omitPrev="false" />
        </div>

      </div>
    </div>
    <div class="row">
      <div class="container-fluid">

        <table class="table table-striped">
          <thead>
            <tr>
              <th>ID</th>
              <th>SLUG</th>
              <th>Name</th>
              <th>URL</th>
              <th>Type</th>
              <th>Geom</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <g:each in="${subscriptions}" var="${sub}">
              <tr>
                <td><g:link controller="subscriptions" action="details" id="${sub.id}">${sub.id}</g:link></td>
                <td>${sub.subscriptionId}</td>
                <td>${sub.subscriptionName}</td>
                <td>${sub.subscriptionUrl}</td>
                <td>${sub.filterType}</td>
                <td>${sub.filterGeometry}</td>
                <td>
                  <sec:ifAnyGranted roles="ROLE_ADMIN">
                    <g:link controller="subscriptions" action="touch" class="btn btn-primary" role="button" id="${sub.id}">Touch</g:link>
                  </sec:ifAnyGranted>
                </td>
              </tr>
            </g:each>
          </tbody>
        </table>
      </div>
    </div>
</body>
</html>
