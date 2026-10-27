package com.ejemplo.alquiler.repository;

import com.ejemplo.alquiler.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author Jhunior Eric Herrera Condori
 */
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByReservaId(Long reservaId);
}
