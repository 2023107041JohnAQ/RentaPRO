package com.ejemplo.alquiler.repository;
import com.ejemplo.alquiler.model.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * @author Jhunior Eric Herrera Condori
 */
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> { }
