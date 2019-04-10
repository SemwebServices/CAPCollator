package capcollator;

public class CapCollatorSystemService {

  private Map state = [:]

  public Map getCurrentState() {
    return state;
  }

  public void init() {
    freshenState();  
  }

  public synchronized void freshenState() {
    Setting setup_completed = Setting.findByKey('capcollator.setupcompleted') ?: new Setting(key:'capcollator.setupcompleted', value:'false').save(flush:true, failOnError:true);
    if ( setup_completed.value == 'true' ) {
      state.setup_completed = true;
    }
    else {
      state.setup_completed = false;
    }
  }
}
