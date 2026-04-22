package at.ac.hcw.se.service

import at.ac.hcw.se.dto.CurrencyDto
import org.tempuri.CurrencyService as CurrencyServiceWs

object CurrencyService {
    private lateinit var apiKey: String

    fun init(apiKey: String) {
        this.apiKey = apiKey
    }

    private val port by lazy {
        val wsdlUrl = object {}.javaClass.getResource("/wsdl/CurrencyService.wsdl")
        CurrencyServiceWs(wsdlUrl).basicHttpBindingICurrencyService
    }

    fun convertFromUSD(amount: Double, toCurrency: String): CurrencyDto
    {
        val fromCurrency = "USD"
        val toCurrency = toCurrency.uppercase().trim()

        if(toCurrency == fromCurrency) {
            return CurrencyDto(amount = amount, currencyCode = toCurrency)
        }

        if(!getSupportedCurrencies().containsAll(listOf(fromCurrency, toCurrency))) {
            throw IllegalArgumentException("Unsupported currency code! Supported currencies: ${getSupportedCurrencies()}")
        }

        val convertedAmount = port.convert(fromCurrency, toCurrency, amount, apiKey)
        return CurrencyDto(amount = convertedAmount, currencyCode = toCurrency)
    }

    fun getSupportedCurrencies(): List<String> {
        return port.supportedCurrencies.string
    }
}
