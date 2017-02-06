<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title></title>

    <asset:link rel="icon" href="favicon.ico" type="image/x-ico" />
</head>
<body>
  Subscriptions
  <table class="table table-striped">
    <thead>
      <tr>
        <th>ID</th>
        <th>SLUG</th>
        <th>Name</th>
        <th>URL</th>
        <th>Type</th>
        <th>Geom</th>
      </tr>
    </thead>
    <tbody>
      <g:each in="${subscriptions}" var="${sub}">
        <tr>
          <td>${sub.id}</td>
          <td>${sub.subscriptionId}</td>
          <td>${sub.subscriptionName}</td>
          <td>${sub.subscriptionUrl}</td>
          <td>${sub.filterType}</td>
          <td>${sub.filterGeometry}</td>
        </tr>
      </g:each>
    </tbody>
  </table>
</body>
</html>
