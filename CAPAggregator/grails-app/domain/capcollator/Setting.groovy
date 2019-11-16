package capcollator

class Setting {

  String key
  String value

  static constraints = {
    key nullable:false, blank: false, unique: true
    value nullable:true, blank: true
  }

  static mapping = {
    table 'cc_setting'
  }

  public String toString() {
    "${key}:${value}"
  }
}
