package com.rundeck.plugin

import com.dtolabs.rundeck.core.authorization.Authorization
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.IProjectInfo
import com.dtolabs.rundeck.core.common.IProjectNodes
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.NodeFileParserException
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Specification
import javax.security.auth.Subject
import com.dtolabs.rundeck.core.authentication.Group
import com.dtolabs.rundeck.core.authentication.Username
import com.dtolabs.rundeck.core.authorization.SubjectAuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext

class EditProjectControllerSpec extends Specification implements ControllerUnitTest<EditProjectController> {

    def setup() {
    }

    def cleanup() {
    }

    def "test getExecutionLater auth"(){
        given:
        String projectName = "Test"

        controller.editProjectService = Mock(EditProjectService){
            getScheduleExecutionLater(_,_)>>[executions:[active:false]]
        }
        controller.frameworkService = new MockFrameworkService(authorizeApplicationResource: true)

        when:
        controller.getExecutionLater(projectName)

        then:

        response.json  != null
        response.json.executions  != null
        response.json.executions  == [active:false]

    }

    def "test getNextExecutionChangeStatus auth"(){
        given:
        String projectName = "Test"

        controller.editProjectService = Mock(EditProjectService){
            nextExecutionTime(_,_)>>[active:false]
        }
        controller.frameworkService = new MockFrameworkService(authorizeApplicationResource: true)

        when:
        controller.getNextExecutionChangeStatus(projectName)

        then:

        response.json  != null
        response.json.execution  != null
        response.json.execution  == [active:false]
        response.json.schedule  != null
        response.json.schedule  == [active:false]

    }

    def "test api apiProjectEnableLater auth"(){
        given:
        String project = "TestProject"
        controller.frameworkService = new MockFrameworkService(authorizeApplicationResource: false)
        controller.apiService = new MockApiService(requireVersion: true)
        controller.editProjectService = Mock(EditProjectService)

        when:
        request.method = "POST"
        request.addHeader('accept', 'application/json')
        controller.apiProjectEnableLater(project)

        then:

        response.status == 403
    }

    def "test api apiProjectDisableLater auth"(){
        given:
        String project = "TestProject"
        controller.frameworkService = new MockFrameworkService(authorizeApplicationResource: false)
        controller.apiService = new MockApiService(requireVersion: true)
        controller.editProjectService = Mock(EditProjectService)

        when:
        request.method = "POST"
        request.addHeader('accept', 'application/json')
        controller.apiProjectDisableLater(project)

        then:

        response.status == 403
    }

    def "test api apiProjectEnableLater method"(){
        given:
        String project = "TestProject"
        controller.frameworkService = new MockFrameworkService(authorizeApplicationResource: true)
        controller.apiService = new MockApiService(requireVersion: true)
        controller.editProjectService = Mock(EditProjectService)


        when:
        request.method = method
        request.addHeader('accept', 'application/json')
        controller.apiProjectEnableLater(project)

        then:

        response.status == statusCode

        where:
        method      | statusCode
        'POST'      | 400
        'GET'       | 405
        'PUT'       | 405
        'DELETE'    | 405

    }

    def "test api apiProjectDisableLater method"(){
        given:
        String project = "TestProject"
        controller.frameworkService = new MockFrameworkService(authorizeApplicationResource: true)
        controller.apiService = new MockApiService(requireVersion: true)
        controller.editProjectService = Mock(EditProjectService)


        when:
        request.method = method
        request.addHeader('accept', 'application/json')
        controller.apiProjectDisableLater(project)

        then:

        response.status == statusCode

        where:
        method      | statusCode
        'POST'      | 400
        'GET'       | 405
        'PUT'       | 405
        'DELETE'    | 405

    }

    def "test api apiProjectDisableLater test"(){
        given:
        String project = "TestProject"
        Properties properties = new Properties()
        properties.put("project.disable.executions","false")
        properties.put("project.disable.schedule","false")

        def rundeckProject = Mock(IRundeckProject){
            getProjectProperties() >> properties
        }

        MockFrameworkService mockFrameworkService = new MockFrameworkService(authorizeApplicationResource: true)
        mockFrameworkService.setRundeckProject(rundeckProject)

        controller.frameworkService = mockFrameworkService
        controller.apiService = new MockApiService(requireVersion: true)
        controller.editProjectService = Mock(EditProjectService)

        when:
        request.method = 'POST'
        request.content = body
        request.addHeader('accept', 'application/json')
        controller.apiProjectDisableLater(project)

        then:

        saveCall*controller.editProjectService.saveExecutionLaterSettings(project, _)>>saved
        response.json  != null
        response.json  == [msg:msg, saved:saved]
        response.status == responseStatus

        where:
        body                                                | saved     | responseStatus | msg                                   | saveCall
        '{"type":"executions","value": "3m"}'.bytes         | true      | 200            | "Project Execution Mode Later saved"  | 1
        '{"type":"schedule","value": "30m"}'.bytes          | true      | 200            | "Project Execution Mode Later saved"  | 1
        '{"type":"executions","value": "3m"}'.bytes         | false     | 200            | "No changed found"                    | 1
        '{"type":"schedule","value": "30m"}'.bytes          | false     | 200            | "No changed found"                    | 1
        '{"type":"sdadasdsa","value": "3m"}'.bytes          | false     | 400            | "Format was not valid, the attribute type must be set with the proper value(executions or schedule)."  | 0
        '{"value": "3m"}'.bytes                             | false     | 400            | "Format was not valid, the attribute type must be set (executions or schedule)."  | 0
        '{"type":"executions","value": "badvalue"}'.bytes   | false     | 400            | "Format was not valid, the attribute value is not set properly. Use something like: 3m, 1h, 3d"  | 0
        '{"type":"schedule"}'.bytes                         | false     | 400            | "Format was not valid, the attribute value must be set."  | 0
        'badvalue'.bytes                                    | false     | 400            | "Format was not valid, the request must be a json object with the format: {\"type\":\"<executions|schedule>\",\"value\":\"<timeExpression>\"}"  | 0
        null                                                | false     | 400            | "Format was not valid, the request must be a json object with the format: {\"type\":\"<executions|schedule>\",\"value\":\"<timeExpression>\"}"  | 0

    }

    def "test api apiProjectEnableLater test"(){
        given:
        String project = "TestProject"
        Properties properties = new Properties()
        properties.put("project.disable.executions","true")
        properties.put("project.disable.schedule","true")

        def rundeckProject = Mock(IRundeckProject){
            getProjectProperties() >> properties
        }

        MockFrameworkService mockFrameworkService = new MockFrameworkService(authorizeApplicationResource: true)
        mockFrameworkService.setRundeckProject(rundeckProject)

        controller.frameworkService = mockFrameworkService
        controller.apiService = new MockApiService(requireVersion: true)
        controller.editProjectService = Mock(EditProjectService)

        when:
        request.method = 'POST'
        request.content = body
        request.addHeader('accept', 'application/json')
        controller.apiProjectEnableLater(project)

        then:

        saveCall*controller.editProjectService.saveExecutionLaterSettings(project, _)>>saved
        response.json  != null
        response.json  == [msg:msg, saved:saved]
        response.status == responseStatus

        where:
        body                                                | saved     | responseStatus | msg                                   | saveCall
        '{"type":"executions","value": "3m"}'.bytes         | true      | 200            | "Project Execution Mode Later saved"  | 1
        '{"type":"schedule","value": "30m"}'.bytes          | true      | 200            | "Project Execution Mode Later saved"  | 1
        '{"type":"executions","value": "3m"}'.bytes         | false     | 200            | "No changed found"                    | 1
        '{"type":"schedule","value": "30m"}'.bytes          | false     | 200            | "No changed found"                    | 1
        '{"type":"sdadasdsa","value": "3m"}'.bytes          | false     | 400            | "Format was not valid, the attribute type must be set with the proper value(executions or schedule)."  | 0
        '{"value": "3m"}'.bytes                             | false     | 400            | "Format was not valid, the attribute type must be set (executions or schedule)."  | 0
        '{"type":"executions","value": "badvalue"}'.bytes   | false     | 400            | "Format was not valid, the attribute value is not set properly. Use something like: 3m, 1h, 3d"  | 0
        '{"type":"schedule"}'.bytes                         | false     | 400            | "Format was not valid, the attribute value must be set."  | 0
        'badvalue'.bytes                                    | false     | 400            | "Format was not valid, the request must be a json object with the format: {\"type\":\"<executions|schedule>\",\"value\":\"<timeExpression>\"}"  | 0
        null                                                | false     | 400            | "Format was not valid, the request must be a json object with the format: {\"type\":\"<executions|schedule>\",\"value\":\"<timeExpression>\"}"  | 0

    }

}

class MockFrameworkService{

    String serverUUID
    IRundeckProject rundeckProject

    boolean authorizeApplicationResource = true
    boolean authorizeApplicationResourceNonAdmin = false
    def projectList
    String frameworkNodeName
    Map frameworkPropertiesMap = [:]
    def getAuthContextForSubjectAndProject(Object a, Object b){
        return new SubjectAuthContext(null, null)
    }

    Map frameworkProjectsTestData = [:]

    def getFrameworkProject(String name) {
        if(rundeckProject){
            return rundeckProject
        }
        frameworkProjectsTestData[name]
    }

    UserAndRolesAuthContext getAuthContextForUserAndRolesAndProject(String user, Collection roles, String project) {
        def sub = new Subject()
        sub.principals = [
                new Username(user),
                new Group("expect:" + project)
        ] + roles.collect {
            new Group(it)
        }
        return new SubjectAuthContext(sub, null)
    }
    def getAuthContextForSubject(Object a){
        return new SubjectAuthContext(null, null)
    }

    def authorizeProjectResources(Object a, Object b, Object c, Object d){
        return []
    }
    def authorizeApplicationResource(Object a, Object b, Object c){
        return authorizeApplicationResource
    }

    def authorizeApplicationResourceAny(Object a, Object b, Object c){
        return authorizeApplicationResource || authorizeApplicationResourceNonAdmin
    }

    def authorizeProjectJobAny(Object a, Object b, Object c, Object d){
        return authorizeApplicationResource
    }

    IRundeckProject getRundeckProject() {
        return rundeckProject
    }

    void setRundeckProject(IRundeckProject rundeckProject) {
        this.rundeckProject = rundeckProject
    }

    def updateFrameworkProjectConfig(String project,Properties properties, Set<String> removePrefixes){
        [success:true]
    }

    boolean isClusterModeEnabled() {
        serverUUID==null?false:true
    }

    def projectNames(){
        projectList
    }

}

