package com.rundeck.plugin

import com.dtolabs.rundeck.core.common.IRundeckProject
import grails.testing.services.ServiceUnitTest
import org.quartz.Scheduler
import org.quartz.Trigger
import spock.lang.Specification

class EditProjectServiceSpec extends Specification implements ServiceUnitTest<EditProjectService>{

    def setup() {
    }

    def cleanup() {
    }


    def "test simple saveExecutionLaterSettings without changes"(){
        given:
        String project = "TestProject"

        def propertiesData = ["project.disable.schedule": "false",
                              "project.disable.executions": "false",
                              "project.later.executions.enable": "false",
                              "project.later.executions.disable": "true",
                              "project.later.executions.enable.value": null,
                              "project.later.executions.disable.value": "1h",
                              "project.later.schedule.enable": "false",
                              "project.later.schedule.disable": "true",
                              "project.later.schedule.enable.value": null,
                              "project.later.schedule.disable.value": "1h",
        ]

        Properties properties = new Properties()
        propertiesData.each {key, value->
            if(value){
                properties.put(key,value)
            }
        }

        String propertiesString = '{"executions":{"active":"false", "action":"disable", "value":"1h"}, "schedule":{"active":"false", "action":"disable", "value":"1h"}}'

        def rundeckProject = Mock(IRundeckProject){
            existsFileResource(_) >> true
            loadFileResource(_, _) >> { args ->
                args[1].write(propertiesString.bytes)
                propertiesString.length()
            }
        }

        def mockFrameworkService = new MockFrameworkService(authorizeApplicationResource: true,
                frameworkProjectsTestData: [
                        TestProject: [projectProperties: propertiesData]
                ]
        )
        mockFrameworkService.setRundeckProject(rundeckProject)


        service.frameworkService  = mockFrameworkService

        when:
        def result = service.saveExecutionLaterSettings(project,properties)
        then:
        result==true


    }

    def "test simple saveExecutionLaterSettings with changes"(){
        given:
        String project = "TestProject"

        def propertiesData = ["project.disable.schedule": "false",
                              "project.disable.executions": "false",
                              "project.later.executions.enable": "false",
                              "project.later.executions.disable": executionLater,
                              "project.later.executions.enable.value": null,
                              "project.later.executions.disable.value": "3h",
                              "project.later.schedule.enable": "false",
                              "project.later.schedule.disable": schedulerLater,
                              "project.later.schedule.enable.value": null,
                              "project.later.schedule.disable.value": "3h",
        ]

        Properties properties = new Properties()
        propertiesData.each {key, value->
            if(value){
                properties.put(key,value)
            }
        }

        String propertiesString = '{"executions":{"active":"false", "action":"disable", "value":"1h"}, "schedule":{"active":"false", "action":"disable", "value":"1h"}}'

        def rundeckProject = Mock(IRundeckProject){
            existsFileResource(_) >> true
            loadFileResource(_, _) >> { args ->
                args[1].write(propertiesString.bytes)
                propertiesString.length()
            }
        }

        def mockFrameworkService = new MockFrameworkService(authorizeApplicationResource: true,
                frameworkProjectsTestData: [
                        TestProject: [projectProperties: propertiesData]
                ]
        )
        mockFrameworkService.setRundeckProject(rundeckProject)


        service.frameworkService  = mockFrameworkService
        service.scheduledExecutionService  = new MockScheduledExecutionService(isScheduledRegister: false)

        service.quartzScheduler = Mock(Scheduler){
            quartzCalls*scheduleJob(_,_)
        }
        when:
        def result = service.saveExecutionLaterSettings(project,properties)
        then:
        result==true

        where:
        executionLater | schedulerLater | quartzCalls
        "false"       | "false"        |      0
        "true"        | "false"        |      1
        "true"        | "true"         |      2


    }

    def "test saveExecutionLaterSettings enable or disable now has changed"(){
        given:
        String project = "TestProject"

        def propertiesData = ["project.disable.schedule": scheduleDisable,
                              "project.disable.executions": executionDisable,
                              "project.later.executions.enable": "false",
                              "project.later.executions.disable": "true",
                              "project.later.executions.enable.value": null,
                              "project.later.executions.disable.value": "1h",
                              "project.later.schedule.enable": "false",
                              "project.later.schedule.disable": "true",
                              "project.later.schedule.enable.value": null,
                              "project.later.schedule.disable.value": "1h",
        ]

        Properties properties = new Properties()
        propertiesData.each {key, value->
            if(value){
                properties.put(key,value)
            }
        }

        String propertiesString = '{"global": {"executionDisable":false,"scheduleDisable":false},"executions":{"active":false, "action":"disable", "value":"1h"}, "schedule":{"active":false, "action":"disable", "value":"1h"}}'

        def rundeckProject = Mock(IRundeckProject){
            existsFileResource(_) >> true
            loadFileResource(_, _) >> { args ->
                args[1].write(propertiesString.bytes)
                propertiesString.length()
            }
        }

        def mockFrameworkService = new MockFrameworkService(authorizeApplicationResource: true,
                frameworkProjectsTestData: [
                        TestProject: [projectProperties: propertiesData]
                ]
        )
        mockFrameworkService.setRundeckProject(rundeckProject)

        service.frameworkService  = mockFrameworkService
        service.scheduledExecutionService  = new MockScheduledExecutionService(isScheduledRegister: false)

        service.quartzScheduler = Mock(Scheduler){
            quartzCalls*getTrigger(_) >> Mock(org.quartz.Trigger)
            quartzCalls*deleteJob(_)
        }
        when:
        def result = service.saveExecutionLaterSettings(project,properties)
        then:
        result==true

        where:
        executionDisable | scheduleDisable | quartzCalls
        "false"          | "false"         |     0
        "true"           | "false"         |     1
        "true"           | "true"          |     2


    }



}

class MockScheduledExecutionService{

    boolean isScheduledRegister = false

    def hasJobScheduled(String jobName, String groupName){
        isScheduledRegister
    }
}
