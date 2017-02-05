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
class ESWrapperService {

  TransportClient esclient = null;

  @javax.annotation.PostConstruct
  def init() {
    log.debug("init ES wrapper service");

    Settings settings = Settings.builder().put("cluster.name", "elasticsearch").build();
    esclient = new org.elasticsearch.transport.client.PreBuiltTransportClient(settings);
    esclient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300));
  }

  def index(index,typename,record) {
    def result=null;
    try {
      def future = esclient.prepareIndex(index,typename).setSource(record)
      result=future.get()
    }
    catch ( Exception e ) {
      log.error("Error processing ${toJson(record)}",e);
    }
    result
  }

}
