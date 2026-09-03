package com.nexus.nexusorchestrator.diagnostic;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

/**
 * Qayda-əsaslı (heuristik) diaqnostika: xəta tipinə görə hədəf servis üçün
 * insanın anlayacağı formada izah üretir. Xarici AI çağırışı yoxdur.
 */
@Service
public class FailureDiagnosticService {

    public String diagnose(Exception e) {
        if (e instanceof CallNotPermittedException) {
            return "Circuit Breaker açıqdır — hədəf servis son müddətdə dəfələrlə uğursuz " +
                    "olduğu üçün sistem müvəqqəti olaraq ona sorğu göndərməyi dayandırıb. " +
                    "Bir müddət sonra avtomatik yenidən sınayacaq.";
        }

        Throwable rootCause = rootCause(e);

        if (rootCause instanceof ConnectException) {
            return "Hədəf server əlçatan deyil (connection refused) — servis söndürülüb " +
                    "və ya port bağlıdır. Hədəf ünvanın işlək olduğunu yoxlayın.";
        }

        if (rootCause instanceof SocketTimeoutException) {
            return "Hədəf server müəyyən vaxt ərzində cavab vermədi (timeout) — server " +
                    "yüklənmiş ola bilər və ya şəbəkə problemi var.";
        }

        if (e instanceof HttpServerErrorException httpServerError) {
            return "Hədəf serverdə daxili xəta baş verdi (HTTP " + httpServerError.getStatusCode().value() +
                    ") — problem bizim tərəfdə deyil, hədəf servisdədir.";
        }

        if (e instanceof HttpClientErrorException httpClientError) {
            return "Hədəf server sorğunu qəbul etmədi (HTTP " + httpClientError.getStatusCode().value() +
                    ") — göndərilən payload formatını yoxlayın.";
        }

        if (e instanceof ResourceAccessException) {
            return "Hədəf servisə şəbəkə səviyyəsində qoşulmaq mümkün olmadı — ünvan, port " +
                    "və ya şəbəkə konfiqurasiyasını yoxlayın.";
        }

        String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        return "Naməlum xəta baş verdi: " + detail;
    }

    private Throwable rootCause(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }
}
