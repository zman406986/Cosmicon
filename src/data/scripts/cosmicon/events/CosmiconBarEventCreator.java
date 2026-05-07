package data.scripts.cosmicon.events;

import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventCreator;
import data.scripts.cosmicon.util.CosmiconRandom;

public class CosmiconBarEventCreator extends BaseBarEventCreator {

    @Override
    public PortsideBarEvent createBarEvent() {
        return new CosmiconBarEvent();
    }

    @Override
    public float getBarEventFrequencyWeight() {
        return 1000f;
    }

    @Override
    public float getBarEventActiveDuration() {
        return 30f + CosmiconRandom.nextFloat() * 20f;
    }

    @Override
    public float getBarEventTimeoutDuration() {
        return 10f + CosmiconRandom.nextFloat() * 5f;
    }

    @Override
    public float getBarEventAcceptedTimeoutDuration() {
        return 7f + CosmiconRandom.nextFloat() * 3f;
    }

    @Override
    public String getBarEventId() {
        return "cosmicon_bar_event";
    }
}
