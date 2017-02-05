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

@Transactional
class CAPIndexingService {

  def ESWrapperService

  def reindexSubscriptions() {
    Subscription.findAll() { sub ->

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
