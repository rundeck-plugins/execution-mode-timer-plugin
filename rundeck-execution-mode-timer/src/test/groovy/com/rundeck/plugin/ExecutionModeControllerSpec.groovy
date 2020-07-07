package com.rundeck.plugin

import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification

import javax.servlet.http.HttpServletResponse


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
            getSystemModeChangeStatus()>> [active:false, msg:null]
        }

        when:
        controller.getNextExecutionChangeStatus()


        then:

        response.json  != null
        response.json  == [active:false,msg:null]
    }

    def "test api apiExecutionModeLaterActive auth"(){
        given:
        controller.frameworkService = new MockFrameworkService(authorizeApplicationResource: false)
        controller.apiService = new MockApiService(requireVersion: true)
        controller.executionModeService = Mock(ExecutionModeService){
            saveExecutionModeLater(_)>>true
        }

        when:
        request.method = "POST"
        request.content = '{"value": "3m"}'.bytes
        request.addHeader('accept', 'application/json')
        controller.apiExecutionModeLaterActive()

        then:

        response.status == 403
    }

    def "test api apiExecutionModeLaterPassive auth"(){
        given:
        controller.frameworkService = new MockFrameworkService(authorizeApplicationResource: false)
        controller.apiService = new MockApiService(requireVersion: true)
        controller.executionModeService = Mock(ExecutionModeService){
            saveExecutionModeLater(_)>>true
        }

        when:
        request.method = "POST"
        request.content = '{"value": "3m"}'.bytes
        request.addHeader('accept', 'application/json')
        controller.apiExecutionModeLaterActive()

        then:

        response.status == 403
    }

    def "test api apiExecutionModeLaterActive valid"(){
        given:
        controller.frameworkService = new MockFrameworkService(authorizeApplicationResource: true)
        controller.apiService = new MockApiService(requireVersion: true)
        controller.executionModeService = Mock(ExecutionModeService){
            saveExecutionModeLater(_)>>true
        }

        when:
        request.method = method
        request.content = '{"value": "3m"}'.bytes
        request.addHeader('accept', 'application/json')
        controller.apiExecutionModeLaterActive()

        then:

        response.status == statusCode

        where:
        method      | statusCode
        'POST'      | 200
        'GET'       | 405
        'PUT'       | 405
        'DELETE'    | 405

    }

    def "test api apiExecutionModeLaterPassive valid"(){
        given:
        controller.frameworkService = new MockFrameworkService(authorizeApplicationResource: true)
        controller.apiService = new MockApiService(requireVersion: true)
        controller.executionModeService = Mock(ExecutionModeService){
            saveExecutionModeLater(_)>>true
        }

        when:
        request.method = method
        request.addHeader('accept', 'application/json')
        request.content = '{"value": "3m"}'.bytes
        controller.apiExecutionModeLaterPassive()

        then:

        response.status == statusCode

        where:
        method      | statusCode
        'POST'      | 200
        'GET'       | 405
        'PUT'       | 405
        'DELETE'    | 405

    }


    def "test api apiExecutionModeLaterActive test"(){
        given:
        controller.frameworkService = new MockFrameworkService(authorizeApplicationResource: true)
        controller.apiService = new MockApiService(requireVersion: true)
        controller.executionModeService = Mock(ExecutionModeService){
            getCurrentStatus()>>false
        }

        when:
        request.method = 'POST'
        request.content = body
        request.addHeader('accept', 'application/json')
        controller.apiExecutionModeLaterActive()

        then:

        saveCall*controller.executionModeService.saveExecutionModeLater(_)>>saved
        response.json  != null
        response.json  == [msg:msg, saved:saved]
        response.status == responseStatus

        where:
        body                            | saved     | responseStatus | msg                           | saveCall
        '{"value": "3m"}'.bytes         | true      | 200            | "Execution Mode Later saved"  | 1
        '{"value": "3m"}'.bytes         | false     | 200            | "No changed found"            | 1
        '{"value": "badvalue"}'.bytes   | false     | 400            | "Format was not valid, the attribute value is not set properly. Use something like: 3m, 1h, 3d"  | 0
        'dsadsadsadsad'.bytes           | false     | 400            |  "Format was not valid. the request must be a json object, for example: {\"value\":\"<timeExpression>\"}"  | 0
        null                            | false     | 400            | "Format was not valid. the request must be a json object, for example: {\"value\":\"<timeExpression>\"}"  | 0

    }

    def "test api apiExecutionModeLaterPassive test"(){
        given:
        controller.frameworkService = new MockFrameworkService(authorizeApplicationResource: true)
        controller.apiService = new MockApiService(requireVersion: true)
        controller.executionModeService = Mock(ExecutionModeService){
            getCurrentStatus()>>true
        }

        when:
        request.method = 'POST'
        request.content = body
        request.addHeader('accept', 'application/json')
        controller.apiExecutionModeLaterPassive()

        then:

        saveCall*controller.executionModeService.saveExecutionModeLater(_)>>saved
        response.json  != null
        response.json  == [msg:msg, saved:saved]
        response.status == responseStatus

        where:
        body                            | saved     | responseStatus     | msg                           | saveCall
        '{"value": "3m"}'.bytes         | true      | 200                | "Execution Mode Later saved"  | 1
        '{"value": "3m"}'.bytes         | false     | 200                | "No changed found"            | 1
        '{"value": "badvalue"}'.bytes   | false     | 400                | "Format was not valid, the attribute value is not set properly. Use something like: 3m, 1h, 3d"  | 0
        'dsadsadsadsad'.bytes           | false     | 400                | "Format was not valid. the request must be a json object, for example: {\"value\":\"<timeExpression>\"}"  | 0
        null                            | false     | 400                | "Format was not valid. the request must be a json object, for example: {\"value\":\"<timeExpression>\"}"  | 0

    }



}


class MockApiService{
    boolean requireVersion = false

    def requireVersion(request, HttpServletResponse response, int min){
        requireVersion
    }

}
