package astralis.module.impl;

import astralis.module.Module;
import astralis.setting.BooleanSetting;

import pub.astralis.client.module.core.Category;

public class PartySettings extends Module {
    private final BooleanSetting protectPartyMembers;
    private final BooleanSetting sharedWaypoints;
    private final BooleanSetting waypointsThroughBlocks;

    public PartySettings() {

        super(MODULE_ID, "", "", Category.Misc);

        this.protectPartyMembers = new BooleanSetting(PROTECT_PARTY_MEMBERS_SETTING_ID, true);

        this.sharedWaypoints = new BooleanSetting(SHARED_WAYPOINTS_SETTING_ID, true);

        this.waypointsThroughBlocks = new BooleanSetting(WAYPOINTS_THROUGH_BLOCKS_SETTING_ID, true);
        this.setEnabled(true);
        this.addSettings(
                this.protectPartyMembers, this.sharedWaypoints, this.waypointsThroughBlocks);
    }

    public BooleanSetting getSharedWaypoints() {
        return this.sharedWaypoints;
    }

    public BooleanSetting getWaypointsThroughBlocks() {
        return this.waypointsThroughBlocks;
    }

    public BooleanSetting getProtectPartyMembers() {
        return this.protectPartyMembers;
    }

    private static final String MODULE_ID = "f2739acb7ea62686477d24c08e2c8564";
    private static final String PROTECT_PARTY_MEMBERS_SETTING_ID =
            "71b73ec39a87464a645f3fd3031ca6a2e41cfc9ad099a925b31e4070d19f099e";
    private static final String SHARED_WAYPOINTS_SETTING_ID = "b94bf8c99cb0a74dc05fdb4a3b3a6466";
    private static final String WAYPOINTS_THROUGH_BLOCKS_SETTING_ID =
            "66e05f3415f5066030642c4cbf825c8923361ff0e529ba8114cca187f0f26c46";
}
