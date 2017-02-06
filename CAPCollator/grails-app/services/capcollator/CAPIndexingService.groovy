package capcollator

import grails.transaction.Transactional

import java.text.SimpleDateFormat
import java.net.InetAddress;

import org.elasticsearch.client.Client
import org.elasticsearch.node.Node
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress

import static groovy.json.JsonOutput.*

import org.grails.datastore.mapping.engine.event.*
import grails.events.*
import javax.annotation.*

@Transactional
class CAPIndexingService {

  def ESWrapperService

  @javax.annotation.PostConstruct
  def init() {

    log.debug("Register gorm:saveOrUpdateEvent");

    on("gorm:saveOrUpdateEvent") { saveOrUpdateEvent ->
      log.debug("got saveOrUpdateEvent ${saveOrUpdateEvent}");
      if ( saveOrUpdateEvent.getEntity().getName() == 'capcollator.Subscription' ) {
        log.debug("Subscription saved or updated");
      }
    }
  }

  def freshen() {
    log.debug("freshen...");
  }

  def reindexSubscriptions() {
    Subscription.findAll().each { sub ->

      def es_record = [
                    recid:sub.subscriptionId,
                    name:sub.subscriptionName,
                    shortcode:sub.subscriptionId,
                    subshape:[:],
                    subscriptionUrl:null,
                    languageOnly:null,
                    highPriorityOnly: null,
                    officialOnly: null,
                    xPathFilterId: null,
                    xPathFilter: null,
                    areaFilterId: null,
                    loadSubsVersion: "1.1"
                  ]

      es_record.subshape.type=sub.filterType
      es_record.subshape.coordinates=sub.filterGeometry


      ESWrapperService.index('alertssubscriptions','alertssubscription',es_record);
    }
  }


}
