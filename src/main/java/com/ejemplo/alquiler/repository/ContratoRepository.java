package com.ejemplo.alquiler.repository;
import com.ejemplo.alquiler.model.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * @author Jhon Alessandro Arpi Quispe
 */
public interface ContratoRepository extends JpaRepository<Contrato, Long> { }
