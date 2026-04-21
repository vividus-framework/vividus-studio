!-- SYNTAX TEST "source.vividus"
Description: Test demoing VIVIDUS capabilities for Web Applications
!-- <------------ vividus.keyword
!--         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ comment.line
!-- This scenario verifies login functionality
!-- <---------------------------------------------- comment.line
Meta:
!-- <----- vividus.meta
 @author vividus
!-- <~------- vividus.meta.key
!--      ^^^^^^^ vividus.meta.value
 @severity critical
!-- <~--------- vividus.meta.key
!--        ^^^^^^^^ vividus.meta.value
Lifecycle:
!-- <---------- vividus.keyword
Before:
!-- <------- vividus.keyword
Scope: STORY
!-- <------ vividus.keyword
!--    ^^^^^ vividus.keyword.attribute
Given I am on page with URL `https://the-internet.herokuapp.com/login`
!-- <----- vividus.step.type
!--  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ vividus.step.wording
After:
!-- <------ vividus.keyword
Scope: STORY
!-- <------ vividus.keyword
!--    ^^^^^ vividus.keyword.attribute
Outcome: ANY
!-- <-------- vividus.keyword
!--      ^^^ vividus.keyword.attribute
Then the browser is closed
!-- <---- vividus.step.type
!-- ^^^^^^^^^^^^^^^^^^^^^^ vividus.step.wording
Scenario: Test login
!-- <--------- vividus.scenario.keyword
!--      ^^^^^^^^^^^ vividus.scenario.title
GivenStories: /story/setup/Authentication.story
!-- <------------- vividus.keyword
!--          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ vividus.keyword.attribute
Given I am on page with URL `https://the-internet.herokuapp.com/login`
!-- <----- vividus.step.type
!--  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ vividus.step.wording
Then text `Login Page` exists
!-- <---- vividus.step.type
!-- ^^^^^^^^^^^^^^^^^^^^^^^^^ vividus.step.wording
When I enter `tomsmith` in field located by `fieldName(username)`
!-- <---- vividus.step.type
!-- ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ vividus.step.wording
When I click on element located by `buttonName(submit)`
!-- <---- vividus.step.type
!-- ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ vividus.step.wording
Then text `You logged into a secure area!` exists
!-- <---- vividus.step.type
!-- ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ vividus.step.wording
Scenario: Test login with multiple users
!-- <--------- vividus.scenario.keyword
!--      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ vividus.scenario.title
Given I am on page with URL `https://the-internet.herokuapp.com/login`
!-- <----- vividus.step.type
!--  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ vividus.step.wording
When I enter `<username>` in field located by `fieldName(username)`
!-- <---- vividus.step.type
!-- ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ vividus.step.wording
When I click on element located by `buttonName(submit)`
!-- <---- vividus.step.type
!-- ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ vividus.step.wording
Then text `<result>` exists
!-- <---- vividus.step.type
!-- ^^^^^^^^^^^^^^^^^^^^^^^ vividus.step.wording
Examples:
!-- <--------- vividus.keyword
|username|password|result|
!-- <- vividus.keyword.attribute
|tomsmith|SuperSecretPassword!|You logged into a secure area!|
!-- <- vividus.keyword.attribute
