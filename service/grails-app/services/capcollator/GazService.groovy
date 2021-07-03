package capcollator

import grails.gorm.transactions.*

@Transactional
class GazService {

  def ESWrapperService

  def resolve(String authority, String symbol) {
    def result = null;
    log.debug("resolve(${authority},${symbol}");
    def getresp = ESWrapperService.get('gazetteer','gazentry',authority+':'+symbol);
 
    if ( getresp.isExists() ) {
      result = getresp.getSource()
    }

    result
  }

  def cache(String authority, String symbol, Object polygon) {
    log.debug("cache(${authority},${symbol})");
    if ( resolve(authority,symbol) ) {
      log.debug("  -> Already found in cache");
    }
    else {
      log.debug("Create new cache entry (${authority},${symbol},${polygon})");
      def gazrec = [
        authority: authority,
        symbol: symbol,
        subshape : [:]
      ]
      gazrec.subshape.type='polygon'
      gazrec.subshape.coordinates=[polygon]
      ESWrapperService.doIndex('gazetteer','_doc',authority+':'+symbol,gazrec)
    }
  }
}
