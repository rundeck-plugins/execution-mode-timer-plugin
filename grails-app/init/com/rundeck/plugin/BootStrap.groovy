package com.rundeck.plugin

class BootStrap {

    def executionModeService
    def editProjectService


    def timer(String name,Closure clos){
        long bstart=System.currentTimeMillis()
        log.debug("BEGIN: ${name}")
        def res=clos()
        log.debug("${name} in ${System.currentTimeMillis()-bstart}ms")
        return res
    }

    def init = { servletContext ->

        timer("executionModeService.init") {
            executionModeService.initProcess()
        }

        timer("editProjectService.init") {
            editProjectService.initProcess()
        }


    }
    def destroy = {
    }
}
