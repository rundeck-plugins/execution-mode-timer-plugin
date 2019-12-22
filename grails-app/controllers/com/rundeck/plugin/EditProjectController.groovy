package com.rundeck.plugin

import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.IRundeckProject
import grails.converters.JSON

import javax.servlet.http.HttpServletResponse

class EditProjectController {

    static final String ACTION_ADMIN = "admin"
    static final String RES_TYPE_SYSTEM = "system"

    static allowedMethods = [
            getExecutionLater: 'GET',
    ]

    def frameworkService
    def editProjectService

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

    def getExecutionLater(String project) {
        if (!authorizeSystemAdmin()) {
            return
        }

        String executionLaterPath="extraConfig/executionLater.properties"
        IRundeckProject rundeckProject =  frameworkService.getFrameworkProject(project)
        Map result = editProjectService.getScheduleExecutionLater(rundeckProject, executionLaterPath)

        render(
                result as JSON,
                contentType: 'application/json'
        )
    }

    def getNextExecutionChangeStatus(String project){
        if (!authorizeSystemAdmin()) {
            return
        }

        def executionStatus = editProjectService.nextExecutionTime(project,"executions")
        def scheduleStatus = editProjectService.nextExecutionTime(project,"schedule")

        render(
                [execution: executionStatus, schedule: scheduleStatus] as JSON,
                contentType: 'application/json'
        )
    }

}
