package com.vlektra.batterydoctor.diagnosis

/** How bad it is, and therefore who deals with it. */
enum class Severity(val rank: Int) {
    OK(0),
    WATCH(1),
    WORKSHOP(2),
    ENGINEER(3);

    val badge: String
        get() = when (this) {
            OK -> "NO FAULT"
            WATCH -> "MINOR"
            WORKSHOP -> "REPAIR NEEDED"
            ENGINEER -> "ENGINEER ONLY"
        }

    val verdict: String
        get() = when (this) {
            OK -> "Pack is OK"
            WATCH -> "Keep watching"
            WORKSHOP -> "Send to workshop"
            ENGINEER -> "Escalate to engineer"
        }

    val verdictUrdu: String
        get() = when (this) {
            OK -> "Battery theek hai"
            WATCH -> "Nazar rakhein"
            WORKSHOP -> "Workshop bhejein"
            ENGINEER -> "Engineer ko bhejein"
        }

    val subtitle: String
        get() = when (this) {
            OK -> "Nothing in the readings or the log needs work. Hand it back."
            WATCH -> "Usable now. Note the points below and re-check at the next service."
            WORKSHOP -> "Do not hand this pack back yet. The steps below fix it."
            ENGINEER -> "Stop here. This needs an engineer before the pack goes back on a bike."
        }
}

/** One thing the data showed, with what to do about it. */
data class Finding(
    val severity: Severity,
    val title: String,
    /** The numbers that prove it, so the verdict can be checked. */
    val evidence: String,
    /** Why this reading means what it means. */
    val reasoning: String,
    /** What the technician does, in the language of the workshop floor. */
    val actionUrdu: String,
    /** The same step in English, for the report and for the engineer. */
    val actionEnglish: String
)

/** Everything the engine concluded about one pack. */
data class DiagnosisResult(
    val worst: Severity,
    val findings: List<Finding>,
    val headline: String
) {
    val actionable: List<Finding> get() = findings.filter { it.severity != Severity.OK }
}
