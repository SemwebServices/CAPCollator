package capcollator

import grails.transaction.Transactional

@Transactional
class CapEventHandlerService {

  def process(cap_url) {
    log.debug("CapEventHandlerService::process ${cap_url}");
  }


  def handleNotification(body,context) {
    log.debug("CapEventHandlerService::handleNotification(...,${context})");
    try {
      def cap_link = body?.link?.'@href'
      if ( cap_link ) {
        log.debug("Fetch and parse ${cap_link}");

        def parsed_cap = new XmlSlurper().parse(cap_link)
        //                      .declareNamespace(
        //                                        xmlschema:"http://www.w3.org/2001/XMLSchema",
        //                                        cap11:"urn:oasis:names:tc:emergency:cap:1.1",
        //                                        cap12:"urn:oasis:names:tc:emergency:cap:1.2")
        
        log.debug("handleNotification ::\"${parsed_cap.identifier}\"");

        def entry = domNodeToString(parsed_cap)

        // Render the cap object as JSON
        String json_text = capcollator.Utils.XmlToJson(entry);

        log.debug("Cap AS JSON : ${json_text}");
      }
      else {
        log.error("Unable to find CAP link in ${body}");
      }
    }
    catch ( Exception e ) {
      log.error("problem handling cap alert",e);
    }
  }

  def domNodeToString(node) {
    //Create stand-alone XML for the entry
    String xml_text =  groovy.xml.XmlUtil.serialize(node)
    xml_text
  }

}
