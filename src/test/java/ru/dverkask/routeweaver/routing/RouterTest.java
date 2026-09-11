package ru.dverkask.routeweaver.routing;

import org.junit.jupiter.api.Test;
import ru.dverkask.routeweaver.config.ProxyProfile;
import ru.dverkask.routeweaver.config.RouteWeaverConfig;
import ru.dverkask.routeweaver.config.RoutingRule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouterTest {
    private final RouteWeaverConfig config = new RouteWeaverConfig();

    private ProxyProfile profile(String id, boolean enabled) {
        ProxyProfile profile = new ProxyProfile();
        profile.setId(id);
        profile.setEnabled(enabled);
        config.getProfiles().add(profile);
        return profile;
    }

    private RoutingRule rule(String pattern, int port, String profileId) {
        RoutingRule rule = new RoutingRule();
        rule.setId(pattern + ":" + port);
        rule.setServerPattern(pattern);
        rule.setPort(port);
        rule.setProxyProfileId(profileId);
        config.getRules().add(rule);
        return rule;
    }

    @Test
    void goesDirectWithoutAMatchingRule() {
        profile("nl", true);
        rule("hypixel.net", RoutingRule.ANY_PORT, "nl");

        assertSame(RouteDecision.DIRECT, Router.resolve(config, new ServerAddress("mc.example.net", 25565)));
    }

    @Test
    void usesTheFirstMatchingRuleWithAnEnabledProfile() {
        ProxyProfile nl = profile("nl", true);
        profile("de", true);
        RoutingRule first = rule("*.example.net", RoutingRule.ANY_PORT, "nl");
        rule("mc.example.net", RoutingRule.ANY_PORT, "de");

        RouteDecision decision = Router.resolve(config, new ServerAddress("mc.example.net", 25565));

        assertTrue(decision.usesProxy());
        assertSame(first, decision.rule());
        assertSame(nl, decision.profile());
    }

    @Test
    void skipsDisabledRulesAndPortMismatches() {
        profile("nl", true);
        rule("mc.example.net", 25566, "nl");
        rule("mc.example.net", RoutingRule.ANY_PORT, "nl").setEnabled(false);

        assertSame(RouteDecision.DIRECT, Router.resolve(config, new ServerAddress("mc.example.net", 25565)));
    }

    @Test
    void explainsAMatchWhoseProfileIsSwitchedOff() {
        ProxyProfile off = profile("off", false);
        rule("mc.example.net", RoutingRule.ANY_PORT, "off");

        RouteDecision decision = Router.resolve(config, new ServerAddress("mc.example.net", 25565));

        assertEquals(RouteDecision.Kind.PROXY_DISABLED, decision.kind());
        assertSame(off, decision.profile());
        assertFalse(decision.usesProxy());
    }

    @Test
    void fallsThroughADisabledProfileToALaterRule() {
        profile("off", false);
        ProxyProfile on = profile("on", true);
        rule("mc.example.net", RoutingRule.ANY_PORT, "off");
        rule("*.example.net", RoutingRule.ANY_PORT, "on");

        assertSame(on, Router.resolve(config, new ServerAddress("mc.example.net", 25565)).profile());
    }

    @Test
    void findsTheExactRuleEvenWhenDisabled() {
        profile("nl", true);
        rule("*.example.net", RoutingRule.ANY_PORT, "nl");
        RoutingRule exact = rule("MC.example.net", 25565, "nl");
        exact.setEnabled(false);

        assertSame(exact, Router.findExactRule(config, new ServerAddress("mc.example.net", 25565)));
        assertNull(Router.findExactRule(config, new ServerAddress("mc.example.net", 25566)));
    }
}
