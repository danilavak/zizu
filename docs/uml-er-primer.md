# UML and ER Primer

This note closes the theory part of task 1 and gives a compact reference for the diagrams that will be used in the next stages of the project.

## UML diagrams

UML diagrams are split into two large groups:

- Structural diagrams: show what the system consists of.
- Behavioral diagrams: show how the system behaves over time.

### Structural UML diagrams

- Class diagram: classes, fields, methods, inheritance, associations.
- Component diagram: large modules and their dependencies.
- Deployment diagram: nodes, servers, containers, external systems.
- Package diagram: logical grouping of code and dependencies between groups.

### Behavioral UML diagrams

- Use case diagram: actors and high-level system capabilities.
- Sequence diagram: order of calls between participants over time.
- Activity diagram: flow of actions, branches and parallel steps.
- State diagram: state transitions of a business entity.

## Why they matter for this project

- Sequence diagrams will drive license operations and binary API flows.
- Class and package diagrams help keep the server architecture clean.
- State diagrams are useful for license status and malware signature lifecycle.
- Deployment and component diagrams help describe PostgreSQL, MinIO, CI/CD and service boundaries.

## ER diagrams

ER diagrams describe the relational data model:

- Entities: future database tables.
- Attributes: columns.
- Relationships: links between entities.
- Cardinality: `1:1`, `1:N`, `N:M`.
- Optionality: whether a relationship is required or optional.

## Why ER matters for this project

- License management is built around a relational schema with users, licenses, devices and history.
- Malware signatures require a clear model for current records, history and audit.
- Binary export depends on stable persisted records and timestamps.

## Practical rule for implementation

Before implementing a module:

1. Read the ER diagram to define tables and constraints.
2. Read the sequence diagram to define service methods and transaction boundaries.
3. Only then implement controllers, services, repositories and migrations.
