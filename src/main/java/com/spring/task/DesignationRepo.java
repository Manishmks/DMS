package com.spring.task;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DesignationRepo extends JpaRepository<Designation,Integer>
{
    public Designation findByDesgName(String desName);
}
