package com.rundeck.plugin.jobs

import com.rundeck.plugin.EditProjectService
import org.quartz.InterruptableJob
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException
import org.quartz.UnableToInterruptJobException

class ExecutionStatusJob implements InterruptableJob {

    @Override
    void interrupt() throws UnableToInterruptJobException {

    }

    @Override
    void execute(JobExecutionContext context) throws JobExecutionException {
        String project = context.jobDetail.jobDataMap.get('project')
        String type = context.jobDetail.jobDataMap.get('type')
        Map config = context.jobDetail.jobDataMap.get('config')
        def rundeckProject = context.jobDetail.jobDataMap.get('rundeckProject')

        String executionLaterPath="extraConfig/executionLater.properties"
        EditProjectService editProjectService = fetchEditProjectService(context.jobDetail.jobDataMap)

        //get saved value
        def executionLater = [:]
        def scheduleLater = [:]
        def result = [:]

        if(type == "schedule"){
            config.active = false
            config.action = null
            config.value = null

            executionLater = editProjectService.getExecutionLaterValues(rundeckProject, executionLaterPath)

            result = [executions: executionLater, schedule: config]

            boolean isScheduleDisabledNow = false
            if(config.action == "disable"){
                isScheduleDisabledNow=true
            }
            editProjectService.editProject(rundeckProject, project, isScheduleDisabledNow, false, true)
        }


        if(type == "executions"){
            config.active = false
            config.action = null
            config.value = null

            scheduleLater = editProjectService.getScheduleLaterValues(rundeckProject, executionLaterPath)
            result = [executions: config, schedule: scheduleLater]

            boolean isExecutionDisabledNow = false
            if(config.action == "disable"){
                isExecutionDisabledNow=true
            }
            editProjectService.editProject(rundeckProject, project, isExecutionDisabledNow, true, false)
        }

        def currentStatus = editProjectService.getProjectExecutionStatus(rundeckProject)
        result.global = [executionDisable: currentStatus.isExecutionDisabled, scheduleDisable: currentStatus.isScheduleDisabled]

        editProjectService.saveExecutionLater(rundeckProject, executionLaterPath , result)

    }

    private EditProjectService fetchEditProjectService(def jobDataMap) {
        def es = jobDataMap.get("editProjectService")
        if (es==null) {
            throw new RuntimeException("ExecutionService could not be retrieved from JobDataMap!")
        }
        if (! (es instanceof EditProjectService)) {
            throw new RuntimeException("JobDataMap contained invalid ExecutionService type: " + es.getClass().getName())
        }
        return es

    }
}
