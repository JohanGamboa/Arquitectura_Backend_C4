package com.udla.consulta.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Service
public class ConsultaService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final StringRedisTemplate redisTemplate;

    public ConsultaService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Map<String, Object> validarYConsultarSri(String ruc) {
        String urlExiste = "https://srienlinea.sri.gob.ec/sri-catastro-sujeto-servicio-internet/rest/ConsolidadoContribuyente/existePorNumeroRuc?numeroRuc=" + ruc;
        Boolean existe = restTemplate.getForObject(urlExiste, Boolean.class);

        if (Boolean.FALSE.equals(existe)) {
            throw new RuntimeException("El RUC no existe en el SRI.");
        }

        String urlDatos = "https://srienlinea.sri.gob.ec/sri-catastro-sujeto-servicio-internet/rest/ConsolidadoContribuyente/obtenerPorNumerosRuc?&ruc=" + ruc;
        Object[] contribuyentes = restTemplate.getForObject(urlDatos, Object[].class);

        if (contribuyentes == null || contribuyentes.length == 0) {
            throw new RuntimeException("No se encontraron datos del contribuyente.");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> datos = (Map<String, Object>) contribuyentes[0];
        String tipoContribuyente = String.valueOf(datos.get("tipoContribuyente"));

        if (!tipoContribuyente.toUpperCase().contains("NATURAL")) {
            throw new RuntimeException("El RUC no corresponde a una Persona Natural.");
        }

        return datos;
    }

    public Object consultarVehiculo(String placa) {
        String urlVehiculo = "https://srienlinea.sri.gob.ec/sri-matriculacion-vehicular-recaudacion-servicio-internet/rest/BaseVehiculo/obtenerPorNumeroPlacaOPorNumeroCampvOPorNumeroCpn?numeroPlacaCampvCpn=" + placa;
        return restTemplate.getForObject(urlVehiculo, Object.class);
    }

    public Map<String, String> consultarPuntosAntConCache(String cedula) {
        String cacheKey = "ant:puntos:" + cedula;

        String cachedData = redisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null) {
            return Map.of("cedula", cedula, "puntos", cachedData, "origen", "CACHE_CLOUD");
        }

        try {
            String urlAnt = "https://consultaweb.ant.gob.ec/PortalWEB/paginas/clientes/clp_grid_citaciones.jsp?ps_tipo_identificacion=CED&ps_identificacion=" + cedula + "&ps_placa=";
            Document doc = Jsoup.connect(urlAnt).timeout(5000).get();

            String puntos = doc.select("td.titulo1").text();
            if (puntos.isEmpty()) {
                puntos = "30 (sin infracciones registradas)";
            }

            redisTemplate.opsForValue().set(cacheKey, puntos, Duration.ofHours(24));
            return Map.of("cedula", cedula, "puntos", puntos, "origen", "ANT_LIVE");

        } catch (Exception e) {
            return Map.of("cedula", cedula, "puntos", "Servicio ANT no disponible y sin caché previo", "origen", "FALLBACK_ERROR");
        }
    }
}