package capcollator

import java.text.SimpleDateFormat

class EventCleanupJob {

  def ESWrapperService

  static triggers = {
    simple repeatInterval: 300000l // execute job once in 300 seconds
  }

  def execute() {

    // Current timestamp
    SimpleDateFormat ts_sdf = new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ss.SSS'Z'".toString());
    ts_sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    def now = ts_sdf.format(new Date())


    log.debug("Expire alerts where expire less than or equal ${now}");
    
    try {
      ESWrapperService.deleteByJsonQuery('alerts','{ "range" : { "AlertMetadata.Expires" : { "lte" : "'+now+'" } } }');

      long one_hour_ago_ms = System.currentTimeMillis() - ( 1 * 3600 * 1000 ) // 1 hour, 3600 seconds in an hour, 1000ms in a second
      String one_hour_ago = ts_sdf.format(new Date(one_hour_ago_ms))
      ESWrapperService.deleteByJsonQuery('events','{ "range" : { "timestamp" : { "lte" : "'+one_hour_ago+'" } } }');
    }
    catch ( Exception e ) {
      log.warn("Problem in EventCleanupJob - ${e.message}");
    }
  }
}
