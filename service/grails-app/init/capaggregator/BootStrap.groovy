package capaggregator

import capcollator.LocalFeedSettings;

import grails.util.Environment 

class BootStrap {

  def CAPIndexingService
  def grailsApplication
  def servletContext
  def capCollatorSystemService

  def init = { servletContext ->

    log.info("CAPAggregator Starting.....");
    log.info("  -> rabbitmq.connections.host: ${grailsApplication.config.rabbitmq.connections.host}");
    log.info("  -> rabbitmq.connections.username: ${grailsApplication.config.rabbitmq.connections[0].username}");
    log.info("  -> datasource.url : ${grailsApplication.config.dataSource.url}");
    log.info("  -> datasource.username : ${grailsApplication.config.dataSource.username}");
    log.info("  -> datasource.dialect : ${grailsApplication.config.dataSource.dialect}");
    log.info("  -> datasource.driverClassName : ${grailsApplication.config.dataSource.driverClassName}");
    log.info("  -> grails.serverUrl : ${grailsApplication.config.grails?.serverUrl}");
    log.info("  -> esconfig : ${grailsApplication.config.grails?.elasticsearch?.client}");

    
    LocalFeedSettings.withTransaction { status ->
      // load local overrrides first
      if ( grailsApplication.config.fah.localFeedSettings != null ) {
        File local_feed_settings_file = new File(grailsApplication.config.capcol.localFeedSettings)
        if ( local_feed_settings_file.canRead() ) {
          log.debug("Attempting to read local feed settings from ${grailsApplication.config.fah.localFeedSettings}");
          capCollatorSystemService.loadLocalFeedSettings("file://${local_feed_settings_file}");
        }
        else {
          log.warn("Unable to locate local feed settings file: ${grailsApplication.config.fah.localFeedSettings}");
        }
      }
    } 

    capCollatorSystemService.init()
    CAPIndexingService.freshen()
  }

  def destroy = {
  }
}
