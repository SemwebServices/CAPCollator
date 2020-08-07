package capcollator;

import grails.events.annotation.Publisher

public class CapCollatorSystemService {

  private Map state = null;

  public Map getCurrentState() {
    // log.debug("CapCollatorSystemService::getCurrentState()");
    if ( state==null)
      init();

    return state;
  }

  public void init() {
    log.debug("CapCollatorSystemService::init()");
    freshenState();  
  }

  @Publisher
  capcolSettingsUpdated() {
    // log.debug("Broadcast settings updated event so anyone depending on these can update");
    return state;
  }

  public synchronized void freshenState() {

    // log.debug("Freshen State");
    Setting.withNewSession { session ->
      Setting.withTransaction { ts ->
  
        if ( state == null ) {
          state = [:]
        }
    
        Setting setup_completed = Setting.findByKey('capcollator.setupcompleted') ?: new Setting(key:'capcollator.setupcompleted', value:'false').save(flush:true, failOnError:true);
        if ( setup_completed.value == 'true' ) {
          state.setup_completed = true;
        }
        else {
          state.setup_completed = false;
        }
  
        Setting.list().each { setting ->
          state[setting.key] = setting.value;
        }
      }
    }

    log.debug("Notify all observers that state has changed: ${state}");
    capcolSettingsUpdated();
  }

  private void cacheSetting(String setting, String def_value) {
    log.debug("cacheSetting(${setting},${def_value?:'NULL'})");
    Setting s = Setting.findByKey(setting) ?: new Setting(key:setting, value:def_value?:'').save(flush:true, failOnError:true);
    state[setting] = s.value;
  }

  public Object getSetting(String key) {
    return this.state[key]
  }

  public void loadLocalFeedSettings(String source_url) {
    try {
      def live_json_data = new groovy.json.JsonSlurper().parse(new java.net.URL(source_url))
      ingestLocalFeedSettings(live_json_data)
    }
    catch ( Exception e ) {
      log.error("problem syncing cap feed list",e);
    }
  }

  private void ingestLocalFeedSettings(Object fd) {
    fd?.each { s ->
      log.debug("Processing ${s}");
      try {
        // Array of maps containing a source elenment
        if ( s?.uriname ) {
          log.debug("Validate source ${s}");
          def source = LocalFeedSettings.findByUriname(s.uriname)
          if ( source == null ) {
            source = new LocalFeedSettings(
                                           uriname: s.uriname,
                                           alternateFeedURL: s.alternateFeedURL,
                                           authenticationMethod: s.authenticationMethod,
                                           credentials: s.credentials).save(flush:true, failOnError:true);
          }
          else {
            source.alternateFeedURL = s.alternateFeedURL
            source.authenticationMethod = s.authenticationMethod
            source.credentials = s.credentials
            source.save(flush:true, failOnError:true);
          }
        }
      }
      catch ( Exception e ) {
        log.error("Problem trying to add or update entry ${s?.uriname}",e);
      }
    }
  }

}
