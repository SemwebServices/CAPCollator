package capcollator

class ApiController {

  def nearby() {
    def result = [a:'1']
    log.debug("ApiController::index(${params})");
    respond result, formats: ['json', 'xml']
  }
}
