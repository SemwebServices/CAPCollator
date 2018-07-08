package capcollator

class ApiController {

  def capEventHandlerService

  def nearby() {
    def result = [:]
    log.debug("ApiController::index(${params})");
    log.debug("accept: ${request.getHeader('ACCEPT')}");

    // capEventHandlerService exposes matchSubscriptionCircle(float lat, float lon, float radius)
    def lat = Float.parseFloat(params.lat?.toString())
    def lon = Float.parseFloat(params.lon?.toString())

    log.debug("Call matchSubscriptionCircle(${lat},${lon},2.0f);");
    result.nearby = capEventHandlerService.matchAlertCircle(lat,lon,2.0f);

    log.debug("Nearby result: ${result}");

    respond result, formats: ['json', 'xml']
  }
}
