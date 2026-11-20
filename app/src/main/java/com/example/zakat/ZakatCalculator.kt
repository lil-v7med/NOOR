package com.example.zakat

data class ZakatInput(
    val cashAndSavings: Double = 0.0,
    val gold24kGrams: Double = 0.0,
    val gold21kGrams: Double = 0.0,
    val gold18kGrams: Double = 0.0,
    val silverGrams: Double = 0.0,
    val investmentsValue: Double = 0.0,
    val immediateDebts: Double = 0.0,
    val gold24kPricePerGram: Double = 3500.0,
    val silverPricePerGram: Double = 45.0,
    val nisabBasisIsGold: Boolean = true
)

data class ZakatResult(
    val totalWealth: Double,
    val netZakatableWealth: Double,
    val nisabThreshold: Double,
    val isZakatDue: Boolean,
    val zakatAmountDue: Double
)

object ZakatCalculator {
    const val GOLD_NISAB_GRAMS = 85.0
    const val SILVER_NISAB_GRAMS = 595.0
    const val ZAKAT_RATE = 0.025

    fun calculate(input: ZakatInput): ZakatResult {
        val gold24EquivalentGrams = input.gold24kGrams +
            (input.gold21kGrams * 21.0 / 24.0) +
            (input.gold18kGrams * 18.0 / 24.0)

        val goldValue = gold24EquivalentGrams * input.gold24kPricePerGram.coerceAtLeast(0.0)
        val silverValue = input.silverGrams.coerceAtLeast(0.0) * input.silverPricePerGram.coerceAtLeast(0.0)

        val grossWealth = (
            input.cashAndSavings.coerceAtLeast(0.0) +
                goldValue +
                silverValue +
                input.investmentsValue.coerceAtLeast(0.0)
            )
        val netWealth = (grossWealth - input.immediateDebts.coerceAtLeast(0.0)).coerceAtLeast(0.0)

        val nisab = if (input.nisabBasisIsGold) {
            GOLD_NISAB_GRAMS * input.gold24kPricePerGram.coerceAtLeast(0.0)
        } else {
            SILVER_NISAB_GRAMS * input.silverPricePerGram.coerceAtLeast(0.0)
        }

        val isDue = nisab > 0.0 && netWealth >= nisab
        val zakatDue = if (isDue) netWealth * ZAKAT_RATE else 0.0

        return ZakatResult(
            totalWealth = grossWealth,
            netZakatableWealth = netWealth,
            nisabThreshold = nisab,
            isZakatDue = isDue,
            zakatAmountDue = zakatDue
        )
    }
}
