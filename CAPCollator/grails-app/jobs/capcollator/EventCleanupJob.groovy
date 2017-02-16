package capcollator

import java.text.SimpleDateFormat

class EventCleanupJob {

  def ESWrapperService

  static triggers = {
    simple repeatInterval: 300000l // execute job once in 300 seconds
  }

  def execute() {

    // Current timestamp
    def sdt = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
    def now = std.format(new Date())

    log.debug("Expire alerts where expire less than or equal ${now}");
    
    ESWrapperService.deleteByQuery('alerts','"range" : { "AlertMetadata.expires" : { "lte" : "'+now+' } }');
  }
}
