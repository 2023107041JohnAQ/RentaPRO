package com.ejemplo.alquiler.repository;
import com.ejemplo.alquiler.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * @author Julio Cesar Tutacano de la Cruz
 */
public interface ReservaRepository extends JpaRepository<Reserva, Long> { }
