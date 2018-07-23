package capcollator

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode(includes='subscriptionId')
@ToString(includes='subscriptionId', includeNames=true, includePackage=false)
class Subscription {

  private static final long serialVersionUID = 1

  String subscriptionId
  String subscriptionName
  String subscriptionDescription
  String subscriptionUrl
  String filterType
  String filterGeometry

  String notes

  static transients = []

  static constraints = {
             subscriptionId blank: false, nullable: false, unique: true
           subscriptionName blank: false, nullable: false
    subscriptionDescription blank: false, nullable: false
            subscriptionUrl blank: true, nullable: false
                 filterType blank:true, nullable:true
             filterGeometry blank:true, nullable:true
                      notes blank:true, nullable:true
  }

  static mapping = {
    table 'cc_sub'
             subscriptionId column: 'sub_txt_id'
           subscriptionName column: 'sub_name'
    subscriptionDescription column: 'sub_desc'
            subscriptionUrl column: 'sub_url'
             filterGeometry column: 'sub_filter_geom', type:'text'
  }

}
