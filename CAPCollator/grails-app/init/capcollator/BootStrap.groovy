package capcollator

class BootStrap {

  def CAPIndexingService
  def grailsApplication
  def servletContext

  def sysusers = [
    [
     name:'admin',
     pass: System.getenv('YARM_ADM_PW')?:'ChangeMeImmediately',
     display:'Admin',
     email:'admin@semweb.co', 
     roles:['ROLE_ADMIN','ROLE_USER']
    ]
  ]

  def init = { servletContext ->
    log.debug("Starting CAPCollator. ${grailsApplication.metadata.getApplicationName()} ${grailsApplication.metadata.getApplicationVersion()}");
    if ( grailsApplication.config.gtmcode != null ) {
      log.debug("Using ${grailsApplication.config.gtmcode} as GTM code");
    }
    else {
      log.debug("No GTM tag found in context. Please add a context file called CAPCollator.xml to TOMCAT_HOME/conf/Catalina/localhost and set a property gtmcode. A sample file can be found in src/main/webapp/META-INF");
    }

    if ( grailsApplication.config.staticFeedsDir ) {
      log.debug("Static feeds are configured - ${grailsApplication.config.staticFeedsDir}");
    }

    setUpUserAccounts()
    CAPIndexingService.freshen()
  }

  def setUpUserAccounts() {
    sysusers.each { su ->
      log.debug("test ${su.name} ${su.pass} ${su.display} ${su.roles}");
      def user = User.findByUsername(su.name)
      if ( user ) {
        if ( user.password != su.pass ) {
          log.debug("Hard change of user password from config ${user.password} -> ${su.pass}");
          user.password = su.pass;
          user.save(failOnError: true)
        }
        else {
          log.debug("${su.name} present and correct");
        }
      }
      else {
        log.debug("Create user...");
        user = new User(
                      username: su.name,
                      password: su.pass,
                      display: su.display,
                      email: su.email,
                      enabled: true).save(failOnError: true)
      }

      log.debug("Add roles for ${su.name} (${su.roles})");
      su.roles.each { r ->

        def role = Role.findByAuthority(r) ?: new Role(authority:r).save(flush:true, failOnError:true)

        if ( ! ( user.authorities.contains(role) ) ) {
          log.debug("  -> adding role ${role} (${r})");
          UserRole.create user, role
        }
        else {
          log.debug("  -> ${role} already present");
        }
      }
    }
  }

  def destroy = {
  }

}
