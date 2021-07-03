package capaggregator

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import groovy.transform.CompileStatic
import grails.plugins.DefaultGrailsPluginManager
import org.springframework.boot.web.embedded.undertow.UndertowBuilderCustomizer
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory
import org.springframework.context.annotation.Bean
import io.undertow.Undertow.Builder
import io.undertow.UndertowOptions
import groovy.util.logging.Slf4j
import groovy.transform.CompileStatic


@CompileStatic
class Application extends GrailsAutoConfiguration {

  private void info ( final String message) {
    println "Application Initialization: ${message}"
  }

  static void main(String[] args) {
    final TimeZone utcTz = TimeZone.getTimeZone("UTC")
    if (TimeZone.default != utcTz) {
      println("Timezone default is ${TimeZone.default.displayName}. Setting to UTC")
      TimeZone.default = utcTz
    }
      GrailsApp.run(Application, args)
  }


  @Bean
  public UndertowServletWebServerFactory embeddedServletContainerFactory(){

    UndertowServletWebServerFactory factory = new UndertowServletWebServerFactory()
    factory.builderCustomizers << new UndertowBuilderCustomizer() {

      @Override
      public void customize(Builder builder) {

        // Min of 2, Max of 4 I/O threads
        final int ioThreadCount = Math.min(Math.max(Runtime.getRuntime().availableProcessors(), 2), 4)
        builder.ioThreads = ioThreadCount

        final int heap_coef = ((Runtime.getRuntime().maxMemory() / 1024 / 1024)/256) as int
        int workers_per_io = 8
        if (heap_coef <= 2) {
          workers_per_io = 6
        } else if (heap_coef <= 1) {
          workers_per_io = 4
        }

        // Workers per I/O thread
        final int workerThreadCount = ioThreadCount * workers_per_io
        builder.workerThreads = workerThreadCount

        // Enable HTTP2
        // builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true)

        info "Runtime memory reported ${Runtime.getRuntime().maxMemory() / 1024 / 1024} mb"
        info "Runtime CPUs reported ${Runtime.getRuntime().availableProcessors()}"
        info "Allocated ${ioThreadCount} IO Threads"
        info "Allocated ${workerThreadCount} worker threads"
      }
    }
    factory
  }

}

