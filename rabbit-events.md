# Rabbit Events

CAPCollator listense for, and emits a number of events, this file is the place to document those

## Subscription Creation

When a new subscription is created, an event is emitted to exchange "CAPExchange" with the routing key "CAPSubAdmin.{subscriptionId}" and a body JSON document containing:

    event
    subscriptionId
    subscriptionName
    subscriptionUrl
    filterType
    filterGeometry
    languageOnly
    highPriorityOnly
    officialOnly
    xPathFilterId

Create a Queue listening to this key if you wish to trigger your own actions on feed creation


## Subscription Match

When an alert is matched to a subscription an event is emitted to exchange "CAPExchange" with the routing key "CAPSubMatch.{subscriptionId}" and a body JSON document containing:

    ToDO

# FeedFacade Events

The CAP Collator listens for raw CAP events emitted from FeedFacade - these are

## Alert detected

When an alert is detected, an event is emitted  to exchange "CAPExchange" with the routing key "CAPAlert."

