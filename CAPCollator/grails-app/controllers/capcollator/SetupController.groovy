package capcollator

import grails.plugin.springsecurity.annotation.Secured

class SetupController {

  def index() {
    def result=[:]
    Setting setup_completed = Setting.findByKey('capcollator.setupcompleted') ?: new Setting(key:'capcollator.setupcompleted', value:'false').save(flush:true, failOnError:true);

    if ( setup_completed.value=='false' ) {
      log.debug("Do system setup");
      if ( request.method=='POST' ) {
        if ( ( params.adminUsername?.length() > 0 ) && 
             ( params.adminPassword?.length() > 0 ) ) {
          log.debug("Create new admin account ${params} adn then set setupcompleted to true");
          User user = new User(
                          username: params.adminUsername,
                          password: params.adminPassword,
                          display: params.adminUsername,
                          email: params.adminEmail,
                          enabled: true).save(failOnError: true)

        }
      }
    }
    else {
      redirect(controller:'home', action:'index');
    }

    result;
  }
}
