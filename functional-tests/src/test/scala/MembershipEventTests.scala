

class MembershipEventTests extends BaseMembershipTest {

  info("Tests for sprint 1 of the Membership project")

  feature("See event list") {

    /*
     I order to view an event's details
     As a user
     I want to see a list of events
     */
    scenarioWeb("1. Logged in user sees event list") {
      given {
        MembershipSteps().IAmLoggedIn
      }
      .when {
        _.IGoToTheEventsPage
      }
      .then {
        _.ISeeAListOfEvents
      }
    }

    scenarioWeb("2. Non logged in user sees event list") {
      given {
        MembershipSteps().IAmNotLoggedIn
      }
      .when {
        _.IGoToTheEventsPage
      }
      .then {
        _.ISeeAListOfEvents
      }
    }
  }

  feature("See details for an event") {
    /*
     In order to choose an event that I like
     As a user
     I want to see the details of an event
     */
    scenarioWeb("3. Logged in user sees details for an event") {
      given {
        MembershipSteps().IAmLoggedIn
      }
      .when {
        _.IClickOnAnEvent
      }
      .then {
        _.ISeeTheEventDetails
      }
    }

    scenarioWeb("4. Non logged in user sees details for an event") {
      given {
        MembershipSteps().IAmNotLoggedIn
      }
      .when {
        _.IClickOnAnEvent
      }
      .then {
        _.ISeeTheEventDetails
      }
    }

    scenarioWeb("5. Event details are the same as on the event provider") {
      given {
        MembershipSteps().IAmLoggedIn
      }
      .when {
        _.IClickOnAnEvent
      }
      .then {
        _.TheDetailsAreTheSameAsOnTheEventProvider
      }
    }
  }

  feature("Require login for purchase") {
    /*
     In order to purchase a ticket
     As a user
     I need to be logged in
     */
    scenarioWeb("6. Logged in user can purchase a ticket") {
      given {
        MembershipSteps().IAmLoggedIn
      }
      .when {
        _.IClickThePurchaseButton
      }
      .then {
        _.ICanPurchaseATicket
      }
    }

    scenarioWeb("7. Non logged in user has to login in order to purchase a ticket") {
      given {
        MembershipSteps().IAmNotLoggedIn
      }
      .when {
        _.IClickThePurchaseButton
      }
      .then {
        _.IAmRedirectedToTheLoginPage
      }
    }

    scenarioWeb("27. Non-registered user can register and purchase a ticket") {
      given {
        MembershipSteps().IAmNotLoggedIn
      }
      .when {
        _.IClickThePurchaseButton
      }
      .then {
        _.ICanRegisterAndPurchaseASubscription
      }
    }
  }
}