package com.ejemplo.alquiler.repository;
import com.ejemplo.alquiler.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * @author Jhunior Eric Herrera Condori
 */
public interface ClienteRepository extends JpaRepository<Cliente, Long> { }
