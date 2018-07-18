model {
    Map result
    // view-source:https://alert-feeds.s3.amazonaws.com/unfiltered/rss.xml
}
xmlDeclaration()
rss('xmlns:atom':'http://www.w3.org/2005/Atom', version='2.0') {
  notes('https://s3-eu-west-1.amazonaws.com/alert-feeds/unfiltered/rss.xml')
  channel {
    atom:link(rel:'self', href:'')
    atom:link(rel:'alternate', title="RSS", href:'', type:'application/rss+xml')
    description('This feed lists the most recent valid CAP alerts uploaded to the Filtered Alert Hub.')
    language('en')
    copyright('publi domain')
    image {
      title('Latest Valid CAP alerts received, unfiltered')
      url('https://s3-eu-west-1.amazonaws.com/alert-feeds/unfiltered/capLogo.jpg')
      link('https://s3-eu-west-1.amazonaws.com/alert-feeds/unfiltered/rss.xml')
    }
    
    max(result?.max)
    offset(result?.offset)
    totalAlerts(result?.totalAlerts)

    result.rows.each { org.elasticsearch.search.internal.InternalSearchHit row ->
      item {
        ((Map)(row.getSource().AlertMetadata))info
        title(row.)
        link(((Map)(row.getSource().AlertMetadata)).SourceUrl)
        description()
        category('Met')
        pubDate()
        guid()
        dc:creator()
        dc:date()
      }
    }
  }
}
