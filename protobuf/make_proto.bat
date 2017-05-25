@echo off
SET CSHARP_OUT=..\TorchDesktop\TorchDesktop\Networking\Protos
SET JAVA_OUT=..\TorchMobile\app\src\main\java
SET PYTHON_OUT=..\TorchServer

protoc -I=. --java_out=%JAVA_OUT% --csharp_out=%CSHARP_OUT% %*
