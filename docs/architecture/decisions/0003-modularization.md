# 3. Modularization

Date: 2019-12-10

## Status

Accepted

## Context

Riptide started as a monolithic project. That worked good enough as long as there were only few features and minimal dependencies. But with a growing feature set several issues came up:

1. The single artifact became bloated and it was hard to tell core functionality apart from additional, optional features.
2. Picking features *à la carte* was unnecesarily hard since it usually involved the need to carefully include/exclude optional dependencies.

## Decision

In order to counter both of these aspects Riptide is fundamentally build on the idea of modules. That manifests itself in two aspects:

1. Each module is a sub-directory, hosting a Maven sub-module, containing a separate package with a dedicated README.
2. The core has several API extension points or SPIs that encourage modularization:
   1. The `Plugin` interface to extend the processing pipeline of requests and responses
   2. The `Route` interface to build, compose and reuse response handling functions.
   3. The `Navigator` interface to build custom routing algorithms.
   4. The `Attribute` interface to provide public interaction points between plugins (usually).

The criteria for when to extract something into a separate module include but are not limited to:

- Isolate rarely used dependencies to ideally a single module
- Group functionality logically (Single Responsibility Principle)
- Different maturity level and therefore higher probabilty to change (Open/Closed Principle)

## Consequences

Having multiple modules, each with a unique but distinct purpose, allows users to pick those modules that provide something they really need. It also keeps the list of dependencies small and manageable. Another side-effect of having modules is that by exposing SPIs, even if it's initually just for our own modules, opens up the possibility for users to provide their own extensions.

One aspect to keep in mind though is that interactions between modules are now slightly harder to implement since they always have to consider the fact that the other module is not there. 

