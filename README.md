# Enable/Disable Execution/Scheduled Later plugin

This plugin adds to Rundeck the functionality to enable/disable the execution/schedule for X time. This can be set at a project or the system level.

## Install

* to build the plugin run: 
 
```
gradle clean build
```

*  copy grails plugin to `$RDECK_BASE/server/lib`
```
cp build/libs/enable-later-executions-plugin-X.Y.Z.jar $RDECK_BASE/server/lib
```

*  copy UI plugin to `$RDECK_BASE/libext`
```
cp ui-enable-execution-later/build/distributions/ui-execution-mode-later-X.Y.Z.zip $RDECK_BASE/libext
```

## How to use it

### Global Execution Mode

Global Execution mode enables/disables executions and schedules for all Rundeck system.

When you install the `execution enable/disable later` plugin, 
you will see an extra form that allows you to enable/disable the execution after X hours, which will enable or disable the executions later (defining a time variable like `30s`, `30m`,`4h`,`2d`)

![Gloabal Form](docs/global-execution-later-form.png?raw=true "Title")


* To disable the execution after X, the execution mode must be Active

![Gloabal Form](docs/global-execution-later-disable.png?raw=true "Title")


* To enable the execution after X, the execution mode must be Passive

 ![Gloabal Form](docs/global-execution-later-enable.png?raw=true "Title")

This process registers an internal trigger that will be executed after X time passed from the moment is saved. 
If Rundeck is restarted, the trigger will be registered again in the startup process (in case the target date is still valid).

Finally, a message on the Rundeck's home page will be displayed if the trigger is registered

 ![Gloabal Form](docs/globa-execution-message.png?raw=true "Title")


### Project Execution Mode

When you install the plugin, 
you will see an extra form on the `Configure Project / Execution Mode` page,  that will allow you to enable or disable the executions/scheduled after X time 
(defining a time variable like `30s`, `30m`,`4h`,`2d`)

 ![Gloabal Form](docs/project-execution-later-form.png?raw=true "Title")

* to enable/disable the project execution after X time, you will need to check the `Enable/Disable Execution Later` checkbox, and the set a time value.

 ![Gloabal Form](docs/project-execution-later-disable-executions.png?raw=true "Title")

* to enable/disable the project scheduled later, you will need to check the `Enable/Disable Schedule Later` checkbox, and the set a time value.

 ![Gloabal Form](docs/project-execution-later-disable-scheduled.png?raw=true "Title")

When a trigger event is enabled, a message will be displayed on the project's home page

 ![Gloabal Form](docs/project-execution-later-msg.png?raw=true "Title")

If any of the project status is changed (for example passed from disable to enable executions) and aan execution later trigger is scheduled, the trigger will be removed.

If Rundeck is restarted, the triggers will be registered again in the startup process (if the date has not been reached).

## API 

This plugin enable the following endpoints

The calendar feature has some APIs:

### System Enable Execution Later

**Request**:

POST  /api/34/system/executions/enable/later

**Request Content**:

Content-Type: application/json

**Request Body**:

* value: Time value after the execution mode will be enabled (example: 2h, 2m, 30s)

Body Example:

```
{"value":"3m"}
```

CURL example:
```
curl -X POST --header "Content-Type: application/json" -H "X-Rundeck-Auth-Token: XXX"  http://rundeck:4440/api/34/system/executions/enable/later --data '{"value":"1m"}'
```


### System Disable Execution Later

**Request**:

POST  /api/34/system/executions/disable/later

**Request Content**:

Content-Type: application/json

**Request Body**:

* value: Time value after the execution mode will be disabled (example: 2h, 2m, 30s)

Body Example:

```
{"value":"3m"}
```

CURL example:
```
curl -X POST --header "Content-Type: application/json" -H "X-Rundeck-Auth-Token: XXX"  http://rundeck:4440/api/34/system/executions/disable/later --data '{"value":"1m"}'
```


### Project Enable Execution Later

**Request**:

POST  /api/34/project/<PROJECT>/enable/later

**Request Content**:

Content-Type: application/json

**Request Body**:

* type: Type that will be enabled: executions, schedule
* value: Time value after the execution mode will be enabled (example: 2h, 2m, 30s)

Body Example:

```
{"type":"schedule","value":"10m"}
```

CURL example:
```
curl -X POST --header "Content-Type: application/json" -H "X-Rundeck-Auth-Token: XXXXX"  http://rundeck:4440/api/34/project/<PROJECT>/enable/later --data '{"type":"schedule","value":"10m"}'
```


### Project Disable Execution Later

**Request**:

POST  /api/34/project/<PROJECT>/disable/later

**Request Content**:

Content-Type: application/json

**Request Body**:

* type: Type that will be disabled: executions, schedule
* value: Time value after the execution mode will be disabled (example: 2h, 2m, 30s)

Body Example:

```
{"type":"executions","value":"10m"}
```

CURL example:
```
curl -X POST --header "Content-Type: application/json" -H "X-Rundeck-Auth-Token: XXXXX"  http://rundeck:4440/api/34/project/<PROJECT>/disable/later --data '{"type":"executions","value":"10m"}'
```

## Rundeck CLI

For now, download the BETA version that include the execution mode later API [here](https://github.com/robertopaez/rundeck-cli/releases/tag/v1.1.9)

### System Enable Execution Later

```
rd system mode activelater 

Set execution mode Active Later

Usage: activeLater options
	[--quiet -q] : Reduce output.
	--timeValue -t value : Set the time value where the execution will be enable/disable. For example: 3m, 1h, 5d
```

### System Disable Execution Later

```
rd system mode disablelater

Set execution mode Disable Later

Usage: activeLater options
	[--quiet -q] : Reduce output.
	--timeValue -t value : Set the time value where the execution will be enable/disable. For example: 3m, 1h, 5d
```
### Project Enable Execution Later

```
rd projects mode enablelater

enable executions/schedule mode later

Usage: enableLater options
	--project -p value : Project Name
	[--quiet -q] : Reduce output.
	--timeValue -v value : Set the time value where the execution will be enable/disable. For example: 3m, 1h, 5d
	--type -t value : Type: executions or schedule

```

### Disable Enable Execution Later

```
rd projects mode disablelater

disable executions/schedule mode later

Usage: enableLater options
	--project -p value : Project Name
	[--quiet -q] : Reduce output.
	--timeValue -v value : Set the time value where the execution will be enable/disable. For example: 3m, 1h, 5d
	--type -t value : Type: executions or schedule
```

