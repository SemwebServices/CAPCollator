package capcollator;

import grails.events.annotation.Publisher

public class CapCollatorSystemService {

  private Map state = null;

  public Map getCurrentState() {
    if ( state==null)
      init();

    return state;
  }

  public void init() {
    freshenState();  
  }

  @Publisher
  capcolSettingsUpdated() {
    log.debug("Broadcast settings updated event so anyone depending on these can update");
    return state;
  }

  public synchronized void freshenState() {

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
    cacheSetting('capcollator.feedTitlePrefix','');
    cacheSetting('capcollator.feedTitlePostfix','');
    cacheSetting('capcollator.feedEntryPrefix','');
    cacheSetting('capcollator.feedEntryPostfix','');
    cacheSetting('capcollator.awsBucketName','');

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
}
