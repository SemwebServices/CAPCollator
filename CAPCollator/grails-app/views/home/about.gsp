<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>About CAP Collator : Middleware for aggregating emergency alerts from many heterogeneous sources</title>

    <asset:link rel="icon" href="favicon.ico" type="image/x-ico" />
</head>
<body>

  <div class="container">
    <div class="row">
      <div class="container">

  <h1>About CAPCollator</h1>

  <p>
    CAPCollator is an open source project resulting from volunteer effort
    to support the work of 
<a href="https://www.linkedin.com/in/eliot-christian-03317497/">Eliot Christian</a>
in improving a global network of emergency alerting systems. <a href="https://en.wikipedia.org/wiki/Common_Alerting_Protocol">CAP</a> is
    a collection of standards (Primarily the use of ATOM and RSS in publishing systems and an XML Schema which Items in CAP feeds should point to) by
    which specialist vertical organisations (For example, <a href="https://public.wmo.int/en">WMO</a> the World Meterological Organisation) can collate
    early warnings in their specailist field and then publish those alerts for other systems to consume. Some other info about CAP can be found in these links: 
<a href="http://www.wmo.int/pages/prog/drr/events/Barbados/Pres/3-WMO-CAP.pdf">WMO Brefing</a>;
<a href="https://vimeo.com/221233175">CAP Alerting Pres</a>;
<a href="https://www.preparecenter.org/resources/cap-common-alerting-protocol">CAP Info</a>;
<a href="https://www.preparecenter.org/th/member-directory/eliotchristian">Eliots page at preparecentre </a> 
  </p>

  </p>
    A challenge in moving the CAP ecosystem forwards is the diverse nature of the source systems. Systems can be diverse over subject: Weather, Air Quality,
    Civic Emergency, Active Shooter, Missing Child, ... and also split by geographical boundaries. Some CAP publishers have a global remit, others are bound
    to a territory or other spatial area. Initially, Ian worked with Eliot on the challenges around efficient spatial searching for a large database of subscription definitions
    given a real time stream of incoming alerts each with their own spatial information.
  <p>

  <p>
    It quickly became apparent that a core challenge was the polling of ATOM and RSS feeds. We discussed PubSubHubBub gateways, and building generic software to
    turn polled RSS and ATOM into push streams of events. This separation led to the development of CAPCollators sister project : <a href="https://github.com/SemwebServices/PubSubHubBubFacade">feedFacade</a>. Initially
    this service was targeted at being a generic ATOM/RSS to PubSubHubBub service. At the same time, AWS Lambda was the initial target environment for a service. After discussion, we were concerned
    that building infrastructure tied to a specific platform might not be the best choice, so this requirement was softened. This led to feedFacade being a system to poll remote services, and publish events
    to a local RabbitMQ message broker. This allowed us to write adapters which fan out those alerts to many consumers.
  </p>

  <p>
    Eliots original CAP Alert Hub itself is extremely resilient and low resource, using lambda events to update static files of alerts. CAPCollator takes a slightly different approach and is an attempt at
    leveraging the real-time features of an engine like elasticsearch to see how we might innovate in this space. CAPCollator consumes CAP events from feedFacade and indexes all published alerts in real time where
    they can be searched as a single homogenous whole. Alerts are tagged with the Alert Hub subscriptions they match, and that tagging forms the basis of one of the views in the CAPCollator interface.
    That view is, however, just a logical separation of the current index of live global alerts.
  </p>

  <p>
    CAPCollator then is an attempt to create a single unified index of current live CAP events at the lowest possible granularity (Single Events). It leverages RabbitMQ to subscribe a queue to a topic, and elasticsearch
    as a real time indexing engine. In essence, CAPCollator is a middleware software solution that sits between CAP Event Producers and CAP Event Consumers. The system bridges the gap created by the fact that our
    alert publishing systems (EG our local Police department, or the national weather service) are structurally different to our event consuming systems (EG an app running on my mobile phone that doesn't care who published the alert,
    only that I am physically in a place that should be told about the alert).<br/>
    The underlying ES data can be exposed as an API level interface for anyone wishing to build services on top of the feed. Links to demo systems follow:
<ul>
  <li><a href="https://demo.semweb.co/CAPCollator">CAPCollator demo</a></li>
  <li><a href="https://demo.semweb.co/es/alerts/_search?q=sheffield">Example search of ES endpoint (Alerts)</a></li>
  <li><a href="https://demo.semweb.co/es/alertssubscriptions/_search?q=sheffield">Example search of ES endpoint (Subs)</a></li>
  <li><a href="https://demo.semweb.co/feedFacade">feedFacade demo</a></li>
</ul>
  </p>

<h2>Source Code</h2>
<p>
  <ul>
    <li><a href="https://github.com/SemwebServices/CAPCollator">CAPCollator</a></li>
    <li><a href="https://github.com/SemwebServices/PubSubHubBubFacade">feedFacade</a></li>
  </ul>
</p>

<h2>About SemwebServices</h2>
<p>
  Semweb Services Ltd was formed in 2015 as a vehicle for Ian to undertake professional IT consultancy services
  at rates (Including pro-bono projects) which are predicated more on the needs of charities, local authorities and volunteer groups than traditional corporate models.
  Where possible, proceeds are retained to support continuing civic activity in the areas of evidence based governance, open data, civic society and grass roots initiatives.
  As an organisation, we are interested in the commercial explotiation of these services to the extent that such exploitation supports that continuing civic activity.
  Anyone with an interest, or a need we might be able to help with can contact ian at semweb dot co.
</p>
      </div>
    </div>
  </div>
</body>
</html>
