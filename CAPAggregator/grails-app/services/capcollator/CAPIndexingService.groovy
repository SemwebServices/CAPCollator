package capcollator

import grails.gorm.transactions.*
import java.text.SimpleDateFormat
import java.net.InetAddress;
import static groovy.json.JsonOutput.*
import org.grails.datastore.mapping.engine.event.*
import grails.events.*
import javax.annotation.*
import org.grails.datastore.mapping.engine.event.SaveOrUpdateEvent
import grails.events.annotation.gorm.Listener

class CAPIndexingService {

  static transactional = false
  ESWrapperService ESWrapperService

  @javax.annotation.PostConstruct
  def init() {
  }

  @Listener
  indexSubEventHandler(SaveOrUpdateEvent event) {
    if ( event.entityObject instanceof Subscription ) {
      indexSub(event.entityObject)
    }
  }
  

  def freshen() {
    log.debug("freshen...");
  }

  def reindexSubscriptions() {
    int counter=0;
    Subscription.findAll().each { sub ->
      log.debug("Reindex (${counter++})");
      indexSub(sub)
    }
    log.debug("CAPIndexingService::reindexSubscriptions done");
  }

  def indexSub(Subscription sub) {
    log.debug("indexSub ${sub}");
    try {
      def es_record = [
                      recid:sub.subscriptionId,
                      name:sub.subscriptionName,
                      shortcode:sub.subscriptionId,
                      subshape:[:],
                      subscriptionUrl:null,
                      languageOnly:sub.languageOnly,
                      highPriorityOnly: sub.highPriorityOnly,
                      officialOnly: sub.officialOnly,
                      xPathFilterId: sub.xPathFilterId,
                      xPathFilter: sub.xPathFilter,
                      areaFilterId: null,
                      loadSubsVersion: "1.1"
                    ]

      es_record.subshape.type=sub.filterType

      // geometry is a string containing a geo json structure
      es_record.subshape.coordinates=new groovy.json.JsonSlurper().parseText(sub.filterGeometry)

      log.debug("Send es record ${es_record.subshape.coordinates} - recordid is ${sub.id}");

      ESWrapperService.doIndex('alertssubscriptions','_doc',"${sub.subscriptionId}".toString(),es_record);
    }
    catch ( Exception e ) {
      log.warn("Problem trying to submit sub ${sub} for indexing: ${e.message}",e);
    }
  }


}
