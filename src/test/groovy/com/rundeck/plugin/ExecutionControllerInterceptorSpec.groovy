package com.rundeck.plugin

import grails.testing.web.interceptor.InterceptorUnitTest
import spock.lang.Specification

class ExecutionControllerInterceptorSpec extends Specification implements InterceptorUnitTest<ExecutionControllerInterceptor> {

    def setup() {
    }

    def cleanup() {

    }

    void "Test executionController interceptor matching"() {
        when:"A request matches the interceptor"
            withRequest(controller:"executionController")

        then:"The interceptor does match"
            interceptor.doesMatch()
    }
}
