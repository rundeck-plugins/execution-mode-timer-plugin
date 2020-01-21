package com.rundeck.plugin

import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.common.IRundeckProject
import grails.converters.JSON

import javax.servlet.http.HttpServletResponse

class EditProjectController {

    static final String ACTION_ADMIN = "admin"
    static final String RES_TYPE_SYSTEM = "system"

    static allowedMethods = [
            getExecutionLater: 'GET',
            apiProjectDisableLater: 'POST',
            apiProjectEnableLater: 'POST',

    ]

    def frameworkService
    def updateModeProjectService
    def apiService

    def boolean requireAuth(String project) {

        def authContext =
                frameworkService.getAuthContextForSubjectAndProject(session.subject, project)
        if (!frameworkService.authorizeApplicationResource(
                authContext,
                AuthorizationUtil.resourceType(RES_TYPE_SYSTEM),
                ACTION_ADMIN
        )) {
            request.errorCode = 'request.error.unauthorized.message'
            request.errorArgs = ['Calendar (admin)', 'Server']
            response.status = HttpServletResponse.SC_FORBIDDEN
            request.titleCode = 'request.error.unauthorized.title'

            render(view: "/common/error", model: [:])
            return false
        }
        return true
    }

    def getExecutionLater(String project) {
        if (!requireAuth(project)) {
            return
        }
        String executionLaterPath="extraConfig/executionLater.properties"
        IRundeckProject rundeckProject =  frameworkService.getFrameworkProject(project)
        Map result = updateModeProjectService.getScheduleExecutionLater(rundeckProject, executionLaterPath)

        render(
                result as JSON,
                contentType: 'application/json'
        )
    }

    def getNextExecutionChangeStatus(String project){
        if (!requireAuth(project)) {
            return
        }

        def executionStatus = updateModeProjectService.nextExecutionTime(project,"executions")
        def scheduleStatus = updateModeProjectService.nextExecutionTime(project,"schedule")

        render(
                [execution: executionStatus, schedule: scheduleStatus] as JSON,
                contentType: 'application/json'
        )
    }

    def apiProjectEnableLater(String project){
        if (!requireAuth(project)) {
            return
        }

        if (!apiService.requireVersion(request, response, PluginUtil.V34)) {
            return
        }

        def result = validateApi(request, response)
        def saved = false
        def msg = ""

        if(!result.fail){
            Properties projProps = new Properties()
            Set removePrefixes=[]

            IRundeckProject rundeckProject =  frameworkService.getFrameworkProject(project)
            Map properties = rundeckProject.getProjectProperties()
            projProps.putAll(properties)

            //check if current status of project
            def isExecutionDisabledNow = properties[UpdateModeProjectService.CONF_PROJECT_DISABLE_EXECUTION] == 'true'
            def isScheduleDisabledNow = properties[UpdateModeProjectService.CONF_PROJECT_DISABLE_SCHEDULE] == 'true'

            boolean canSave = true

            if(result.config.type == "executions"){

                if(!isExecutionDisabledNow){
                    canSave=false
                    saved=false
                    msg="Executions are already enabled, cannot disable later"
                }

                projProps.put("project.later.executions.enable", "true")
                projProps.put("project.later.executions.enable.value", result.config.value)

                removePrefixes.add("project.later.executions")

            }else{

                if(!isScheduleDisabledNow){
                    canSave=false
                    saved=false
                    msg="Schedule are already enabled, cannot enable later"
                }

                projProps.put("project.later.schedule.enable", "true")
                projProps.put("project.later.schedule.enable.value", result.config.value)

                removePrefixes.add("project.later.schedule")
            }

            if(canSave){
                //save project settings
                frameworkService.updateFrameworkProjectConfig(project, projProps, removePrefixes)

                saved = updateModeProjectService.saveExecutionLaterSettings(project, projProps)
                if(saved){
                    msg = "Project Execution Mode Later saved"
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


    def apiProjectDisableLater(String project){

        if (!requireAuth(project)) {
            return
        }

        if (!apiService.requireVersion(request, response, PluginUtil.V34)) {
            return
        }

        def result = validateApi(request, response)
        def saved = false
        def msg = ""

        if(!result.fail){
            Properties projProps = new Properties()
            Set removePrefixes=[]

            IRundeckProject rundeckProject =  frameworkService.getFrameworkProject(project)
            Map properties = rundeckProject.getProjectProperties()
            projProps.putAll(properties)

            //check if current status of project
            def isExecutionDisabledNow = properties[UpdateModeProjectService.CONF_PROJECT_DISABLE_EXECUTION] == 'true'
            def isScheduleDisabledNow = properties[UpdateModeProjectService.CONF_PROJECT_DISABLE_SCHEDULE] == 'true'

            boolean canSave = true

            if(result.config.type == "executions"){
                if(isExecutionDisabledNow){
                    canSave=false
                    saved=false
                    msg="Execution are already disabled, cannot disable later"
                }

                projProps.put("project.later.executions.disable", "true")
                projProps.put("project.later.executions.disable.value", result.config.value)

                removePrefixes.add("project.later.executions")
            }else{
                if(isScheduleDisabledNow){
                    canSave=false
                    saved=false
                    msg="Schedule are already disabled, cannot disable later"
                }

                projProps.put("project.later.schedule.disable", "true")
                projProps.put("project.later.schedule.disable.value", result.config.value)

                removePrefixes.add("project.later.schedule")
            }

            if(canSave){
                //save project settings
                frameworkService.updateFrameworkProjectConfig(project, projProps, removePrefixes)

                saved = updateModeProjectService.saveExecutionLaterSettings(project, projProps)
                if(saved){
                    msg = "Project Execution Mode Later saved"
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
        def config = null

        try{
            config = request.JSON
        }catch(Exception e){
            errormsg = e.message
        }

        //review config value
        if(!config){
            fail=true
            errormsg = 'Format was not valid, the request must be a json object with the format: {"type":"<executions|schedule>","value":"<timeExpression>"}'
            return [config: config, fail: fail, errormsg:errormsg]

        }

        if(!config.type){
            fail=true
            errormsg = "Format was not valid, the attribute type must be set (executions or schedule)."
            return [config: config, fail: fail, errormsg:errormsg]

        }

        if(config.type){
            def allowedValues = ["executions","schedule"]
            if(!allowedValues.contains(config.type)){
                fail=true
                errormsg = "Format was not valid, the attribute type must be set with the proper value(executions or schedule)."
                return [config: config, fail: fail, errormsg:errormsg]

            }
        }

        if(!config.value){
            fail=true
            errormsg = "Format was not valid, the attribute value must be set."
            return [config: config, fail: fail, errormsg:errormsg]

        }

        if(config.value){
            if(!PluginUtil.validateTimeDuration(config.value)){
                fail=true
                errormsg = "Format was not valid, the attribute value is not set properly. Use something like: 3m, 1h, 3d"
                return [config: config, fail: fail, errormsg:errormsg]

            }
        }

        return [config: config, fail: fail, errormsg:errormsg]


    }

}
