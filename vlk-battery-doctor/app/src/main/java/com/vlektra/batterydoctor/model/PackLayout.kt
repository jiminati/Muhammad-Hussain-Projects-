package com.vlektra.batterydoctor.model

/**
 * Where each cell group physically sits inside the pack.
 *
 * This is not decoration. On the 36 Ah 24S3P packs the series path runs down
 * one half of the upper module, crosses to the lower module, runs to the far
 * side, then climbs back up. The two crossings — group 6 to 7 and group 18 to
 * 19 — are busbar bridges between two separate modules. They carry the whole
 * pack current across a physical gap and take the vibration of the bike, which
 * is exactly why Batch 6 packs break at cells 7 and 18 and nowhere else.
 *
 * Drawing the real geometry lets a technician see that a fault is at a joint
 * rather than in a cell, without being told.
 */

/** One cell group's slot in the physical grid. */
data class CellSlot(
    val group: Int,   // 1-based series position
    val col: Int,     // 0-based
    val row: Int      // 0-based
)

/** A busbar that links two modules. */
data class Bridge(
    val fromGroup: Int,
    val toGroup: Int,
    /** Columns the bridge spans. */
    val cols: IntRange,
    /** The gap it crosses sits under this row. */
    val belowRow: Int,
    /** true when current flows downward on screen. */
    val downward: Boolean
)

data class PackLayout(
    val key: String,
    /** Shown to the technician, in plain words. */
    val title: String,
    val subtitle: String,
    val groups: Int,
    val parallelPerGroup: Int,
    val columns: Int,
    val rows: Int,
    /** Row ranges that form one physical module, top to bottom. */
    val modules: List<IntRange>,
    val moduleNames: List<String>,
    val slots: List<CellSlot>,
    val bridges: List<Bridge>
) {
    /** Every grid cell belonging to one group — 3 of them on a 3P pack. */
    fun slotsFor(group: Int): List<CellSlot> = slots.filter { it.group == group }

    fun groupAt(col: Int, row: Int): Int? =
        slots.firstOrNull { it.col == col && it.row == row }?.group

    /** Groups that sit at the end of a module bridge, so a break shows here first. */
    val bridgeGroups: Set<Int> =
        bridges.flatMap { listOf(it.fromGroup, it.toGroup) }.toSet()
}

object PackLayouts {

    /**
     * 36 Ah, 24S3P, two modules of six groups per side.
     *
     *   cols 0-2        cols 3-5
     *   ┌──────────┬──────────┐
     *   │  1 .. 6  │ 24 .. 19 │  upper module
     *   ├──────────┼──────────┤
     *   │  7 .. 12 │ 18 .. 13 │  lower module
     *   └──────────┴──────────┘
     *
     * Series path: 1 down to 6, bridge down to 7, down to 12, across to 13,
     * up to 18, bridge up to 19, up to 24.
     */
    val LFP_36AH_24S3P: PackLayout = run {
        val slots = mutableListOf<CellSlot>()

        // Left half, upper module: groups 1..6 going down
        for (col in 0..2) for (row in 0..5) {
            slots += CellSlot(group = row + 1, col = col, row = row)
        }
        // Left half, lower module: groups 7..12 going down
        for (col in 0..2) for (row in 6..11) {
            slots += CellSlot(group = row + 1, col = col, row = row)
        }
        // Right half, lower module: 18 at the top down to 13 at the bottom
        for (col in 3..5) for (row in 6..11) {
            slots += CellSlot(group = 18 - (row - 6), col = col, row = row)
        }
        // Right half, upper module: 24 at the top down to 19 at the bottom
        for (col in 3..5) for (row in 0..5) {
            slots += CellSlot(group = 24 - row, col = col, row = row)
        }

        PackLayout(
            key = "lfp_36ah_24s3p",
            title = "36 Ah",
            subtitle = "24 groups · 3 cells each · two modules",
            groups = 24,
            parallelPerGroup = 3,
            columns = 6,
            rows = 12,
            modules = listOf(0..5, 6..11),
            moduleNames = listOf("Upper module", "Lower module"),
            slots = slots,
            bridges = listOf(
                Bridge(6, 7, 0..2, belowRow = 5, downward = true),
                Bridge(18, 19, 3..5, belowRow = 5, downward = false)
            )
        )
    }

    /**
     * Atom 30 Ah, 24S2P. One module, laid out four across so the blocks stay
     * big enough to read and tap on a phone.
     */
    val LFP_30AH_24S2P: PackLayout = run {
        val slots = (1..24).map { g ->
            CellSlot(group = g, col = (g - 1) % 4, row = (g - 1) / 4)
        }
        PackLayout(
            key = "lfp_30ah_24s2p",
            title = "30 Ah",
            subtitle = "24 groups · 2 cells each · single module",
            groups = 24,
            parallelPerGroup = 2,
            columns = 4,
            rows = 6,
            modules = listOf(0..5),
            moduleNames = listOf("Pack"),
            slots = slots,
            bridges = emptyList()
        )
    }

    /** Batch 5 and earlier. Layout not recorded, so show a plain 24. */
    val LEGACY_24S: PackLayout = LFP_30AH_24S2P.copy(
        key = "legacy_24s",
        title = "24 cells",
        subtitle = "24 groups · earlier build",
        parallelPerGroup = 1
    )

    /**
     * Pick the layout from what the pack reports about itself.
     *
     * @param batteryCode the BMS battery code, e.g. R24TK1A-24S100A
     * @param batchHint   "Batch 6", "Batch 7", "Atom" and so on
     * @param ratedAh     rated capacity
     */
    fun forPack(batteryCode: String, batchHint: String?, ratedAh: Double): PackLayout {
        val batch = batchHint.orEmpty().lowercase()
        val code = batteryCode.lowercase()

        val isAtom = batch.contains("atom") || code.startsWith("apvlk")
        if (isAtom) return LFP_30AH_24S2P

        val batchNumber = Regex("""batch\s*(\d+)""").find(batch)
            ?.groupValues?.get(1)?.toIntOrNull()

        return when {
            batchNumber != null && batchNumber >= 6 -> LFP_36AH_24S3P
            batchNumber != null && batchNumber <= 5 -> LEGACY_24S
            ratedAh >= 34.0 -> LFP_36AH_24S3P      // 36 Ah packs are the 3P build
            ratedAh in 28.0..33.9 -> LFP_30AH_24S2P
            else -> LEGACY_24S
        }
    }
}
