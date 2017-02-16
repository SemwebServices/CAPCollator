package capcollator

class HomeController {

  def eventService

  def index() { 
    def result = [:]
    result.statsCache = eventService.getStatsCache()
    result
  }

  def status() { 
    def result = [:]
    result.statsCache = eventService.getStatsCache()
    result
  }

  def topic() {
    log.debug("Topic: ${params}");
    def result = [:]
    result
  }

}
