package capcollator;

import org.codehaus.groovy.runtime.memoize.*;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import java.util.HashMap;

public class AlertCacheService {

  // private LRUCache alert_cache = new LRUCache(250)
  // private LRUMap alert_cache = new LRUMap(500)

  // Store entries for up to 15 minutes
  private Map alert_cache = Collections.synchronizedMap(new PassiveExpiringMap(1000*60*15))

  public void put(String k,byte[] v) {
    log.debug("AlertCacheService::Put ${k}[${v?.length}] (${alert_cache.size()})");
    alert_cache.put(k,v);
  }

  public byte[] get(String k) {
    byte[] result = alert_cache.get(k)
    log.debug("AlertCacheService::Get ${k}[${result?.length}] (${alert_cache.size()})");
    return result
  }
}
