#!/bin/bash

[[ -f SM.jar ]] && rm SM.jar

jar cfm SM.jar Manifest.txt cli/*.class gui/*.class
