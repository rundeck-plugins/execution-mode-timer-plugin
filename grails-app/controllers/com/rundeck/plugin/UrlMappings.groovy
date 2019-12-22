package com.rundeck.plugin

class UrlMappings {

    static mappings = {
        "/menu/executionMode/executionLater"(controller: 'executionMode', action: 'getExecutionLater')
        "/menu/executionMode/executionLater/nextTime"(controller: 'executionMode', action: 'getNextExecutionChangeStatus')

        "/project/$project/configure/executionLater"(controller: 'editProject', action: 'getExecutionLater')
        "/project/$project/configure/executionLater/nextTime"(controller: 'editProject', action: 'getNextExecutionChangeStatus')

        "/"(view:"/index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
