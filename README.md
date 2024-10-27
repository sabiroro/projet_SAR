# SAR's project

This repo contains all files needed to the Eclipse's project for building and launching the project from the IDE.
This project is an implementation of a communication protocol, one with brokers and channels ; and the other with an overlay of that with messagequeues and queuebrokers.
Both implementations are based on full-event programming.

## Group members
Here is member names of the group:
- ROSANO Romain (rosanor)
- CHERBLANC Noah (cherblan)
- PICAUD Nicolas (picaudn)
- ROUX Yann (rouxya)

## Git structure
The project is divided is two parts as saying before. Each part has different branches associated with it:
- for channel and broker part, the branch are named with the root "**channel**".
- for the overlay part, the branch are named with the root "**queue**".
- the master branch is the main branch, and contains the last version of the project.

To access different branches, you can checkout into them. The branches' name are:
- root_name.specification: the branch containing the specification of the part concerning saying how to use the API.
- root_name.design: the branch containing the design of the part concerning saying how the implementation is done.
- root_name.implementation: the branch containing the implementation of the part concerning.
- root_name.tests: the branch containing the tests of the part concerning.

## Division of the project
The project is divided is two parts as saying before. So to access them:
- for channel and broker part, the code is in different packages naming with the same root name "**channel.**", like : "channel.API", "channel.implem", "channel.tests", etc.
- for the overlay part, as before, the code is in different packages naming with the same root name "**queue.**", like : "queue.API", "queue.implem", "queue.tests", etc.  

The API package contains the interfaces of the part concerning.  
The implem package contains the implementation of the interfaces, and eventually, child packages naming "root_name. **implem.** sub_impl_package_name".  
The tests package contains the tests of the part concerning.

This project being mainly an API project, there is no main method. However, there are tests that can be run to check the proper functioning of the project.

## How to launch project?
To launch the project, you can use the programmed tests (connection tests, echo server (simple and with multiple clients), simple timeout test (with a breakdown simulation of the server), auto-ping...):
- Open the project with Eclipse
- Go on "Test" class of the part you want to test (channel or queue).
- Run the main method
  - When tests are running, you can see pieces of information in the console, like the test name, the test result, errors eventually caught, etc.
  - The message "TestX done" will be displayed when the test are *really* finished (really, because of semaphore's use in test when the expected result is obtained).
    > Note: When all tests are done, the message "That's all folks" will be displayed.
  - You can eventually see what tests are testing by looking the comments above the test method.