package capcollator

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode(includes='username')
@ToString(includes='username', includeNames=true, includePackage=false)
class User implements Serializable {

  private static final long serialVersionUID = 1

  transient springSecurityService

  String username
  String password
  boolean enabled = true
  boolean accountExpired
  boolean accountLocked
  boolean passwordExpired

  Set<Role> getAuthorities() {
    UserRole.findAllByUser(this)*.role
  }

  static transients = ['springSecurityService']

  static constraints = {
    password blank: false, password: true
    username blank: false, unique: true
  }

  static mapping = {
    table 'cc_user'
    password column: '`password`'
  }
}
