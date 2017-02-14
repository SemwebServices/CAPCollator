package capcollator

class HomeController {

  def eventService

  def index() { 
    def result = [:]
    result.statsCache = eventService.statsCache
    result
  }
}
