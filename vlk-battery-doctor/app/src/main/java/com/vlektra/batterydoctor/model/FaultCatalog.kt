package com.vlektra.batterydoctor.model

/**
 * One entry of the DALY BMS fault / status flag table.
 *
 * The names and their order were recovered from the official DALY BMS app
 * (com.daly.bms.app 4.1.0) so that the labels this app shows match exactly
 * what a technician sees in the DALY app and in the Hist.Log export.
 *
 * [index] is the position in the app's own flag list. It is NOT yet confirmed
 * to be the bit position in the live status bitmap — that mapping is filled in
 * once the DALY protocol document or the decoded BLE capture confirms it.
 */
data class FaultFlag(
    val index: Int,
    val key: String,
    val english: String
)

object FaultCatalog {

    /** Every fault and status flag the DALY firmware can raise. */
    val flags: List<FaultFlag> = listOf(
        FaultFlag(0, "uart_charge_mos_off", "Charge MOS Forced OFF via UART Command"),
        FaultFlag(1, "uart_discharge_mos_off", "Discharge MOS Forced OFF via UART Command"),
        FaultFlag(2, "uart_over_voltage", "Cell Overvoltage"),
        FaultFlag(3, "uart_under_voltage", "Cell Undervoltage"),
        FaultFlag(4, "uart_voltage_difference", "Excessive Voltage Difference"),
        FaultFlag(5, "uart_charge_temp_high", "Charging Over-Temperature"),
        FaultFlag(6, "uart_charge_temp_low", "Charging Low-Temperature"),
        FaultFlag(7, "uart_discharge_temp_high", "Discharging Over-Temperature"),
        FaultFlag(8, "uart_discharge_temp_low", "Discharging Low-Temperature"),
        FaultFlag(9, "uart_temp_difference", "Excessive Temperature Difference"),
        FaultFlag(10, "uart_total_voltage_high", "Pack Overvoltage"),
        FaultFlag(11, "uart_total_voltage_low", "Pack Undervoltage"),
        FaultFlag(12, "uart_charge_current_high", "Charging Overcurrent"),
        FaultFlag(13, "uart_discharge_current_high", "Discharging Overcurrent"),
        FaultFlag(14, "uart_soc_low", "SOC Too Low"),
        FaultFlag(15, "uart_soh_low", "SOH Too Low"),
        FaultFlag(16, "uart_mos_temp_high", "MOS Over-Temperature"),
        FaultFlag(17, "uart_env_temp_high", "Ambient Over-Temperature"),
        FaultFlag(18, "uart_fault_2", "Fault Code 2"),
        FaultFlag(19, "uart_fault_3", "Fault Code 3"),
        FaultFlag(20, "uart_fault_4", "Fault Code 4"),
        FaultFlag(21, "uart_fault_5", "Fault Code 5"),
        FaultFlag(22, "uart_fault_6", "Fault Code 6"),
        FaultFlag(23, "uart_charger_connect_success", "Smart Charger Connected Successfully"),
        FaultFlag(24, "uart_charger_connect_fail", "Smart Charger Connection Failed"),
        FaultFlag(25, "uart_discharger_connect_success", "Smart Discharge Cabinet Connected Successfully"),
        FaultFlag(26, "uart_discharger_connect_fail", "Smart Discharge Cabinet Connection Failed"),
        FaultFlag(27, "uart_charge_mos_temp_high", "Charging MOS Overheating"),
        FaultFlag(28, "uart_charge_mos_temp_fault", "Charging MOS Temperature Detection Fault"),
        FaultFlag(29, "uart_discharge_mos_temp_high", "Discharging MOS Overheating"),
        FaultFlag(30, "uart_discharge_mos_temp_fault", "Discharging MOS Temperature Detection Fault"),
        FaultFlag(31, "uart_short_circuit_protect", "Short-Circuit Protection Triggered"),
        FaultFlag(32, "uart_upgrade_flag", "Upgrade Flag"),
        FaultFlag(33, "uart_low_voltage_charge_forbid", "Low Voltage Charge Inhibit Fault"),
        FaultFlag(34, "uart_high_voltage_discharge_forbid", "High Voltage Discharge Inhibit Fault"),
        FaultFlag(35, "uart_intranet_parallel_success", "Internal Parallel Communication Succeeded"),
        FaultFlag(36, "uart_intranet_parallel_fail", "Internal Parallel Communication Failed"),
        FaultFlag(37, "uart_fault_byte7_bit6", "Fault Code: Byte7 Bit6"),
        FaultFlag(38, "uart_fault_byte7_bit7", "Fault Code: Byte7 Bit7"),
        FaultFlag(39, "uart_fault_byte8_bit6", "Fault Code: Byte8 Bit6"),
        FaultFlag(40, "uart_fault_byte8_bit7", "Fault Code: Byte8 Bit7"),
        FaultFlag(41, "uart_fault_byte9_bit6", "Fault Code: Byte9 Bit6"),
        FaultFlag(42, "uart_fault_byte9_bit7", "Fault Code: Byte9 Bit7"),
        FaultFlag(43, "uart_fault_byte10_bit6", "Fault Code: Byte10 Bit6"),
        FaultFlag(44, "uart_fault_byte10_bit7", "Fault Code: Byte10 Bit7"),
        FaultFlag(45, "uart_collect_chip_fault", "Sampling Chip Fault"),
        FaultFlag(46, "uart_collect_chip_comm_fault", "Sampling Chip Communication Fault"),
        FaultFlag(47, "uart_collect_chip_sample_fault", "Sampling Chip Sampling Failure"),
        FaultFlag(48, "uart_voltage_collect_fault", "Voltage Sampling Fault"),
        FaultFlag(49, "uart_voltage_line_offline", "Voltage Sampling Wire Disconnected"),
        FaultFlag(50, "uart_total_voltage_detect_fault", "Total Pack Voltage Detection Fault"),
        FaultFlag(51, "uart_current_detect_fault", "Current Detection Fault"),
        FaultFlag(52, "uart_temp_collect_fault", "Temperature Sampling Fault"),
        FaultFlag(53, "uart_temp_line_offline", "Temperature Sampling Wire Disconnected"),
        FaultFlag(54, "uart_flash_storage_fault", "FLASH Storage Fault"),
        FaultFlag(55, "uart_charge_mos_fault", "Charging MOS Device Fault"),
        FaultFlag(56, "uart_discharge_mos_fault", "Discharging MOS Device Fault"),
        FaultFlag(57, "uart_precharge_mos_fault", "Pre-charge MOS Device Fault"),
        FaultFlag(58, "uart_comm_cut_charge_mos", "Charge MOS Cut Off by UART Command"),
        FaultFlag(59, "uart_comm_cut_discharge_mos", "Discharge MOS Cut Off by UART Command"),
        FaultFlag(60, "uart_switch_cut_charge_mos", "Charge MOS Cut Off by Hardware Switch"),
        FaultFlag(61, "uart_switch_cut_discharge_mos", "Discharge MOS Cut Off by Hardware Switch"),
        FaultFlag(62, "uart_fan_working", "Fan Running"),
        FaultFlag(63, "uart_heat_working", "Heater Running"),
        FaultFlag(64, "uart_current_limit_working", "Current Limiting Module Activated"),
        FaultFlag(65, "uart_heat_on_status", "Heater Enabled Status"),
        FaultFlag(66, "uart_force_start_status", "Forced Start Enabled Status"),
        FaultFlag(67, "uart_force_status", "Forced Activation Status"),
        FaultFlag(68, "uart_full_charge_fault", "Full Charge Triggered Protection Fault"),
        FaultFlag(69, "uart_balance_comm_fault", "Balancing Module Communication Fault"),
        FaultFlag(70, "uart_charger_reverse_fault", "Charger Reverse Polarity Fault"),
        FaultFlag(71, "uart_b_line_offline_fault", "B- Wire Disconnected Fault"),
        FaultFlag(72, "uart_balance_mode_fault", "Balancing Mode Abnormality Fault"),
        FaultFlag(73, "uart_load_reverse_fault", "Load Reverse Polarity Fault"),
        FaultFlag(74, "uart_current_limit_ntc_fault", "Current Limit NTC Sampling Abnormality"),
        FaultFlag(75, "uart_ble_power_fault", "Bluetooth Power Supply Abnormality Fault"),
        FaultFlag(76, "uart_protect_shutdown", "Protection Board Powered Off by Protection"),
        FaultFlag(77, "uart_protect_power_on", "Protection Board Powered On"),    )

    private val byKey: Map<String, FaultFlag> = flags.associateBy { it.key }

    fun nameFor(key: String): String = byKey[key]?.english ?: key

    /**
     * Sort a history-log line into the category the diagnosis engine reasons
     * about. Matching is on the displayed text, so it works equally on an
     * entry read over Bluetooth and on a line pasted from the CSV export.
     */
    fun categorise(name: String): EventCategory {
        val n = name.lowercase()
        return when {
            n.contains("short") && n.contains("circuit") -> EventCategory.SHORT_CIRCUIT

            n.contains("voltage collect") || n.contains("voltage sampling") ||
                n.contains("voltage line") || n.contains("collection disconnected") ||
                n.contains("single_collect") || n.contains("cell sampling") ->
                EventCategory.VOLTAGE_SENSE

            n.contains("temp collect") || n.contains("temperature sampling") ||
                n.contains("temp line") || n.contains("temperature sensor fault") ->
                EventCategory.TEMP_SENSE

            n.contains("deltat") || (n.contains("temp") && n.contains("difference")) ->
                EventCategory.TEMP_DIFFERENCE

            n.contains("charg") && (n.contains("high temp") || n.contains("over-temp") ||
                n.contains("over temp")) -> EventCategory.CHARGE_TEMP_HIGH

            n.contains("charg") && n.contains("low temp") -> EventCategory.CHARGE_TEMP_LOW

            n.contains("overcurrent") || n.contains("over current") -> EventCategory.OVERCURRENT

            n.contains("soc") && n.contains("low") -> EventCategory.SOC_LOW

            n.contains("cellv low") || n.contains("cell undervoltage") ||
                n.contains("over-discharge") -> EventCategory.CELL_UNDERVOLTAGE

            n.contains("cellv high") || n.contains("cell overvoltage") ||
                n.contains("overcharge") -> EventCategory.CELL_OVERVOLTAGE

            n.contains("power on") || n.contains("sleep") || n.contains("power off") ||
                n.contains("shutdown") -> EventCategory.POWER

            else -> EventCategory.OTHER
        }
    }

    /** Pull "3" out of "ErrCellV Low:3 Level", or 0 when the line carries no level. */
    fun levelOf(name: String): Int {
        Regex("""[:\s](\d)\s*Level""", RegexOption.IGNORE_CASE).find(name)?.let {
            return it.groupValues[1].toIntOrNull() ?: 0
        }
        Regex("""Level\s*(\d)""", RegexOption.IGNORE_CASE).find(name)?.let {
            return it.groupValues[1].toIntOrNull() ?: 0
        }
        return 0
    }
}
