package at.ac.hcw.se.service

import org.tempuri.CurrencyService as CurrencyServiceWs

object CurrencyService {
    private val port by lazy {
        val wsdlUrl = object {}.javaClass.getResource("/wsdl/CurrencyService.wsdl")
        CurrencyServiceWs(wsdlUrl).basicHttpBindingICurrencyService
    }

    fun convert(fromCurrency: String, toCurrency: String, amount: Double, apiKey: String): Double {
        return port.convert(fromCurrency, toCurrency, amount, apiKey)
    }

    fun getSupportedCurrencies(): List<String> {
        return port.supportedCurrencies.string
    }
}
