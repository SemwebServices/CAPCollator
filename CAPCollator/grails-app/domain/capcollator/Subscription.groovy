package capcollator

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode(includes='subscriptionId')
@ToString(includes='subscriptionId', includeNames=true, includePackage=false)
class Subscription {

  private static final long serialVersionUID = 1

  String id
  String subscriptionId
  String subscriptionName
  String subscriptionDescription
  String subscriptionUrl
  String filterType
  String filterGeometry

  String languageOnly
  String highPriorityOnly
  String officialOnly
  String xPathFilterId

  String feedXmlTemplate
  Long feedItemLimit


  String notes

  static transients = []

  static constraints = {
             subscriptionId blank: false, nullable: false, unique: true
           subscriptionName blank: false, nullable: false
    subscriptionDescription blank: false, nullable: true
            subscriptionUrl blank: true, nullable: false
                 filterType blank: true, nullable: true
             filterGeometry blank: true, nullable: true
                      notes blank: true, nullable: true
               languageOnly blank: true, nullable: true
           highPriorityOnly blank: true, nullable: true
               officialOnly blank: true, nullable: true
              xPathFilterId blank: true, nullable: true
            feedXmlTemplate blank: true, nullable: true
              feedItemLimit blank: true, nullable: true
  }

  static mapping = {
    table 'cc_sub'
                         id column: 'sub_id', generator: 'uuid', length:36
             subscriptionId column: 'sub_txt_id'
           subscriptionName column: 'sub_name'
    subscriptionDescription column: 'sub_desc'
            subscriptionUrl column: 'sub_url'
             filterGeometry column: 'sub_filter_geom', type:'text'
               languageOnly column: 'sub_filter_lang'
           highPriorityOnly column: 'sub_filter_high_prio'
               officialOnly column: 'sub_filter_official'
              xPathFilterId column: 'sub_filter_xpathid'
            feedXmlTemplate column: 'sub_feed_xml_template', type:'text'
              feedItemLimit column: 'sub_feed_item_limit'
  }

}
