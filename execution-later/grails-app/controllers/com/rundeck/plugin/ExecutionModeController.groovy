package com.rundeck.plugin

import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import grails.converters.JSON

import javax.servlet.http.HttpServletResponse


class ExecutionModeController {
    static final String ACTION_ADMIN = "admin"
    static final String RES_TYPE_SYSTEM = "system"

    static allowedMethods = [
            getExecutionLater: 'GET',
            getNextExecutionChangeStatus: 'GET',
            apiExecutionModeLaterActive: 'POST',
            apiExecutionModeLaterPassive: 'POST'

    ]

    def executionModeService
    def frameworkService
    def apiService

    private boolean authorizeSystemAdmin() {

        UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if (!frameworkService.authorizeApplicationResource(
                authContext,
                AuthorizationUtil.resourceType('system'),
                ACTION_ADMIN
        )) {
            request.errorCode = 'request.error.unauthorized.message'
            request.errorArgs = [
                    'Calendar (admin)',
                    'Server',
                    frameworkService.getServerUUID()]
            response.status = HttpServletResponse.SC_FORBIDDEN
            request.titleCode = 'request.error.unauthorized.title'

            render(view: "/common/error", model: [:])
            return false
        }
        return true
    }

    def getExecutionLater() {
        if (!authorizeSystemAdmin()) {
            return
        }

        render(
                executionModeService.getExecutionModeLater() as JSON,
                contentType: 'application/json'
        )
    }

    def getNextExecutionChangeStatus(){
        if (!authorizeSystemAdmin()) {
            return
        }

        def status = executionModeService.nextExecutionTime()

        render(
                status as JSON,
                contentType: 'application/json'
        )
    }

    def apiExecutionModeLaterActive(){

        if (!authorizeSystemAdmin()) {
            return
        }

        if (!apiService.requireVersion(request, response, PluginUtil.V34)) {
            return
        }

        def result = validateApi(request, response)

        def saved = false
        def msg = ""

        if(!result.fail){

            def status = executionModeService.getCurrentStatus()
            if(status){
                msg = "Executions are already set on active mode, cannot active later"
            }else{
                def config = [activeLater: true, activeLaterValue:result.value]
                saved = executionModeService.saveExecutionModeLater(config)
                if(saved){
                    msg = "Execution Mode Later saved"
                }else{
                    msg = "No changed found"
                }
            }
        }

        if(result.errormsg){
            msg = result.errormsg
            response.status = 400
        }

        render(
                [saved: saved, msg: msg] as JSON,
                contentType: 'application/json'
        )

    }


    def apiExecutionModeLaterPassive(){

        if (!authorizeSystemAdmin()) {
            return
        }

        if (!apiService.requireVersion(request, response, PluginUtil.V34)) {
            return
        }

        def result = validateApi(request, response)

        def saved = false
        def msg = ""

        if(!result.fail){

            def status = executionModeService.getCurrentStatus()
            if(!status){
                msg = "Executions are already set on passive mode, cannot disable later"
            }else{
                Map config = [passiveLater:true,passiveLaterValue:result.value]
                saved = executionModeService.saveExecutionModeLater(config)
                if(saved){
                    msg = "Execution Mode Later saved"
                }else{
                    msg = "No changed found"
                }
            }
        }

        if(result.errormsg){
            msg = result.errormsg
            response.status = 400
        }

        render(
                [saved: saved, msg: msg] as JSON,
                contentType: 'application/json'
        )

    }

    def validateApi( request,  response){
        boolean fail = false
        def errormsg = ""
        def value = null

        def data
        try{
            data = request.JSON
        }catch(Exception e){
            errormsg = e.message
        }

        if(!data){
            fail=true
            errormsg = 'Format was not valid. the request must be a json object, for example: {"value":"<timeExpression>"}'
            return [value: data.value, fail: fail, errormsg:errormsg]
        }

        if(!data.value){
            fail=true
            errormsg = 'Format was not valid. the request must be a json object, for example: {"value":"30m"}'
            return [value: data.value, fail: fail, errormsg:errormsg]
        }

        if(!PluginUtil.validateTimeDuration(data.value)){
            fail=true
            errormsg = "Format was not valid, the attribute value is not set properly. Use something like: 3m, 1h, 3d"
            return [value: data.value, fail: fail, errormsg:errormsg]
        }

        return [value: data.value, fail: fail, errormsg:errormsg]


    }
}
