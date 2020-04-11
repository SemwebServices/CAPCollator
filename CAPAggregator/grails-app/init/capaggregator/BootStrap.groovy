package capaggregator

import capcollator.LocalFeedSettings;

import grails.util.Environment 

class BootStrap {

  def CAPIndexingService
  def grailsApplication
  def servletContext
  def capCollatorSystemService

  def init = { servletContext ->
    log.info("Starting CAPCollator. ${grailsApplication.metadata?.getApplicationName()} ${grailsApplication.metadata?.getApplicationVersion()}");

    
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
