package capcollator;

import org.codehaus.groovy.runtime.memoize.*;

public class AlertCacheService {

  private LRUCache alert_cache = new LRUCache(100)

  public void put(k,v) {
    alert_cache.put(k,v);
  }

  public Object get(k) {
    return alert_cache.get(k);
  }
}
