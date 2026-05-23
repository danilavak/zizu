# Клиентская часть

Здесь лежит отдельный Windows-клиент, который соответствует нижней части общей схемы и не смешивается с серверным кодом.

Текущий стек:

- `C++`
- `WinUI 3`
- `MSBuild` на конвейере

Локальная сборка в Visual Studio:

```powershell
nuget restore .\windows-client\packages.config -PackagesDirectory .\windows-client\packages
msbuild .\windows-client\zizu-windows-client.vcxproj /p:Configuration=Release /p:Platform=x64
```

Скрытый запуск:

```powershell
.\windows-client\x64\Release\ZizuWindowsClient.exe --hidden
```
