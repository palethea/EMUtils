$ErrorActionPreference = 'Stop'

$repo = Split-Path -Parent $PSScriptRoot
$env:JAVA_HOME = 'C:\Users\matti\.jdks\jdk-25.0.3+9'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Push-Location $repo
try {
    java -classpath '.\gradle\wrapper\gradle-wrapper.jar' org.gradle.wrapper.GradleWrapperMain genEclipseRuns -PmcFamily='26.x' -PmcVersion='26.1.2'
}
finally {
    Pop-Location
}
