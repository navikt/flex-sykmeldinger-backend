package no.nav.helse.flex.client.ereg

data class Nokkelinfo(
    val navn: Navn,
)

data class Navn(
    val navnelinje1: String?,
    val navnelinje2: String? = null,
    val navnelinje3: String? = null,
    val navnelinje4: String? = null,
    val navnelinje5: String? = null,
    val redigertnavn: String? = null,
) {
    val navn: String
        get() {
            val builder = StringBuilder()
            if (!navnelinje1.isNullOrBlank()) {
                builder.appendLine(navnelinje1)
            }
            if (!navnelinje2.isNullOrBlank()) {
                builder.appendLine(navnelinje2)
            }
            if (!navnelinje3.isNullOrBlank()) {
                builder.appendLine(navnelinje3)
            }
            if (!navnelinje4.isNullOrBlank()) {
                builder.appendLine(navnelinje4)
            }
            if (!navnelinje5.isNullOrBlank()) {
                builder.appendLine(navnelinje5)
            }
            return builder.lineSequence().filter { it.isNotBlank() }.joinToString(separator = ",")
        }
}
