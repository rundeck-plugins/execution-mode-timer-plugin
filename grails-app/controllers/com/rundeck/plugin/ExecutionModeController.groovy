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
            getNextExecutionChangeStatus: 'GET'
    ]

    def executionModeService
    def frameworkService

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
}
