package astralis.module.impl.beddefender;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DefenseLayouts {
    private DefenseLayouts() {}

    public static Map<String, List<DefenseBlock>> createLayouts() {
        LinkedHashMap layouts = new LinkedHashMap();

        DefenseBlock[] endWoolCorners = new DefenseBlock[30];

        endWoolCorners[0] = new DefenseBlock("end_stone", 0, 0, 2);

        endWoolCorners[1] = new DefenseBlock("wool", 0, 0, 3);

        endWoolCorners[2] = new DefenseBlock("wool", 0, 0, -2);

        endWoolCorners[3] = new DefenseBlock("end_stone", 0, 0, -1);

        endWoolCorners[4] = new DefenseBlock("wool", 1, 0, -1);

        endWoolCorners[5] = new DefenseBlock("wool", 1, 1, -1);

        endWoolCorners[6] = new DefenseBlock("wool", 0, 1, -1);

        endWoolCorners[7] = new DefenseBlock("wool", -1, 0, -1);

        endWoolCorners[8] = new DefenseBlock("wool", -1, 1, -1);

        endWoolCorners[9] = new DefenseBlock("end_stone", -2, 0, 0);

        endWoolCorners[10] = new DefenseBlock("end_stone", -2, 0, 1);

        endWoolCorners[11] = new DefenseBlock("end_stone", -1, 0, 0);

        endWoolCorners[12] = new DefenseBlock("end_stone", -1, 0, 1);

        endWoolCorners[13] = new DefenseBlock("wool", -1, 1, 0);

        endWoolCorners[14] = new DefenseBlock("wool", -1, 1, 1);

        endWoolCorners[15] = new DefenseBlock("wool", -1, 0, 2);

        endWoolCorners[16] = new DefenseBlock("end_stone", 2, 0, 0);

        endWoolCorners[17] = new DefenseBlock("end_stone", 2, 0, 1);

        endWoolCorners[18] = new DefenseBlock("end_stone", 1, 0, 0);

        endWoolCorners[19] = new DefenseBlock("end_stone", 1, 0, 1);

        endWoolCorners[20] = new DefenseBlock("wool", 1, 1, 0);

        endWoolCorners[21] = new DefenseBlock("wool", 1, 1, 1);

        endWoolCorners[22] = new DefenseBlock("wool", 1, 0, 2);

        endWoolCorners[23] = new DefenseBlock("end_stone", 0, 1, 0);

        endWoolCorners[24] = new DefenseBlock("end_stone", 0, 1, 1);

        endWoolCorners[25] = new DefenseBlock("wool", 0, 2, 0);

        endWoolCorners[26] = new DefenseBlock("wool", 0, 2, 1);

        endWoolCorners[27] = new DefenseBlock("wool", -1, 1, 2);

        endWoolCorners[28] = new DefenseBlock("wool", 1, 1, 2);

        endWoolCorners[29] = new DefenseBlock("wool", 0, 1, 2);
        layouts.put("EndWoolCorners", List.of(endWoolCorners));

        DefenseBlock[] endWoolCorners2 = new DefenseBlock[30];

        endWoolCorners2[0] = new DefenseBlock("end_stone", -2, 0, 0);

        endWoolCorners2[1] = new DefenseBlock("end_stone", -2, 0, 1);

        endWoolCorners2[2] = new DefenseBlock("end_stone", -1, 0, 0);

        endWoolCorners2[3] = new DefenseBlock("end_stone", -1, 0, 1);

        endWoolCorners2[4] = new DefenseBlock("end_stone", 0, 0, 2);

        endWoolCorners2[5] = new DefenseBlock("end_stone", 1, 0, 0);

        endWoolCorners2[6] = new DefenseBlock("end_stone", 1, 0, 1);

        endWoolCorners2[7] = new DefenseBlock("end_stone", 2, 0, 0);

        endWoolCorners2[8] = new DefenseBlock("end_stone", 2, 0, 1);

        endWoolCorners2[9] = new DefenseBlock("end_stone", 0, 1, 1);

        endWoolCorners2[10] = new DefenseBlock("end_stone", 0, 1, 0);

        endWoolCorners2[11] = new DefenseBlock("end_stone", 0, 0, -1);

        endWoolCorners2[12] = new DefenseBlock("wool", 1, 0, -1);

        endWoolCorners2[13] = new DefenseBlock("wool", 0, 1, -1);

        endWoolCorners2[14] = new DefenseBlock("wool", 0, 0, -2);

        endWoolCorners2[15] = new DefenseBlock("wool", -1, 0, -1);

        endWoolCorners2[16] = new DefenseBlock("wool", 1, 1, 1);

        endWoolCorners2[17] = new DefenseBlock("wool", 1, 1, 0);

        endWoolCorners2[18] = new DefenseBlock("wool", 1, 1, -1);

        endWoolCorners2[19] = new DefenseBlock("wool", 0, 2, 1);

        endWoolCorners2[20] = new DefenseBlock("wool", 0, 2, 0);

        endWoolCorners2[21] = new DefenseBlock("wool", -1, 1, 1);

        endWoolCorners2[22] = new DefenseBlock("wool", -1, 1, 0);

        endWoolCorners2[23] = new DefenseBlock("wool", -1, 1, -1);

        endWoolCorners2[24] = new DefenseBlock("wool", -1, 0, 2);

        endWoolCorners2[25] = new DefenseBlock("wool", -1, 1, 2);

        endWoolCorners2[26] = new DefenseBlock("wool", 0, 1, 2);

        endWoolCorners2[27] = new DefenseBlock("wool", 1, 1, 2);

        endWoolCorners2[28] = new DefenseBlock("wool", 1, 0, 2);

        endWoolCorners2[29] = new DefenseBlock("wool", 0, 0, 3);
        layouts.put("EndWoolCorners2", List.of(endWoolCorners2));

        DefenseBlock[] endGlassNoCorners = new DefenseBlock[26];

        endGlassNoCorners[0] = new DefenseBlock("end_stone", 0, 0, 2);

        endGlassNoCorners[1] = new DefenseBlock("stained_glass", 0, 0, 3);

        endGlassNoCorners[2] = new DefenseBlock("stained_glass", -1, 0, -1);

        endGlassNoCorners[3] = new DefenseBlock("stained_glass", 0, 0, -2);

        endGlassNoCorners[4] = new DefenseBlock("stained_glass", 1, 0, -1);

        endGlassNoCorners[5] = new DefenseBlock("end_stone", 0, 0, -1);

        endGlassNoCorners[6] = new DefenseBlock("end_stone", -2, 0, 0);

        endGlassNoCorners[7] = new DefenseBlock("end_stone", -2, 0, 1);

        endGlassNoCorners[8] = new DefenseBlock("end_stone", -1, 0, 0);

        endGlassNoCorners[9] = new DefenseBlock("end_stone", -1, 0, 1);

        endGlassNoCorners[10] = new DefenseBlock("end_stone", 2, 0, 0);

        endGlassNoCorners[11] = new DefenseBlock("end_stone", 2, 0, 1);

        endGlassNoCorners[12] = new DefenseBlock("end_stone", 1, 0, 0);

        endGlassNoCorners[13] = new DefenseBlock("end_stone", 1, 0, 1);

        endGlassNoCorners[14] = new DefenseBlock("stained_glass", -1, 1, 0);

        endGlassNoCorners[15] = new DefenseBlock("stained_glass", -1, 1, 1);

        endGlassNoCorners[16] = new DefenseBlock("stained_glass", -1, 0, 2);

        endGlassNoCorners[17] = new DefenseBlock("stained_glass", 0, 1, -1);

        endGlassNoCorners[18] = new DefenseBlock("stained_glass", 1, 1, 0);

        endGlassNoCorners[19] = new DefenseBlock("stained_glass", 1, 1, 1);

        endGlassNoCorners[20] = new DefenseBlock("stained_glass", 1, 0, 2);

        endGlassNoCorners[21] = new DefenseBlock("end_stone", 0, 1, 0);

        endGlassNoCorners[22] = new DefenseBlock("end_stone", 0, 1, 1);

        endGlassNoCorners[23] = new DefenseBlock("stained_glass", 0, 2, 0);

        endGlassNoCorners[24] = new DefenseBlock("stained_glass", 0, 2, 1);

        endGlassNoCorners[25] = new DefenseBlock("stained_glass", 0, 1, 2);
        layouts.put("EndGlassNoCorners", List.of(endGlassNoCorners));

        DefenseBlock[] woodWoolCorners = new DefenseBlock[30];

        woodWoolCorners[0] = new DefenseBlock("planks", -1, 0, -1);

        woodWoolCorners[1] = new DefenseBlock("planks", -1, 0, 0);

        woodWoolCorners[2] = new DefenseBlock("planks", -1, 0, 1);

        woodWoolCorners[3] = new DefenseBlock("planks", -1, 0, 2);

        woodWoolCorners[4] = new DefenseBlock("planks", 1, 0, -1);

        woodWoolCorners[5] = new DefenseBlock("planks", 1, 0, 0);

        woodWoolCorners[6] = new DefenseBlock("planks", 1, 0, 1);

        woodWoolCorners[7] = new DefenseBlock("planks", 1, 0, 2);

        woodWoolCorners[8] = new DefenseBlock("planks", 0, 0, 2);

        woodWoolCorners[9] = new DefenseBlock("planks", 0, 0, 3);

        woodWoolCorners[10] = new DefenseBlock("planks", 0, 0, -2);

        woodWoolCorners[11] = new DefenseBlock("planks", 0, 0, -1);

        woodWoolCorners[12] = new DefenseBlock("planks", 0, 1, -1);

        woodWoolCorners[13] = new DefenseBlock("planks", 0, 1, 0);

        woodWoolCorners[14] = new DefenseBlock("planks", 0, 1, 1);

        woodWoolCorners[15] = new DefenseBlock("planks", 0, 1, 2);

        woodWoolCorners[16] = new DefenseBlock("wool", -2, 0, 0);

        woodWoolCorners[17] = new DefenseBlock("wool", -2, 0, 1);

        woodWoolCorners[18] = new DefenseBlock("wool", -1, 1, -1);

        woodWoolCorners[19] = new DefenseBlock("wool", -1, 1, 0);

        woodWoolCorners[20] = new DefenseBlock("wool", -1, 1, 1);

        woodWoolCorners[21] = new DefenseBlock("wool", -1, 1, 2);

        woodWoolCorners[22] = new DefenseBlock("wool", 0, 2, 0);

        woodWoolCorners[23] = new DefenseBlock("wool", 0, 2, 1);

        woodWoolCorners[24] = new DefenseBlock("wool", 1, 1, -1);

        woodWoolCorners[25] = new DefenseBlock("wool", 1, 1, 0);

        woodWoolCorners[26] = new DefenseBlock("wool", 1, 1, 1);

        woodWoolCorners[27] = new DefenseBlock("wool", 1, 1, 2);

        woodWoolCorners[28] = new DefenseBlock("wool", 2, 0, 0);

        woodWoolCorners[29] = new DefenseBlock("wool", 2, 0, 1);
        layouts.put("WoodWoolCorners", List.of(woodWoolCorners));

        DefenseBlock[] endGlassWool = new DefenseBlock[58];

        endGlassWool[0] = new DefenseBlock("end_stone", -2, 0, 0);

        endGlassWool[1] = new DefenseBlock("end_stone", -2, 0, 1);

        endGlassWool[2] = new DefenseBlock("end_stone", -1, 0, 0);

        endGlassWool[3] = new DefenseBlock("end_stone", -1, 0, 1);

        endGlassWool[4] = new DefenseBlock("end_stone", 0, 0, 2);

        endGlassWool[5] = new DefenseBlock("end_stone", 1, 0, 0);

        endGlassWool[6] = new DefenseBlock("end_stone", 1, 0, 1);

        endGlassWool[7] = new DefenseBlock("end_stone", 2, 0, 0);

        endGlassWool[8] = new DefenseBlock("end_stone", 2, 0, 1);

        endGlassWool[9] = new DefenseBlock("end_stone", 0, 1, 1);

        endGlassWool[10] = new DefenseBlock("end_stone", 0, 1, 0);

        endGlassWool[11] = new DefenseBlock("end_stone", 0, 0, -1);

        endGlassWool[12] = new DefenseBlock("stained_glass", 1, 0, -1);

        endGlassWool[13] = new DefenseBlock("stained_glass", 0, 1, -1);

        endGlassWool[14] = new DefenseBlock("stained_glass", 0, 0, -2);

        endGlassWool[15] = new DefenseBlock("stained_glass", -1, 0, -1);

        endGlassWool[16] = new DefenseBlock("stained_glass", 1, 1, 1);

        endGlassWool[17] = new DefenseBlock("stained_glass", 1, 1, 0);

        endGlassWool[18] = new DefenseBlock("stained_glass", 0, 2, 1);

        endGlassWool[19] = new DefenseBlock("stained_glass", 0, 2, 0);

        endGlassWool[20] = new DefenseBlock("stained_glass", -1, 1, 1);

        endGlassWool[21] = new DefenseBlock("stained_glass", -1, 1, 0);

        endGlassWool[22] = new DefenseBlock("stained_glass", -1, 0, 2);

        endGlassWool[23] = new DefenseBlock("stained_glass", 0, 1, 2);

        endGlassWool[24] = new DefenseBlock("stained_glass", 1, 0, 2);

        endGlassWool[25] = new DefenseBlock("stained_glass", 0, 0, 3);

        endGlassWool[26] = new DefenseBlock("wool", -3, 0, 0);

        endGlassWool[27] = new DefenseBlock("wool", -3, 0, 1);

        endGlassWool[28] = new DefenseBlock("wool", -2, 0, 2);

        endGlassWool[29] = new DefenseBlock("wool", -2, 1, 0);

        endGlassWool[30] = new DefenseBlock("wool", -2, 1, 1);

        endGlassWool[31] = new DefenseBlock("wool", -1, 1, 2);

        endGlassWool[32] = new DefenseBlock("wool", -1, 0, 3);

        endGlassWool[33] = new DefenseBlock("wool", 0, 1, 3);

        endGlassWool[34] = new DefenseBlock("wool", 0, 0, 4);

        endGlassWool[35] = new DefenseBlock("wool", 1, 0, 3);

        endGlassWool[36] = new DefenseBlock("wool", 1, 1, 2);

        endGlassWool[37] = new DefenseBlock("wool", 2, 1, 0);

        endGlassWool[38] = new DefenseBlock("wool", 2, 1, 1);

        endGlassWool[39] = new DefenseBlock("wool", 2, 0, 2);

        endGlassWool[40] = new DefenseBlock("wool", 3, 0, 0);

        endGlassWool[41] = new DefenseBlock("wool", 3, 0, 1);

        endGlassWool[42] = new DefenseBlock("wool", 1, 2, 0);

        endGlassWool[43] = new DefenseBlock("wool", 1, 2, 1);

        endGlassWool[44] = new DefenseBlock("wool", 0, 2, 2);

        endGlassWool[45] = new DefenseBlock("wool", -1, 2, 0);

        endGlassWool[46] = new DefenseBlock("wool", -1, 2, 1);

        endGlassWool[47] = new DefenseBlock("wool", -2, 0, -1);

        endGlassWool[48] = new DefenseBlock("wool", -1, 0, -2);

        endGlassWool[49] = new DefenseBlock("wool", 0, 0, -3);

        endGlassWool[50] = new DefenseBlock("wool", 1, 0, -2);

        endGlassWool[51] = new DefenseBlock("wool", 2, 0, -1);

        endGlassWool[52] = new DefenseBlock("wool", 1, 1, -1);

        endGlassWool[53] = new DefenseBlock("wool", 0, 1, -2);

        endGlassWool[54] = new DefenseBlock("wool", -1, 1, -1);

        endGlassWool[55] = new DefenseBlock("wool", 0, 2, -1);

        endGlassWool[56] = new DefenseBlock("wool", 0, 3, 0);

        endGlassWool[57] = new DefenseBlock("wool", 0, 3, 1);
        layouts.put("EndGlassWool", List.of(endGlassWool));
        DefenseBlock woolBlock1 = new DefenseBlock("wool", -1, 0, 0);

        DefenseBlock woolBlock2 = new DefenseBlock("wool", -1, 0, 1);
        DefenseBlock woolBlock3 = new DefenseBlock("wool", 0, 0, 2);

        DefenseBlock woolBlock4 = new DefenseBlock("wool", 1, 0, 0);

        DefenseBlock woolBlock5 = new DefenseBlock("wool", 1, 0, 1);

        DefenseBlock woolBlock6 = new DefenseBlock("wool", 0, 1, 1);
        DefenseBlock woolBlock7 = new DefenseBlock("wool", 0, 1, 0);

        layouts.put(
                "Wool1Layer",
                List.of(
                        woolBlock1,
                        woolBlock2,
                        woolBlock3,
                        woolBlock4,
                        woolBlock5,
                        woolBlock6,
                        woolBlock7,
                        new DefenseBlock("wool", 0, 0, -1)));

        DefenseBlock[] endWoodLongCorners = new DefenseBlock[28];

        endWoodLongCorners[0] = new DefenseBlock("end_stone", 0, 0, 2);

        endWoodLongCorners[1] = new DefenseBlock("end_stone", -1, 0, -1);

        endWoodLongCorners[2] = new DefenseBlock("end_stone", -1, 0, 0);

        endWoodLongCorners[3] = new DefenseBlock("end_stone", -1, 0, 1);

        endWoodLongCorners[4] = new DefenseBlock("end_stone", 0, 0, -2);

        endWoodLongCorners[5] = new DefenseBlock("end_stone", 0, 0, -1);

        endWoodLongCorners[6] = new DefenseBlock("end_stone", 1, 0, -1);

        endWoodLongCorners[7] = new DefenseBlock("end_stone", 1, 0, 0);

        endWoodLongCorners[8] = new DefenseBlock("end_stone", 1, 0, 1);

        endWoodLongCorners[9] = new DefenseBlock("end_stone", 0, 1, -1);

        endWoodLongCorners[10] = new DefenseBlock("end_stone", 0, 1, 0);

        endWoodLongCorners[11] = new DefenseBlock("end_stone", 0, 1, 1);

        endWoodLongCorners[12] = new DefenseBlock("planks", -2, 0, 0);

        endWoodLongCorners[13] = new DefenseBlock("planks", -2, 0, 1);

        endWoodLongCorners[14] = new DefenseBlock("planks", -1, 1, -1);

        endWoodLongCorners[15] = new DefenseBlock("planks", -1, 1, 0);

        endWoodLongCorners[16] = new DefenseBlock("planks", -1, 1, 1);

        endWoodLongCorners[17] = new DefenseBlock("planks", 0, 2, 0);

        endWoodLongCorners[18] = new DefenseBlock("planks", 0, 2, 1);

        endWoodLongCorners[19] = new DefenseBlock("planks", 1, 1, -1);

        endWoodLongCorners[20] = new DefenseBlock("planks", 1, 1, 0);

        endWoodLongCorners[21] = new DefenseBlock("planks", 1, 1, 1);

        endWoodLongCorners[22] = new DefenseBlock("planks", 2, 0, 0);

        endWoodLongCorners[23] = new DefenseBlock("planks", 2, 0, 1);

        endWoodLongCorners[24] = new DefenseBlock("planks", 1, 0, 2);

        endWoodLongCorners[25] = new DefenseBlock("planks", 0, 1, 2);

        endWoodLongCorners[26] = new DefenseBlock("planks", 0, 0, 3);

        endWoodLongCorners[27] = new DefenseBlock("planks", -1, 0, 2);
        layouts.put("EndWoodLongCorners", List.of(endWoodLongCorners));

        DefenseBlock[] endWoolLong = new DefenseBlock[35];

        endWoolLong[0] = new DefenseBlock("end_stone", 0, 0, 2);

        endWoolLong[1] = new DefenseBlock("end_stone", -1, 0, -1);

        endWoolLong[2] = new DefenseBlock("end_stone", -1, 0, 0);

        endWoolLong[3] = new DefenseBlock("end_stone", -1, 0, 1);

        endWoolLong[4] = new DefenseBlock("end_stone", 0, 0, -2);

        endWoolLong[5] = new DefenseBlock("end_stone", 0, 0, -1);

        endWoolLong[6] = new DefenseBlock("end_stone", 1, 0, -1);

        endWoolLong[7] = new DefenseBlock("end_stone", 1, 0, 0);

        endWoolLong[8] = new DefenseBlock("end_stone", 1, 0, 1);

        endWoolLong[9] = new DefenseBlock("end_stone", 0, 1, -1);

        endWoolLong[10] = new DefenseBlock("end_stone", 0, 1, 0);

        endWoolLong[11] = new DefenseBlock("end_stone", 0, 1, 1);

        endWoolLong[12] = new DefenseBlock("wool", -2, 0, -1);

        endWoolLong[13] = new DefenseBlock("wool", -2, 0, 0);

        endWoolLong[14] = new DefenseBlock("wool", -2, 0, 1);

        endWoolLong[15] = new DefenseBlock("wool", -1, 1, -1);

        endWoolLong[16] = new DefenseBlock("wool", -1, 1, 0);

        endWoolLong[17] = new DefenseBlock("wool", -1, 1, 1);

        endWoolLong[18] = new DefenseBlock("wool", 0, 2, -1);

        endWoolLong[19] = new DefenseBlock("wool", 0, 2, 0);

        endWoolLong[20] = new DefenseBlock("wool", 0, 2, 1);

        endWoolLong[21] = new DefenseBlock("wool", 1, 1, -1);

        endWoolLong[22] = new DefenseBlock("wool", 1, 1, 0);

        endWoolLong[23] = new DefenseBlock("wool", 1, 1, 1);

        endWoolLong[24] = new DefenseBlock("wool", 2, 0, -1);

        endWoolLong[25] = new DefenseBlock("wool", 2, 0, 0);

        endWoolLong[26] = new DefenseBlock("wool", 2, 0, 1);

        endWoolLong[27] = new DefenseBlock("wool", -1, 0, 2);

        endWoolLong[28] = new DefenseBlock("wool", 0, 1, 2);

        endWoolLong[29] = new DefenseBlock("wool", 0, 0, 3);

        endWoolLong[30] = new DefenseBlock("wool", 1, 0, 2);

        endWoolLong[31] = new DefenseBlock("wool", 1, 0, -2);

        endWoolLong[32] = new DefenseBlock("wool", 0, 1, -2);

        endWoolLong[33] = new DefenseBlock("wool", 0, 0, -3);

        endWoolLong[34] = new DefenseBlock("wool", -1, 0, -2);
        layouts.put("EndWoolLong", List.of(endWoolLong));
        return layouts;
    }
}
