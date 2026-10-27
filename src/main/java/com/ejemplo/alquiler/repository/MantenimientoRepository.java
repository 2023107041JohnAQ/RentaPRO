package com.ejemplo.alquiler.repository;

import com.ejemplo.alquiler.model.Mantenimiento;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Julio Cesar Tutacano de la Cruz
 */
public interface MantenimientoRepository extends JpaRepository<Mantenimiento, Long> {

    void deleteByVehiculoId(Long vehiculoId);
}
