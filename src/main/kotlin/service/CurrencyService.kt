package at.ac.hcw.se.service

import at.ac.hcw.se.dto.CurrencyDto
import jakarta.xml.ws.BindingProvider
import org.tempuri.CurrencyService as CurrencyServiceWs
import org.tempuri.ICurrencyService

object CurrencyService {
    private lateinit var apiKey: String
    private lateinit var wsdlServiceUrl: String

    fun init(apiKey: String, wsdlServiceUrl: String = "http://localhost:5125") {
        this.apiKey = apiKey
        this.wsdlServiceUrl = wsdlServiceUrl
    }

    private val port: ICurrencyService by lazy {
        val wsdlUrl = object {}.javaClass.getResource("/wsdl/CurrencyService.wsdl")
        val service = CurrencyServiceWs(wsdlUrl)
        val servicePort = service.basicHttpBindingICurrencyService
        (servicePort as BindingProvider).requestContext[BindingProvider.ENDPOINT_ADDRESS_PROPERTY] =
            "$wsdlServiceUrl/CurrencyService.svc"
        servicePort
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
