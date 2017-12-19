package capcollator

class AlertController {

  def index() { 
    log.debug("AlertController::index(${params})");
  }

  def details() { 
    log.debug("AlertController::details(${params})");
  }
}
