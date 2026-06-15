package data.scripts.cosmicon.events;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventCreator;
import data.scripts.CosmiconConfig;
import data.scripts.cosmicon.util.CosmiconRandom;

// Retained for backward compatibility with saves created before the proactive cleanup fix.
// CosmiconModPlugin.onGameLoad() now purges all references to this class from
// BarEventManager (creators list, timeout tracker, barEventCreators map) on every load.
// After one load+save cycle with the fix, saves are clean and these classes can be removed.
public class CosmiconBarEventCreator extends BaseBarEventCreator {

    @Override
    public PortsideBarEvent createBarEvent() {
        if (CosmiconConfig.VERBOSE_ENABLED) {
            boolean inTimeout = BarEventManager.getInstance().getTimeout().contains(this);
            Global.getLogger(this.getClass()).info("Cosmicon bar event CREATE called, inTimeout=" + inTimeout);
        }
        return new CosmiconBarEvent();
    }

    @Override
    public float getBarEventFrequencyWeight() {
        return 5000f;
    }

    @Override
    public float getBarEventActiveDuration() {
        return 30f + CosmiconRandom.nextFloat() * 20f;
    }

    @Override
    public float getBarEventTimeoutDuration() {
        if (CosmiconConfig.VERBOSE_ENABLED) {
            Global.getLogger(this.getClass()).info("Cosmicon getBarEventTimeoutDuration called");
        }
        return 1f;
    }

    @Override
    public float getBarEventAcceptedTimeoutDuration() {
        if (CosmiconConfig.VERBOSE_ENABLED) {
            Global.getLogger(this.getClass()).info("Cosmicon getBarEventAcceptedTimeoutDuration called");
        }
        return 1f;
    }

    @Override
    public String getBarEventId() {
        return "cosmicon_bar_event";
    }
}