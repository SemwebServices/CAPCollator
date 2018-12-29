import capcollator.UserPasswordEncoderListener
import grails.plugins.executor.PersistenceContextExecutorWrapper
import java.util.concurrent.Executors

// Place your Spring DSL code here
beans = {
  userPasswordEncoderListener(UserPasswordEncoderListener)

  alertFetcherExecutorService(PersistenceContextExecutorWrapper) { bean ->
    bean.destroyMethod = 'destroy'
    persistenceInterceptor = ref("persistenceInterceptor")
    executor = Executors.newFixedThreadPool(5);
  }
}

