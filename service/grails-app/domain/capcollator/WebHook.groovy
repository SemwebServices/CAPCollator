package capcollator

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

class WebHook {

  private static final long serialVersionUID = 1

  String id
  String hookUrl

  static transients = []

  static hasMany = [
  ]

  static mappedBy = [
  ]

  static belongsTo = [
    ownerSubscription: Subscription
  ]


  static constraints = {
  }

  static mapping = {
    table 'cc_web_hook'
                   id column: 'wh_id', generator: 'uuid', length:36
              hookUrl column: 'wh_url'
    ownerSubscription column: 'wh_owner_sub_fk'
  }

}
