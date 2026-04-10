package at.ac.hcw.se.service

import org.tempuri.ICurrencyService_Service
import java.net.URI

object CurrencyService {
    private const val WSDL_URL = "http://localhost:5125/CurrencyService.svc?wsdl"

    fun convert(fromCurrency: String, toCurrency: String, amount: Double, apiKey: String): Double {
        val service = ICurrencyService_Service(URI(WSDL_URL).toURL())
        val port = service.basicHttpBindingICurrencyService
        return port.convert(fromCurrency, toCurrency, amount, apiKey)
    }
}