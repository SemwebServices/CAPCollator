package capcollator

import grails.transaction.Transactional

@Transactional
class EventService {

  def stats_cache = [:]

  def registerEvent(String eventCode, long timestamp) {
    log.debug("registerEvent ${eventCode} ${timestamp}");
    Calendar c = new Calendar()
    c.setTimeInMillis(timestamp)

    if ( stats_cache[eventCode] == null ) {
      stats_cache[eventCode] = [:]
    }

    // We keep three counters for each event code - per minute for the last 60 mins, per hour for the last day, per day for the last 30 days
    incrementCounter(stats_cache[eventCode], 'minute', c.get(Calendar.MINUTE), 60)
    incrementCounter(stats_cache[eventCode], 'hour', c.get(Calendar.HOUR_OF_DAY), 24)
    incrementCounter(stats_cache[eventCode], 'day', c.get(Calendar.DAY_OF_MONTH) - 1, 30)  // DAY_OF_MONTH is 1 based

    log.debug("After registerEvent ${eventCode} ${timestamp} : ${stats_cache[eventCode]}");
  }

  def incrementCounter(counter_data, counter_code, slot, num_slots) {
    if ( counter_data[counter_code] == null ) {
      counter_data[counter_code] = [ current_slot:null, counter_data: new long[num_slots] ];
    }

    // If we've clicked onto the next slot in the circular queue, then reset the counter.
    if ( ( counter_data[counter_code].current_slot == null ) ||
         ( counter_data[counter_code].current_slot != slot ) ) {
      counter_data[counter_code].counter_data[slot] = 0;
    }

    counter_data[counter_code].counter_data[slot]++
  }
}
