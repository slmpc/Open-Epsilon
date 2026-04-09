package com.github.epsilon.events;

import net.neoforged.bus.api.Event;

public class EpsilonRenderGuiEvent extends Event {

    public static class BeforeInGameGui extends EpsilonRenderGuiEvent {
    }

    public static class AfterInGameGui extends EpsilonRenderGuiEvent {
    }

    public static class BeforeGui extends EpsilonRenderGuiEvent {
    }

    public static class AfterGui extends EpsilonRenderGuiEvent {
    }

}
