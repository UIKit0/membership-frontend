

/**
 * Created by jao on 06/06/2014.
 */
class MembershipBenefitTests extends BaseMembershipTest {

  info("Tests for Membership benefits")

  feature("User gets benefits from being a member") {

    scenarioWeb("25. Member gets a discount") {
      given {
        MembershipSteps().IAmNotLoggedIn
      }
      .when {
        _.IClickOnAnEvent
      }
      .then {
        _.PriceIsHigherThanIfIAmAMember
      }
    }

    scenarioWeb("26. Discount gets compared to non-discounted price") {
      given {
        MembershipSteps().IAmLoggedIn
      }
      .when {
        _.IClickOnAnEvent
      }
      .then {
        _.OriginalPriceIsComparedToDiscountedPrice
      }
    }
  }

  feature("Membership tab") {
    scenarioWeb("28. Membership tab appears if you are a member") {
      given {
        MembershipSteps().IAmLoggedIn
      }
      .when {
        _.IBecomeAPartner
      }
      .then {
        _.ICanSeeTheMembershipTab
      }
    }

    scenarioWeb("29. Membership tab does not appear if you are not a member") {
      given {
        MembershipSteps().IAmNotLoggedIn
      }
      .when {
        _.IGoToIdentity
      }
      .then {
        _.IDontSeeTheMembershipTab
      }
    }
  }
}