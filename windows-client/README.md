# Windows Client

This directory is reserved for the Windows client part of the RBPO system shown in the architecture diagram.

Planned scope:

- tray application for Windows
- authentication and license client module
- update module for manifest verification and download
- local signature storage
- future integration with the server from the repository root

The client will be implemented as an isolated subproject with its own build pipeline and pull requests that touch only `windows-client/**`.
