#!/usr/bin/env groovy
import groovy.grape.Grape

@GrabResolver(name='mvnRepository', root='http://central.maven.org/maven2/')
@GrabResolver(name='kint', root='http://nexus.k-int.com/content/repositories/releases')
// @Grab('io.github.http-builder-ng:http-builder-ng-core:1.0.4')




groovy.util.XmlParser xml_parser = new XmlParser(false,true,true)
xml_parser.startPrefixMapping('atom','http://www.w3.org/2005/Atom');
xml_parser.startPrefixMapping('','');
def rss= xml_parser.parse(new URL('https://cap-alerts.s3.amazonaws.com/unfiltered/rss.xml').openStream());
def atomns = new groovy.xml.Namespace('http://www.w3.org/2005/Atom','atom')

rss.channel[0].children().each { c ->
  println("updated: ${c.getAt(atomns.'updated')?.text()}")
}

