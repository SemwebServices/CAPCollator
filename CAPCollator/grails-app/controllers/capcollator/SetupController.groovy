package capcollator

import grails.plugin.springsecurity.annotation.Secured

class SetupController {

  def index() {
    def result=[:]
    Setting setup_completed = Setting.findByKey('capcollator.setupcompleted') ?: new Setting(key:'capcollator.setupcompleted', value:'false').save(flush:true, failOnError:true);

    if ( setup_completed.value=='false' ) {
      log.debug("Do system setup");
    }
    else {
      redirect(controller:'home', action:'index');
    }

    result;
  }
}
