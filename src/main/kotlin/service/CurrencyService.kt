package at.ac.hcw.se

import org.tempuri.CurrencyService as CurrencyServiceWs
import java.net.URI

object CurrencyService {
    private const val WSDL_URL = "http://localhost:5125/CurrencyService.svc?wsdl"

    private val port by lazy {
        CurrencyServiceWs(URI(WSDL_URL).toURL()).basicHttpBindingICurrencyService
    }

    fun convert(fromCurrency: String, toCurrency: String, amount: Double, apiKey: String): Double {
        return port.convert(fromCurrency, toCurrency, amount, apiKey)
    }

    fun getSupportedCurrencies(): List<String> {
        return port.supportedCurrencies.string
    }
}
