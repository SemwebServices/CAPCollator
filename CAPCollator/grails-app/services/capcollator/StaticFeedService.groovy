package capcollator

import grails.transaction.Transactional
import groovy.xml.MarkupBuilder
import java.io.File
import java.io.FileWriter
import groovy.xml.XmlUtil

@Transactional
class StaticFeedService {

  def grailsApplication

  def update(routingKey, body, context) {
    String[] key_components = routingKey.split('\\.');
    if ( key_components.length == 2 ) {
      String sub_name = key_components[1]
      String full_path = grailsApplication.config.staticFeedsDir+'/'+sub_name;
      File sub_dir = new File(full_path)
      if ( ! sub_dir.exists() ) {
        log.debug("Setting up new static sub DIR ${full_path}");
        sub_dir.mkdirs()
      }

      File rss_file = new File(full_path+'/rss.xml')
      if ( ! rss_file.exists() )
        createStarterFeed(full_path, sub_name);

      addItem(full_path, body)
    }
    else {
      log.error("Unexpected number of routing key components:: ${key_components}");
    }
  }

  private void createStarterFeed(String path, subname) {

    // <?xml version='1.0' encoding='UTF-8'?>
    // <rss xmlns:atom="http://www.w3.org/2005/Atom" version="2.0">
    //   <channel>
    //     <atom:link rel="self" href="https://s3-eu-west-1.amazonaws.com/alert-feeds/unfiltered/rss.xml" type="application/rss+xml"/>
    //     <atom:link rel="alternate" title="RSS" href="https://s3-eu-west-1.amazonaws.com/alert-feeds/unfiltered/rss.xml" type="application/rss+xml"/>
    //     <title>Latest Valid CAP alerts received, unfiltered</title>
    //     <link>https://s3-eu-west-1.amazonaws.com/alert-feeds/unfiltered/rss.xml</link>
    //     <description>This feed lists the most recent valid CAP alerts uploaded to the Filtered Alert Hub.</description>
    //     <language>en</language>
    //     <copyright>public domain</copyright>
    //     <pubDate>Sun, 26 Aug 2018 21:23:20 GMT</pubDate>
    //     <lastBuildDate>Wed, 4 Jan 2017 11:31:34 GMT</lastBuildDate>
    //     <docs>http://blogs.law.harvard.edu/tech/rss</docs>
    //     <image>
    //      <title>Latest Valid CAP alerts received, unfiltered</title>
    //      <url>https://s3-eu-west-1.amazonaws.com/alert-feeds/unfiltered/capLogo.jpg</url>
    //      <link>https://s3-eu-west-1.amazonaws.com/alert-feeds/unfiltered/rss.xml</link>
    //     </image>
    //   </channel>
    // </rss>

    def entityNs = [
        'xmlns:atom': "http://www.w3.org/2005/Atom"
    ]
    def fileWriter = new FileWriter(path+'/rss.xml');
    def rssBuilder = new MarkupBuilder(fileWriter)
    rssBuilder.'atom:rss'(entityNs,version:"2.0") {
      channel {
        'atom:link'('John')
        'atom:link'('John')
        title("Latest Valid CAP alerts received, ${subname}")
        link("https://s3-eu-west-1.amazonaws.com/alert-feeds/${subname}")
        description("This feed lists the most recent valid CAP alerts uploaded to the Filtered Alert Hub.")
        language("en");
        copyright("public domain");
        pubDate("Sun, 26 Aug 2018 21:23:20 GMT");
        lastBuildDate("Wed, 4 Jan 2017 11:31:34 GMT");
        docs("http://blogs.law.harvard.edu/tech/rss");
        image {
          title("Latest Valid CAP alerts received, ${subname}");
          url("https://s3-eu-west-1.amazonaws.com/alert-feeds/${subname}/capLogo.jpg");
          link("https://s3-eu-west-1.amazonaws.com/alert-feeds/${subname}/rss.xml");
        }
      }
    }
    fileWriter.close();
  }


  private void addItem(String path, node) {
    def xml = new XmlSlurper().parse(path+'/rss.xml')

    //Edit File e.g. append an element called foo with attribute bar
    xml.channel.appendNode {
       item {
         title(bar: "bar value")
       }
    }

    //Save File
    def writer = new FileWriter(path+'/rss.xml')

    // Append new element

    // then sort in date order desc
    // rootNode.children().sort(true) {it.attribute('name')}

    //Option 1: Write XML all on one line
    // def builder = new StreamingMarkupBuilder()
    // writer << builder.bind {
    //   mkp.yield xml
    // }

    //Option 2: Pretty print XML
    XmlUtil.serialize(xml, writer)
  }
}
