!-- SYNTAX TEST "source.vividus"
Description: Sample story description
!-- <------------ vividus.keyword
!--             ^^^^^^^^^^^^^^^^^^^^^^^^^ comment.line
!-- A plain comment line
!-- <------------------------ comment.line
Meta:
!-- <----- vividus.meta
 @tag value
!-- <~---- vividus.meta.key
!--   ^^^^^ vividus.meta.value
Scenario: Basic scenario
!-- <--------- vividus.scenario.keyword
!--          ^^^^^^^^^^^^^^^ vividus.scenario.title
Given I open the browser
!-- <----- vividus.step.type
!--       ^^^^^^^^^^^^^^^^^^^ vividus.step.wording
When I navigate to the page
!-- <---- vividus.step.type
!-- ^^^^^^^^^^^^^^^^^^^^^^^ vividus.step.wording
Then the title is correct
!-- <---- vividus.step.type
!-- ^^^^^^^^^^^^^^^^^^^^ vividus.step.wording
And the URL is valid
!-- <--- vividus.step.type
!--^^^^^^^^^^^^^^^^^ vividus.step.wording
GivenStories: path/to/story.story
!-- <------------- vividus.keyword
!--          ^^^^^^^^^^^^^^^^^^^^ vividus.keyword.attribute
Examples:
!-- <--------- vividus.keyword
|col1|col2|
!-- <- vividus.keyword.attribute
|val1|val2|
!-- <- vividus.keyword.attribute
Lifecycle:
!-- <---------- vividus.keyword
Before:
!-- <------- vividus.keyword
Scope: STORY
!-- <------ vividus.keyword
!--    ^^^^^ vividus.keyword.attribute
After:
!-- <------ vividus.keyword
Outcome: ANY
!-- <-------- vividus.keyword
!--      ^^^ vividus.keyword.attribute
Given I perform cleanup
!-- <----- vividus.step.type
!--  ^^^^^^^^^^^^^^^^^^ vividus.step.wording
