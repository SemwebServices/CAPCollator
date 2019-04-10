package capcollator

import java.text.SimpleDateFormat

class EventCleanupJob {

  def ESWrapperService

  static triggers = {
    simple repeatInterval: 300000l // execute job once in 300 seconds
  }

  def execute() {

    // Current timestamp
    def sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
    def now = sdf.format(new Date())

    log.debug("Expire alerts where expire less than or equal ${now}");
    
    try {
      ESWrapperService.deleteByQuery('alerts','{ "range" : { "AlertMetadata.Expires" : { "lte" : "'+now+'" } } }');

      long one_hour_ago_ms = System.currentTimeMillis() - ( 1 * 3600 * 1000 ) // 1 hour, 3600 seconds in an hour, 1000ms in a second
      String one_hour_ago = sdf.format(new Date(one_hour_ago_ms))
      ESWrapperService.deleteByQuery('events','{ "range" : { "timestamp" : { "lte" : "'+one_hour_ago+'" } } }');
    }
    catch ( Exception e ) {
      log.warn("Problem in EventCleanupJob - ${e.message}");
    }
  }
}
