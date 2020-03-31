<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>CAP Aggregator: Search through all currently known subscriptions</title>

    <asset:link rel="icon" href="favicon.ico" type="image/x-ico" />
</head>
<body>

  <div class="container-fluid">
    <div class="row">
      <div class="container" style="vertical-align: middle; text-align:center;">

        <h1>Subscriptions (${totalSubscriptions} found)</h1>

        <p>This page provides a searchable index of all the currently known CAP subscriptions. Subscriptions are virtual feeds that collect alerts from
           all known providers which intersect with a given geographical area. You can search using full text in the subscription definitions. Use the unfiltered feed to
           drink from the firehose.</p>

        <g:form controller="subscriptions" action="index" method="get" class="container">
          <div class="input-group">
              <input type="text" name="q" class="form-control " placeholder="Enter any keywords for your subscription, eg Sheffield" value="${params.q}">
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
              <th>Filter</th>
              <th>Geom</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <g:each in="${subscriptions}" var="${sub}">
              <tr>
                <td>${sub.id}</td>
                <td><g:link controller="subscriptions" action="details" id="${sub.subscriptionId}">${sub.subscriptionId}</g:link></td>
                <td>${sub.subscriptionName}</td>
                <td>${sub.subscriptionUrl}</td>
                <td>${sub.filterType}</td>
                <td>
                  <ul>
                    <li>FilterId: ${sub.xPathFilterId}</li>
                    <li>XPath: ${sub.xPathFilter}</li>
                    <li>Language: ${languageOnly ?: 'all'}</li>
                    <li>High Priority: ${highPriorityOnly ?: 'all'}</li>
                    <li>Official: ${officialOnly ?: 'all'}</li>
                  </ul>
                </td>
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
