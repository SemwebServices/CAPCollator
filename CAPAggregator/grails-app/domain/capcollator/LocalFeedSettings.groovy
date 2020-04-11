package capcollator

class LocalFeedSettings {

  // The "Code" by which this feed is known
  String uriname
  String alternateFeedURL
  String authenticationMethod
  String credentials

  static constraints = {
    uriname blank: false, nullable:false, unique: true
    alternateFeedURL blank: true, nullable:true
    authenticationMethod blank: true, nullable:true
    credentials blank: true, nullable:true
  }

 
}
