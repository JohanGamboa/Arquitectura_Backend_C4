package com.udla.consulta.controller;

import com.udla.consulta.service.ConsultaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/consulta")
@CrossOrigin(origins = "*")
public class ConsultaController {

    private final ConsultaService consultaService;

    public ConsultaController(ConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    @GetMapping("/contribuyente/{ruc}")
    public ResponseEntity<?> getContribuyente(@PathVariable String ruc) {
        try {
            return ResponseEntity.ok(consultaService.validarYConsultarSri(ruc));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/vehiculo/{placa}")
    public ResponseEntity<?> getVehiculo(@PathVariable String placa) {
        return ResponseEntity.ok(consultaService.consultarVehiculo(placa));
    }

    @GetMapping("/licencia/{cedula}")
    public ResponseEntity<?> getPuntosLicencia(@PathVariable String cedula) {
        return ResponseEntity.ok(consultaService.consultarPuntosAntConCache(cedula));
    }
}