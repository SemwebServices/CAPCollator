import capcollator.Subscription


model {
    Map result
    // view-source:https://alert-feeds.s3.amazonaws.com/unfiltered/rss.xml
}

xmlDeclaration()

Subscription sub = (Subscription) (result.subscription);

rss('xmlns:atom':'http://www.w3.org/2005/Atom', version='2.0') {
  channel {
    atom:link(rel:'self', href:'')
    atom:link(rel:'alternate', title="RSS", href:'', type:'application/rss+xml')
    title(sub.subscriptionName)
    description(sub.subscriptionDescription)
    language('en')
    copyright('publi domain')
    image {
      title('Latest Valid CAP alerts received, unfiltered')
      url('https://s3-eu-west-1.amazonaws.com/alert-feeds/unfiltered/capLogo.jpg')
      link('https://s3-eu-west-1.amazonaws.com/alert-feeds/unfiltered/rss.xml')
    }
    result.rows.each { Map row_data ->
      item {
        identifier(row_data.identifier)
        title(row_data.title)
        description(row_data.description)
        url(row_data.url)
      }
    }
  }
}
