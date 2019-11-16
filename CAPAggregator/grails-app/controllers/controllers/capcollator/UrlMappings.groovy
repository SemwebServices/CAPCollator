package capcollator

class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

        "/"(controller:'home', action:'index')
        "/topics/$id"(controller:'home', action:'topic')

        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
