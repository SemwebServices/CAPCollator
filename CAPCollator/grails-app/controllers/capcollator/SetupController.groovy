package capcollator

import grails.plugin.springsecurity.annotation.Secured

class SetupController {

  def capCollatorSystemService

  def index() {
    def result=[:]
    Setting setup_completed = Setting.findByKey('capcollator.setupcompleted') ?: new Setting(key:'capcollator.setupcompleted', value:'false').save(flush:true, failOnError:true);

    
    if ( capCollatorSystemService.getCurrentState().setup_completed == false ) {
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
                          enabled: true).save(failOnError: true, flush:true)
          if ( user != null ) {
            log.debug("Detected new user with username ${user.username}, id: ${user.id}");

            Role role_admin = Role.findByAuthority('ROLE_ADMIN') ?: new Role(authority:'ROLE_ADMIN').save(flush:true, failOnError:true)
            Role role_user = Role.findByAuthority('ROLE_USER') ?: new Role(authority:'ROLE_USER').save(flush:true, failOnError:true)

            UserRole.create user, role_admin
            UserRole.create user, role_user

            setup_completed.value='true'
            setup_completed.save(flush:true, failOnError:true);
            redirect(controller:'home', action:'index');
          }
        }

        updateSetting('capcollator.feedTitlePrefix', params.feedTitlePrefix);
        updateSetting('capcollator.feedTitlePostfix', params.feedTitlePostfix);
        updateSetting('capcollator.feedEntryPrefix', params.feedEntryPrefix);
        updateSetting('capcollator.feedEntryPostfix', params.feedEntryPostfix);
        updateSetting('capcollator.awsBucketName', params.awsBucketName);
      }

      capCollatorSystemService.freshenState();
    }
    else {
      redirect(controller:'home', action:'index');
    }

    result;
  }

  private void updateSetting(String setting, String value) {
    Setting s = Setting.findByKey(setting)
    if ( s == null ) {
      s = new Setting(key:setting, value:value).save(flush:true, failOnError:true);
    }
    else {
      s.value=value
      s.save(flush:true, failOnError:true);
    }
  }
}
