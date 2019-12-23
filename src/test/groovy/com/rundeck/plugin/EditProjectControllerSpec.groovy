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

}

class MockFrameworkService{

    IRundeckProject rundeckProject

    boolean authorizeApplicationResource = true
    boolean authorizeApplicationResourceNonAdmin = false
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
}

