package com.rundeck.plugin

import com.rundeck.plugin.jobs.SystemExecutionModeJob
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.time.TimeCategory
import groovy.time.TimeDuration
import org.quartz.JobBuilder
import org.quartz.JobDataMap
import org.quartz.JobDetail
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.TriggerKey

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

class ExecutionModeService{
    public static final String EXECUTION_MODE_STORAGE_PATH_BASE = 'executionMode/'
    static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
    public static final String EXECUTIONS_JOB_GROUP_NAME = 'SystemExecutionLater'

    def executionService
    def configStorageService
    def scheduledExecutionService
    def Scheduler quartzScheduler


    def saveExecutionModeLater(def config) {

        //last status
        def requestActive=executionService.executionsAreActive

        def newSettings = convert(config)

        def storagePath = EXECUTION_MODE_STORAGE_PATH_BASE + "executionModeLater.properties"
        def savedSettings = getConfig(storagePath)

        boolean newExecutionMode = false
        boolean save = false

        if(savedSettings){
            if(requestActive != savedSettings?.executionsAreActive){
                newExecutionMode = true
                save = true
                savedSettings.executionsAreActive = requestActive
            }
        }


        if(newExecutionMode){
            removeScheduleJob()
            if(savedSettings?.active){
                savedSettings.active = false
            }
        }

        //check if something changed
        //compare
        def compareLater = { Map savedValue, Map newValue->
            boolean newData = false

            if(newValue.isEmpty()){
                return false
            }

            if(!savedValue){
                newData = true
            }else{
                //compare if something change to save it
                newData = compareLaterData(savedValue, newValue)
            }

            if(newData){
                newValue.active = true
                Date date = new Date()
                DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
                newValue.dateSaved = dateFormat.format(date)
                return true
            }

            return false
        }

        if(compareLater(savedSettings,newSettings )){
            save = true
            newSettings.executionsAreActive = requestActive
            scheduleExecutionsLaterJob([config: newSettings,
                                        executionModeService: this])

        }else{
            newSettings = savedSettings
        }

        if(save) {
            saveConfig(storagePath, newSettings)
        }

        return save

    }

    def convert(Map config){
        boolean activeLater = false
        boolean passiveLater = false
        String activeLaterValue = ""
        String passiveLaterValue = ""

        if(config.activeLater){
            activeLater = true
        }

        if(config.activeLaterValue){
            activeLaterValue = config.activeLaterValue
        }

        if(config.passiveLater){
            passiveLater = true
        }

        if(config.passiveLaterValue){
            passiveLaterValue = config.passiveLaterValue
        }

        def result = [:]

        if(activeLater){
            result = [action: "enable", value: activeLaterValue ]
        }

        if(passiveLater){
            result = [action: "disable", value: passiveLaterValue ]
        }

        return result
    }

    def compareLaterData(Map savedSetting, Map newSettings){
        def savedCompare = [action: savedSetting.action, value: savedSetting.value ]

        if(!savedCompare.equals(newSettings)){
            return true
        }else{
            return false
        }
    }

    def saveConfig(String storagePath, Map data){
        def mapAsJson = JsonOutput.toJson(data)
        configStorageService.writeFileResource(storagePath, new ByteArrayInputStream(mapAsJson.getBytes('UTF-8')), [:])
    }

    def getConfig(String storagePath){
        if(configStorageService.existsFileResource(storagePath)){
            Map executionLater

            try {
                def resource = configStorageService.getFileResource(storagePath)
                executionLater = new JsonSlurper().parseText(resource.contents.inputStream.getText())
            } catch (Exception e) {
                return null
            }

            return executionLater
        }else{
            return null
        }
    }

    def getExecutionModeLater(){
        def storagePath = EXECUTION_MODE_STORAGE_PATH_BASE + "executionModeLater.properties"
        def savedSettings = getConfig(storagePath)

        if(savedSettings){
            return savedSettings
        }else{
            return [:]
        }
    }

    Date scheduleExecutionsLaterJob(Map data) {
        Date nextTime
        Date startAt = PluginUtil.laterDate(data.config.dateSaved, data.config.value, DATE_FORMAT)

        String jobName = "execution-mode-later"
        def trigger = PluginUtil.createTrigger(jobName, EXECUTIONS_JOB_GROUP_NAME, startAt)
        JobDetail jobDetail = createExecutionLaterJobDetail(jobName,
                                                                       EXECUTIONS_JOB_GROUP_NAME,
                                                                       data,
                                                                       "Execution Later System Execution Mode")

        if ( scheduledExecutionService.hasJobScheduled(jobName, EXECUTIONS_JOB_GROUP_NAME) ) {
            log.info("rescheduling existing schedule/execution enable/disable system execution mode")

            nextTime = quartzScheduler.rescheduleJob(TriggerKey.triggerKey(jobName, EXECUTIONS_JOB_GROUP_NAME), trigger)
        } else {
            log.info("scheduling new schedule/execution later enable/disable system execution mode")
            nextTime = quartzScheduler.scheduleJob(jobDetail, trigger)
        }

        log.info("scheduled schedule/execution job next run: " + nextTime.toString())
        return nextTime
    }

    def createExecutionLaterJobDetail(String jobname, String jobgroup, Map config , String description) {
        def jobDetailBuilder = JobBuilder.newJob(SystemExecutionModeJob)
                .withIdentity(jobname, jobgroup)
                .withDescription(description)
                .usingJobData(new JobDataMap(config))

        return jobDetailBuilder.build()
    }

    def setExecutionsAreActive(boolean active){
        executionService.setExecutionsAreActive(active)
    }

    def nextExecutionTime() {

        def settings = this.getExecutionModeLater()

        def getNextExecution = { String jobName->
            def trigger = quartzScheduler.getTrigger(TriggerKey.triggerKey(jobName , EXECUTIONS_JOB_GROUP_NAME))
            if(trigger){
                Date date = trigger.getNextFireTime()
                Date now = new Date()

                TimeDuration duration = TimeCategory.minus(date, now)

                String msg

                if(duration.days != 0){
                    msg = "Execution will be ${settings.action} in ${duration.days} days, ${duration.hours} hrs, ${duration.minutes} min ."
                }else if(duration.hours != 0){
                    msg = "Execution will be ${settings.action} in ${duration.hours} hrs, ${duration.minutes} min."
                }else if(duration.minutes != 0){
                    msg = "Execution will be ${settings.action} in ${duration.minutes} min."
                }else{
                    msg = "Execution will be ${settings.action} in ${duration.seconds} sec."
                }

                return msg
            } else {
                return null
            }
        }

        if(!settings){
            return [active: false, action: null, msg: null]
        }

        if(!settings.active){
            return [active: false, action: null, msg: null]
        }

        String jobName = "execution-mode-later"
        def nextExecution = getNextExecution(jobName)

        if(nextExecution){
            return [active: true, action: settings.action, msg: nextExecution]
        }else{
            return [active: false,action: null,  msg: null]
        }
    }

    def removeScheduleJob(){
        String jobName = "execution-mode-later"

        def trigger = quartzScheduler.getTrigger(TriggerKey.triggerKey(jobName , EXECUTIONS_JOB_GROUP_NAME))
        if(trigger){
            quartzScheduler.deleteJob(new JobKey(jobName, EXECUTIONS_JOB_GROUP_NAME))
        }
    }

    void initProcess() {

        try{
            def storagePath = EXECUTION_MODE_STORAGE_PATH_BASE + "executionModeLater.properties"
            def savedSettings = getConfig(storagePath)

            if(savedSettings?.active){
                Date startAt = PluginUtil.laterDate(savedSettings.dateSaved, savedSettings.value, DATE_FORMAT)
                Date now = new Date()
                def difference = PluginUtil.getDateDiff(now, startAt, TimeUnit.MINUTES)

                if(difference>0){
                    scheduleExecutionsLaterJob([config: savedSettings,
                                                executionModeService: this])
                }else{
                    savedSettings.active=false
                    savedSettings.action=null
                    savedSettings.value=null

                    saveConfig(storagePath, savedSettings)
                }
            }
        }catch(Exception e){
            log.warn("error initProcess: ${e.message}")
        }
    }

    def getCurrentStatus(){
        executionService.executionsAreActive
    }
}
