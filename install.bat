@ECHO OFF

.\prunsrv.exe //DS//CUSTOMHWINFO

.\prunsrv.exe //IS//CUSTOMHWINFO --DisplayName="AACustom HWInfo" --Description="Process data from COM port and write it to registry" ^
--Install="%~dp0prunsrv.exe" --Jvm="C:\Program Files\Zulu\zulu-17\bin\server\jvm.dll" ^
++JvmOptions=-Dservice.config="%~dp0service.properties" ^
--Classpath="%~dp0TestService-0.2-jar-with-dependencies.jar" --LogLevel=DEBUG ^
--LogPath=d:\pets --LogPrefix=testService.log --Startup=manual --StdOutput=%~dp0stdout.log ^
--StdError=%~dp0stderr.log --JvmMs=1 --JvmMx=10 --StartMode=jvm --StartClass=org.example.ServiceLauncher ^
--StartMethod=windowsService ++StartParams="start" --StopMode=jvm --StopClass=org.example.ServiceLauncher ^
--StopMethod=windowsService ++StopParams="stop" --JavaHome="C:\Program Files\Zulu\zulu-17"

PAUSE
