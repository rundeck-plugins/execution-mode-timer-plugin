package com.rundeck.plugin

import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification


class ExecutionModeControllerSpec extends Specification implements ControllerUnitTest<ExecutionModeController> {

    def "test getExecutionLater() auth"(){
        given:
        controller.frameworkService = new MockFrameworkService(authorizeApplicationResource: true)

        controller.executionModeService = Mock(ExecutionModeService){
            getExecutionModeLater()>>[active:false]
        }

        when:
        controller.getExecutionLater()


        then:

        response.json  != null
        response.json  == [active:false]
    }

    def "test getNextExecutionChangeStatus() auth"(){
        given:
        controller.frameworkService = new MockFrameworkService(authorizeApplicationResource: true)

        controller.executionModeService = Mock(ExecutionModeService){
            nextExecutionTime()>>[active:false,msg:null]
        }

        when:
        controller.getNextExecutionChangeStatus()


        then:

        response.json  != null
        response.json  == [active:false,msg:null]
    }

}
