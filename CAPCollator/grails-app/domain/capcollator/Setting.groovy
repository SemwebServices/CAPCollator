package capcollator

class Setting {

  String key
  String value

  static constraints = {
    key blank: false, unique: true
    value blank: false
  }

  static mapping = {
    table 'cc_setting'
  }
}
