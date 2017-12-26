<!doctype html>
<html lang="en" class="no-js">
<head>

<g:if test="${!grailsApplication.config.gtmcode.equals('none')}">
  <!-- Google Tag Manager -->
    <script>(function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({'gtm.start':new Date().getTime(),event:'gtm.js'});var f=d.getElementsByTagName(s)[0],j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';j.async=true;j.src='https://www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);})(window,document,'script','dataLayer','${grailsApplication.config.gtmcode}');</script>
  <!-- End Google Tag Manager -->
</g:if>

    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>

    <meta name="description" content="CAP Collator is a tool for aggregating and indexing Common Alerting Protocol alerts into a single searchable resource that can be used to build useful emergency alerting applications. You can think of CAPCollator as middleware that sits between CAP event publishers and CAP event consumers."/>

    <title>
        <g:layoutTitle default="CAP Collator - Emergency Alert Aggregation Middleware"/>
    </title>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>

    <asset:stylesheet src="application.css"/>

    <g:layoutHead/>
</head>
<body>

  <g:if test="${!grailsApplication.config.gtmcode.equals('none')}">
    <!-- Google Tag Manager (noscript) -->
      <noscript><iframe src="https://www.googletagmanager.com/ns.html?id=${grailsApplication.config.gtmcode}" height="0" width="0" style="display:none;visibility:hidden"></iframe></noscript>
    <!-- End Google Tag Manager (noscript) -->
  </g:if>

  <div class="navbar navbar-default navbar-fixed-top">
    <div class="container-fluid">
      <div class="navbar-header">
        <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
          <span class="icon-bar"></span>
          <span class="icon-bar"></span>
          <span class="icon-bar"></span>
        </button>
        <g:link controller="home" action="index" class="navbar-brand">CAPCollator <g:meta name="info.app.version"/></g:link>
      </div>

      <div class="collapse navbar-collapse pull-right">
        <ul class="nav navbar-nav">
          <sec:ifLoggedIn>
            <li class="dropdown">
              <a href="#" class="dropdown-toggle" data-toggle="dropdown"><sec:username/><b class="caret"></b></a>
              <ul class="dropdown-menu">
                <li><g:link controller="home" action="profile">Profile</g:link></li>
                <li class="divider"></li>
                <li><g:link controller="home" action="logout">Logout</g:link></li>
              </ul>
            </li>
          </sec:ifLoggedIn>
          <sec:ifNotLoggedIn>
            <li class="${controllerName=='home' && actionName=='login' ? 'active' : ''}"><g:link controller="home" action="login">Login</g:link></li>
          </sec:ifNotLoggedIn>
        </ul>
      </div>


      <div class="collapse navbar-collapse">
        <ul class="nav navbar-nav">
          <li class="${controllerName=='home' && actionName=='index' ? 'active' : ''}"><g:link controller="home" action="index">Home</g:link></li>
          <g:if test="${grailsApplication.config.featureNear?.equals('on')}">
            <li class="${controllerName=='alert' && actionName=='nearby' ? 'active' : ''}"><g:link controller="alert" action="nearby">Nearby</g:link></li>
          </g:if>
          <li class="${controllerName=='home' && actionName=='about' ? 'active' : ''}"><g:link controller="home" action="about">About</g:link></li>
          <sec:ifLoggedIn>
            <sec:ifAnyGranted roles="ROLE_ADMIN">
              <li class="dropdown">
                <a href="#" class="dropdown-toggle" data-toggle="dropdown">Admin <b class="caret"></b></a>
                <ul class="dropdown-menu">
                  <li class="${controllerName=='admin' && actionName=='registerConsumer' ? 'active' : ''}"><g:link controller="admin" action="registerConsumer">Register Consumer</g:link></li>
                  <li class="${controllerName=='admin' && actionName=='reindex' ? 'active' : ''}"><g:link controller="admin" action="reindex">Reindex</g:link></li>
                  <li class="${controllerName=='admin' && actionName=='syncSubList' ? 'active' : ''}"><g:link controller="admin" action="syncSubList">Load Subscription List</g:link></li>
                </ul>
              </li>
            </sec:ifAnyGranted>
            <li class="${controllerName=='subscriptions' && actionName=='index' ? 'active' : ''}"><g:link controller="subscriptions" action="index">Subscriptions</g:link></li>
          </sec:ifLoggedIn>
          <li class="${controllerName=='home' && actionName=='topic' ? 'active' : ''}"><g:link controller="subscriptions" action="details" id="unfiltered">Unfiltered</g:link></li>
        </ul>
      </div><!--/.nav-collapse -->
    </div>
  </div>
  
  <g:layoutBody/>
  <script type="text/javascript" src="https://maps.googleapis.com/maps/api/js?key=${grailsApplication.config.mapskey}"></script>
  <asset:javascript src="application.js"/>
  <asset:deferredScripts/>
</body>
</html>
